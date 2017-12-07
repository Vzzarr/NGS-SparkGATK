package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.util.Arrays;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

public class HelloWorld {

	private String inputFilePath;
	private String outputFilePath;
	
	
	public HelloWorld(String inputFilePath, String outputFilePath) {
		this.inputFilePath = inputFilePath;
		this.outputFilePath = outputFilePath;
	}
	
	public void run(JavaSparkContext sc) {
		JavaRDD<String> textFile = sc.textFile(this.inputFilePath);
		JavaPairRDD<String, Integer> counts = textFile
		    .flatMap(s -> Arrays.asList(s.split(" ")).iterator())
		    .mapToPair(word -> new Tuple2<>(word, 1))
		    .reduceByKey((a, b) -> a + b);
		counts.saveAsTextFile(this.outputFilePath);
	}
}
