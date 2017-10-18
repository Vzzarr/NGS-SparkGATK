package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class BQSRPipelineSpark extends AbstractGATKParallel {

	private String gatkPath;
	private String inputFolder;		//folder that contains the samples (converted in uBAM in the previous step) to process
	private String referenceFolder;
	private String knownSites;
	
	public BQSRPipelineSpark(String gatkPath, String inputFolder, String referenceFolder, String knownSites) {
		this.gatkPath = gatkPath;
		this.inputFolder = inputFolder;
		this.referenceFolder = referenceFolder;
		this.knownSites = knownSites;
		
		super.gatkCommand = "";
	}
	
	@Override
	void run(JavaSparkContext sc) {
		File folder = new File(inputFolder);
		List<File> listOfFiles = Arrays.asList(folder.listFiles()).parallelStream()
				.filter(file -> file.getName().endsWith("dedup_reads.bam")).collect(Collectors.toList());

		JavaRDD<File> ubams = sc.parallelize(listOfFiles);
		createBashScript(super.gatkCommand);
		
		JavaRDD<String> bashExec = ubams.map(ubam -> 
		this.gatkPath + "|" + ubam.getAbsolutePath() + "|" + getReferenceFile() + "|" + ubam.getAbsolutePath().replaceAll("fastqtosam.bam", "dedup_reads.bam"))
				.pipe(System.getProperty("user.dir") + "/" + this.getClass().getSimpleName() + ".sh");

		//TODO finish to implement
//		./data/ngs/libraries/gatk_4/gatk-launch BQSRPipelineSpark --input /data0/execution_gatk/PFC_0030_dedup_reads.bam --reference /data/ngs/reference/hg19-ucsc/ucsc.hg19.2bit --output /data0/execution_gatk/PFC_0030_recal_reads.bam --disableSequenceDictionaryValidation true --knownSites /data/ngs/dbsnp1.3.8/dbsnp_138.hg19.vcf --knownSites /data/ngs/mills_and_1000G-hg19/Mills_and_1000G_gold_standard.indels.hg19.vcf --knownSites /data/ngs/1000G_phase1/1000G_phase1.indels.hg19.sites.vcf
		
		
		
		
		for (String string : bashExec.collect()) 
			System.out.println(string);

		try {
			Files.delete(Paths.get(System.getProperty("user.dir") + "/" + this.getClass().getSimpleName() + ".sh"));
		} catch (IOException x) { System.err.println(x); }

		
	}
	
	private String getReferenceFile() {
		File reference = new File(this.referenceFolder);
		
		for (File file : reference.listFiles()) 
			if(file.getName().endsWith(".2bit"))
				return file.getAbsolutePath();
		
		System.err.println("Unespected: .2bit file not present");

		return "";
	}


}
