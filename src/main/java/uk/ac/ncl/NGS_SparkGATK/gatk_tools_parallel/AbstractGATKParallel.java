package uk.ac.ncl.NGS_SparkGATK.gatk_tools_parallel;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.spark.api.java.JavaSparkContext;

public abstract class AbstractGATKParallel {

	protected String gatkCommand;		//the command that calls and executes the GATK tool
	
	abstract void run(JavaSparkContext sc);
	
	protected void createBashScript(String gatkCommand) {
		String bashScript = "#!/bin/bash\n" +
				"cd /\n" + 
				"while read LINE; do\n" + 
				"        IFS='|' read -a files <<< \"$LINE\"\n" + 
				"        " + gatkCommand + "\n" +
				"done\n";

		try {
			PrintWriter pw = new PrintWriter(new FileWriter(this.getClass().getSimpleName() + ".sh"));	//TODO test if this.getClass().getName() works
			pw.println(bashScript);
			pw.close();

			Runtime.getRuntime().exec("chmod +x " + System.getProperty("user.dir") + "/" + this.getClass().getSimpleName() + ".sh");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
}
