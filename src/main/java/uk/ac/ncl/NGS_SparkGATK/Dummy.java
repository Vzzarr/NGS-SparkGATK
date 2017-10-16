package uk.ac.ncl.NGS_SparkGATK;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Dummy {

	public static void main(String[] args) throws IOException {
		List<String> referenceExtensions = Arrays.asList(".2bit", ".dict", ".fasta", ".amb", ".ann", ".bwt", ".fai", ".img", ".pac", ".sa");
		File referenceFolder = new File("/home/nicholas/Documents");
		List<String> listOfFiles = Arrays.asList(referenceFolder.listFiles()).parallelStream()
				.map(file -> file.getName()).collect(Collectors.toList());
		
		System.out.println(allExtensionsPresent(listOfFiles, referenceExtensions));

	}
	
	private static boolean allExtensionsPresent(List<String> files, List<String> extensions) {
		for (String extension : extensions)
			if(!containsExtension(files, extension))
				return false;
		
		return true;
	}
	
	private static boolean containsExtension(List<String> list, String containedExtension) {
		for (String string : list) 
			if(string.endsWith(containedExtension))
				return true;
		return false;
	}
}