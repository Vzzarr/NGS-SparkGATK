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
	
//		String command = "/data/ngs/libraries/gatk_4/gatk-launch BwaAndMarkDuplicatesPipelineSpark --input /data0/NGS-SparkGATK/output/PFC_0029_IUH_AGTTCC_L007_R_fastqtosam.bam --reference /data/ngs/reference/hg19-ucsc/ucsc.hg19.fasta --disableSequenceDictionaryValidation true --output /data0/NGS-SparkGATK/output/PFC_0029_IUH_AGTTCC_L007_R_dedup_reads.bam";
//		Runtime.getRuntime().exec(command);
		find("eclipse");
//		System.out.println(cane);
	}
//	locate -br '^hapmap_3.3.hg19.vcf$'
	    public static void find(String fileName) {
	        File root = new File("/");
	        try {
	            boolean recursive = true;

	            Collection<File> files = FileUtils.listFiles(root, null, recursive);

	            for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
	                File file = (File) iterator.next();
	                if (file.getName().equals(fileName))
	                    System.out.println(file.getAbsolutePath());
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
}