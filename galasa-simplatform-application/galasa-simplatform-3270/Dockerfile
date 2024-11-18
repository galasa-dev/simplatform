ARG dockerRepository
ARG jdkImage

FROM ${dockerRepository}/${jdkImage}

RUN mkdir /galasa

RUN useradd -u 1000 -d /galasa galasa              && \
    chown -R galasa:galasa /galasa
    
WORKDIR /galasa

USER galasa

COPY target/galasa-simplatform-0.38.0.jar /galasa/simplatform.jar
    
VOLUME /galasa/.galasa
VOLUME /galasa/load

EXPOSE 2080/tcp
EXPOSE 2023/tcp
EXPOSE 2027/tcp
EXPOSE 2040/tcp

