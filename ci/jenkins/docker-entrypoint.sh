#!/bin/bash
# Local-only: make Docker Desktop socket usable, then run Jenkins as the jenkins user.
# Running the whole controller as root breaks some git lightweight-checkout paths.
set -euo pipefail

if [[ -S /var/run/docker.sock ]]; then
  # Docker Desktop often mounts the socket root:root mode 660.
  chmod 666 /var/run/docker.sock 2>/dev/null || true
fi

# Repair ownership if a previous root run left files jenkins cannot write.
if [[ -d /var/jenkins_home ]]; then
  chown -R jenkins:jenkins /var/jenkins_home 2>/dev/null || true
fi

# Official image entrypoint (tini + jenkins.sh), as non-root.
exec runuser -u jenkins -- /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"
