# Use an appropriate base image with Java support
FROM ubuntu:20.04

# Set environment variables
ENV QUPATH_VERSION=0.5.1
#ENV DISPLAY=host.docker.internal:0
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Install dependencies
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y wget tar xz-utils libxext6 libxrender1 libxtst6 libfreetype6 libxi6 libxdamage1 libxfixes3 libgtk-3-0 x11-apps openjdk-11-jre curl groovy expect && \
    rm -rf /var/lib/apt/lists/*

# Install OpenJDK 11 and JavaFX
#RUN apt-get update && \
#    DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-11-jdk openjfx && \
# rm -rf /var/lib/apt/lists/*

# Download and install QuPath
RUN wget https://github.com/qupath/qupath/releases/download/v$QUPATH_VERSION/QuPath-v$QUPATH_VERSION-Linux.tar.xz && \
    tar -xvf QuPath-v$QUPATH_VERSION-Linux.tar.xz -C /opt/ && \
    rm QuPath-v$QUPATH_VERSION-Linux.tar.xz

#ENV PATH="/usr/lib/jvm/java-11-openjdk-amd64/bin:${PATH}"
#ENV LD_LIBRARY_PATH="/usr/lib/jvm/java-11-openjdk-amd64/lib:${LD_LIBRARY_PATH}"

RUN mkdir -p /root/QuPath/v0.5/extensions

RUN curl -L -o /root/QuPath/v0.5/extensions/qupath-extension-stardist-0.5.0.jar https://github.com/qupath/qupath-extension-stardist/releases/download/v0.5.0/qupath-extension-stardist-0.5.0.jar

RUN echo 'qupath.lib.gui.prefs.PathPrefs.userPathProperty().set("/root/QuPath/v0.5"); \n print "Extensions directory set!"' > /root/qupath_config.groovy

RUN /opt/QuPath-v0.5.1-Linux/QuPath/bin/QuPath script /root/qupath_config.groovy
# Set working directory
WORKDIR /data

#RUN /opt/QuPath-v0.5.1-Linux/QuPath/bin/QuPath script load_stardist_ext.groovy
# Expose necessary ports (if any)
EXPOSE 8080

# Define the entry point /opt/QuPath-v0.5.1-Linux/QuPath/bin/QuPath
# /QuPath/bin/QuPath: no such file
#ENTRYPOINT ["/opt/QuPath-v0.5.1-Linux/QuPath/bin/QuPath"]
#CMD ["/opt/QuPath-v0.5.1-Linux/QuPath/bin/QuPath script load_stardist_ext.groovy"]
#CMD ["script","/root/qupath_config.groovy"]
#CMD ["/opt/QuPath-v0.5.1-Linux/QuPath/bin/QuPath","script", "/root/qupath_config.groovy"]
#CMD ["sh", "-c", "/opt/QuPath-v0.5.1-Linux/QuPath/bin/QuPath -s /root/qupath_config.groovy && /opt/QuPath-v0.5.1-Linux/QuPath/bin/QuPath -p /QuPathBWBAutomation/project.qpproj"]


