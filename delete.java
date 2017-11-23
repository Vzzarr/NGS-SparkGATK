package uk.ac.ncl.eGenome.blocks;

/**
 * e-Science Central Copyright (C) 2008-2013 School of Computing Science,
 * Newcastle University
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation at: http://www.gnu.org/licenses/gpl-2.0.html
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, 5th Floor, Boston, MA 02110-1301, USA.
 */
import java.io.File;
import java.util.ArrayList;

import org.pipeline.core.data.Column;
import org.pipeline.core.data.Data;

import uk.ac.ncl.eSC.CommonTools;
import uk.ac.ncl.eSC.CommonTools.DumperThread;

import com.connexience.server.workflow.cloud.library.CloudWorkflowServiceLibraryItem;
import com.connexience.server.workflow.cloud.services.CloudDataProcessorService;
import com.connexience.server.workflow.engine.datatypes.FileWrapper;


public class Annotate extends CloudDataProcessorService
{
    private static final String _LibraryName = "IGM_Anno-20140219";
    private static final String _AnnotateCmdName = "AnnotateCmd";
    
    private static final String Input_ANNOVAR_FILES = "annovar-output-files";
    private static final String Input_SAMPLE_NAMES = "sample-names";

    private static final String Output_ANNOTATION_FILES= "annotated-files";


    /**
     * This is the main service execution routine. It is called once if the
     * service has not been configured to accept streaming data or once for each
     * chunk of data if the service has been configured to accept data streams
     */
    public void execute() throws Exception
    {
        CloudWorkflowServiceLibraryItem library = getDependencyItem(_LibraryName);
        if(library == null)
            throw new Exception("Can't locate dependency library: " + _LibraryName);

        // First convert input to the annovar format
        //
        FileWrapper avFiles = (FileWrapper)getInputData(Input_ANNOVAR_FILES);
        if (avFiles.getFileCount() < 1) {
            throw new Exception("Missing input files.");
        }

        //StringListWrapper sampleNames1 = (StringListWrapper)getProperties().xmlStorableValue(Prop_SAMPLE_NAMES);
        //StringBuilder sb = new StringBuilder();
        //for (String name : sampleNames1) {
        //    sb.append(name);
        //    sb.append(';');
        //}

        // Prepare an array of strings that combines columns into sample names
        ArrayList<StringBuilder> sampleListArray = new ArrayList<>();
        Data sampleNames = getInputDataSet(Input_SAMPLE_NAMES);
        int colNo = sampleNames.getColumnCount();
        for (int c = 0; c < colNo; c++) {
            Column snCol = sampleNames.column(c);
            int rows = snCol.getRows();
            for (int row = 0; row < rows; row++) {
                StringBuilder sb;
                if (row >= sampleListArray.size()) {
                    sb = new StringBuilder();
                    sampleListArray.add(sb);
                } else {
                    sb = sampleListArray.get(row);
                }
                sb.append(snCol.getStringValue(row));
                sb.append(';');
            }
        }

        // Check the sample list array and remove the trailing semicolon
        int row = 0;
        for (StringBuilder sb : sampleListArray) {
            if (sb.length() == 0) {
                throw new IllegalArgumentException("Missing sample names in row: " + row);
            } else {
                // Get rid of the last semicolon
                sb.setLength(sb.length() - 1);
            }
            row++;
        }

        // Check whether the number of sample names match the number of input files 
        if (sampleListArray.size() != avFiles.getFileCount()) {
            throw new IllegalArgumentException(String.format(
                    "Mismatch between sample names and input files. The numbers should be equal but the size of %s = %d, whereas %s = %d",
                    Input_SAMPLE_NAMES, sampleListArray.size(), Input_ANNOVAR_FILES, avFiles.getFileCount()));
        }

        // Prepare common input arguments and call the tool in the n-way mode
        ArrayList<String> args = new ArrayList<>();
        args.add("perl");
        args.add(CommonTools.getCommandPath(library.getWrapper(), _AnnotateCmdName).toString());
        //args.add("-samples");
        //args.add(sb.toString());

        FileWrapper outputAnnotations = new FileWrapper();

        _execute_N_way(args, avFiles, sampleListArray, outputAnnotations);

        setOutputData(Output_ANNOTATION_FILES, outputAnnotations);
    }


    private void _execute_N_way(ArrayList<String> args, FileWrapper inputFiles, ArrayList<StringBuilder> inputSampleNames, FileWrapper outputFiles)
    throws Exception
    {
        assert(inputFiles.getFileCount() == inputSampleNames.size());

        System.out.println("Running in the N-way mode.");

        int argLen = args.size();
        args.add("-samples");
        args.add(null);
        args.add("-avoutput");
        args.add(null);
        args.add("-out");
        args.add(null);

        ProcessBuilder pb = new ProcessBuilder(args);
        //pb.redirectOutput(Redirect.INHERIT);
        //pb.redirectError(Redirect.INHERIT);

        int N = inputSampleNames.size();
        for (int i = 0; i < N; i++) {
            // Prepare the INPUT arguments
            args.set(argLen + 1, inputSampleNames.get(i).toString());
            args.set(argLen + 3, inputFiles.getFilePath(i));

            // Reserve an OUTPUT file name to create a non-conflicting filename.
            File outputFile = createTempFile("output.txt", new File("."));
            outputFile.delete();
            args.set(argLen + 5, outputFile.toString());

            System.out.println("The command line arguments: " + CommonTools.toString(", ", args));

            // Start the subprocess... 
            final Process child = pb.start();

            // Add a shutdown hook to kill the child in the case the block has been terminated.
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override public void run() {
                    child.destroy();
                }
            });

            // Start threads that read child's std out and error streams.
            Thread stdOutTh = new DumperThread(child.getInputStream(), System.out, false);
            Thread stdErrTh = new DumperThread(child.getErrorStream(), System.err, false);

            stdOutTh.start();
            stdErrTh.start();

            //... And wait for a result
            int exitCode = child.waitFor();

            // Wait for dumpers. This should be quick once the process has finished.
            stdOutTh.join();
            stdErrTh.join();

            if (exitCode != 0) {
                System.out.println("=====================================================");
                System.out.println("IGM Annotate " + _AnnotateCmdName + " exited with code = " + exitCode);
                throw new Exception("IGM Annotate " + _AnnotateCmdName + " failed; see output message for more details.");
            }

            outputFiles.addFile(outputFile);
        }
    }
}