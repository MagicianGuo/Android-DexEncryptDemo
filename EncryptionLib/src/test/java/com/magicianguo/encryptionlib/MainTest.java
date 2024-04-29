package com.magicianguo.encryptionlib;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainTest {
    /**
     * 工作路径
     */
    private static final String PATH_WORKSPACE = "../workspace";
    /**
     * 签名文件
     */
    private static final String PATH_KEYSTORE = "../testks.jks";
    /**
     * 需要自己配置build-tools路径。建议使用版本30或以下，30以上没有dx.bat文件
     */
    private static final String PATH_BUILD_TOOLS = "E:\\Android\\SDK\\build-tools\\30.0.3";
    private static final String CMD_DX = PATH_BUILD_TOOLS + "\\dx.bat --dex --output ";
    private static final String CMD_ZIPALIGN = PATH_BUILD_TOOLS + "\\zipalign.exe -v -p  4 ";
    private static final String CMD_APKSIGNER = PATH_BUILD_TOOLS + "\\apksigner.bat sign --ks ";

    /**
     * 执行Apk加密
     */
    @Test
    public void doEncryption() throws Exception {
        System.out.println("*************************");
        makeAarDex();
        encryptApkDex();
        makeNewApk();
        zipAlign();
        signApk();
        System.out.println("执行成功！\n\n");
    }

    private void makeAarDex() throws Exception {
        File aarFile = new File(PATH_WORKSPACE + "/DecryptionLib.aar");
        if (!aarFile.exists()) {
            throw new RuntimeException("DecryptionLib.aar不存在！请先编译DecryptionLib模块！");
        }
        File unzipAarDir = new File(PATH_WORKSPACE + "/unzip-aar/");
        System.out.println("正在解压DecryptionLib.aar……");
        FileUtils.unZip(aarFile, unzipAarDir);
        System.out.println("已解压DecryptionLib.aar。");

        File jarFile = new File(unzipAarDir, "classes.jar");
        File dexFile = new File(unzipAarDir, "classes.dex");
        System.out.println("正在生成dex文件……");
        Process process = Runtime.getRuntime().exec(CMD_DX + dexFile + " " + jarFile);
        consumeInputStream(process);
        process.waitFor();
        process.destroy();
        if (process.exitValue() != 0) {
            throw new RuntimeException("未能生成dex文件！请检查dx的路径配置！");
        }
        System.out.println("已生成dex文件。");
    }

    private void encryptApkDex() throws Exception {
        File apkFile = new File(PATH_WORKSPACE + "/App-origin.apk");
        if (!apkFile.exists()) {
            throw new RuntimeException("App-origin.apk不存在！请先编译App模块！");
        }
        File unzipApkDir = new File(PATH_WORKSPACE + "/unzip-apk/");
        System.out.println("正在解压App-origin.apk……");
        FileUtils.unZip(apkFile, unzipApkDir);
        System.out.println("已解压App-origin.apk。");
        File[] dexFiles = unzipApkDir.listFiles((dir, name) -> name.endsWith(".dex"));
        for (File dexFile : dexFiles) {
            byte[] bytes = FileUtils.getBytes(dexFile);
            byte[] encrypt = EncryptUtils.encrypt(bytes, EncryptUtils.BYTES);
            FileOutputStream fos = new FileOutputStream(new File(unzipApkDir,
                    "x-" + dexFile.getName()));
            fos.write(encrypt);
            fos.flush();
            fos.close();
            dexFile.delete();
        }
        System.out.println("已加密APK中的dex文件。");
    }

    private void makeNewApk() throws Exception {
        File unzipAarDir = new File(PATH_WORKSPACE + "/unzip-aar/");
        File unzipApkDir = new File(PATH_WORKSPACE + "/unzip-apk/");
        File classesDex = new File(unzipAarDir, "classes.dex");
        classesDex.renameTo(new File(unzipApkDir, "classes.dex"));
        File unsignedApkFile = new File(PATH_WORKSPACE + "/App-unsigned.apk");
        FileUtils.zip(unzipApkDir, unsignedApkFile);
        System.out.println("已生成App-unsigned.apk");
    }

    private void zipAlign() throws Exception {
        File unSignedApkFile = new File(PATH_WORKSPACE+"/App-unsigned.apk");
        File alignedApkFile = new File(PATH_WORKSPACE+"/App-unsigned-aligned.apk");
        alignedApkFile.delete();
        Thread.sleep(1000);
        Process process = Runtime.getRuntime().exec(CMD_ZIPALIGN + unSignedApkFile.getAbsolutePath()
                + " " + alignedApkFile.getAbsolutePath());
        consumeInputStream(process);
        process.waitFor();
        process.destroy();
        if (process.exitValue() != 0) {
            throw new RuntimeException("对齐失败！");
        }
        System.out.println("已生成App-unsigned-aligned.apk");
    }

    private void signApk() throws Exception {
        File signedApkFile = new File(PATH_WORKSPACE + "/App-signed-aligned.apk");
        File jksFile = new File(PATH_KEYSTORE);
        File alignedApkFile = new File(PATH_WORKSPACE+"/App-unsigned-aligned.apk");

        Process process = Runtime.getRuntime().exec(CMD_APKSIGNER + jksFile.getAbsolutePath()
                + " --ks-key-alias testks --ks-pass pass:testks --key-pass pass:testks --out "
                + signedApkFile.getAbsolutePath() + " " + alignedApkFile.getAbsolutePath());
        consumeInputStream(process);
        process.waitFor();
        process.destroy();
        if (process.exitValue() != 0) {
            throw new RuntimeException("打包失败！");
        }
        System.out.println("打包成功！路径：" + signedApkFile.getAbsolutePath());
    }

    /**
     * 消费掉产生的字节流，防止卡死
     */
    private void consumeInputStream(Process process) {
        new Thread() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = process.getInputStream();
                    InputStream errorStream = process.getErrorStream();
                    BufferedReader br1 = new BufferedReader(new InputStreamReader(inputStream));
                    while (br1.readLine() != null) {
                    }
                    br1.close();
                    BufferedReader br2 = new BufferedReader(new InputStreamReader(errorStream));
                    while (br2.readLine() != null) {
                    }
                    br2.close();
                } catch (Exception ignore) {
                }
            }
        }.start();
    }
}
