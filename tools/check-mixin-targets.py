"""Check every mixin injection point against the bytecode it targets.

Mixin `method` and `@At` targets are annotation strings, so javac never validates them: a descriptor that
drifted between Minecraft versions compiles cleanly and only fails when the game applies the mixin. This walks
the preprocessed sources for one Stonecutter target, resolves each @Mixin class, and asks javap whether the
methods named actually exist with the descriptors given, following supertypes so NeoForge's interface
extensions count.

Run it from the repository root, after a build of that target:

    python tools/check-mixin-targets.py 1.21.11
"""

import io, os, re, subprocess, sys, glob

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

version = sys.argv[1]
jar = os.pathsep.join(p for p in glob.glob("versions/%s/build/moddev/artifacts/*.jar" % version)
                      if "sources" not in p)

_cache = {}

def dump(cls):
    """javap output for a class, or None."""
    if cls not in _cache:
        try:
            _cache[cls] = subprocess.run(["javap", "-p", "-s", "-cp", jar, cls],
                                         capture_output=True, text=True, check=True).stdout
        except subprocess.CalledProcessError:
            _cache[cls] = None
    return _cache[cls]

def supertypes(javap_out):
    header = javap_out.splitlines()[1] if len(javap_out.splitlines()) > 1 else ""
    header = header.split("{")[0]
    names = []
    for kw in ("extends", "implements"):
        if kw in header:
            names += [n.strip().split("<")[0] for n in header.split(kw, 1)[1].split(",")]
    return [n for n in names if n and n[0].islower()]

def has_member(cls, mname, mdesc, depth=0):
    """Whether cls or any supertype declares mname with mdesc."""
    out = dump(cls)
    if out is None:
        return None
    if (mname, mdesc) in members(out):
        return True
    if depth < 3:
        for sup in supertypes(out):
            if has_member(sup, mname, mdesc, depth + 1) is True:
                return True
    return False

root = "versions/%s/build/generated/stonecutter/main/java/me/pepperbell/continuity/client/mixin" % version
if not os.path.isdir(root):
    root = "src/main/java/me/pepperbell/continuity/client/mixin"

problems, checked = [], 0
for name in sorted(os.listdir(root)):
    if not name.endswith(".java"):
        continue
    src = strip_comments(io.open(os.path.join(root, name), encoding="utf-8").read())
    m = re.search(r'@Mixin\(([A-Za-z0-9_.]+)\.class\)', src)
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
    out = dump(target)
    if out is None:
        problems.append("%s: javap failed for %s" % (name, target))
        continue
    have = members(out)
    names = {n for n, _ in have}
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
        if has_member(target, mname, wanted) is not True:
            problems.append("%s: %s%s NOT FOUND on %s" % (name, mname, wanted, target))

    # @At INVOKE targets are descriptors too, and just as unchecked by javac.
    for at in re.findall(r'target = "L([^;]+);([A-Za-z_$<][A-Za-z0-9_$>]*)(\([^"]*)"', src):
        owner, mname, mdesc = at
        checked += 1
        owner = owner.replace("/", ".")
        found = has_member(owner, mname, mdesc)
        if found is None:
            print("  ~ %s: %s not on the classpath, skipped" % (name, owner))
        elif not found:
            problems.append("%s: @At %s.%s%s NOT FOUND" % (name, owner, mname, mdesc))

print("%s: checked %d targets, %d problem(s)" % (version, checked, len(problems)))
for p in problems:
    print("  ! " + p)
