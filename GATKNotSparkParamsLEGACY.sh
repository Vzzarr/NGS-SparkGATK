#!/bin/bash
#legacy version with bash commands: in the latest version these commands are implemented in Java Spark
PICARD_PATH=$1
GATK_PATH=$2
IN_FILES=$3
REFERENCE_FOLDER=$4
KNOWN_SITES=$5
OUT_FOLDER=$6
GATK_PATH_3_8=$7
ANNOVAR_FOLDER=$8
IGM_ANNO_FOLDER=$9
#################################################################
#CREATE DIRECTORIES
dir_prepro=PREPROCESSING/
dir_vardis=VARIANTDISCOVERY/
dir_callref=CALLSETREFINEMENT/

mkdir -p $OUT_FOLDER$dir_prepro
mkdir -p $OUT_FOLDER$dir_vardis
mkdir -p $OUT_FOLDER$dir_callref

np=$(nproc)
#################################################################
#VARIANT DISCOVERY OUTPUTS
raw=raw_variants.vcf

recalSNP=recalibrate_SNP.recal
tranchesSNP=recalibrate_SNP.tranches

recalibratedSNP=recalibrated_snps_raw_indels.vcf

recalINDEL=recalibrate_INDEL.recal
tranchesINDEL=recalibrate_INDEL.tranches

recalibratedINDEL=recalibrated_variants.vcf


#################################################################
#CALLSET REFINEMENT OUTPUTS
recalibrated_postCGP=recalibratedVariants.postCGP.vcf
recalibrated_postCGP_Gfiltered=recalibratedVariants.postCGP.Gfiltered.vcf
recalibrated_postCGP_Gfiltered_deNovos=recalibratedVariants.postCGP.Gfiltered.deNovos.vcf

#################################
#		PRE-PROCESSING			#
#################################

#################################################################
#   GETTING INPUT PAIRED END FASTQ FILES

#################################################################
#   GENEREATING uBAM FROM FASTQ FILES

#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro
#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master spark://127.0.0.1:7077 NGS-SparkGATK.jar FastqToSam $PICARD_PATH $IN_FILES $OUT_FOLDER$dir_prepro


#PRODUCED_UBAM=${#files[@]} / 2

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
	--emitRefConfidence GVCF					
done

#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar VariantDiscovery $GATK_PATH_3_8 $REFERENCE_FOLDER*.fasta $OUT_FOLDER$dir_prepro $OUT_FOLDER$dir_vardis


: <<'COMMENT'
#################################
#		VARIANT DISCOVERY		#
#################################
#################################################################
#   GenotypeGVCFs
variants=" "

for gvcf in $OUT_FOLDER$dir_prepro*_raw_variants.g.vcf
do
	variants="$variants --variant $gvcf "
done

java -jar $GATK_PATH_3_8 -T GenotypeGVCFs -nt "$np" \
-R $REFERENCE_FOLDER*.fasta \
$variants	\
-o $OUT_FOLDER$dir_vardis$raw


#################################################################
#SNP
#VariantRecalibrator
java -jar $GATK_PATH_3_8 -T VariantRecalibrator -nt "$np" \
-R $REFERENCE_FOLDER*.fasta \
-input $OUT_FOLDER$dir_vardis$raw \
-resource:hapmap,known=false,training=true,truth=true,prior=15.0 /data/ngs/hapmap-3.3-hg19/hapmap_3.3.hg19.vcf \
-resource:omni,known=false,training=true,truth=true,prior=12.0 /data/ngs/omni-2.5-hg19/1000G_omni2.5.hg19.vcf \
-resource:1000G,known=false,training=true,truth=false,prior=10.0 /data/ngs/1000G_phase1/1000G_phase1.indels.hg19.sites.vcf \
-resource:dbsnp,known=true,training=false,truth=false,prior=2.0 /data/ngs/dbsnp1.3.8/dbsnp_138.hg19.vcf \
-an DP -an QD -an FS -an SOR -an MQ -an MQRankSum -an ReadPosRankSum \
-mode SNP \
-tranche 100.0 -tranche 99.9 -tranche 99.0 -tranche 90.0 \
-recalFile $OUT_FOLDER$dir_vardis$recalSNP \
-tranchesFile $OUT_FOLDER$dir_vardis$tranchesSNP \

#ApplyVQSR
java -jar $GATK_PATH_3_8 -T ApplyRecalibration -nt "$np" \
-R $REFERENCE_FOLDER*.fasta \
-input $OUT_FOLDER$dir_vardis$raw \
-mode SNP \
--ts_filter_level 99.0 \
-recalFile $OUT_FOLDER$dir_vardis$recalSNP \
-tranchesFile $OUT_FOLDER$dir_vardis$tranchesSNP \
-o $OUT_FOLDER$dir_vardis$recalibratedSNP


#################################################################
#INDEL
#VariantRecalibrator
java -jar $GATK_PATH_3_8 -T VariantRecalibrator -nt "$np" \
-R $REFERENCE_FOLDER*.fasta \
-input $OUT_FOLDER$dir_vardis$recalibratedSNP \
-resource:mills,known=false,training=true,truth=true,prior=12.0 /data/ngs/mills_and_1000G-hg19/Mills_and_1000G_gold_standard.indels.hg19.vcf \
-resource:dbsnp,known=true,training=false,truth=false,prior=2.0 /data/ngs/dbsnp1.3.8/dbsnp_138.hg19.vcf \
-an QD -an DP -an FS -an SOR -an MQRankSum -an ReadPosRankSum \
-mode INDEL \
-tranche 100.0 -tranche 99.9 -tranche 99.0 -tranche 90.0 --maxGaussians 4 \
-recalFile $OUT_FOLDER$dir_vardis$recalINDEL \
-tranchesFile $OUT_FOLDER$dir_vardis$tranchesINDEL \


#ApplyVQSR
java -jar $GATK_PATH_3_8 -T ApplyRecalibration -nt "$np" \
-R $REFERENCE_FOLDER*.fasta \
-input $OUT_FOLDER$dir_vardis$recalibratedSNP \
-mode INDEL \
--ts_filter_level 99.0 \
-recalFile $OUT_FOLDER$dir_vardis$recalINDEL \
-tranchesFile $OUT_FOLDER$dir_vardis$tranchesINDEL \
-o $OUT_FOLDER$dir_vardis$recalibratedINDEL
COMMENT

#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar CallsetRefinement $GATK_PATH_3_8 $REFERENCE_FOLDER*.fasta $OUT_FOLDER$dir_vardis $OUT_FOLDER$dir_callref

: <<'COMMENT'

#################################
#		CALLSET REFINEMENT		#
#################################
#Refine GTs

java -jar $GATK_PATH_3_8 -T CalculateGenotypePosteriors \
-R $REFERENCE_FOLDER*.fasta \
--supporting /data/ngs/1000G_phase1/1000G_phase1.indels.hg19.sites.vcf \
-V $OUT_FOLDER$dir_vardis$recalibratedINDEL \
-o $OUT_FOLDER$dir_callref$recalibrated_postCGP
#-ped trio.ped 

java -jar $GATK_PATH_3_8 -T VariantFiltration \
-R $REFERENCE_FOLDER*.fasta \
-V $OUT_FOLDER$dir_callref$recalibrated_postCGP \
-G_filter "GQ < 20.0" -G_filterName lowGQ \
-o $OUT_FOLDER$dir_callref$recalibrated_postCGP_Gfiltered


java -jar $GATK_PATH_3_8 -T VariantAnnotator -nt "$np" \
-R $REFERENCE_FOLDER*.fasta \
-V $OUT_FOLDER$dir_callref$recalibrated_postCGP_Gfiltered \
-A PossibleDeNovo \
-o $OUT_FOLDER$dir_callref$recalibrated_postCGP_Gfiltered_deNovos
#-ped trio.ped
COMMENT


vcf=".vcf"

ConvertCmd=lib/convert2annovar.pl
ConvertedFile=converted.ann

TableAnnovarCmd=lib/table_annovar.pl
HumanDB=humandb/
AnnovarOutputFormat=.hg19_multianno.txt

AnnotateIGMCmd=src/main/annotate.pl
IGMOutputFormat=igm_anno.txt

ExonicFilterFormat=exonic_filtered.txt

#SPLITTING deNovo.vcf
result=$(cat $OUT_FOLDER$dir_callref$recalibrated_postCGP_Gfiltered_deNovos | egrep '^#CHROM')
IFS=$'\t' read -a files <<< "$result"

for (( i=10; i<${#files[@]}+1; i++ ));	#10 because since 10 there are fields of file names
do
: <<'COMMENT'

	#Splitting deNovos.vcf in more files, according to the sample name
	java -jar $GATK_PATH_3_8 -T SelectVariants -nt "$np" \
	-R $REFERENCE_FOLDER*.fasta \
	-V $OUT_FOLDER$dir_callref$recalibrated_postCGP_Gfiltered_deNovos \
	-o $OUT_FOLDER$dir_callref${files[$i-1]}$vcf \
	-sn ${files[$i-1]}

	#removing rows with *
	sed -i '/\*/d' $OUT_FOLDER$dir_callref${files[$i-1]}$vcf

	#Converting to AnnoVar format
	perl $ANNOVAR_FOLDER$ConvertCmd -format vcf4old -includeinfo $OUT_FOLDER$dir_callref${files[$i-1]}$vcf > $OUT_FOLDER$dir_callref${files[$i-1]}$ConvertedFile

	#AnnoVar annotation
	perl $ANNOVAR_FOLDER$TableAnnovarCmd -remove -otherinfo -buildver hg19 \
	-protocol knownGene,ensGene,refGene,phastConsElements46way,genomicSuperDups,esp6500si_all,1000g2012apr_all,cg69,snp137,ljb26_all \
	-operation g,g,g,r,r,f,f,f,f,f $OUT_FOLDER$dir_callref${files[$i-1]}$ConvertedFile $ANNOVAR_FOLDER$HumanDB
	
	#IGM Annotation
	perl $IGM_ANNO_FOLDER$AnnotateIGMCmd -samples ${files[$i-1]} \
	-avoutput $OUT_FOLDER$dir_callref${files[$i-1]}$ConvertedFile$AnnovarOutputFormat \
	-out $OUT_FOLDER$dir_callref${files[$i-1]}$IGMOutputFormat
COMMENT
	#spark-submit --class uk.ac.ncl.NGS_SparkGATK.Pipeline --master local[*] NGS-SparkGATK.jar ExonicFilter $OUT_FOLDER$dir_callref${files[$i-1]}$IGMOutputFormat $OUT_FOLDER$dir_callref${files[$i-1]}$ExonicFilterFormat


done




#Annotate Variants

#convert
#perl $ANNOVAR_FOLDER$ConvertCmd -format vcf4old -includeinfo $OUT_FOLDER${files[$i-1]}$vcf > $OUT_FOLDER${files[$i-1]}$ConvertedFile

#perl $ANNOVAR_FOLDER$TableAnnovarCmd -remove -otherinfo -buildver hg19 \
#-protocol knownGene,ensGene,refGene,phastConsElements46way,genomicSuperDups,esp6500si_all,1000g2012apr_all,cg69,snp137,ljb26_all \
#-operation g,g,g,r,r,f,f,f,f,f $OUT_FOLDER$ConvertedFile $ANNOVAR_FOLDER$HumanDB


#IGM-Anno/src/main/annotate.pl