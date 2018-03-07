#!/bin/bash
PICARD_PATH=/datadrive/libraries/picard.jar
GATK_PATH=/datadrive/libraries/gatk/gatk
IN_FILES=/datadrive/fastq/PFC_0028/PFC_0028_SW_CGTACG_R1_001.fastq.gz,/datadrive/fastq/PFC_0028/PFC_0028_SW_CGTACG_R2_001.fastq.gz
REFERENCE_FOLDER=/datadrive/reference/hg19-ucsc/
KNOWN_SITES=/datadrive/libraries/mills_and_1000G-hg19/Mills_and_1000G_gold_standard.indels.hg19.vcf,/datadrive/libraries/dbsnp1.3.8/dbsnp_138.hg19.vcf
OUT_FOLDER=/datadrive/NGS-SparkGATK/output/
GATK_PATH_3_8=/datadrive/libraries/GenomeAnalysisTK_v3.8-0-ge9d806836.jar
#################################################################
#CREATE DIRECTORIES
dir_prepro=PREPROCESSING/
dir_vardis=VARIANTDISCOVERY/
dir_callref=CALLSETREFINEMENT/

sudo mkdir -p $OUT_FOLDER$dir_prepro
sudo mkdir -p $OUT_FOLDER$dir_vardis
sudo mkdir -p $OUT_FOLDER$dir_callref


#################################
#		PRE-PROCESSING			#
#################################

#################################################################
#   GETTING INPUT PAIRED END FASTQ FILES

#################################################################
#   GENEREATING uBAM FROM FASTQ FILES

#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro
#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master spark://127.0.0.1:7077 NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro

#Load the produced uBAM file to HDFS, after that the GATK tools should be able to read from HDFS


#################################################################
#	SPARK PARAMETERS TO BE PASSED FOR EACH SPARK PIPELINE TOOL
#spark_params="-- --sparkRunner SPARK --sparkMaster local[*] --num-executors 5 --executor-cores 2 --executor-memory 4g"	#specify input on HDFS
#spark_params=""


: <<'COMMENT'
#################################################################
#   BwaAndMarkDuplicatesPipelineSpark
for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	output="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"
	
	$GATK_PATH BwaAndMarkDuplicatesPipelineSpark  \
	--input $ubam \
	--reference $REFERENCE_FOLDER*.fasta \
	--disable-sequence-dictionary-validation true \
	--output $output 
done
COMMENT

: <<'COMMENT'

#################################################################
#   BQSRPipelineSpark
IFS=',' read -a knownSites <<< "$KNOWN_SITES"

known=" "

for k in "${knownSites[@]}"
do
   : 
   known="$known --known-sites $k "
done

for ubam in $OUT_FOLDER$dir_prepro*_dedup_reads.bam
do
	output="${ubam/_dedup_reads.bam/'_recal_reads.bam'}"
	$GATK_PATH BQSRPipelineSpark				\
	--input $ubam								\
	--reference $REFERENCE_FOLDER*.2bit 		\
	--output $output							\
	--disable-sequence-dictionary-validation true	\
	$known										
done
COMMENT
#################################################################
#   HaplotypeCallerSpark
for ubam in $OUT_FOLDER$dir_prepro*_recal_reads.bam
do
	output="${ubam/_recal_reads.bam/'_raw_variants.g.vcf'}"
	$GATK_PATH HaplotypeCallerSpark				\
	--input $ubam								\
	--reference $REFERENCE_FOLDER*.2bit 		\
	--output $output							\
	--emit-ref-confidence GVCF					
done

: <<'COMMENT'

#################################
#		VARIANT DISCOVERY		#
#################################
spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar VariantDiscovery $GATK_PATH_3_8 $REFERENCE_FOLDER*.fasta $OUT_FOLDER$dir_prepro $OUT_FOLDER$dir_vardis

#################################
#		CALLSET REFINEMENT		#
#################################
spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar CallsetRefinement $GATK_PATH_3_8 $REFERENCE_FOLDER*.fasta $OUT_FOLDER$dir_vardis $OUT_FOLDER$dir_callref
COMMENT

