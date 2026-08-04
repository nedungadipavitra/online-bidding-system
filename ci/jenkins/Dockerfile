# Minimal Jenkins controller that can run OBS service pipelines locally.
# Extends official LTS so we do not install Jenkins on the host OS.
FROM jenkins/jenkins:lts-jdk21

USER root

# Tools expected by jenkins/obs-service-pipeline.groovy on "agent any"
# - docker-cli talks to the host Docker daemon via mounted docker.sock
# - maven builds Spring Boot modules
# - awscli logs into ECR
# - openssh-client used by scp/ssh deploy stages
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        gnupg \
        git \
        openssh-client \
        unzip \
        maven \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
    && chmod a+r /etc/apt/keyrings/docker.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" \
        > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli \
    && curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip \
    && unzip -q /tmp/awscliv2.zip -d /tmp \
    && /tmp/aws/install \
    && rm -rf /tmp/aws /tmp/awscliv2.zip /var/lib/apt/lists/*

# docker group for non-root use; docker-compose also runs as root for Docker Desktop sock access.
RUN groupadd -f docker \
    && usermod -aG docker jenkins

# Stay root for plugin install tooling; compose sets user=root for docker.sock on Desktop.
# Plugins still install into /usr/share/jenkins/ref (copied into JENKINS_HOME on first boot).
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
