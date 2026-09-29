#!/bin/bash
# Start Bwb with a small local shim so autoMap widgets get /data mounted again.
#
# Bwb finds its own container id by parsing /proc/self/mountinfo. On current
# Codespaces that lookup no longer matches, so Bwb records no mounts and
# autoMap widgets (QuPath, etc.) start with NO /data volume at all.
# The shim adds a fallback: also match the container whose id starts with
# this container's hostname (Docker's default hostname is the short id).
#
# Any problem building the shim is logged and Bwb still starts (unpatched),
# so the port always comes up. Log: .devcontainer/.bwb_shim/start.log

IMAGE="${BWB_IMAGE:-biodepot/bwb}"
WORKSPACE="$(cd "$(dirname "$0")/.." && pwd)"
SHIM_DIR="${WORKSPACE}/.devcontainer/.bwb_shim"
SHIM="${SHIM_DIR}/DockerClient.py"
LOG="${SHIM_DIR}/start.log"

mkdir -p "$SHIM_DIR"
: > "$LOG"
log() { echo "[start_bwb] $*" | tee -a "$LOG" >&2; }

log "workspace: ${WORKSPACE}"

# Wait for the Docker daemon (it can lag behind postStartCommand)
for i in $(seq 1 60); do
    docker info >/dev/null 2>&1 && break
    sleep 2
done
if ! docker info >/dev/null 2>&1; then
    log "ERROR: Docker daemon not reachable after 120 s"
    exit 1
fi

docker image inspect "$IMAGE" >/dev/null 2>&1 || docker pull "$IMAGE" 2>&1 | tee -a "$LOG"

SHIM_ARGS=()
build_shim() {
    # Locate DockerClient.py inside this exact image
    local path
    path=$(docker run --rm --entrypoint sh "$IMAGE" -c \
        'for p in /coreutils/DockerClient.py $(find / -xdev -name DockerClient.py -path "*coreutils*" 2>/dev/null); do [ -f "$p" ] && { echo "$p"; break; }; done')
    if [ -z "$path" ]; then
        log "shim skipped: DockerClient.py not found in ${IMAGE}"
        return 1
    fi
    log "found ${path} in image"

    local cid
    cid=$(docker create "$IMAGE") || { log "shim skipped: docker create failed"; return 1; }
    docker cp "${cid}:${path}" "$SHIM" 2>>"$LOG"
    local rc=$?
    docker rm "$cid" >/dev/null 2>&1
    [ $rc -eq 0 ] || { log "shim skipped: docker cp failed"; return 1; }

    # Tolerant of whitespace differences between image versions
    if ! grep -qE 'if[[:space:]]+container_id[[:space:]]*==[[:space:]]*self\.bwb_instance_id[[:space:]]*:' "$SHIM"; then
        log "shim skipped: container-id comparison line not found in ${path}"
        return 1
    fi
    sed -i -E 's/if[[:space:]]+container_id[[:space:]]*==[[:space:]]*self\.bwb_instance_id[[:space:]]*:/if container_id == self.bwb_instance_id or container_id.startswith(os.uname().nodename):/' "$SHIM"
    grep -q 'startswith(os.uname().nodename)' "$SHIM" || { log "shim skipped: sed did not apply"; return 1; }

    SHIM_ARGS=(-v "${SHIM}:${path}:ro")
    log "shim applied"
}
build_shim || log "WARNING: starting Bwb WITHOUT the /data shim (see messages above)"

log "starting Bwb on port 6080"
exec docker run --rm -p 6080:6080 \
    -v "${WORKSPACE}":/data \
    "${SHIM_ARGS[@]}" \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /tmp/.X11-unix:/tmp/.X11-unix \
    --privileged --group-add root \
    -e STARTING_WORKFLOW=/data/workflows/BwbSpatialProteomics/BwbSpatialProteomics.ows \
    "$IMAGE"
