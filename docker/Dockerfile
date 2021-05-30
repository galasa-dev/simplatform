ARG baseVersion
ARG dockerRepository
FROM ${dockerRepository}/galasadev/galasa-maven-base:${baseVersion}

ARG branch
ARG gitHash

RUN sed -i "s/--branchname--/${branch}/"    /usr/local/apache2/conf/httpd.conf
RUN sed -i 's/--repositoryname--/simplatform/'  /usr/local/apache2/conf/httpd.conf

COPY repo/ /usr/local/apache2/htdocs/
RUN echo -n ${gitHash} > /usr/local/apache2/htdocs/simplatform.githash
