FROM bde2020/spark-master

MAINTAINER Nicholas Tucci <nich.tucci@gmail.com>

COPY picard.jar /
COPY GenomeAnalysisTK_v3.8-0-ge9d806836.jar /

RUN git clone https://github.com/broadinstitute/gatk.git && cd /gatk && ./gradlew bundle

ENV PATH="$PATH:/spark/bin"

