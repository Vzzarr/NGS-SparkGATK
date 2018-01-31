#!/bin/bash
GATK_PATH=/gatk/gatk
REFERENCE_FOLDER=/reference/hg19-ucsc/

: <<'COMMENT'
#################################################################
#   BwaAndMarkDuplicatesPipelineSpark
for ubam in output/PREPROCESSING/*_fastqtosam.bam
do
	output="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"

	$GATK_PATH BwaAndMarkDuplicatesPipelineSpark  \
	--input hdfs://namenode:8020/$ubam \
	--reference $REFERENCE_FOLDER*.fasta \
	--disableSequenceDictionaryValidation true \
	--output hdfs://namenode:8020/$output -- \
	--spark-runner SPARK --spark-master local[*] \
	--driver-memory 10g --executor-cores 2 --executor-memory 8g
done
COMMENT

/gatk/gatk BwaAndMarkDuplicatesPipelineSpark --input hdfs://namenode:8020/chr21__fastqtosam.bam --reference /reference/hg19-ucsc/ucsc.hg19.fasta --disable-sequence-dictionary-validation true --output hdfs://namenode:8020/PREPROCESSING/chr21__dedup_reads.bam -- --spark-runner SPARK --spark-master local[*] --driver-memory 20g --executor-cores 4 --executor-memory 32g



#/data0/gatk/gatk BwaAndMarkDuplicatesPipelineSpark --input hdfs://namenode:8020/PREPROCESSING/chr21__fastqtosam.bam --reference /data0/reference/hg19-ucsc/ucsc.hg19.fasta --disable-sequence-dictionary-validation true --output hdfs://namenode:8020/PREPROCESSING/chr21__dedup_reads.bam -- --spark-runner SPARK --spark-master local[*]
