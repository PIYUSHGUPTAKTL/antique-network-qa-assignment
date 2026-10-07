FROM parasoft/parabank@sha256:cebe472d86a10be23ce448f0f419f3c939a818112ab78cd57fc09e185f35d1c0 AS bank
FROM eclipse-temurin:21-jdk@sha256:3e3c176ffed168beb42c607be9bc1639b466cf00261a0fb04425562c9d0c5c2b AS compiler
COPY --from=bank /usr/local/tomcat/webapps/parabank/WEB-INF /runtime/WEB-INF
COPY --from=bank /usr/local/tomcat/lib /runtime/tomcat-lib
COPY src /src
RUN mkdir /classes && find /src -name '*.java' > /sources.txt && javac --release 21 -cp '/runtime/WEB-INF/classes:/runtime/WEB-INF/lib/*:/runtime/tomcat-lib/*' -d /classes @/sources.txt
FROM bank
COPY --from=compiler /classes /usr/local/tomcat/webapps/parabank/WEB-INF/classes
COPY web.xml /usr/local/tomcat/webapps/parabank/WEB-INF/web.xml
COPY cxf.xml /usr/local/tomcat/webapps/parabank/WEB-INF/classes/cxf.xml
LABEL org.opencontainers.image.title="ParaBank local QA remediation" org.opencontainers.image.description="Patched customer ownership, atomic money updates, validation and XML root namespace; original pinned demo retained as base"
