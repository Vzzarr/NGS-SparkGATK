package uk.ac.ncl.NGS_SparkGATK;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
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
		
		FastqToSam fts = new FastqToSam(picardPath, inFiles, outFolder);
		fts.run(sc);

		//JavaPairRDD<String, String> ubam = sc.wholeTextFiles(outFolder);


		sc.close();
		sc.stop();
	}






}
