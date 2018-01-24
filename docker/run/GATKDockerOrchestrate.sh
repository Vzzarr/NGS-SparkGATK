#!/bin/bash

#converting fastq to ubam file
spark_masterID=`sudo docker container ls | awk '/spark-master/ {print $1}'`
#sudo docker exec -it $spark_masterID /NGS-SparkGATK/docker/run/f2s.sh

#loading file to HDFS
namenodeID=`sudo docker container ls | awk '/hadoop-namenode/ {print $1}'`
#sudo docker exec -it $namenodeID hdfs dfs -mkdir /PREPROCESSING

for ubam in /NGS-SparkGATK/docker/run/output/*_fastqtosam.bam	#TODO resolve issue here
do
	sudo docker exec -it $namenodeID hdfs dfs -put $ubam /PREPROCESSING
done


