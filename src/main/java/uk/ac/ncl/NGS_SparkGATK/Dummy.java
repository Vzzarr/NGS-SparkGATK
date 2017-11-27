package uk.ac.ncl.NGS_SparkGATK;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

public class Dummy {

	public static void main(String[] args) throws IOException {
	
		String header = "#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	PFC_0028_SW_CGTACG_R	PFC_0029_IUH_AGTTCC_R	PFC_0030_MSt_GAGTGG_R	PFC_0031_DR_TTAGGC_R	PFC_0032_IMc_CAGATC_R	PFC_0033_MH_AGTTCC_R";

		List<String> headerFields = Arrays.asList(header.split("\t"));
		headerFields = headerFields.subList(9, headerFields.size());	//test it

		for (String field : headerFields) {
			field = "cane|" + field;
			System.out.println(field);
		}
//		String command = "ls -l";
//		exec(command);
//		System.out.println(cane);
	}
//	locate -br '^target$'
	   
	
	
	
	public static String exec(String command) {
		String line, output = "";
		try {
			ProcessBuilder builder = new ProcessBuilder(command.split(" "));
			Process process = builder.start();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

			while ((line = br.readLine()) != null) {
				System.out.println(line);
				output += line + "\n";
			}
		} catch (IOException e) { e.printStackTrace(); }
		return output;
	}
	
}