#!/bin/bash
PICARD_PATH=$1
GATK_PATH=$2
IN_FILES=$3
REFERENCE_FOLDER=$4
KNOWN_SITES=$5
OUT_FOLDER=$6

#################################################################
#   GETTING INPUT PAIRED END FASTQ FILES
IFS=',' read -a files <<< "$IN_FILES"

#   CHECKING INPUT PAIRED END FASTQ FILES
if (( ${#files[@]} % 2 == 1 )); then
	echo "Expected even number of files: Paired End"
	exit
fi



spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar $PICARD_PATH $IN_FILES $OUT_FOLDER

#PRODUCED_UBAM=${#files[@]} / 2




#################################################################
#   BwaAndMarkDuplicatesPipelineSpark

for ubam in $OUT_FOLDER*_fastqtosam.bam
do
	output="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"
	
	$GATK_PATH BwaAndMarkDuplicatesPipelineSpark  \
	--input $ubam \
	--reference $REFERENCE_FOLDER*.fasta \
	--disableSequenceDictionaryValidation true \
	--output $output
done

#################################################################
#   BQSRPipelineSpark
IFS=',' read -a knownSites <<< "$KNOWN_SITES"

known=" "

for k in "${knownSites[@]}"
do
   : 
   known="$known --knownSites $k "
done

for ubam in $OUT_FOLDER*_dedup_reads.bam
do
	output="${ubam/_dedup_reads.bam/'_recal_reads.bam'}"
	$GATK_PATH BQSRPipelineSpark				\
	--input $ubam								\
	--reference $REFERENCE_FOLDER*.2bit 		\
	--output $output							\
	--disableSequenceDictionaryValidation true	\
	$known	
done