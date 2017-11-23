package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by @author nicholas
 */
public class CallsetRefinement extends AbstractGATKSpark {

	private String referencePath;
	
	public CallsetRefinement(String referencePath) {
		this.referencePath = referencePath;
	}
	
	
	@Override
	public void run(JavaSparkContext sc) {
		calculateGenotypePosteriors();
		variantFiltration();
		variantAnnotator();
		

	}

	private void calculateGenotypePosteriors() {
		super.gatkCommand = "java -jar ${files[0]} -T CalculateGenotypePosteriors " + 
				"-R ${files[1]} --supporting ${files[2]} -V ${files[3]} -o ${files[4]}";

	}

	private void variantFiltration() {
		super.gatkCommand = "java -jar ${files[0]} -T VariantFiltration " + 
				"-R ${files[1]} -V ${files[2]} -G_filter \"GQ < 20.0\" -G_filterName lowGQ  -o ${files[3]}";


	}

	private void variantAnnotator() {
		super.gatkCommand = "java -jar ${files[0]} -T VariantAnnotator " + 
				"-R ${files[1]} -V ${files[2]} -A PossibleDeNovo -o ${files[3]}";


	}
	/*
java -jar $GATK_PATH_3_8 -T CalculateGenotypePosteriors \
-R $REFERENCE_FOLDER*.fasta \
--supporting /data/ngs/1000G_phase1/1000G_phase1.indels.hg19.sites.vcf \
-V $OUT_FOLDER$dir_vardis$recalibratedINDEL \
-o $OUT_FOLDER$dir_callref$recalibrated_postCGP
#-ped trio.ped 

java -jar $GATK_PATH_3_8 -T VariantFiltration \
-R $REFERENCE_FOLDER*.fasta \
-V $OUT_FOLDER$dir_callref$recalibrated_postCGP \
-G_filter "GQ < 20.0" -G_filterName lowGQ \
-o $OUT_FOLDER$dir_callref$recalibrated_postCGP_Gfiltered


java -jar $GATK_PATH_3_8 -T VariantAnnotator \
-R $REFERENCE_FOLDER*.fasta \
-V $OUT_FOLDER$dir_callref$recalibrated_postCGP_Gfiltered \
-A PossibleDeNovo \
-o $OUT_FOLDER$dir_callref$recalibrated_postCGP_Gfiltered_deNovos
	 */

}
