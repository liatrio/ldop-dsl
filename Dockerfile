FROM jenkins

USER root

RUN apt-get update \
      && apt-get install -y sudo \
      && rm -rf /var/lib/apt/lists/*

# Setup rvm 
RUN apt-get update
RUN apt-get install -y curl bison build-essential zlib1g-dev libssl-dev libreadline-gplv2-dev libxml2-dev git-core
# RUN gpg --keyserver hkp://pgp.mit.edu --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3
RUN curl -sSL https://get.rvm.io | sudo bash -s stable 
RUN echo 'rvm_install_on_use_flag=1' >> /etc/rvmrc
RUN echo 'rvm_project_rvmrc=1' >> /etc/rvmrc
RUN echo 'rvm_gemset_create_on_use_flag=1' >> /etc/rvmrc
RUN usermod -aG rvm jenkins

# Setup Jenkins
RUN echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers
USER jenkins
COPY plugins.txt /usr/share/jenkins/ref/
RUN /usr/local/bin/install-plugins.sh /usr/share/jenkins/ref/plugins.txt
