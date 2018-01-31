#!/bin/bash
GATK_PATH=/gatk/gatk
REFERENCE_FOLDER=/reference/hg19-ucsc/

spark_masterID=`sudo docker container ls | awk '/spark-master/ {print $1}'`
namenodeID=`sudo docker container ls | awk '/hadoop-namenode/ {print $1}'`

: <<'COMMENT'
#converting fastq to ubam file
sudo docker exec -it $spark_masterID /NGS-SparkGATK/docker/run/fastq2sam.sh

#loading file to HDFS
sudo docker exec -it $namenodeID hdfs dfs -mkdir /output/
sudo docker exec -it $namenodeID hdfs dfs -mkdir /output/PREPROCESSING/

for ubam in output/PREPROCESSING/*_fastqtosam.bam
do
	sudo docker exec -it $namenodeID hdfs dfs -put $ubam /
done

COMMENT

#################################################################
#   BwaAndMarkDuplicatesPipelineSpark
sudo docker exec -it $spark_masterID bash /NGS-SparkGATK/docker/run/preprocessing.sh



