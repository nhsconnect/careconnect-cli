FROM openjdk:11-slim
VOLUME /tmp

ADD target/cc-cli.jar cc-cli.jar

# Copy the EntryPoint
COPY ./entryPoint.sh /
RUN chmod +x /entryPoint.sh

ENTRYPOINT ["/entryPoint.sh"]


