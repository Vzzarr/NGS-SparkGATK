#!/bin/bash
PICARD_PATH=$1
IN_FILES=$2
OUT_FOLDER=$3
#################################################################
#CREATE DIRECTORIES
dir_prepro=PREPROCESSING/
dir_vardis=VARIANTDISCOVERY/
dir_callref=CALLSETREFINEMENT/

mkdir -p $OUT_FOLDER$dir_prepro
mkdir -p $OUT_FOLDER$dir_vardis
mkdir -p $OUT_FOLDER$dir_callref


#################################
#		PRE-PROCESSING			#
#################################

#################################################################
#   GENEREATING uBAM FROM FASTQ FILES

/spark/bin/spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] /NGS-SparkGATK/docker/run/NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro

