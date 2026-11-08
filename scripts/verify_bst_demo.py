#!/usr/bin/env python3
"""Verify the public BST demo in isolated Java 8 Docker containers, without a DB."""
import json
from pathlib import Path
import re
import subprocess
import tempfile
import uuid

REPO = Path(__file__).resolve().parents[1]
DEMO = REPO / 'docs/test-thu-bst'


def normalized(value):
    return value.replace('\r\n', '\n').replace('\r', '\n').rstrip()


def main():
    config = json.loads((DEMO / 'test-cases.json').read_text(encoding='utf-8'))
    original = (DEMO / 'MyBST.java').read_text(encoding='utf-8')
    wrong, replacements = re.subn(
        r'public boolean search\(int value\) \{.*?\n    \}',
        'public boolean search(int value) { return false;\n    }', original, count=1, flags=re.S)
    assert replacements == 1, 'Could not create the intentionally wrong search variant'
    for label, source, expected_passes in [('correct', original, 8), ('wrong-search', wrong, 6)]:
        with tempfile.TemporaryDirectory(prefix='aita-bst-demo-') as directory:
            workspace = Path(directory)
            (workspace / 'MyBST.java').write_text(source, encoding='utf-8')
            (workspace / 'Main.java').write_bytes((DEMO / config['driver']).read_bytes())
            (workspace / '.aita-sources.args').write_text('/workspace/Main.java\n/workspace/MyBST.java\n')
            workspace.chmod(0o755)
            for path in workspace.iterdir():
                path.chmod(0o644)
            passed = 0
            for case in config['cases']:
                assert (DEMO / 'test-cases' / (case['name'] + '.in')).read_text() == case['stdin']
                assert (DEMO / 'test-cases' / (case['name'] + '.out')).read_text() == case['expected']
                name = 'aita-demo-' + str(uuid.uuid4())
                command = ['docker', 'run', '--rm', '--pull', 'never', '--name', name,
                           '--network', 'none', '--memory', '128m', '--memory-swap', '128m',
                           '--cpus', '0.5', '--pids-limit', '64', '--user', 'sandboxuser',
                           '--read-only', '--cap-drop', 'ALL', '--security-opt', 'no-new-privileges:true',
                           '--ulimit', 'nofile=128:128', '--log-driver', 'none',
                           '--tmpfs', '/tmp:rw,noexec,nosuid,nodev,size=32m,mode=1777',
                           '--volume', str(workspace) + ':/workspace:ro', '-i',
                           'aita-sandbox:java8', config['mainClass']]
                try:
                    result = subprocess.run(command, input=case['stdin'], capture_output=True,
                                            text=True, timeout=30, check=False)
                finally:
                    subprocess.run(['docker', 'rm', '-f', name], capture_output=True, timeout=10)
                assert result.returncode == 0, case['name'] + ': ' + result.stderr
                matches = normalized(result.stdout) == normalized(case['expected'])
                passed += int(matches)
                print(label + ' ' + case['name'] + ': ' + ('PASS' if matches else 'WRONG_ANSWER'), flush=True)
            assert passed == expected_passes, (label, passed, expected_passes)
            print('{}: {}/8, score={:.2f}/10'.format(label, passed, 10 * passed / 8), flush=True)


if __name__ == '__main__':
    main()
