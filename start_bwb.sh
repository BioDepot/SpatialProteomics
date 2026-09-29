#!/bin/bash
# Start Bwb with a small local shim so widgets get /data mounted again.
#
# Bwb finds its own container id by parsing /proc/self/mountinfo. On current
# Codespaces that lookup no longer matches, so Bwb records no mounts and
# autoMap widgets (QuPath, etc.) start with NO /data volume at all.
# The shim adds a fallback: also match the container whose id starts with
# this container's hostname (Docker's default hostname is the short id).
# Upstream Bwb is not modified; a patched copy of one file is bind-mounted.
set -euo pipefail

IMAGE="${BWB_IMAGE:-biodepot/bwb}"
WORKSPACE="$(cd "$(dirname "$0")/.." && pwd)"
SHIM_DIR="${WORKSPACE}/.devcontainer/.bwb_shim"
SHIM="${SHIM_DIR}/DockerClient.py"

mkdir -p "$SHIM_DIR"
docker pull -q "$IMAGE" >/dev/null || true

# Copy DockerClient.py out of the exact image we are about to run
cid=$(docker create "$IMAGE")
docker cp "${cid}:/coreutils/DockerClient.py" "$SHIM"
docker rm "$cid" >/dev/null

OLD='if container_id == self.bwb_instance_id:'
NEW='if container_id == self.bwb_instance_id or container_id.startswith(os.uname().nodename):'
if ! grep -qF "$OLD" "$SHIM"; then
    echo "start_bwb.sh: expected line not found in DockerClient.py; image changed, shim not applied" >&2
    exit 1
fi
sed -i "s/${OLD}/${NEW}/" "$SHIM"

exec docker run --rm -p 6080:6080 \
    -v "${WORKSPACE}":/data \
    -v "${SHIM}":/coreutils/DockerClient.py:ro \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /tmp/.X11-unix:/tmp/.X11-unix \
    --privileged --group-add root \
    -e STARTING_WORKFLOW=/data/workflows/BwbSpatialProteomics/BwbSpatialProteomics.ows \
    "$IMAGE"
