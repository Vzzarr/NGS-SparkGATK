package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by @author nicholas
 */
public class CallsetRefinement extends AbstractGATKSpark {

	private String gatk3_8path;
	private String referenceFile;

	private String inFolder;
	private String outFolder;

	private int availableProcessors;

	public CallsetRefinement(String gatk3_8path, String referenceFile, String inFolder, String outFolder) {
		this.gatk3_8path = gatk3_8path;
		this.referenceFile = referenceFile;
		this.inFolder = inFolder;
		this.outFolder = outFolder;

		this.availableProcessors = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void run(JavaSparkContext sc) {
		calculateGenotypePosteriors(sc);
		variantFiltration(sc);
		variantAnnotator(sc);

		selectVariants(sc);
		convert2annovar(sc);
		table_annovar(sc);
		igm_anno(sc);
		exonicFilter(sc);
	}


	private void calculateGenotypePosteriors(JavaSparkContext sc) {
		//TODO check one more time if locate works effectively in CallsetRefinement
		super.gatkCommand = "java -jar ${files[0]} -T CalculateGenotypePosteriors " +	//-nt parallel execution not supported
				" -R ${files[1]} --supporting " + super.locate("1000G_phase1.indels.hg19.sites.vcf") + " -V ${files[2]} -o ${files[3]}";

		List<String> parameters = new LinkedList<>();
		parameters.add(this.gatk3_8path + "|" + this.referenceFile + "|" +	this.inFolder + "recalibrated_variants.vcf|" + 
				this.outFolder + "recalibratedVariants.postCGP.vcf");

		super.parallelPipe(sc, parameters);
	}

	private void variantFiltration(JavaSparkContext sc) {
		super.gatkCommand = "java -jar ${files[0]} -T VariantFiltration " +	//-nt parallel execution not supported
				" -R ${files[1]} -V ${files[2]} -G_filter \"GQ < 20.0\" -G_filterName lowGQ  -o ${files[3]}";

		List<String> parameters = new LinkedList<>();
		parameters.add(this.gatk3_8path + "|" + this.referenceFile + "|" +	this.outFolder + "recalibratedVariants.postCGP.vcf|" + 
				this.outFolder + "recalibratedVariants.postCGP.Gfiltered.vcf");

		super.parallelPipe(sc, parameters);
	}

	private void variantAnnotator(JavaSparkContext sc) {
		super.gatkCommand = "java -jar ${files[0]} -T VariantAnnotator -nt " + this.availableProcessors +
				" -R ${files[1]} -V ${files[2]} -A PossibleDeNovo -o ${files[3]}";

		List<String> parameters = new LinkedList<>();
		parameters.add(this.gatk3_8path + "|" + this.referenceFile + "|" +	this.outFolder + "recalibratedVariants.postCGP.Gfiltered.vcf|" + 
				this.outFolder + "recalibratedVariants.postCGP.Gfiltered.deNovos.vcf");

		super.parallelPipe(sc, parameters);
	}


	private void selectVariants(JavaSparkContext sc) {
		List<String> headerFields = getHeader(this.outFolder + "recalibratedVariants.postCGP.Gfiltered.deNovos.vcf");

		List<String> parameters = new LinkedList<>();
		for (String field : headerFields.subList(9, headerFields.size())	)	//discarding the first 9 fields, in order to obtain only the sample name
			parameters.add(this.gatk3_8path + "|" + this.referenceFile + "|" + 
					this.outFolder + "recalibratedVariants.postCGP.Gfiltered.deNovos.vcf|" + 
					this.outFolder + field + ".vcf|" + field);

		super.gatkCommand = "java -jar ${files[0]} -T SelectVariants -nt " + this.availableProcessors +
				" -R ${files[1]} -V ${files[2]} -o ${files[3]} -sn ${files[4]}";		
		super.gatkCommand += "\n sed -i '/\\*/d' ${files[3]} \n";	//removing line with *, because causes issues to Annovar

		super.parallelPipe(sc, parameters);
	}

	private void convert2annovar(JavaSparkContext sc) {
		List<String> headerFields = getHeader(this.outFolder + "recalibratedVariants.postCGP.Gfiltered.deNovos.vcf");

		List<String> parameters = new LinkedList<>();
		for (String field : headerFields.subList(9, headerFields.size())	)	//discarding the first 9 fields, in order to obtain only the sample name
			parameters.add(this.outFolder + field + ".vcf|" + this.outFolder + field + "converted.ann");

		super.gatkCommand = "perl " + super.locate("convert2annovar.pl") + " -format vcf4old -includeinfo ${files[0]} > ${files[1]}";

		super.parallelPipe(sc, parameters);
	}

	private void table_annovar(JavaSparkContext sc) {
		List<String> headerFields = getHeader(this.outFolder + "recalibratedVariants.postCGP.Gfiltered.deNovos.vcf");

		List<String> parameters = new LinkedList<>();
		for (String field : headerFields.subList(9, headerFields.size())	)	//discarding the first 9 fields, in order to obtain only the sample name
			parameters.add(this.outFolder + field + "converted.ann|TODO");	//TODO modify the script in order to accept a single input

		super.gatkCommand = "perl " + super.locate("table_annovar.pl") + " -remove -otherinfo -buildver hg19 "
				+ " -protocol knownGene,ensGene,refGene,phastConsElements46way,genomicSuperDups,esp6500si_all,1000g2012apr_all,cg69,snp137,ljb26_all "
				+ " -operation g,g,g,r,r,f,f,f,f,f ${files[0]} " + super.locate("humandb");

		super.parallelPipe(sc, parameters);
	}

	private void igm_anno(JavaSparkContext sc) {
		List<String> headerFields = getHeader(this.outFolder + "recalibratedVariants.postCGP.Gfiltered.deNovos.vcf");

		List<String> parameters = new LinkedList<>();
		for (String field : headerFields.subList(9, headerFields.size())	)	//discarding the first 9 fields, in order to obtain only the sample name
			parameters.add(field + "|" + this.outFolder + field + "converted.ann.hg19_multianno.txt|" + this.outFolder + field + "igm_anno.txt");	

		super.gatkCommand = "perl " + super.locate("annotate.pl") + " -samples ${files[0]} "
				+ " -avoutput ${files[1]} -out ${files[2]} ";

		super.parallelPipe(sc, parameters);
	}

	private void exonicFilter(JavaSparkContext sc) {
		List<String> headerFieldsDeNovo = getHeader(this.outFolder + "recalibratedVariants.postCGP.Gfiltered.deNovos.vcf");

		for (String field : headerFieldsDeNovo.subList(9, headerFieldsDeNovo.size())	) {
			JavaRDD<String> data = sc.textFile("file://" + this.outFolder + field + "igm_anno.txt");		//local FS, for HDFS just remove "file://"
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
				FileWriter writer = new FileWriter(this.outFolder + field + "exonic_filtered.txt");	//$OUT_FOLDER$dir_callref${files[$i-1]}$ExonicFilterFormat
				for(String str: filtered)
					writer.write(str + "\n");
				writer.close();
			} catch (IOException e1) { e1.printStackTrace(); }
		}
	}

	/**
	 * 
	 * @param filePath
	 * @return a list containing the header fields; in error case (header line not found) returns null
	 */
	private List<String> getHeader(String filePath) {
		String line = "";
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileReader(filePath));
			while(scanner.hasNextLine())
				if( (line = scanner.nextLine()).startsWith("#CHROM"))
					return Arrays.asList(line.split("\t"));

		} catch (FileNotFoundException e) { e.printStackTrace(); }
		finally { scanner.close(); }

		return null;
	}
}