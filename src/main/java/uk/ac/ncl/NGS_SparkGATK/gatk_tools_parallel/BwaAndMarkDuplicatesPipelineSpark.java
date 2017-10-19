package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by Nicholas
 */
public class BwaAndMarkDuplicatesPipelineSpark extends AbstractGATKParallel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String gatkPath;
	private String inputFolder;		//folder that contains the samples (converted in uBAM in the previous step) to process
	private String referenceFolder;

	public BwaAndMarkDuplicatesPipelineSpark(String gatkPath, String inputFolder, String referenceFolder) {
		this.gatkPath = gatkPath;
		this.inputFolder = inputFolder;
		this.referenceFolder = referenceFolder;
	}


	@Override
	public void run(JavaSparkContext sc) {
		List<File> listOfFiles = Arrays.asList(new File(inputFolder).listFiles()).parallelStream()
				.filter(file -> file.getName().endsWith("_fastqtosam.bam")).collect(Collectors.toList());

		listOfFiles.forEach(ubam -> super.exec(this.gatkPath + " BwaAndMarkDuplicatesPipelineSpark --input " + 
				ubam.getAbsolutePath() + " --reference " + getReferenceFile() + " --disableSequenceDictionaryValidation true --output " + 
				ubam.getAbsolutePath().replaceAll("fastqtosam.bam", "dedup_reads.bam"))	);
	}


	private String getReferenceFile() {
		File reference = new File(this.referenceFolder);

		for (File file : reference.listFiles()) 
			if(file.getName().endsWith(".fasta"))
				return file.getAbsolutePath();

		System.err.println("Unespected: .fasta file not present");

		return "";
	}

}