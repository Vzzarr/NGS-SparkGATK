package uk.ac.ncl.NGS_SparkGATK;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by Nicholas
 */
public class BwaAndMarkDuplicatesPipelineSpark {

	private String gatkPath;
	private String inputFolder;		//folder that contains the samples (converted in uBAM in the previous step) to process
	private String referenceFolder;
	
	public BwaAndMarkDuplicatesPipelineSpark(String gatkPath, String inputFolder, String referenceFolder) {
		this.gatkPath = gatkPath;
		this.inputFolder = inputFolder;
		this.referenceFolder = referenceFolder;
	}
	
	public void run(JavaSparkContext sc) {
		File folder = new File(inputFolder);
		File[] listOfFiles = folder.listFiles();
		
		JavaRDD<File> ubams = sc.parallelize(Arrays.asList(listOfFiles));
		createBashScript();
		
		
		JavaRDD<String> bashExec = ubams.map(ubam -> 
		this.gatkPath + "|" + ubam.getName() + "|" + getReferenceFile() + "|" + ubam.getName().replaceAll("fastqtosam.bam", "dedup_reads.bam"))
				.pipe(System.getProperty("user.dir") + "/BwaAndMarkDuplicatesPipelineSpark.sh");
		
		//TODO execute bash commands and create Abstract Class
		for (String string : bashExec.collect()) 
			System.out.println(string);

		try {
			Files.delete(Paths.get(System.getProperty("user.dir") + "/BwaAndMarkDuplicatesPipelineSpark.sh"));
		} catch (IOException x) { System.err.println(x); }
		
	}

	
	private void createBashScript() {
		String bashScript = "#!/bin/bash\n" +
				"while read LINE; do\n" + 
				"        IFS='|' read -a files <<< \"$LINE\"\n" + 
				"		 ./data/ngs/libraries/gatk_4/gatk-launch BwaAndMarkDuplicatesPipelineSpark --input /data0/execution_gatk/PFC_0030_fastqtosam.bam --reference /data/ngs/reference/hg19-ucsc/ucsc.hg19.fasta --disableSequenceDictionaryValidation true --output /data0/execution_gatk/PFC_0030_dedup_reads.bam" +
				"		 .${files[0]} BwaAndMarkDuplicatesPipelineSpark --input ${files[1]} --reference ${files[2]} --disableSequenceDictionaryValidation true --output ${files[3]}" +
				"done\n";
		//${files[0]}

		try {
			PrintWriter pw = new PrintWriter(new FileWriter("BwaAndMarkDuplicatesPipelineSpark.sh"));
			pw.println(bashScript);
			pw.close();

			Runtime.getRuntime().exec("chmod +x " + System.getProperty("user.dir") + "/BwaAndMarkDuplicatesPipelineSpark.sh");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private String getReferenceFile() {
		File reference = new File(this.referenceFolder);
		
		for (File file : reference.listFiles()) 
			if(file.getName().endsWith(".fasta"))
				return file.getName();
		
		System.err.println("Unespected: .fasta file not present");

		return "";
	}
}
