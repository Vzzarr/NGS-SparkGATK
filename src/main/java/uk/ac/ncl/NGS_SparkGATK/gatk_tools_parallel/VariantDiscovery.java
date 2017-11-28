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
		super.gatkCommand = "java -jar ${files[0]} -T VariantRecalibrator -nt " + this.availableProcessors +
				" -R ${files[1]} -input ${files[2]} " + 
				" -resource:hapmap,known=false,training=true,truth=true,prior=15.0 " + super.locate("hapmap_3.3.hg19.vcf") + 
				" -resource:omni,known=false,training=true,truth=true,prior=12.0 " + super.locate("1000G_omni2.5.hg19.vcf") + 
				" -resource:1000G,known=false,training=true,truth=false,prior=10.0 " + super.locate("1000G_phase1.indels.hg19.sites.vcf") + 
				" -resource:dbsnp,known=true,training=false,truth=false,prior=2.0 " + super.locate("dbsnp_138.hg19.vcf") + 
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
		super.gatkCommand = "java -jar ${files[0]} -T VariantRecalibrator -nt " + this.availableProcessors + 
				" -R ${files[1]} -input ${files[2]} " + 
				" -resource:mills,known=false,training=true,truth=true,prior=12.0 " + super.locate("Mills_and_1000G_gold_standard.indels.hg19.vcf") +
				" -resource:dbsnp,known=true,training=false,truth=false,prior=2.0 " + super.locate("dbsnp_138.hg19.vcf") +
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