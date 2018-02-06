#!/bin/bash
PICARD_PATH=/picard.jar
GATK_PATH=/gatk/gatk
IN_FILES=/fastq/PFC_0028/PFC_0028_SW_CGTACG_R1_001.fastq.gz,/fastq/PFC_0028/PFC_0028_SW_CGTACG_R2_001.fastq.gz,\
/fastq/PFC_0029/PFC_0029_IUH_AGTTCC_R1_001.fastq.gz,/fastq/PFC_0029/PFC_0029_IUH_AGTTCC_R2_001.fastq.gz,\
/fastq/PFC_0030/PFC_0030_MSt_GAGTGG_R1_001.fastq.gz,/fastq/PFC_0030/PFC_0030_MSt_GAGTGG_R2_001.fastq.gz,\
/fastq/PFC_0031/PFC_0031_DR_TTAGGC_R1_001.fastq.gz,/fastq/PFC_0031/PFC_0031_DR_TTAGGC_R2_001.fastq.gz,\
/fastq/PFC_0032/PFC_0032_IMc_CAGATC_R1_001.fastq.gz,/fastq/PFC_0032/PFC_0032_IMc_CAGATC_R2_001.fastq.gz,\
/fastq/PFC_0033/PFC_0033_MH_AGTTCC_R1_001.fastq.gz,/fastq/PFC_0033/PFC_0033_MH_AGTTCC_R2_001.fastq.gz

REFERENCE_FOLDER=/reference/hg19-ucsc/
KNOWN_SITES=/ngs/dbsnp1.3.8/dbsnp_138.hg19.vcf,/ngs/mills_and_1000G-hg19/Mills_and_1000G_gold_standard.indels.hg19.vcf
OUT_FOLDER=/NGS-SparkGATK/docker/run/output/
GATK_PATH_3_8=/GenomeAnalysisTK_v3.8-0-ge9d806836.jar

spark_masterID=`sudo docker container ls | awk '/spark-master/ {print $1}'`
namenodeID=`sudo docker container ls | awk '/hadoop-namenode/ {print $1}'`


#################################################################
#CREATE DIRECTORIES
dir_prepro=PREPROCESSING/
dir_vardis=VARIANTDISCOVERY/
dir_callref=CALLSETREFINEMENT/




mkdir -p $OUT_FOLDER$dir_prepro
mkdir -p $OUT_FOLDER$dir_vardis
mkdir -p $OUT_FOLDER$dir_callref


#converting fastq to ubam file
#sudo docker exec -t $spark_masterID /NGS-SparkGATK/docker/run/fastq2sam.sh $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro


#loading file to HDFS
: <<'COMMENT'
for ubam in output/$dir_prepro*_fastqtosam.bam
do
	sudo docker exec -t $namenodeID hdfs dfs -put $ubam /
done
COMMENT

#sudo docker exec -t $namenodeID hdfs dfs -put output/$dir_prepro /
#sudo docker exec -t $namenodeID hdfs dfs -put $REFERENCE_FOLDER /

sudo docker exec -t $spark_masterID bash /NGS-SparkGATK/docker/run/pipeline.sh $GATK_PATH $REFERENCE_FOLDER $OUT_FOLDER $KNOWN_SITES


