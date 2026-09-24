"""Check every mixin injection point against the bytecode it targets.

Mixin `method` and `@At` targets are annotation strings, so javac never validates them: a descriptor that
drifted between Minecraft versions compiles cleanly and only fails when the game applies the mixin. This walks
the preprocessed sources for a Stonecutter target, resolves each @Mixin class, and asks javap whether the
methods named actually exist with the descriptors given, following supertypes so NeoForge's interface
extensions count.

Run it from the repository root, after building the targets to check:

	./gradlew build
	python tools/check-mixin-targets.py            # every target that has been built
	python tools/check-mixin-targets.py 1.21.11    # just the ones named
	python tools/check-mixin-targets.py 26.1 --neoforge 26.1.0.1-beta

The last form checks against another NeoForge build the target accepts. Its game jar only exists once Gradle
has set that build up, which writing out its compile classpath does:

	./gradlew :26.1:writeCompileClasspath -Pneo_version=26.1.0.1-beta -Pminecraft_version=26.1

It exits non-zero if any target has a problem.
"""

import glob, io, os, re, subprocess, sys


def strip_comments(src):
	out, i, n = [], 0, len(src)
	while i < n:
		if src.startswith("//", i):
			j = src.find("\n", i)
			i = n if j < 0 else j
		elif src.startswith("/*", i):
			j = src.find("*/", i + 2)
			seg = src[i:(n if j < 0 else j + 2)]
			out.append("\n" * seg.count("\n"))
			i = n if j < 0 else j + 2
		elif src[i] == '"':
			j = i + 1
			while j < n and src[j] != '"':
				if src[j] == "\\":
					j += 1
				j += 1
			out.append(src[i:j + 1])
			i = j + 1
		else:
			out.append(src[i])
			i += 1
	return "".join(out)


def members(javap_out):
	"""[(name, descriptor)] for every method javap printed."""
	result = []
	pending = None
	for line in javap_out.splitlines():
		stripped = line.strip()
		m = re.match(r'descriptor: (\S+)$', stripped)
		if m:
			if pending is not None:
				result.append((pending, m.group(1)))
				pending = None
			continue
		m = re.search(r'([A-Za-z_$][A-Za-z0-9_$.]*)\s*\(', stripped)
		if m and stripped.endswith(";"):
			pending = m.group(1).split(".")[-1]
		else:
			pending = None
	return result


class Classpath:
	def __init__(self, jars):
		self.jars = os.pathsep.join(jars)
		self.cache = {}

	def dump(self, cls):
		"""javap output for a class, or None."""
		if cls not in self.cache:
			try:
				self.cache[cls] = subprocess.run(["javap", "-p", "-s", "-cp", self.jars, cls],
												 capture_output=True, text=True, check=True).stdout
			except subprocess.CalledProcessError:
				self.cache[cls] = None
		return self.cache[cls]

	def supertypes(self, javap_out):
		header = next((l for l in javap_out.splitlines() if re.search(r'\b(class|interface) ', l)), "")
		header = header.split("{")[0]
		# Generics go first, innermost out: a class's own type parameters can say extends too, as in
		# "class EntityRenderer<T extends Entity, ...>", and would otherwise be read as its superclass.
		while True:
			stripped = re.sub(r'<[^<>]*>', "", header)
			if stripped == header:
				break
			header = stripped
		names = []
		for kw in ("extends", "implements"):
			m = re.search(r'\b%s\b(.*?)(?:\bimplements\b|$)' % kw, header)
			if m:
				names += [n.strip() for n in m.group(1).split(",")]
		return [n for n in names if n and n[0].islower()]

	def has_member(self, cls, mname, mdesc, seen=None):
		"""Whether cls or any supertype declares mname with mdesc; None if cls is not on the classpath."""
		seen = seen if seen is not None else set()
		if cls in seen:
			return False
		seen.add(cls)
		out = self.dump(cls)
		if out is None:
			return None
		if (mname, mdesc) in members(out):
			return True
		return any(self.has_member(sup, mname, mdesc, seen) is True for sup in self.supertypes(out))


ACTIVE = re.search(r'stonecutter active "([^"]+)"', io.open("stonecutter.gradle.kts", encoding="utf-8").read()).group(1)


def neo_version(version):
	props = io.open("versions/%s/gradle.properties" % version, encoding="utf-8").read()
	return re.search(r'^neo_version=(\S+)', props, re.M).group(1)


def check_version(version, neo):
	# The artifacts folder keeps the game jar of every NeoForge build the target has been set up against, so
	# the one wanted is picked by name rather than taking the folder wholesale.
	jars = glob.glob("versions/%s/build/moddev/artifacts/*-%s.jar" % (version, neo))
	if not jars:
		return None
	# From 26.1 NeoForge's own classes are no longer patched into the game jar, so its universal jar joins the
	# classpath. Earlier builds have none to add.
	cache = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1",
						 "net.neoforged", "neoforge", neo)
	jars += glob.glob(os.path.join(cache, "*", "neoforge-%s-universal.jar" % neo))
	cp = Classpath(jars)

	# The active version compiles straight from src/main/java, so anything in its generated folder is left over from
	# a time when another version was active, and stale.
	root = "versions/%s/build/generated/stonecutter/main/java/me/pepperbell/continuity/client/mixin" % version
	if version == ACTIVE or not os.path.isdir(root):
		root = "src/main/java/me/pepperbell/continuity/client/mixin"

	# The generated mixin config is the authority on which mixins this target actually applies; the source
	# tree still holds the ones excluded from compilation for this version's band.
	active = None
	for config in ("versions/%s/build/generated/sources/modMetadata/continuity.mixins.json" % version,
				   "versions/%s/build/resources/main/continuity.mixins.json" % version):
		if os.path.isfile(config):
			active = set(re.findall(r'"([A-Za-z0-9_$]+)"', io.open(config, encoding="utf-8").read()))
			break

	problems, notes, checked = [], [], 0
	for name in sorted(os.listdir(root)):
		if not name.endswith(".java"):
			continue
		if active is not None and name[:-5] not in active:
			continue
		src = strip_comments(io.open(os.path.join(root, name), encoding="utf-8").read())
		m = re.search(r'@Mixin\((?:value = )?([A-Za-z0-9_.]+)\.class', src)
		if not m:
			continue
		simple = m.group(1)
		head = simple.split(".")[0]
		imp = re.search(r'import ([a-z0-9_.]+\.%s);' % re.escape(head), src)
		if not imp:
			problems.append("%s: cannot resolve @Mixin target %s" % (name, simple))
			continue
		target = imp.group(1)
		if "." in simple:
			target += "$" + simple.split(".", 1)[1].replace(".", "$")
		out = cp.dump(target)
		if out is None:
			problems.append("%s: javap failed for %s" % (name, target))
			continue
		names = {n for n, _ in members(out)}
		for desc in re.findall(r'method = "([^"]+)"', src):
			checked += 1
			mname = desc.split("(")[0]
			if "(" not in desc:
				if mname not in names:
					problems.append("%s: %s has no method named %s" % (name, target, mname))
				continue
			wanted = desc[len(mname):]
			if mname == "<init>":
				mname = target.split(".")[-1].split("$")[-1]
			if cp.has_member(target, mname, wanted) is not True:
				problems.append("%s: %s%s NOT FOUND on %s" % (name, mname, wanted, target))

		# @At INVOKE targets are descriptors too, and just as unchecked by javac.
		for owner, mname, mdesc in re.findall(r'target = "L([^;]+);([A-Za-z_$<][A-Za-z0-9_$>]*)(\([^"]*)"', src):
			checked += 1
			owner = owner.replace("/", ".")
			found = cp.has_member(owner, mname, mdesc)
			if found is None:
				notes.append("%s: %s not on the classpath, skipped" % (name, owner))
			elif not found:
				problems.append("%s: @At %s.%s%s NOT FOUND" % (name, owner, mname, mdesc))

	print("%s on NeoForge %s: checked %d targets, %d problem(s)" % (version, neo, checked, len(problems)))
	for n in notes:
		print("  ~ " + n)
	for p in problems:
		print("  ! " + p)
	return len(problems)


args = sys.argv[1:]
against = None
if "--neoforge" in args:
	i = args.index("--neoforge")
	against = args[i + 1]
	del args[i:i + 2]
versions = args or sorted(os.path.basename(p) for p in glob.glob("versions/*") if os.path.isdir(p))
failed, ran = 0, 0
for v in versions:
	neo = against or neo_version(v)
	result = check_version(v, neo)
	if result is None:
		print("%s on NeoForge %s: not set up, skipped" % (v, neo))
		continue
	ran += 1
	failed += result
if ran == 0:
	sys.exit("Nothing to check. Build the targets first.")
sys.exit(1 if failed else 0)
