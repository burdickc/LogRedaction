package com.codetest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for gunzipping and gzipping the source log files
 */
public class GZipUtil {

    /**
     * Decompresses the original source file, and writes to a new temp file
     *
     * @param gzipFile - Filename for the original source file
     * @param newFile - Filename for the uncompressed temp file
     */
    protected static void decompressGzipFile(String gzipFile, String newFile) {
        try {
            FileInputStream fis = new FileInputStream(gzipFile);
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream(newFile);
            byte[] buffer = new byte[1024];
            int len;
            while((len = gis.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            gis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Used to compresses the redacted temp file
     *
     * @param redactedTextFileName - Filename for uncompressed temp file
     * @param gzipFile - Filename for the compressed and redacted file
     */
    protected static void compressGzipFile(String redactedTextFileName, String gzipFile) {
        try {
            FileInputStream fis = new FileInputStream(redactedTextFileName);
            FileOutputStream fos = new FileOutputStream(gzipFile);
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[1024];
            int len;
            while((len=fis.read(buffer)) != -1){
                gzipOS.write(buffer, 0, len);
            }
            //close resources
            gzipOS.close();
            fos.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}