package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class ExonicFilter {

	private String inFilePath;
	private String outFilePath;
		
	public ExonicFilter(String inFilePath, String outFilePath) {
		this.inFilePath = inFilePath;
		this.outFilePath = outFilePath;
	}
	
	public void run(JavaSparkContext sc) {
		JavaRDD<String> data = sc.textFile("file://" + this.inFilePath);		//local FS, for HDFS just remove "file://"
		List<String> headerFields = new ArrayList<>(Arrays.asList(data.first().split("\t")));
		
		int knownGeneIndex = headerFields.indexOf("Func.knownGene");
		int refGeneIndex = headerFields.indexOf("Func.refGene");
		int ensGeneIndex = headerFields.indexOf("Func.ensGene");
		
		List<String> filtered = data.filter(row -> {
			String[] rowFields = row.split("\t");
			return rowFields[knownGeneIndex].equals("exonic") || rowFields[knownGeneIndex].equals("splicing") || rowFields[knownGeneIndex].equals("exonic;splicing")
				|| rowFields[refGeneIndex].equals("exonic") || rowFields[refGeneIndex].equals("splicing") || rowFields[refGeneIndex].equals("exonic;splicing")	
				|| rowFields[ensGeneIndex].equals("exonic") || rowFields[ensGeneIndex].equals("splicing") || rowFields[ensGeneIndex].equals("exonic;splicing");
		})//.saveAsTextFile("file://" + this.outFilePath);		//local FS, for HDFS just remove "file://"
		.collect();
		
		try {
			FileWriter writer = new FileWriter(this.outFilePath);
			for(String str: filtered)
				writer.write(str);
			writer.close();
		} catch (IOException e1) { e1.printStackTrace(); }
		
	}

}
