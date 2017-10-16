package uk.ac.ncl.NGS_SparkGATK;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CheckArgs {
	
	private String picardPath;
	private String gatkPath;
	private String inFiles;			//specify files path in "/path/file1,/path/file2.."
	private String referenceFolder;	//3
	private String knownSites;
	private String outFolder;
	
	public CheckArgs(String[] args) {
		this.picardPath = args[0];
		this.gatkPath = args[1];
		this.inFiles = args[2];				//checked
		this.referenceFolder = args[3];		//checked
		this.knownSites = args[4];			//checked
		this.outFolder = args[5];			//checked
	}
	
	public boolean check() {
		return checkInput() && checkReference() && checkKnownSites() && checkOutput();
	}
	
	private boolean checkPicardPath() {
		//TODO
		return true;
	}
	
	private boolean checkGATKPath() {
		//TODO
		return true;
	}
	
	
	private boolean checkInput() {
		if(!this.inFiles.contains(",") || this.inFiles.split(",").length % 2 == 1) {
			System.err.println("Number of input file paths must be Even and Coma Separated: expected Paired End .fastq");
			return false;
		}
		return true;
	}
	
	
	private boolean checkReference() {
		List<String> referenceExtensions = Arrays.asList(".2bit", ".dict", ".fasta", ".amb", ".ann", ".bwt", ".fai", ".img", ".pac", ".sa");
		File reference = new File(this.referenceFolder);
		List<String> listOfFiles = Arrays.asList(reference.listFiles()).parallelStream()
				.map(file -> file.getName()).collect(Collectors.toList());
		
		return allExtensionsPresent(listOfFiles, referenceExtensions);
	}
	
	private boolean allExtensionsPresent(List<String> files, List<String> extensions) {
		for (String extension : extensions)
			if(!containsExtension(files, extension)) {
				System.err.println("Reference folder do not contains all the required files:\n"
						+ ".2bit .dict .fasta .amb .ann .bwt .fai .img .pac .sa\n"
						+ "Missing: " + extension);
				return false;
			}
		return true;
	}
	
	private boolean containsExtension(List<String> list, String containedExtension) {
		for (String string : list) 
			if(string.endsWith(containedExtension))
				return true;
		return false;
	}
	
	private boolean checkKnownSites() {
		String[] sites = this.knownSites.split(",");
		for (String site : sites) {
			File s = new File(site);
			if(!s.isFile() || !s.getName().endsWith(".vcf")) {
				System.err.println("Known Sites must be listed in Coma Separated path to at least one .vcf file");
				return false;
			}
				
		}
		return true;
	}
	
	private boolean checkOutput() {
		File of = new File(this.outFolder);
		if(!of.isDirectory() || of.list().length > 0) {
			System.err.println("<output-folder> must be an empty directory");
			return false;
		}
		return true;
	}

}
