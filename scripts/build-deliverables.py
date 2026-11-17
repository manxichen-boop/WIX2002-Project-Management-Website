#!/usr/bin/env python3
"""
Regenerates the two generated deliverables from the files on disk:
  * documentation/FILE_MANIFEST.md  - every project file and its responsibility
  * ECOSWAP_ALL_FILES.md            - the complete source of every text file, grouped

Run from the project root:  python3 scripts/build-deliverables.py
Responsibilities are taken from each file's own header comment / Javadoc, so keeping
those comments accurate keeps the manifest accurate.
"""
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXCLUDED_DIRS = {".git", "target", ".idea", ".vscode", "node_modules"}
GENERATED = {"ECOSWAP_ALL_FILES.md", "documentation/FILE_MANIFEST.md"}
TEXT_EXT = {".java", ".sql", ".yml", ".yaml", ".properties", ".html", ".css", ".js", ".md", ".xml", ".sh", ".ps1",
            ".py", ".svg", ".cmd", ".example", ".gitignore", ".gitkeep", ""}

BINARY_DESCRIPTIONS = {
    "src/main/resources/static/img/hero-scene.webp": "Hero illustration (green arch, armchair, plants, clothing rail, books, backpack, camera, box). Taken from the approved reference design; category cards are rendered in HTML on top.",
    "src/main/resources/static/img/landscape.webp": "Campus landscape band (university building, hills, trees) under the statistics strip.",
    "src/main/resources/static/img/leaf-tl.webp": "Decorative leaves, top-left of the homepage (transparent).",
    "src/main/resources/static/img/leaf-r.webp": "Decorative leaves, right edge of the homepage (transparent).",
    "src/main/resources/static/img/leaf-bl.webp": "Decorative leaves, bottom-left of the homepage (transparent).",
    "src/main/resources/static/fonts/manrope-variable.woff2": "Manrope variable font (headings), self-hosted, SIL Open Font License.",
    "src/main/resources/static/fonts/inter-variable.woff2": "Inter variable font (body text), self-hosted, SIL Open Font License.",
    "src/main/resources/static/fonts/caveat-600.woff2": "Caveat 600 (handwritten tagline only), self-hosted, SIL Open Font License.",
    ".mvn/wrapper/maven-wrapper.properties": "Maven wrapper settings (Maven 3.9.11).",
}

# Files whose purpose isn't stated in a header comment.
OVERRIDES = {
    ".gitignore": "Keeps build output, IDE files, .env and uploaded files out of version control.",
    "pom.xml": "Maven build: Spring Boot 3.5 parent, Java 21, web/Thymeleaf/security/JPA/validation/mail starters, MySQL driver, Flyway, test libraries.",
    "mvnw": "Maven wrapper launcher (Linux/macOS).",
    "mvnw.cmd": "Maven wrapper launcher (Windows).",
    "storage/.gitkeep": "Keeps the (git-ignored) local file-storage folder in the repository.",
    "src/main/resources/static/img/favicon.svg": "Browser tab icon: the Eco Swape swap-loop mark.",
    "src/main/resources/static/img/icons.svg": "SVG sprite of the Lucide icons used across the UI (ISC licence), referenced as /img/icons.svg#i-name.",
    "src/main/java/com/ecoswap/EcoSwapApplication.java": "Spring Boot entry point; sets the JVM default time zone to UTC and enables scheduling.",
    "src/main/java/com/ecoswap/admin/dto/AdminSidebarCounts.java": "Pending registrations, pending listings, open reports and open tickets shown on the admin sidebar.",
    "src/main/java/com/ecoswap/common/exception/NotFoundException.java": "404 business exception for missing (or not-visible) resources.",
    "src/main/java/com/ecoswap/common/util/Slugs.java": "Builds URL-safe category slugs from names.",
    "src/main/java/com/ecoswap/common/validation/StrongPasswordValidator.java": "Implements the @StrongPassword rule (8–72 bytes, a letter and a digit).",
}

GROUPS = [
    ("Configuration & build", lambda p: p in {"pom.xml", ".gitignore", ".env.example", "mvnw", "mvnw.cmd"} or p.startswith(".mvn/")
        or p.endswith((".yml", ".yaml"))),
    ("Database (Flyway migrations)", lambda p: p.startswith("src/main/resources/db/")),
    ("Backend (Java)", lambda p: p.startswith("src/main/java/")),
    ("Frontend — Thymeleaf templates", lambda p: p.startswith("src/main/resources/templates/")),
    ("Frontend — CSS", lambda p: p.startswith("src/main/resources/static/css/")),
    ("Frontend — JavaScript (ES modules)", lambda p: p.startswith("src/main/resources/static/js/")),
    ("Frontend — images, icons & fonts", lambda p: p.startswith("src/main/resources/static/")),
    ("Tests", lambda p: p.startswith("src/test/")),
    ("Scripts", lambda p: p.startswith("scripts/") or p == "storage/.gitkeep"),
    ("Documentation", lambda p: True),
]


def project_files():
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = sorted(d for d in dirnames if d not in EXCLUDED_DIRS)
        rel_dir = Path(dirpath).relative_to(ROOT).as_posix()
        if rel_dir.startswith("storage") and rel_dir != "storage":
            continue                                   # uploaded user files are never part of the deliverable
        for name in sorted(filenames):
            rel = name if rel_dir == "." else f"{rel_dir}/{name}"
            if rel in GENERATED or name == ".env" or name.endswith((".zip", ".log")):
                continue
            if rel.startswith("storage/") and name != ".gitkeep":
                continue
            yield rel


def is_text(rel):
    p = Path(rel)
    return p.suffix.lower() in TEXT_EXT and not rel.endswith((".png", ".webp", ".woff2", ".jpg"))


def describe(rel, text):
    if rel in OVERRIDES:
        return OVERRIDES[rel]
    if rel in BINARY_DESCRIPTIONS:
        return BINARY_DESCRIPTIONS[rel]
    if rel.endswith(".sql") and text:
        tables = re.findall(r"CREATE TABLE (\w+)", text)
        if tables and not re.match(r"\s*--", text):
            return "Flyway migration: creates table `" + "`, `".join(tables) + "` with keys, constraints and indexes."
    if rel.startswith("src/test/") and rel.endswith(".java") and text:
        doc = re.search(r"/\*\*\s*(.*?)\*/\s*(?:@[\w.]+(?:\([^)]*\))?\s*)*(?:public )?(?:abstract )?class", text, re.S)
        tests = len(re.findall(r"@Test", text))
        name = re.search(r"class (\w+)", text).group(1)
        summary = re.sub(r"\s*\*\s*", " ", doc.group(1)).strip().lstrip("* ").split(". ")[0] if doc else name
        summary = summary.rstrip(".")
        return f"{summary} ({tests} test{'s' if tests != 1 else ''})." if tests else summary + "."
    name = Path(rel).name
    if "wireframes/" in rel:
        return "Low-fidelity wireframe (SVG): " + name.replace("-wireframe.svg", "").replace("-", " ") + " layout."
    if rel.endswith(".png") and "screenshots" in rel:
        return "Screenshot of the running application: " + name[3:-4].replace("-", " ") + "."
    if rel.startswith("src/main/resources/static/img/categories/"):
        return f"Category card icon ({Path(rel).stem})."
    if text is None:
        return "Binary asset."
    if rel.endswith(".java"):
        doc = re.search(r"/\*\*\s*(.*?)\*/\s*(?:@[\w.]+(?:\([^)]*\))?\s*)*public", text, re.S)
        if doc:
            first = re.sub(r"\s*\*\s*", " ", doc.group(1)).strip()
            first = re.split(r"(?<=[.!?])\s", first)[0]
            return re.sub(r"\{@\w+ ([^}]*)\}", r"\1", first)
        kind = re.search(r"public (?:final |abstract )?(class|interface|record|enum|@interface) (\w+)", text)
        if kind:
            k, n = kind.groups()
            for suffix, label in [("Controller", "HTTP endpoints"), ("Repository", "Spring Data repository"),
                                  ("Service", "business logic service"), ("Request", "request body DTO"),
                                  ("Response", "response DTO"), ("View", "read-model DTO"), ("Form", "form DTO"),
                                  ("Body", "request body DTO"), ("Test", "test class")]:
                if n.endswith(suffix):
                    return f"{n}: {label}."
            if k == "enum":
                values = re.search(r"enum \w+\s*\{\s*([A-Z_,\s(\")a-z]+?)[;}]", text)
                return f"Enum {n}" + (f": {', '.join(v.split('(')[0].strip() for v in values.group(1).split(',') if v.strip())[:120]}." if values else ".")
            if "@Entity" in text:
                table = re.search(r'@Table\(name = "(\w+)"', text)
                return f"JPA entity mapped to table `{table.group(1) if table else n}`."
            return f"{k.capitalize()} {n}."
    patterns = [r"<!--/\*\s*(.*?)\*/-->", r"/\*\s*=+\s*(.*?)\s*=+\s*\*/", r"/\*\s*(.*?)\*/", r"^\s*//\s*(.+)$",
                r"^\s*--\s*(.+)$", r"^\s*#\s*(?!!)(.+)$", r'"""\s*(.*?)"""', r"<title>(.*?)</title>"]
    for pat in patterns:
        m = re.search(pat, text, re.S | re.M)
        if m:
            line = " ".join(m.group(1).split())
            line = re.split(r"(?<=[.!?])\s", line)[0]
            if len(line) > 8:
                return line[:200]
    if rel.endswith(".html"):
        return "Thymeleaf page template."
    if rel.endswith(".md"):
        h = re.search(r"^#\s+(.+)$", text, re.M)
        return h.group(1) if h else "Documentation."
    return "Project file."


def group_of(rel):
    for title, test in GROUPS:
        if test(rel):
            return title
    return "Documentation"


def lang(rel):
    ext = Path(rel).suffix.lstrip(".")
    return {"yml": "yaml", "md": "markdown", "ps1": "powershell", "sh": "bash", "js": "javascript", "py": "python",
            "svg": "xml", "cmd": "bat"}.get(ext, ext or "text")


def main():
    files = list(project_files())
    contents = {}
    for rel in files:
        if is_text(rel):
            try:
                contents[rel] = (ROOT / rel).read_text(encoding="utf-8")
            except UnicodeDecodeError:
                contents[rel] = None
        else:
            contents[rel] = None

    grouped = {}
    for rel in files:
        grouped.setdefault(group_of(rel), []).append(rel)
    order = [g for g, _ in GROUPS]

    # ---- FILE_MANIFEST.md
    lines = ["# File Manifest", "",
             f"All {len(files)} files of the EcoSwap project and their responsibility (generated by `scripts/build-deliverables.py`).",
             "Build output (`target/`), uploaded user files (`storage/*`) and `.env` are intentionally excluded.", ""]
    for g in order:
        if g not in grouped:
            continue
        lines += [f"## {g} ({len(grouped[g])})", "", "| File | Responsibility |", "|---|---|"]
        for rel in grouped[g]:
            lines.append(f"| `{rel}` | {describe(rel, contents[rel]).replace('|', '/')} |")
        lines.append("")
    (ROOT / "documentation/FILE_MANIFEST.md").write_text("\n".join(lines), encoding="utf-8")

    # ---- ECOSWAP_ALL_FILES.md
    text_files = [f for f in files if contents[f] is not None]
    out = ["# ECOSWAP — Complete Source Code", "",
           "Every text-based file of the project, complete and unabridged, grouped by area. Binary assets (images, fonts,",
           "screenshots) are listed with their paths and descriptions and remain as individual files in the ZIP.",
           f"Text files included: {len(text_files)}. Generated by `scripts/build-deliverables.py`.", "", "## Contents", ""]
    for g in order:
        if g in grouped:
            out.append(f"- {g}: {sum(1 for f in grouped[g] if contents[f] is not None)} text files")
    for g in order:
        if g not in grouped:
            continue
        out += ["", f"# {g}", ""]
        for rel in grouped[g]:
            if contents[rel] is None:
                continue
            body = contents[rel].rstrip("\n")
            fence = "````" if "```" in body else "```"
            out += ["=" * 50, f"FILE: {rel}", "=" * 50, "", f"{fence}{lang(rel)}", body, fence, ""]
    binaries = [f for f in files if contents[f] is None]
    out += ["", "# Binary assets (kept as individual files)", "", "| Path | Description |", "|---|---|"]
    out += [f"| `{rel}` | {describe(rel, None)} |" for rel in binaries]
    (ROOT / "ECOSWAP_ALL_FILES.md").write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"Manifest: {len(files)} files. Merged source: {len(text_files)} text files, {len(binaries)} binary assets listed.")


if __name__ == "__main__":
    main()
