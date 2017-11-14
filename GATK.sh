#!/bin/bash
PICARD_PATH=$1
GATK_PATH=$2
IN_FILES=$3
REFERENCE_FOLDER=$4
KNOWN_SITES=$5
OUT_FOLDER=$6

#################################################################
#   GETTING INPUT PAIRED END FASTQ FILES
#IFS=',' read -a files <<< "$IN_FILES"

#   CHECKING INPUT PAIRED END FASTQ FILES
#if (( ${#files[@]} % 2 == 1 )); then
#	echo "Expected even number of files: Paired End"
#	exit
#fi


#################################################################
#   GENEREATING uBAM FROM FASTQ FILES
#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] \
#NGS-SparkGATK.jar $PICARD_PATH $IN_FILES $OUT_FOLDER

#PRODUCED_UBAM=${#files[@]} / 2

#################################################################
#	SPARK PARAMETERS TO BE PASSED FOR EACH SPARK PIPELINE TOOL
#spark_params="-- --sparkRunner SPARK --sparkMaster local[*] --driver-memory 20g --num-executors 8 --executor-cores 1 --executor-memory 1g"	#specify input on HDFS
#spark_params=""


#################################################################
#   BwaAndMarkDuplicatesPipelineSpark
ubam=($(/data0/hadoop-2.8.1/bin/hdfs dfs -ls / | egrep '_fastqtosam.bam$'))

i=7
while [  $i -lt ${#ubam[@]} ]; do
	u=${ubam[$i]}
	output="${u/_fastqtosam.bam/'_dedup_reads.bam'}"
	
	$GATK_PATH BwaAndMarkDuplicatesPipelineSpark  \
	--input ${ubam[$i]} \
	--reference $REFERENCE_FOLDER*.fasta \
	--disableSequenceDictionaryValidation true \
	--output $output #\
	#$spark_params
done

#################################################################
#   BQSRPipelineSpark
if [[ $KNOWN_SITES == *","* ]]; then
	IFS=',' read -a knownSites <<< "$KNOWN_SITES"
	known=" "

	for k in "${knownSites[@]}"
	do
	   : 
	   known="$known --knownSites $k "
	done
else
        known=" --knownSites $KNOWN_SITES "
fi

ubam=($(/data0/hadoop-2.8.1/bin/hdfs dfs -ls / | egrep '_dedup_reads.bam$'))

i=7
while [  $i -lt ${#ubam[@]} ]; do
	u=${ubam[$i]}
	output="${u/_dedup_reads.bam/'_recal_reads.bam'}"
	
	$GATK_PATH BQSRPipelineSpark  \
	--input ${ubam[$i]} \
	--reference $REFERENCE_FOLDER*.2bit \
	--output $output \
	--disableSequenceDictionaryValidation true \
	$known										#\
	#$spark_params
done

#################################################################
#   HaplotypeCallerSpark
ubam=($(/data0/hadoop-2.8.1/bin/hdfs dfs -ls / | egrep '_recal_reads.bam$'))

i=7
while [  $i -lt ${#ubam[@]} ]; do
	u=${ubam[$i]}
	output="${u/_recal_reads.bam/'_raw_variants.g.vcf'}"
	
	$GATK_PATH HaplotypeCallerSpark  \
	--input ${ubam[$i]} \
	--reference $REFERENCE_FOLDER*.2bit \
	--output $output \
	--emitRefConfidence GVCF					#\
	#$spark_params
done

#################################################################
#   GenotypeGVCFs
: <<'COMMENT'

variants=" "

for gvcf in $OUT_FOLDER*_raw_variants.g.vcf
do
	variants="$variants --variant $gvcf"
done

output="${ubam/_raw_variants.g.vcf/'_raw_variants.vcf'}"

$GATK_PATH --javaOptions "-Xmx4g" GenotypeGVCFs \
--reference $REFERENCE_FOLDER*.fasta			\
$variants --output $output
COMMENT

#SNP
#VariantRecalibrator
#ApplyVQSR

#INDEL
#VariantRecalibrator
#ApplyVQSR

