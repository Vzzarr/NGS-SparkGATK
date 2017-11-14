package uk.ac.ncl.NGS_SparkGATK;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel.BQSRPipelineSpark;
import uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel.BwaAndMarkDuplicatesPipelineSpark;
import uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel.FastqToSam;


/**
 * Created by Nicholas
 */
public class Pipeline {

	private String picardPath;
	private String gatkPath;
	private String inFiles;			//specify files path in "/path/file1,/path/file2.."
	private String referenceFolder;	//3
	private String knownSites;
	private String outFolder;


	//testing FastqToSam
	public Pipeline(String picardPath, String inFiles, String outFile) {
		this.picardPath = picardPath;
		this.inFiles = inFiles;
		this.outFolder = outFile;
	}
	
	//testing BwaAndMarkDuplicatesPipelineSpark
	/*public Pipeline(String gatkPath, String inFolder, String referenceFolder) {
		this.gatkPath = gatkPath;
		this.inFiles = inFolder;
		this.referenceFolder = referenceFolder;
	}*/
	
	//testing BQSRPipelineSpark
	/*public Pipeline(String gatkPath, String inFolder, String referenceFolder, String knownSites) {
		this.gatkPath = gatkPath;
		this.inFiles = inFolder;
		this.referenceFolder = referenceFolder;
		this.knownSites = knownSites;
	}*/
	
	/*public Pipeline(String picardPath, String gatkPath, String inFiles, String referenceFolder, String knownSites, String outFolder) {
		this.picardPath = picardPath;
		this.gatkPath = gatkPath;
		this.inFiles = inFiles;
		this.referenceFolder = referenceFolder;
		this.knownSites = knownSites;
		this.outFolder = outFolder;
	}
	
	public Pipeline(String[] args) {
		this.picardPath = args[0];
		this.gatkPath = args[1];
		this.inFiles = args[2];
		this.referenceFolder = args[3];
		this.knownSites = args[4];
		this.outFolder = args[5];
	}*/

	public static void main(String[] args) {
		double startTime = System.currentTimeMillis();

		/*if(args.length < 6)
			System.err.println("Usage:\n <picard-path> <gatk-path> <path-input-file1>,<path-input-file2> <reference-folder> "
					+ "<known-sites1>,<known-sites2>,<known-sites3> <output-folder>");*/

//		CheckArgs ca = new CheckArgs(args);
//		if(ca.check()) {
//			Pipeline pipeline = new Pipeline(args);
//			pipeline.run();
//		}
		
		Pipeline pipeline = new Pipeline(args[0], args[1], args[2]);
		pipeline.run();
		
		/*Pipeline pipeline = new Pipeline(args[0], args[1], args[2], args[3]);
		pipeline.run();*/
		
		double stopTime = System.currentTimeMillis();
		double elapsedTime = (stopTime - startTime) / 1000;
		System.out.println("EXECUTION TIME:\t" + elapsedTime + "s");
	}

	private void run() {
		SparkConf conf = new SparkConf().setAppName(this.getClass().getName());
		JavaSparkContext sc = new JavaSparkContext(conf);
		
		//testing FastqToSam
		FastqToSam fts = new FastqToSam(picardPath, inFiles, outFolder);
		fts.run(sc);
		
		//testing BwaAndMarkDuplicatesPipelineSpark
		/*BwaAndMarkDuplicatesPipelineSpark bwa_markDuplicates = new BwaAndMarkDuplicatesPipelineSpark(this.gatkPath, this.outFolder, this.referenceFolder);
		bwa_markDuplicates.run(sc);*/

		//testing BwaAndMarkDuplicatesPipelineSpark
		/*BQSRPipelineSpark bqsr = new BQSRPipelineSpark(this.gatkPath, this.outFolder, this.referenceFolder, this.knownSites);
		bqsr.run(sc);*/

		//JavaPairRDD<String, String> ubam = sc.wholeTextFiles(outFolder);


		sc.close();
		sc.stop();
	}

}