package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by Nicholas
 */
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
	public void run(JavaSparkContext sc) {
		List<File> listOfFiles = Arrays.asList(new File(inputFolder).listFiles()).parallelStream()
				.filter(file -> file.getName().endsWith("_dedup_reads.bam")).collect(Collectors.toList());

		listOfFiles.forEach(ubam -> super.exec(this.gatkPath + " BQSRPipelineSpark --input " + 
				ubam.getAbsolutePath() + " --reference " + getReferenceFile() + " --disableSequenceDictionaryValidation true --output " + 
				ubam.getAbsolutePath().replaceAll("dedup_reads.bam", "recal_reads.bam") + getKnownSites())	);
	}
	
	
	private String getReferenceFile() {
		File reference = new File(this.referenceFolder);
		
		for (File file : reference.listFiles()) 
			if(file.getName().endsWith(".2bit"))
				return file.getAbsolutePath();
		
		System.err.println("Unespected: .2bit file not present");

		return "";
	}

	private String getKnownSites() {
		String known = "";
		for (String knownSite : this.knownSites.split(","))
			known += (" --knownSites " + knownSite);
		
		return known ;
	}

}