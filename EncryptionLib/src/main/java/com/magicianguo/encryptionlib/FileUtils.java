package com.magicianguo.encryptionlib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtils {
    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles != null) {
                for (File childFile : childFiles) {
                    deleteFile(childFile);
                }
            }
        }
        file.delete();
    }

    public static void unZip(File file, File destDir) {
        try {
            deleteFile(destDir);
            destDir.mkdirs();
            ZipFile zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String name = zipEntry.getName();

                File f = new File(destDir, name);
                //创建目录
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
                //写文件
                FileOutputStream fos = new FileOutputStream(f);
                InputStream is = zipFile.getInputStream(zipEntry);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                is.close();
                fos.close();
            }
            zipFile.close();
        } catch (IOException e) {
            throw new RuntimeException("解压出现异常！", e);
        }
    }

    public static void zip(File dir, File destFile) throws Exception {
        destFile.delete();
        CheckedOutputStream cos = new CheckedOutputStream(new FileOutputStream(destFile), new CRC32());
        ZipOutputStream zos = new ZipOutputStream(cos);
        //压缩
        compress(dir, zos, "");
        zos.flush();
        zos.close();
    }

    private static void compress(File srcFile, ZipOutputStream zos, String basePath) throws Exception {
        if (srcFile.isDirectory()) {
            File[] files = srcFile.listFiles();
            for (File file : files) {
                compress(file, zos, basePath + srcFile.getName() + "/");
            }
        } else {
            compressFile(srcFile, zos, basePath);
        }
    }

    private static void compressFile(File file, ZipOutputStream zos, String dir) throws Exception {
        String fullName = dir + file.getName();
        String[] fileNames = fullName.split("/");
        StringBuilder sb = new StringBuilder();
        if (fileNames.length > 1){
            for (int i = 1;i<fileNames.length;++i){
                sb.append("/");
                sb.append(fileNames[i]);
            }
        }else{
            sb.append("/");
        }
        ZipEntry entry = new ZipEntry(sb.substring(1));
        zos.putNextEntry(entry);
        FileInputStream fis = new FileInputStream(file);
        int len;
        byte[] data = new byte[2048];
        while ((len = fis.read(data, 0, 2048)) != -1) {
            zos.write(data, 0, len);
        }
        fis.close();
        zos.closeEntry();
    }

    public static byte[] getBytes(File file) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        byte[] bytes = new byte[(int) raf.length()];
        raf.readFully(bytes);
        raf.close();
        return bytes;
    }
}
