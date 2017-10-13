package uk.ac.ncl.NGS_SparkGATK;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class FastqToSam {

	private String picardPath;
	private String inFiles;
	private String outFolder;

	public FastqToSam(String picardPath, String inFiles, String outFolder) {
		this.picardPath = picardPath;
		this.inFiles = inFiles;
		this.outFolder = outFolder;
	}

	public void run(JavaSparkContext sc) {
		List<String> fastq_r1_r2 = new LinkedList<>();

		String[] filesPath = inFiles.split(",");
		int i = 0;
		while (i < filesPath.length) {
			String pairedEnd_r1 = filesPath[i];
			i++;
			String pairedEnd_r2 = filesPath[i];
			fastq_r1_r2.add(picardPath + "|" + pairedEnd_r1 + "|" + pairedEnd_r2 + "|" + outFolder + 
					greatestCommonPrefix(pairedEnd_r1.substring(pairedEnd_r1.lastIndexOf("/") + 1), pairedEnd_r2.substring(pairedEnd_r2.lastIndexOf("/") + 1))  + "_fastqtosam.bam");
			i++;
		}

		JavaRDD<String> rdd_fastq_r1_r2 = sc.parallelize(fastq_r1_r2);
		createBashScript();

		JavaRDD<String> bashExec = rdd_fastq_r1_r2.pipe(System.getProperty("user.dir") + "/FastqToSam.sh");


		for (String string : bashExec.collect()) 
			System.out.println(string);

		try {
			Files.delete(Paths.get(System.getProperty("user.dir") + "/FastqToSam.sh"));
		} catch (IOException x) { System.err.println(x); }
	}

	private void createBashScript() {
		String bashScript = "#!/bin/bash\n" +
				"while read LINE; do\n" + 
				"        IFS='|' read -a files <<< \"$LINE\"\n" + 
				"        java -Xmx8G -jar ${files[0]} FastqToSam FASTQ=${files[1]} FASTQ2=${files[2]} OUTPUT=${files[3]} READ_GROUP_NAME=H0164.2 SAMPLE_NAME=NA12878 LIBRARY_NAME=Solexa-272222 PLATFORM_UNIT=H0164ALXX140820.2 PLATFORM=illumina SEQUENCING_CENTER=BI\n" + 
				"done\n";

		try {
			PrintWriter pw = new PrintWriter(new FileWriter("FastqToSam.sh"));
			pw.println(bashScript);
			pw.close();

			Runtime.getRuntime().exec("chmod +x " + System.getProperty("user.dir") + "/FastqToSam.sh");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}



	private String greatestCommonPrefix(String a, String b) {
		int minLength = Math.min(a.length(), b.length());
		for (int i = 0; i < minLength; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return a.substring(0, minLength);
	}
}
