package com.codetest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogRedaction {

    private String originalLogFileName;
    private String tempLogFileName;
    private String redactedTextFileName;
    private String newGzipFile;
    private String pathToLogs;

    public LogRedaction (String originalLogFileName, String pathToLogs) {
        this.originalLogFileName = originalLogFileName;
        tempLogFileName = originalLogFileName.substring(0, originalLogFileName.length()-3) + ".tmp";
        redactedTextFileName = originalLogFileName.substring(0, originalLogFileName.length()-3) + ".redacted";
        newGzipFile = originalLogFileName.substring(0, originalLogFileName.length()-3) + ".redacted.gz";
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
            GZipUtil.decompressGzipFile(originalLogFileName, tempLogFileName);
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
     * @param originalLogFileName - Name of the original log file
     * @param pathToLogs - Path to the log files
     * @param numberOfLinesProcessed - Number of lines processed in the original log file
     * @param numberOfLinesRedacted - Number of lines modified in the redacted file
     */
    private void writeToAuditLog(String originalLogFileName, String pathToLogs, int numberOfLinesProcessed, int numberOfLinesRedacted) {
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
                        "  |  Modified Filename: " + originalLogFileName +
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
     * Preserves the original log file metadata / file permissions, group, and owner.
     * Persists these attributes to the redacted log file
     *
     * @param originalLogFileName - Name of the original log file
     * @param newGzipFile - Name of the newly created, redacted and gzipped log file
     */
    private void preserveFileMetadataAndPermissions(String originalLogFileName, String newGzipFile) throws IOException {
        Path originalFilePath = Paths.get(originalLogFileName);    //get the paths to the original and redacted files
        Path newFilePath = Paths.get(newGzipFile);
        BasicFileAttributes bfa = Files.readAttributes(originalFilePath, BasicFileAttributes.class);

        //set the metadata of the redacted file to match the original log file
        Files.setAttribute(newFilePath, "basic:creationTime", bfa.creationTime());
        Files.setAttribute(newFilePath, "basic:lastAccessTime", bfa.lastAccessTime());
        Files.setAttribute(newFilePath, "basic:lastModifiedTime", bfa.lastModifiedTime());

        File orginalFile = new File(originalLogFileName);
        File newFile = new File(newGzipFile);
        if(orginalFile.exists() && newFile.exists()){

            //set the permissions to match the original log file
            newFile.setExecutable(orginalFile.canExecute());
            newFile.setWritable(orginalFile.canWrite());
            newFile.setReadable(orginalFile.canRead());

            //set the group / owner of the new file to match the original log file
            GroupPrincipal group = Files.readAttributes(originalFilePath, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group();
            UserPrincipal owner =  Files.readAttributes(originalFilePath, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).owner();
            Files.getFileAttributeView(newFilePath, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
            Files.getFileAttributeView(newFilePath, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setOwner(owner);
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
        preserveFileMetadataAndPermissions(originalLogFileName, newGzipFile);
        cleanupTempFiles();

        //Write to Audit Log
        writeToAuditLog(originalLogFileName, pathToLogs, numberOfLinesProcessed, numberOfLinesRedacted);
    }
}
