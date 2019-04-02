FROM hseeberger/scala-sbt:11.0.2_2.12.8_1.2.8

RUN apt-get update && apt-get install -y --no-install-recommends \
    gdal-bin \
    python-gdal


EXPOSE 9000

ENV BASEDIR=/gdal2tiles2hdfs

RUN mkdir $BASEDIR
WORKDIR $BASEDIR
COPY ./service67 .
RUN sbt clean stage

ENTRYPOINT target/universal/stage/bin/service67
