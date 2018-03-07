FROM bde2020/spark-master:2.2.0-hadoop2.8-hive-java8

MAINTAINER Nicholas Tucci <nich.tucci@gmail.com>

COPY picard.jar /
COPY GenomeAnalysisTK_v3.8-0-ge9d806836.jar /

RUN apt-get update && apt-get install -y python git mlocate htop && export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8 && \
    git clone https://github.com/broadinstitute/gatk.git && cd /gatk && git checkout 2f8939710d63729341708ed5dee9417e83adce2f && \
    ./gradlew bundle && cp /spark/conf/spark-defaults.conf.template /spark/conf/spark-defaults.conf && \
    echo "spark.eventLog.enabled             true" >> /spark/conf/spark-defaults.conf && \
    echo "spark.eventLog.dir                 file:///spark/logs/" >> /spark/conf/spark-defaults.conf

ENV PATH="$PATH:/spark/bin"

