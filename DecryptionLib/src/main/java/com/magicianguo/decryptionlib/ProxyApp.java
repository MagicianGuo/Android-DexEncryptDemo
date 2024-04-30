package com.magicianguo.decryptionlib;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ProxyApp extends Application {
    private String app_name;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        getMetaData();
        File apkFile = new File(getApplicationInfo().sourceDir);

        File versionDir = getDir("XXX", Context.MODE_PRIVATE);
        File appDir = new File(versionDir, "app");
        File dexDir = new File(appDir, "dexDir");

        List<File> dexFiles = new ArrayList<>();
        if (!dexDir.exists() || dexDir.list().length == 0) {
            FileUtils.unZip(apkFile, appDir);
            dexDir.mkdirs();
            File[] files = appDir.listFiles();
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".dex") && !TextUtils.equals(name, "classes.dex")) {
                    try {
                        byte[] bytes = FileUtils.getBytes(file);
                        byte[] decrypt = EncryptUtils.decrypt(bytes, EncryptUtils.BYTES);
                        File file1 = new File(dexDir.getPath() + "/" + name);
                        file1.createNewFile();
                        FileOutputStream fos = new FileOutputStream(file1);
                        fos.write(decrypt);
                        fos.flush();
                        fos.close();
                        dexFiles.add(file1);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            for (File file : dexDir.listFiles()) {
                dexFiles.add(file);
            }
        }

        try {
            loadDex(dexFiles, versionDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getMetaData() {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;
            if (null != metaData) {
                if (metaData.containsKey("app_name")) {
                    app_name = metaData.getString("app_name");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDex(List<File> dexFiles, File versionDir) throws Exception {
        Field pathListField = ReflectUtils.findField(getClassLoader(), "pathList");
        Object pathList = pathListField.get(getClassLoader());
        Field dexElementsField = ReflectUtils.findField(pathList, "dexElements");
        Object[] dexElements = (Object[]) dexElementsField.get(pathList);
        Method makeDexElements = ReflectUtils.findMethod(pathList, "makePathElements", List.class, File.class, List.class);
        ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
        Object[] addElements = (Object[]) makeDexElements.invoke(pathList, dexFiles, versionDir, suppressedExceptions);
        Object[] newElements = (Object[]) Array.newInstance(dexElements.getClass().getComponentType(), dexElements.length + addElements.length);
        System.arraycopy(dexElements, 0, newElements, 0, dexElements.length);
        System.arraycopy(addElements, 0, newElements, dexElements.length, addElements.length);
        dexElementsField.set(pathList, newElements);
    }
}
