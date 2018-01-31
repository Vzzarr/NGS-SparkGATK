#!/bin/bash
PICARD_PATH=/picard.jar
IN_FILES=/fastq/chr21_1.fq.gz,/fastq/chr21_2.fq.gz
OUT_FOLDER=/NGS-SparkGATK/docker/run/output/
GATK_PATH=/gatk/gatk
REFERENCE_FOLDER=/reference/hg19-ucsc/

#################################################################
#CREATE DIRECTORIES
dir_prepro=PREPROCESSING/
dir_vardis=VARIANTDISCOVERY/
dir_callref=CALLSETREFINEMENT/

mkdir -p $OUT_FOLDER$dir_prepro
mkdir -p $OUT_FOLDER$dir_vardis
mkdir -p $OUT_FOLDER$dir_callref

: <<'COMMENT'

#################################
#		PRE-PROCESSING			#
#################################

#################################################################
#   GENEREATING uBAM FROM FASTQ FILES

spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] /NGS-SparkGATK/docker/run/NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro

#loading file to HDFS
hdfs dfs -mkdir /output/
hdfs dfs -mkdir /output/PREPROCESSING/

for ubam in $OUT_FOLDER/PREPROCESSING/*_fastqtosam.bam
do
	hdfs dfs -put $ubam /
done
COMMENT


#################################################################
#   BwaAndMarkDuplicatesPipelineSpark
for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	output="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"

	$GATK_PATH BwaAndMarkDuplicatesPipelineSpark  \
	--input hdfs://namenode:8020/$ubam \
	--reference $REFERENCE_FOLDER*.fasta \
	--disable-sequence-dictionary-validation true \
	--output hdfs://namenode:8020/$output -- \
	--spark-runner SPARK --spark-master local[*] \
	--driver-memory 10g --executor-cores 2 --executor-memory 8g
done


#/gatk/gatk BwaAndMarkDuplicatesPipelineSpark --input hdfs://namenode:8020/chr21__fastqtosam.bam --reference /reference/hg19-ucsc/ucsc.hg19.fasta --disable-sequence-dictionary-validation true --output hdfs://namenode:8020/PREPROCESSING/chr21__dedup_reads.bam -- --spark-runner SPARK --spark-master local[*] --driver-memory 20g --executor-cores 4 --executor-memory 32g



#/data0/gatk/gatk BwaAndMarkDuplicatesPipelineSpark --input hdfs://namenode:8020/PREPROCESSING/chr21__fastqtosam.bam --reference /data0/reference/hg19-ucsc/ucsc.hg19.fasta --disable-sequence-dictionary-validation true --output hdfs://namenode:8020/PREPROCESSING/chr21__dedup_reads.bam -- --spark-runner SPARK --spark-master local[*]
