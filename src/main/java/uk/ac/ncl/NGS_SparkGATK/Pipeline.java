package uk.ac.ncl.NGS_SparkGATK;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

public class Pipeline {

	private String picardPath;
	private String inFiles;	//specify files path in "/path/file1,/path/file2.."
	private String outFolder;
	


	public Pipeline(String picardPath, String inFile, String outFile) {
		this.picardPath = picardPath;
		this.inFiles = inFile;
		this.outFolder = outFile;
	}

	public static void main(String[] args) {
		double startTime = System.currentTimeMillis();

		if(args.length < 3)
			System.err.println("Usage: <picard-path> <path-input-file1>,<path-input-file2> <output-folder>");
		if(!args[1].contains(",") || args[1].split(",").length % 2 == 1)
			System.err.println("Number of input file paths must be Even and Coma Separated: expected Paired End .fastq");
		File of = new File(args[2]);
		if(!of.isDirectory() || of.list().length > 0)
			System.err.println("<output-folder> must be an empty directory");

		Pipeline pipeline = new Pipeline(args[0], args[1], args[2]);
		pipeline.run();

		double stopTime = System.currentTimeMillis();
		double elapsedTime = (stopTime - startTime) / 1000;
		System.out.println("EXECUTION TIME:\t" + elapsedTime + "s");
	}

	private void run() {
		SparkConf conf = new SparkConf().setAppName(this.getClass().getName());
		JavaSparkContext sc = new JavaSparkContext(conf);
		
	    List<Tuple2<String, String>> fastq_r1_r2 = new LinkedList<>();

	    String[] filesPath = inFiles.split(",");
	    int i = 0;
	    while (i < filesPath.length) {
			String pairedEnd_r1 = filesPath[i];
			i++;
			String pairedEnd_r2 = filesPath[i];
			fastq_r1_r2.add(new Tuple2<String, String>(pairedEnd_r1, pairedEnd_r2));
		}
	    
	    JavaRDD<Tuple2<String, String>> rdd_fastq_r1_r2 = sc.parallelize(fastq_r1_r2);
	    
	    rdd_fastq_r1_r2.map(pair -> {
	    	String command = "java -Xmx8G -jar " + picardPath + " FastqToSam"
					+ " FASTQ=" + pair._1
					+ " FASTQ2=" + pair._2
					+ " OUTPUT=" + outFolder + greatestCommonPrefix(pair._1, pair._2) + "_fastqtosam.bam"
					+ " READ_GROUP_NAME=H0164.2 SAMPLE_NAME=NA12878 LIBRARY_NAME=Solexa-272222 PLATFORM_UNIT=H0164ALXX140820.2"
					+ " PLATFORM=illumina SEQUENCING_CENTER=BI";
	    	Runtime.getRuntime().exec(command);
	    	return "";
	    });
				


		//JavaPairRDD<String, String> ubam = sc.wholeTextFiles(outFolder);
		
	    
	    /*.map(x -> {
			x._1
			return "";
		}); */
		

		sc.close();
		sc.stop();
	}
	
	
	

	public String greatestCommonPrefix(String a, String b) {
	    int minLength = Math.min(a.length(), b.length());
	    for (int i = 0; i < minLength; i++) {
	        if (a.charAt(i) != b.charAt(i)) {
	            return a.substring(0, i);
	        }
	    }
	    return a.substring(0, minLength);
	}


}
