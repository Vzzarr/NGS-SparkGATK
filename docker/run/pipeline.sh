#!/bin/bash
GATK_PATH=$1
REFERENCE_FOLDER=$2
OUT_FOLDER=$3
KNOWN_SITES=$4

dir_prepro=PREPROCESSING/
dir_vardis=VARIANTDISCOVERY/
dir_callref=CALLSETREFINEMENT/
SPARK_MASTER_HOST=`hostname`

$GATK_PATH BwaAndMarkDuplicatesPipelineSpark --input hdfs://namenode:8020/PREPROCESSING/PFC_0028_SW_CGTACG_R_fastqtosam.bam \
--reference hdfs://namenode:8020/hg19-ucsc/ucsc.hg19.fasta --disable-sequence-dictionary-validation true \
--output hdfs://namenode:8020/PREPROCESSING/PFC_0028_SW_CGTACG_R_dedup_reads.bam \
-- --spark-runner SPARK --spark-master spark://$SPARK_MASTER_HOST:7077 --driver-memory 30g --executor-cores 4 --executor-memory 15g




: <<'COMMENT'

#################################################################
#   BwaAndMarkDuplicatesPipelineSpark
for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	ubam=${ubam##*/}	#getting only the file name without path
	output="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"


	$GATK_PATH BwaAndMarkDuplicatesPipelineSpark  \
	--input hdfs://namenode:8020/$ubam \
	--reference $REFERENCE_FOLDER*.fasta \
	--disable-sequence-dictionary-validation true \
	--output hdfs://namenode:8020/$output -- \
	--spark-runner SPARK --spark-master spark://$SPARK_MASTER_HOST:7077 \
	--driver-memory 30g --executor-cores 4 --executor-memory 15g


	#local[*]
done


#################################################################
#   BQSRPipelineSpark
#create knownsites field
IFS=',' read -a knownSites <<< "$KNOWN_SITES"
known=" "
for k in "${knownSites[@]}"
do
   : 
   known="$known --known-sites $k "
done

for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	ubam=${ubam##*/}
	ubam="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"
	output="${ubam/_dedup_reads.bam/'_recal_reads.bam'}"


	$GATK_PATH BQSRPipelineSpark				\
	--input hdfs://namenode:8020/$ubam			\
	--reference $REFERENCE_FOLDER*.2bit 		\
	--output hdfs://namenode:8020/$output		\
	--disable-sequence-dictionary-validation true	\
	$known -- \
	--spark-runner SPARK --spark-master local[*] \
	--driver-memory 30g --executor-cores 4 --executor-memory 15g
done




#################################################################
#   HaplotypeCallerSpark
for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	ubam=${ubam##*/}
	ubam="${ubam/_fastqtosam.bam/'_recal_reads.bam'}"
	output="${ubam/_recal_reads.bam/'_raw_variants.g.vcf'}"

	#saving on FS because the following step (GenotypeGVCFs) is not implemented in Spark
	$GATK_PATH HaplotypeCallerSpark				\
	--input hdfs://namenode:8020/$ubam			\
	--reference $REFERENCE_FOLDER*.2bit 		\
	--output $OUT_FOLDER$dir_prepro$output		\
	--emit-ref-confidence GVCF -- \
	--spark-runner SPARK --spark-master local[*] \
	--driver-memory 30g --executor-cores 4 --executor-memory 15g

done

#################################
#		VARIANT DISCOVERY		#
#################################
spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] /NGS-SparkGATK/docker/run/NGS-SparkGATK.jar VariantDiscovery $GATK_PATH_3_8 $REFERENCE_FOLDER*.fasta $OUT_FOLDER$dir_prepro $OUT_FOLDER$dir_vardis

#################################
#		CALLSET REFINEMENT		#
#################################
spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] /NGS-SparkGATK/docker/run/NGS-SparkGATK.jar CallsetRefinement $GATK_PATH_3_8 $REFERENCE_FOLDER*.fasta $OUT_FOLDER$dir_vardis $OUT_FOLDER$dir_callref
COMMENT
