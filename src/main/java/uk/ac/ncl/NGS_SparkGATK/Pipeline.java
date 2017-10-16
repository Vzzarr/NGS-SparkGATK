package uk.ac.ncl.NGS_SparkGATK;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;


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


	public Pipeline(String picardPath, String inFiles, String outFile) {
		this.picardPath = picardPath;
		this.inFiles = inFiles;
		this.outFolder = outFile;
	}
	
	public Pipeline(String picardPath, String gatkPath, String inFiles, String referenceFolder, String knownSites, String outFolder) {
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
	}

	public static void main(String[] args) {
		double startTime = System.currentTimeMillis();

		if(args.length < 6)
			System.err.println("Usage:\n <picard-path> <gatk-path> <path-input-file1>,<path-input-file2> <reference-folder> "
					+ "<known-sites1>,<known-sites2>,<known-sites3> <output-folder>");

		CheckArgs ca = new CheckArgs(args);
		if(ca.check()) {
			Pipeline pipeline = new Pipeline(args);
			pipeline.run();
		}

		double stopTime = System.currentTimeMillis();
		double elapsedTime = (stopTime - startTime) / 1000;
		System.out.println("EXECUTION TIME:\t" + elapsedTime + "s");
	}

	private void run() {
		SparkConf conf = new SparkConf().setAppName(this.getClass().getName());
		JavaSparkContext sc = new JavaSparkContext(conf);
		
		FastqToSam fts = new FastqToSam(picardPath, inFiles, outFolder);
		fts.run(sc);
		
		BwaAndMarkDuplicatesPipelineSpark bwa_markDuplicates = new BwaAndMarkDuplicatesPipelineSpark(this.gatkPath, this.outFolder, this.referenceFolder);
		bwa_markDuplicates.run(sc);

		//JavaPairRDD<String, String> ubam = sc.wholeTextFiles(outFolder);


		sc.close();
		sc.stop();
	}

}
