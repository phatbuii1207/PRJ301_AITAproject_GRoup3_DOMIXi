#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

demo_case="${1:-all}"
case "$demo_case" in
    prepare|all|hello|tle|files|network|compile|zip-slip|flood|inspect) ;;
    *) echo "Usage: $0 {prepare|all|hello|tle|files|network|compile|zip-slip|flood|inspect}" >&2; exit 2 ;;
esac

if [[ "$demo_case" == inspect ]]; then
    sandbox_listing=$(docker ps --filter label=aita.sandbox=true --format '{{.ID}}')
    if [[ -z "$sandbox_listing" ]]; then
        echo "No running sandbox container (normal before/after a demo)."
        exit 0
    fi
    while IFS= read -r sandbox_id; do
        docker inspect --format 'Name={{.Name}}
User={{.Config.User}} Network={{.HostConfig.NetworkMode}}
RAM={{.HostConfig.Memory}} MemorySwap={{.HostConfig.MemorySwap}} NanoCPUs={{.HostConfig.NanoCpus}}
ReadOnlyRootfs={{.HostConfig.ReadonlyRootfs}} AutoRemove={{.HostConfig.AutoRemove}} PidsLimit={{.HostConfig.PidsLimit}}
CapabilitiesDropped={{.HostConfig.CapDrop}} SecurityOpt={{.HostConfig.SecurityOpt}}
{{range .Mounts}}Mount={{.Destination}} Writable={{.RW}}
{{end}}' "$sandbox_id" || true # --rm can remove a finished container during this snapshot.
    done <<< "$sandbox_listing"
    exit 0
fi

if [[ -n "${MAVEN_BIN:-}" ]]; then
    sandbox_maven="$MAVEN_BIN"
elif command -v mvn >/dev/null 2>&1; then
    sandbox_maven=$(command -v mvn)
else
    sandbox_maven=""
    for candidate in "$HOME"/.cache/csd-tools/apache-maven-*/bin/mvn; do
        if [[ -x "$candidate" ]]; then sandbox_maven="$candidate"; break; fi
    done
    if [[ -z "$sandbox_maven" ]]; then
        echo "Maven not found. Install Maven or set MAVEN_BIN=/path/to/bin/mvn." >&2
        exit 1
    fi
fi

docker info >/dev/null
if ! docker image inspect aita-sandbox:java8 >/dev/null 2>&1; then
    echo "Build the required image first: docker build -t aita-sandbox:java8 docker/sandbox" >&2
    exit 1
fi
if [[ -n "${AITA_SANDBOX_IMAGE:-}" && "$AITA_SANDBOX_IMAGE" != aita-sandbox:java8 ]]; then
    echo "Presentation expects aita-sandbox:java8. Unset AITA_SANDBOX_IMAGE for these fixtures." >&2
    exit 1
fi

echo "Preparing trusted CLI and sample ZIPs (no database required)..."
"$sandbox_maven" -q -DskipTests test-compile
python3 scripts/prepare-sandbox-demo.py
mkdir -p target/sandbox-demo/results
if [[ "$demo_case" == prepare ]]; then exit 0; fi

run_demo() {
    local selected="$1" expected marker=""
    case "$selected" in
        hello) expected=SUCCESS; marker=HELLO_SANDBOX ;;
        tle) expected=TIME_LIMIT_EXCEEDED ;;
        files) expected=SUCCESS; marker=FILE_DELETION_BLOCKED ;;
        network) expected=SUCCESS; marker=NETWORK_BLOCKED ;;
        compile) expected=COMPILE_ERROR ;;
        zip-slip) expected=INVALID_SUBMISSION ;;
        flood) expected=OUTPUT_LIMIT_EXCEEDED ;;
    esac
    echo
    echo "DEMO: $selected | expected: $expected"
    local arguments=("target/sandbox-demo/$selected.zip" "$expected")
    if [[ -n "$marker" ]]; then arguments+=("$marker"); fi
    java -cp target/classes:target/test-classes com.fpt.aita.demo.SandboxDemo "${arguments[@]}" \
        | tee "target/sandbox-demo/results/$selected.txt"
}

if [[ "$demo_case" == all ]]; then
    for selected in hello tle files network compile zip-slip flood; do run_demo "$selected"; done
else
    run_demo "$demo_case"
fi
echo
echo "Remaining sandbox containers (normally empty):"
docker ps -a --filter label=aita.sandbox=true --format '{{.Names}} {{.Status}}'
