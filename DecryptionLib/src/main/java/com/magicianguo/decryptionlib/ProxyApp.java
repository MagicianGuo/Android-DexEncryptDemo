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
    private Application delegate;
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

    @Override
    public void onCreate() {
        super.onCreate();
        bindOriginApplication();
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

    private void bindOriginApplication() {
        if (TextUtils.isEmpty(app_name)) {
            return;
        }
        try {
            //1. 得到 attachBaseContext(context) 传入的上下文 ContextImpl
            Context baseContext = getBaseContext();
            //2. 拿到真实 APK APPlication 的 class
            Class<?> delegateClass = Class.forName(app_name);
            //3. 反射实例化，其实 Android 中四大组件都是这样实例化的。
            delegate = (Application) delegateClass.newInstance();

            //3.1 得到 Application attach() 方法 也就是最先初始化的
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            attach.setAccessible(true);
            //执行 Application#attach(Context)
            //3.2 将真实的 Application 和假的 Application 进行替换。想当于自己手动控制 真实的 Application 生命周期
            attach.invoke(delegate, baseContext);


//        ContextImpl---->mOuterContext(app)   通过Application的attachBaseContext回调参数获取
            //4. 拿到 Context 的实现类
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            //4.1 获取 mOuterContext Context 属性
            Field mOuterContextField = contextImplClass.getDeclaredField("mOuterContext");
            mOuterContextField.setAccessible(true);
            //4.2 将真实的 Application 交于 Context 中。这个根据源码执行，实例化 Application 下一个就行调用 setOuterContext 函数，所以需要绑定 Context
            //  app = mActivityThread.mInstrumentation.newApplication(
            //                    cl, appClass, appContext);
            //  appContext.setOuterContext(app);
            mOuterContextField.set(baseContext, delegate);

//        ActivityThread--->mAllApplications(ArrayList)       ContextImpl的mMainThread属性
            //5. 拿到 ActivityThread 变量
            Field mMainThreadField = contextImplClass.getDeclaredField("mMainThread");
            mMainThreadField.setAccessible(true);
            //5.1 拿到 ActivityThread 对象
            Object mMainThread = mMainThreadField.get(baseContext);

//        ActivityThread--->>mInitialApplication
            //6. 反射拿到 ActivityThread class
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            //6.1 得到当前加载的 Application 类
            Field mInitialApplicationField = activityThreadClass.getDeclaredField("mInitialApplication");
            mInitialApplicationField.setAccessible(true);
            //6.2 将 ActivityThread 中的 Applicaiton 替换为 真实的 Application 可以用于接收相应的声明周期和一些调用等
            mInitialApplicationField.set(mMainThread, delegate);


//        ActivityThread--->mAllApplications(ArrayList)       ContextImpl的mMainThread属性
            //7. 拿到 ActivityThread 中所有的 Application 集合对象，这里是多进程的场景
            Field mAllApplicationsField = activityThreadClass.getDeclaredField("mAllApplications");
            mAllApplicationsField.setAccessible(true);
            ArrayList<Application> mAllApplications = (ArrayList<Application>) mAllApplicationsField.get(mMainThread);
            //7.1 删除 ProxyApplication
            mAllApplications.remove(this);
            //7.2 添加真实的 Application
            mAllApplications.add(delegate);

//        LoadedApk------->mApplication                      ContextImpl的mPackageInfo属性
            //8. 从 ContextImpl 拿到 mPackageInfo 变量
            Field mPackageInfoField = contextImplClass.getDeclaredField("mPackageInfo");
            mPackageInfoField.setAccessible(true);
            //8.1 拿到 LoadedApk 对象
            Object mPackageInfo = mPackageInfoField.get(baseContext);

            //9 反射得到 LoadedApk 对象
            //    @Override
            //    public Context getApplicationContext() {
            //        return (mPackageInfo != null) ?
            //                mPackageInfo.getApplication() : mMainThread.getApplication();
            //    }
            Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
            Field mApplicationField = loadedApkClass.getDeclaredField("mApplication");
            mApplicationField.setAccessible(true);
            //9.1 将 LoadedApk 中的 Application 替换为 真实的 Application
            mApplicationField.set(mPackageInfo, delegate);

            //修改ApplicationInfo className   LooadedApk

            //10. 拿到 LoadApk 中的 mApplicationInfo 变量
            Field mApplicationInfoField = loadedApkClass.getDeclaredField("mApplicationInfo");
            mApplicationInfoField.setAccessible(true);
            //10.1 根据变量反射得到 ApplicationInfo 对象
            ApplicationInfo mApplicationInfo = (ApplicationInfo) mApplicationInfoField.get(mPackageInfo);
            //10.2 将我们真实的 APPlication ClassName 名称赋值于它
            mApplicationInfo.className = app_name;

            //11. 执行 代理 Application onCreate 生命周期
            delegate.onCreate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
