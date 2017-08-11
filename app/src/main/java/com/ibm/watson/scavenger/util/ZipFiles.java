package com.ibm.watson.scavenger.util;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by arpitrastogi on 09/08/17.
 */

public class ZipFiles {

    public File createAndAddZipFiles(String zipName, List<File> img_list) {
        File zipFile = new File(makeTmpFolder()+File.separator+zipName);
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for(File img_jpg:img_list) {
                addToZipFile(img_jpg, zos);
            }
            zos.close();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipFile;
    }

    private File makeTmpFolder(){

        File root = new File(Environment.getExternalStorageDirectory()
                + File.separator + "watson");
        boolean mainfolderexist = root.exists();
        if (!mainfolderexist) {
            try {
                if (Environment.getExternalStorageDirectory().canWrite()) {
                    root.mkdirs();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return root;
    }

    public static void addToZipFile(File file, ZipOutputStream zos) throws FileNotFoundException, IOException {

        System.out.println("Writing '" + file.getName() + "' to zip file");

        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(file.getName());
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }
}
