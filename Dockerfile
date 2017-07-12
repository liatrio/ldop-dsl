FROM jenkins:2.60.1

USER root

RUN apt-get update \
      && apt-get install -y sudo \
      && rm -rf /var/lib/apt/lists/*

# Setup Jenkins
RUN echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers
USER jenkins
COPY plugins.txt /usr/share/jenkins/ref/
RUN xargs /usr/local/bin/install-plugins.sh $(cat /usr/share/jenkins/ref/plugins.txt)
