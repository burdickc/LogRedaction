package com.codetest;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        // create a scanner to read the command-line input, to obtain the path to the logs directory
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nPlease enter the path to the logs directory: ");
        String pathToLogs = scanner.next();

        File dir = new File(pathToLogs);
        File[] directoryListing = dir.listFiles();
        String extension = "";

        //iterate through all files in the user provided directory
        if (directoryListing != null) {

            for (File child : directoryListing) {
                String fileName = child.getName();

                //Check each file to locate the .gz files
                if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
                    extension = fileName.substring(fileName.lastIndexOf(".")+1);
                }
                //Only process files with a .gz extension, but not ones who have already been redacted
                if (extension.equals("gz") && !child.getName().contains("redacted")){
                    LogRedaction lr = new LogRedaction(child.getAbsolutePath(), pathToLogs);
                    try {
                        lr.processLogFile();
                    }
                    catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }
        System.out.println("\nLog Redaction Complete\n");
    }
}
