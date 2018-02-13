package com.codetest;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogRedaction {

    private String oringinalLogFileName;
    private String tempLogFileName;
    private String redactedTextFileName;
    private String newGzipFile;
    private String pathToLogs;

    public LogRedaction (String oringinalLogFileName, String pathToLogs) {
        this.oringinalLogFileName = oringinalLogFileName;
        tempLogFileName = oringinalLogFileName.substring(0, oringinalLogFileName.length()-3) + ".tmp";
        redactedTextFileName = oringinalLogFileName.substring(0, oringinalLogFileName.length()-3) + ".redacted";
        newGzipFile = oringinalLogFileName.substring(0, oringinalLogFileName.length()-3) + ".redacted.gz";
        this.pathToLogs = pathToLogs;
    }

    /**
     * Calls the GZipUtil in order to gzip or gunzip a file
     *
     * @param zip - A boolean value indicating whether to gzip or gunzip the file. True will zip, and false will unzip
     */
    private void zipOrUnzipFile(boolean zip) {
        if (zip){
            GZipUtil.compressGzipFile(redactedTextFileName, newGzipFile);
        }
        else {
            GZipUtil.decompressGzipFile(oringinalLogFileName, tempLogFileName);
        }
    }

    /**
     * Cleans up leftover temp files that were created during the processing step
     */
    private void cleanupTempFiles() {
        File tempText = new File(tempLogFileName);
        File tempRedacted = new File(redactedTextFileName);
        tempText.delete();
        tempRedacted.delete();
    }

    /**
     * Creates new Audit Log file to track the redaction information. Add to the existing log if it exists
     *
     * @param oringinalLogFileName - Name of the redacted file
     * @param pathToLogs - Path to the log files
     * @param numberOfLinesProcessed - Number of lines processed in the original log file
     * @param numberOfLinesRedacted - Number of lines modified in the redacted file
     */
    private void writeToAuditLog(String oringinalLogFileName, String pathToLogs, int numberOfLinesProcessed, int numberOfLinesRedacted) {
        File file = new File(pathToLogs + "/audit-log.txt");
        try {
            //Check for an existing Audit Log file
            if (!file.exists()) {
                file.createNewFile();
            }

            //Write to the audit log file
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            {
                String timeStamp = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date());
                out.println("Timestamp: " + timeStamp +
                        "  |  Modified Filename: " + oringinalLogFileName +
                        "  |  Number of Lines Processed: " + numberOfLinesProcessed +
                        "  |  Number of Lines Redacted: " + numberOfLinesRedacted);

                out.flush();
                out.close();
            }
        } catch (IOException e) {
           e.printStackTrace();
        }
    }

    /**
     * Processes the original log file, and redacts any Social Security or Credit Card informtaion in a new newly created file
     */
    public void processLogFile() throws IOException {
        //gunzip the original log file
        zipOrUnzipFile(false);

        FileReader input = new FileReader(tempLogFileName);
        BufferedReader in = new BufferedReader(input);
        List<String> file = new ArrayList();
        int numberOfLinesProcessed = 0;
        int numberOfLinesRedacted = 0;
        String myLine;

        while ((myLine = in.readLine()) != null)
        {
            numberOfLinesProcessed++;

            //if the line from the original file does not contain sensitive information, write it to the new file
            if (!myLine.contains("SSN") && !myLine.contains("CC")) {
                file.add(myLine);
            }
            else {
                numberOfLinesRedacted++;
                //Split the string into an array, to extract the non-sensitive information
                String[] logArray = myLine.split(" ");
                List<String> redactedLine = new ArrayList();
                for (String element : logArray) {
                    if (!element.contains("SSN") && !element.contains("CC")){
                        redactedLine.add(element);
                    }
                }

                //Build a new string that has been redacted
                StringBuilder builder = new StringBuilder(redactedLine.size());
                for(String string : redactedLine)
                {
                    builder.append(string + " ");
                }
                myLine = builder.toString();

                //Clean up any trailing commas
                if (myLine.endsWith(", ")) {
                    myLine = myLine.substring(0, myLine.length() - 2);
                }
                //Add the redacted line to the new file
                file.add(myLine);
            }

        }
        in.close();

        //Write the redacted Log file
        FileWriter fw = new FileWriter(redactedTextFileName);
        BufferedWriter out = new BufferedWriter(fw);
        for (String line : file) {
            out.write(line + "\n");
        }
        out.flush();
        out.close();

        //GZip the redacted log file and clean up temp files
        zipOrUnzipFile(true);
        cleanupTempFiles();

        //Write to Audit Log
        writeToAuditLog(oringinalLogFileName, pathToLogs, numberOfLinesProcessed, numberOfLinesRedacted);
    }

}
