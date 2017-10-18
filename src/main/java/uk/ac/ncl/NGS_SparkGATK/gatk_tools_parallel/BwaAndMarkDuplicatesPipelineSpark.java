package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaRDD;
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
		super.gatkCommand = ".${files[0]} BwaAndMarkDuplicatesPipelineSpark --input ${files[1]} --reference ${files[2]} --disableSequenceDictionaryValidation true --output ${files[3]}\n";
		//white spaces at the beginning of the command are already in the Abstract Class
	}
	

	@Override
	public void run(JavaSparkContext sc) {
		File folder = new File(inputFolder);
		List<File> listOfFiles = Arrays.asList(folder.listFiles()).parallelStream()
				.filter(file -> file.getName().endsWith("_fastqtosam.bam")).collect(Collectors.toList());

		JavaRDD<File> ubams = sc.parallelize(listOfFiles);
		createBashScript(this.gatkCommand);
		
		
		JavaRDD<String> bashExec = ubams.map(ubam -> 
		this.gatkPath + "|" + ubam.getAbsolutePath() + "|" + getReferenceFile() + "|" + ubam.getAbsolutePath().replaceAll("fastqtosam.bam", "dedup_reads.bam"))
				.pipe(System.getProperty("user.dir") + "/" + this.getClass().getSimpleName() + ".sh");

		for (String string : bashExec.collect()) 
			System.out.println(string);

		try {
			Files.delete(Paths.get(System.getProperty("user.dir") + "/" + this.getClass().getSimpleName() + ".sh"));
		} catch (IOException x) { System.err.println(x); }
		
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
