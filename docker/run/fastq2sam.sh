#!/bin/bash
PICARD_PATH=$1
IN_FILES=$2
OUT_FOLDER=$3
#SPARK_MASTER_HOST=`hostname`

#################################################################
#   GENEREATING uBAM FROM FASTQ FILES
spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] /NGS-SparkGATK/docker/run/NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER

#spark://$SPARK_MASTER_HOST:7077
#can't be executed in distrubuted mode because of creating the //FastqToSam.sh file


