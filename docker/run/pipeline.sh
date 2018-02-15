#!/bin/bash
GATK_PATH=$1
REFERENCE_FOLDER=$2
OUT_FOLDER=$3
known=$4

dir_prepro=PREPROCESSING/
dir_vardis=VARIANTDISCOVERY/
dir_callref=CALLSETREFINEMENT/
SPARK_MASTER_HOST=`hostname`

: <<'COMMENT'
$GATK_PATH BwaAndMarkDuplicatesPipelineSpark --input hdfs://namenode:8020/PFC_0028_SW_CGTACG_R_fastqtosam.bam \
--reference hdfs://namenode:8020/hg19-ucsc/ucsc.hg19.2bit --bwa-mem-index-image /reference_image/ucsc.hg19.fasta.img \
--disable-sequence-dictionary-validation true --output hdfs://namenode:8020/PFC_0028_SW_CGTACG_R_dedup_reads.bam \
-- --spark-runner SPARK --spark-master spark://$SPARK_MASTER_HOST:7077 --driver-memory 30g --executor-cores 4 --executor-memory 15g

COMMENT

#TODO calculate the right number of Spark resources		--bamPartitionSize

#################################################################
#   BwaAndMarkDuplicatesPipelineSpark
for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	ubam=${ubam##*/}	#getting only the file name without path
	output="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"


	$GATK_PATH BwaAndMarkDuplicatesPipelineSpark  \
	--input hdfs://namenode:8020/$dir_prepro$ubam \
	--reference hdfs://namenode:8020/hg19-ucsc/ucsc.hg19.2bit \
	--bwa-mem-index-image /reference_image/ucsc.hg19.fasta.img \
	--disable-sequence-dictionary-validation true \
	--output hdfs://namenode:8020/$dir_prepro$output -- \
	--spark-runner SPARK --spark-master spark://$SPARK_MASTER_HOST:7077 \
	--driver-memory 30g --executor-cores 4 --executor-memory 15g
done


#30 4 15
#--num-executors 7 

: <<'COMMENT'
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
COMMENT

: <<'COMMENT'
for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	ubam=${ubam##*/}
	ubam="${ubam/_fastqtosam.bam/'_dedup_reads.bam'}"
	output="${ubam/_dedup_reads.bam/'_recal_reads.bam'}"


	$GATK_PATH BQSRPipelineSpark								\
	--input hdfs://namenode:8020/$dir_prepro$ubam				\
	--reference hdfs://namenode:8020/hg19-ucsc/ucsc.hg19.2bit	\
	--output hdfs://namenode:8020/$dir_prepro$output			\
	--disable-sequence-dictionary-validation true				\
	$known -- \
	--spark-runner SPARK --spark-master spark://$SPARK_MASTER_HOST:7077 \
	--driver-memory 10g --executor-cores 2 --executor-memory 8g
done
COMMENT
: <<'COMMENT'
#################################################################
#   HaplotypeCallerSpark
for ubam in $OUT_FOLDER$dir_prepro*_fastqtosam.bam
do
	ubam=${ubam##*/}
	ubam="${ubam/_fastqtosam.bam/'_recal_reads.bam'}"
	output="${ubam/_recal_reads.bam/'_raw_variants.g.vcf'}"

	#saving on FS because the following step (GenotypeGVCFs) is not implemented in Spark
	$GATK_PATH HaplotypeCallerSpark				\
	--input hdfs://namenode:8020/$dir_prepro$ubam			\
	--reference hdfs://namenode:8020/hg19-ucsc/ucsc.hg19.2bit 		\
	--output $OUT_FOLDER$dir_prepro$output		\
	--emit-ref-confidence GVCF -- \
	--spark-runner SPARK --spark-master spark://$SPARK_MASTER_HOST:7077 \
	--driver-memory 20g --executor-cores 2 --executor-memory 8g

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
