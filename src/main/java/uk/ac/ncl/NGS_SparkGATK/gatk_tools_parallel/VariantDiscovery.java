package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by @author nicholas
 */
public class VariantDiscovery extends AbstractGATKSpark {

	private String gatk3_8path;
	private String referenceFile;
	
	private String inFolder;
	private String outFolder;
	
	private int availableProcessors;
	
	public VariantDiscovery(String gatk3_8path, String referenceFile, String inFolder, String outFolder) {
		this.gatk3_8path = gatk3_8path;
		this.referenceFile = referenceFile;
		this.inFolder = inFolder;
		this.outFolder = outFolder;
		
		this.availableProcessors = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void run(JavaSparkContext sc) {
		genotypeGVCFs(sc);
		
		variantRecalibratorSNP(sc);
		applyRecalibrationSNP(sc);
		
		variantRecalibratorINDEL(sc);
		applyRecalibrationINDEL(sc);
	}
	
	private void genotypeGVCFs(JavaSparkContext sc) {
		File[] gvcfs = new File(this.inFolder).listFiles(gvcf -> gvcf.getName().endsWith("_raw_variants.g.vcf"));
		String variants = " ";
		for (File gvcf : gvcfs)
			variants += " --variant " + gvcf.getAbsolutePath();
		
		super.gatkCommand = "java -jar ${files[0]} -T GenotypeGVCFs -nt " + this.availableProcessors + 
				" -R ${files[1]} -o ${files[2]}";
		
		List<String> parameters = new LinkedList<>();
		parameters.add(this.gatk3_8path + "|" + this.referenceFile + variants + "|" + this.outFolder + "raw_variants.vcf");
		
		super.parallelPipe(sc, parameters);
	}

	/*SNP*/
	private void variantRecalibratorSNP(JavaSparkContext sc) {
		//TODO locate output is not captured: try to resolve in order to find dynamically in the FS the position of these files:
		String hapmap3_3 = super.exec("locate -br '^hapmap_3.3.hg19.vcf$'");
		String kG_omni2_5 = super.exec("locate -br '^1000G_omni2.5.hg19.vcf$'");
		String kG_phase1_indels = super.exec("locate -br '^1000G_phase1.indels.hg19.sites.vcf$'");
		String dbsnp_138 = super.exec("locate -br '^dbsnp_138.hg19.vcf$'");

		super.gatkCommand = "java -jar ${files[0]} -T VariantRecalibrator -nt " + this.availableProcessors +
				" -R ${files[1]} -input ${files[2]} " + 
				" -resource:hapmap,known=false,training=true,truth=true,prior=15.0 /data/ngs/hapmap-3.3-hg19/hapmap_3.3.hg19.vcf" + hapmap3_3 + 
				" -resource:omni,known=false,training=true,truth=true,prior=12.0 /data/ngs/omni-2.5-hg19/1000G_omni2.5.hg19.vcf" + kG_omni2_5 + 
				" -resource:1000G,known=false,training=true,truth=false,prior=10.0 /data/ngs/1000G_phase1/1000G_phase1.indels.hg19.sites.vcf" + kG_phase1_indels + 
				" -resource:dbsnp,known=true,training=false,truth=false,prior=2.0 /data/ngs/dbsnp1.3.8/dbsnp_138.hg19.vcf" + dbsnp_138 + 
				" -an DP -an QD -an FS -an SOR -an MQ -an MQRankSum -an ReadPosRankSum" + 
				" -mode SNP" + 
				" -tranche 100.0 -tranche 99.9 -tranche 99.0 -tranche 90.0" + 
				" -recalFile ${files[3]} " + 
				" -tranchesFile ${files[4]} ";

		List<String> parameters = new LinkedList<>();
		parameters.add(this.gatk3_8path + "|" + this.referenceFile + "|" + this.outFolder + "raw_variants.vcf|" + 
				this.outFolder + "recalibrate_SNP.recal|" + this.outFolder + "recalibrate_SNP.tranches");
		
		super.parallelPipe(sc, parameters);
	}
	
	private void applyRecalibrationSNP(JavaSparkContext sc) {
		super.gatkCommand = "java -jar ${files[0]} -T ApplyRecalibration -nt " + this.availableProcessors +
				" -R ${files[1]} -input ${files[2]} " + 
				" -mode SNP " + 
				" --ts_filter_level 99.0" + 
				" -recalFile ${files[3]}" + 
				" -tranchesFile ${files[4]}" + 
				" -o ${files[5]}";
		
		List<String> parameters = new LinkedList<>();
		parameters.add(this.gatk3_8path + "|" + this.referenceFile + "|" + this.outFolder + "raw_variants.vcf|" + 
				this.outFolder + "recalibrate_SNP.recal|" + this.outFolder + "recalibrate_SNP.tranches|" + 
				this.outFolder + "recalibrated_snps_raw_indels.vcf");

		super.parallelPipe(sc, parameters);

	}


	/*INDEL*/	
	private void variantRecalibratorINDEL(JavaSparkContext sc) {
		//TODO locate output is not captured: try to resolve in order to find dinamically in the FS the position of these files:
		String Mills_and_1000G_gold_standard = super.exec("locate -br '^Mills_and_1000G_gold_standard.indels.hg19.vcf$'");
		String dbsnp_138 = super.exec("locate -br '^dbsnp_138.hg19.vcf$'");
		
		super.gatkCommand = "java -jar ${files[0]} -T VariantRecalibrator -nt " + this.availableProcessors + 
				" -R ${files[1]} -input ${files[2]} " + 
				" -resource:mills,known=false,training=true,truth=true,prior=12.0 /data/ngs/mills_and_1000G-hg19/Mills_and_1000G_gold_standard.indels.hg19.vcf" + Mills_and_1000G_gold_standard +
				" -resource:dbsnp,known=true,training=false,truth=false,prior=2.0 /data/ngs/dbsnp1.3.8/dbsnp_138.hg19.vcf" + dbsnp_138 +
				" -an QD -an DP -an FS -an SOR -an MQRankSum -an ReadPosRankSum " + 
				" -mode INDEL " + 
				" -tranche 100.0 -tranche 99.9 -tranche 99.0 -tranche 90.0 --maxGaussians 4" + 
				" -recalFile ${files[3]}" + 
				" -tranchesFile ${files[4]}";

		List<String> parameters = new LinkedList<>();
		parameters.add(this.gatk3_8path + "|" + this.referenceFile + "|" + this.outFolder + "recalibrated_snps_raw_indels.vcf|" + 
				this.outFolder + "recalibrate_INDEL.recal|" + this.outFolder + "recalibrate_INDEL.tranches");
		
		super.parallelPipe(sc, parameters);
	}
	
	private void applyRecalibrationINDEL(JavaSparkContext sc) {
		super.gatkCommand = "java -jar ${files[0]} -T ApplyRecalibration -nt " + this.availableProcessors +
				" -R ${files[1]} -input ${files[2]} " + 
				" -mode INDEL" + 
				" --ts_filter_level 99.0" + 
				" -recalFile ${files[3]}" + 
				" -tranchesFile ${files[4]}" + 
				" -o ${files[5]}";
		
		List<String> parameters = new LinkedList<>();
		parameters.add(this.gatk3_8path + "|" + this.referenceFile + "|" + this.outFolder + "recalibrated_snps_raw_indels.vcf|" + 
				this.outFolder + "recalibrate_INDEL.recal|" + this.outFolder + "recalibrate_INDEL.tranches|" + 
				this.outFolder + "recalibrated_variants.vcf");

		super.parallelPipe(sc, parameters);
	}
}