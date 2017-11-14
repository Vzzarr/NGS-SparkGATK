#!/bin/bash
PICARD_PATH=$1
GATK_PATH=$2
IN_FILES=$3
REFERENCE_FOLDER=$4
KNOWN_SITES=$5
OUT_FOLDER=$6
GATK_PATH_3_8=$7

#################################################################
#VARIANT DISCOVERY OUTPUTS
raw=raw_variants.vcf

recalSNP=recalibrate_SNP.recal
tranchesSNP=recalibrate_SNP.tranches

recalibratedSNP=recalibrated_snps_raw_indels.vcf

recalINDEL=recalibrate_INDEL.recal
tranchesINDEL=recalibrate_INDEL.tranches

recalibratedINDEL=recalibrated_variants.vcf
#################################
#		PREPROCESSING			#
#################################

#################################################################
#   GETTING INPUT PAIRED END FASTQ FILES
#IFS=',' read -a files <<< "$IN_FILES"


#   CHECKING INPUT PAIRED END FASTQ FILES
#if (( ${#files[@]} % 2 == 1 )); then
#	echo "Expected even number of files: Paired End"
#	exit
#fi

: <<'COMMENT'

#################################################################
#   GENEREATING uBAM FROM FASTQ FILES
spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar $PICARD_PATH $IN_FILES $OUT_FOLDER

#PRODUCED_UBAM=${#files[@]} / 2

#################################################################
#	SPARK PARAMETERS TO BE PASSED FOR EACH SPARK PIPELINE TOOL
#spark_params="-- --sparkRunner SPARK --sparkMaster local[*] --num-executors 5 --executor-cores 2 --executor-memory 4g"	#specify input on HDFS
#spark_params=""


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

#################################################################
#   HaplotypeCallerSpark
for ubam in $OUT_FOLDER*_recal_reads.bam
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

#################################################################
#   GenotypeGVCFs
#threads=nproc
#lscpu -p | grep -c "^[0-9]"

variants=" "

for gvcf in $OUT_FOLDER*_raw_variants.g.vcf
do
	variants="$variants --variant $gvcf "
done

java -jar $GATK_PATH_3_8 -T GenotypeGVCFs -nt 8 \
-R $REFERENCE_FOLDER*.fasta \
$variants	\
-o $OUT_FOLDER$raw
COMMENT


: <<'COMMENT'
************************************************
$GATK_PATH GenomicsDBImport \
$variants \
--genomicsDBWorkspace my_database \
--intervals 20


$GATK_PATH GenotypeGVCFs \
--reference $REFERENCE_FOLDER*.fasta	\
--variant gendb://my_database			\
--annotationGroup StandardAnnotation -newQual \
--output raw_variants.vcf

#################################################################
#SNP
#VariantRecalibrator
java -jar $GATK_PATH_3_8 -T VariantRecalibrator -nt 8 \
-R $REFERENCE_FOLDER*.fasta \
-input $OUT_FOLDER$raw \
-resource:hapmap,known=false,training=true,truth=true,prior=15.0 /data/ngs/hapmap-3.3-hg19/hapmap_3.3.hg19.vcf \
-resource:omni,known=false,training=true,truth=true,prior=12.0 /data/ngs/omni-2.5-hg19/1000G_omni2.5.hg19.vcf \
-resource:1000G,known=false,training=true,truth=false,prior=10.0 /data/ngs/1000G_phase1/1000G_phase1.indels.hg19.sites.vcf \
-resource:dbsnp,known=true,training=false,truth=false,prior=2.0 /data/ngs/dbsnp1.3.8/dbsnp_138.hg19.vcf \
-an DP -an QD -an FS -an SOR -an MQ -an MQRankSum -an ReadPosRankSum \
-mode SNP \
-tranche 100.0 -tranche 99.9 -tranche 99.0 -tranche 90.0 \
-recalFile $OUT_FOLDER$recalSNP \
-tranchesFile $OUT_FOLDER$tranchesSNP \

#ApplyVQSR
java -jar $GATK_PATH_3_8 -T ApplyRecalibration -nt 8 \
-R $REFERENCE_FOLDER*.fasta \
-input $OUT_FOLDER$raw \
-mode SNP \
--ts_filter_level 99.0 \
-recalFile $OUT_FOLDER$recalSNP \
-tranchesFile $OUT_FOLDER$tranchesSNP \
-o $OUT_FOLDER$recalibratedSNP


#################################################################
#INDEL
#VariantRecalibrator
np=$(eval "nproc")
java -jar $GATK_PATH_3_8 -T VariantRecalibrator -nt 8 \
-R $REFERENCE_FOLDER*.fasta \
-input $OUT_FOLDER$recalibratedSNP \
-resource:mills,known=false,training=true,truth=true,prior=12.0 /data/ngs/mills_and_1000G-hg19/Mills_and_1000G_gold_standard.indels.hg19.vcf \
-resource:dbsnp,known=true,training=false,truth=false,prior=2.0 /data/ngs/dbsnp1.3.8/dbsnp_138.hg19.vcf \
-an QD -an DP -an FS -an SOR -an MQRankSum -an ReadPosRankSum \
-mode INDEL \
-tranche 100.0 -tranche 99.9 -tranche 99.0 -tranche 90.0 --maxGaussians 4 \
-recalFile $OUT_FOLDER$recalINDEL \
-tranchesFile $OUT_FOLDER$tranchesINDEL \
COMMENT

#ApplyVQSR
java -jar $GATK_PATH_3_8 -T ApplyRecalibration -nt 8 \
-R $REFERENCE_FOLDER*.fasta \
-input $OUT_FOLDER$recalibratedSNP \
-mode INDEL \
--ts_filter_level 99.0 \
-recalFile $OUT_FOLDER$recalINDEL \
-tranchesFile $OUT_FOLDER$tranchesINDEL \
-o $OUT_FOLDER$recalibratedINDEL