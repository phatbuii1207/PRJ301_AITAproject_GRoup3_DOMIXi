#!/usr/bin/env python3
"""Create presentation ZIPs without executing or extracting student code on the host."""
from pathlib import Path
import zipfile

PROJECT = Path(__file__).resolve().parent.parent
DESTINATION = PROJECT / "target" / "sandbox-demo"
SOURCES = PROJECT / "src" / "test" / "resources" / "sandbox"


def prepare():
    fixtures = {
        "hello": """public class Main {
    public static void main(String[] args) {
        System.out.println("HELLO_SANDBOX");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("User: " + System.getProperty("user.name"));
    }
}
""",
        "tle": (SOURCES / "tle" / "Main.java").read_text(encoding="utf-8"),
        "files": (SOURCES / "file-deletion" / "Main.java").read_text(encoding="utf-8"),
        "network": (SOURCES / "network" / "Main.java").read_text(encoding="utf-8"),
        "compile": """public class Main {
    public static void main(String[] args) {
        System.out.println("Missing semicolon")
    }
}
""",
        "zip-slip": "public class Main { public static void main(String[] args) {} }\n",
        "flood": """public class Main {
    public static void main(String[] args) {
        while (true) System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    }
}
""",
    }
    DESTINATION.mkdir(parents=True, exist_ok=True)
    for name, code in fixtures.items():
        directory = DESTINATION / "sources" / name
        directory.mkdir(parents=True, exist_ok=True)
        (directory / "Main.java").write_text(code, encoding="utf-8")
        entry = "../Main.java" if name == "zip-slip" else "Main.java"
        archive = DESTINATION / (name + ".zip")
        with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as zipped:
            zipped.writestr(entry, code)
        print(f"Ready: {archive.name} (ZIP entry: {entry})")
    print(f"ZIPs and readable source: {DESTINATION}")


if __name__ == "__main__":
    prepare()
