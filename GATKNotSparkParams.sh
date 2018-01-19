#!/bin/bash
PICARD_PATH=$1
GATK_PATH=$2
IN_FILES=$3
REFERENCE_FOLDER=$4
KNOWN_SITES=$5
OUT_FOLDER=$6
GATK_PATH_3_8=$7
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
#   GETTING INPUT PAIRED END FASTQ FILES

#################################################################
#   GENEREATING uBAM FROM FASTQ FILES

#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro
#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master spark://127.0.0.1:7077 NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro


#################################################################
#	SPARK PARAMETERS TO BE PASSED FOR EACH SPARK PIPELINE TOOL
#spark_params="-- --sparkRunner SPARK --sparkMaster local[*] --num-executors 5 --executor-cores 2 --executor-memory 4g"	#specify input on HDFS
#spark_params=""


#################################################################
#   BwaAndMarkDuplicatesPipelineSpark
for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	output="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"
	
	$GATK_PATH BwaAndMarkDuplicatesPipelineSpark  \
	--input $ubam \
	--reference $REFERENCE_FOLDER*.fasta \
	--disableSequenceDictionaryValidation true \
	--output $output 
done

: <<'COMMENT'


#################################################################
#   BQSRPipelineSpark
IFS=',' read -a knownSites <<< "$KNOWN_SITES"

known=" "

for k in "${knownSites[@]}"
do
   : 
   known="$known --knownSites $k "
done

for ubam in $OUT_FOLDER$dir_prepro*_dedup_reads.bam
do
	output="${ubam/_dedup_reads.bam/'_recal_reads.bam'}"
	$GATK_PATH BQSRPipelineSpark				\
	--input $ubam								\
	--reference $REFERENCE_FOLDER*.2bit 		\
	--output $output							\
	--disableSequenceDictionaryValidation true	\
	$known										
done


#################################################################
#   HaplotypeCallerSpark
for ubam in $OUT_FOLDER$dir_prepro*_recal_reads.bam
do
	output="${ubam/_recal_reads.bam/'_raw_variants.g.vcf'}"
	$GATK_PATH HaplotypeCallerSpark				\
	--input $ubam								\
	--reference $REFERENCE_FOLDER*.2bit 		\
	--output $output							\
	--emitRefConfidence GVCF					
done



#################################
#		VARIANT DISCOVERY		#
#################################
spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar VariantDiscovery $GATK_PATH_3_8 $REFERENCE_FOLDER*.fasta $OUT_FOLDER$dir_prepro $OUT_FOLDER$dir_vardis

#################################
#		CALLSET REFINEMENT		#
#################################
spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar CallsetRefinement $GATK_PATH_3_8 $REFERENCE_FOLDER*.fasta $OUT_FOLDER$dir_vardis $OUT_FOLDER$dir_callref
COMMENT