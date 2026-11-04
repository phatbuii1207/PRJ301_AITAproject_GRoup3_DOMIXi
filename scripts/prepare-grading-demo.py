#!/usr/bin/env python3
"""Create Java ZIP examples. The answer bank is separate and never part of a submission ZIP."""
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parent.parent / "target" / "grading-demo"
root.mkdir(parents=True, exist_ok=True)
examples = {
    "correct-sum": 'public class Main { public static void main(String[] args) { java.util.Scanner s = new java.util.Scanner(System.in); System.out.println(s.nextLong() + s.nextLong()); } }',
    "hardcoded-five": 'public class Main { public static void main(String[] args) { System.out.println(5); } }',
    "compile-error": 'public class Main { public static void main(String[] args) { System.out.println(5) } }',
    "infinite-loop": 'public class Main { public static void main(String[] args) { while (true) {} } }',
}
for name, code in examples.items():
    directory = root / "sources" / name
    directory.mkdir(parents=True, exist_ok=True)
    (directory / "Main.java").write_text(code + "\n", encoding="utf-8")
    with zipfile.ZipFile(root / (name + ".zip"), "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("Main.java", code + "\n")
    print("Created:", name + ".zip")
(root / "teacher-test-bank.txt").write_text(
    "Main class: Main\nEach test weight: 2\n"
    "1. Public: input 2 3 => expected 5\n"
    "2. Hidden: input -2 5 => expected 3\n"
    "3. Hidden: input 0 0 => expected 0\n"
    "4. Hidden: input -4 -6 => expected -10\n"
    "5. Hidden: input 1000000 2000000 => expected 3000000\n"
    "correct-sum: 10/10; hardcoded-five: 2/10; compile-error: 0/10.\n"
    "An infinite loop takes about 5 seconds per test, up to 25 seconds for this bank.\n",
    encoding="utf-8",
)
print("Examples:", root)
