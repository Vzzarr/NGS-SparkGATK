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

		String command = "locate -br ^target$ -n 1";
		String cane = exec(command);
//		System.out.println(cane);
	}
//	locate -br \'^target$\'
	   
	
	
	
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