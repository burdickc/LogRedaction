# LogRedaction

## Description: 

This utility accepts as input one or more text log files that have been compressed with the gzip algorithm. The user is prompted to provide the path to the directory containing the orginial log files.  The utility will redact social security, and credit card information from the original log files, as well as write redacted, gzipped versions of the files to the same directory.  The utility will preserve the original log file metadata / file permissions, group, and owner and persist them to the new files. An audit log file is created in the original directory that includes a timestamp indicating when the file was processed, the name of each file processed, a count of the total number of lines processed in each log file, and a count of the total number of lines redacted from each log file.

### Build Instructions:

- Clone the repository using: **git clone https://github.com/burdickc/LogRedaction.git**

- Change directory, to the root directory of the project: **cd LogRedaction/**

- In this directory there is a pom.xml. Build using maven: **mvn clean package**

### Usage Instructions

- Navigate to the newly created target directory: **cd target/**

- Run the jar using the following command: **java -jar LogRedaction-1.0-SNAPSHOT.jar**

- The user will be prompted to enter a path to the directory containing the original gzipped log files.

- Provide the full path to the logs directory on the system and press enter. For example: **/Users/test.user/Downloads/logs**

- The redacted log files, and audit log will be written to the provided directory.