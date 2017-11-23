package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.File;
import java.util.LinkedList;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class VariantDiscovery extends AbstractGATKParallel {

	private String gatk3_8path;
	private String referenceFile;
	
	private String inFolder;
	private String outFolder;
	
	public VariantDiscovery(String gatk3_8path, String referenceFile, String inFolder, String outFolder) {
		this.gatk3_8path = gatk3_8path;
		this.referenceFile = referenceFile;
		this.inFolder = inFolder;
		this.outFolder = outFolder;
	}

	@Override
	public void run(JavaSparkContext sc) {
		genotypeGVCFs(sc);

	}

	private void genotypeGVCFs(JavaSparkContext sc) {
		File[] gvcfs = new File(this.inFolder).listFiles(gvcf -> gvcf.getName().endsWith("_raw_variants.g.vcf"));
		String variants = " ";
		for (File gvcf : gvcfs)
			variants += " --variant " + gvcf.getAbsolutePath();
		
		super.gatkCommand = "java -jar " + this.gatk3_8path + " -T GenotypeGVCFs " + 
				"-R " + this.referenceFile + variants + " -o " + this.outFolder + "raw_variants.vcf";
		JavaRDD<String> bashExec = sc.parallelize(new LinkedList<>()).pipe(super.gatkCommand);
		
		for (String string : bashExec.collect()) 
			System.out.println(string);

	}

	/**/
	private void variantRecalibratorSNP() {

	}
	
	private void ApplyRecalibrationSNP() {

	}
	

	/**/	
	private void variantRecalibratorINDEL() {

	}
	
	private void ApplyRecalibrationINDEL() {

	}
}



/*
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

 */