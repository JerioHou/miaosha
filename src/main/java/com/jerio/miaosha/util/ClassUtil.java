package com.jerio.miaosha.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Franky on 2018/09/13
 *
 *
 */
public class ClassUtil {


    /**
     *  根据报名获取 Class文件，默认循环迭代子目录
     * @param packageName 包名
     *
     */
    public static List<Class<?>> getClassesByPackageName(String packageName){
        return getClassesByPackageName(packageName,true);
    }

    /**
     *
     * 根据报名获取 Class文件
     * @param packageName  包名
     * @param recursive    是否循环迭代子目录
     *
     */
    public static List<Class<?>> getClassesByPackageName(String packageName, final boolean recursive){

        // 第一个class类的集合
        List<Class<?>> classes = new ArrayList<Class<?>>();
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        //包路径
        String packagePath =Thread.currentThread().getContextClassLoader().getResource("").getPath()+packageDirName;


        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("目录不存在");
            return null;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });

        for (File file : dirfiles){
            if (file.isDirectory()) {
                getClassesByPackageName(packageName+"."+file.getName(),recursive);
            }else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(packageName + "." + className));
                }catch (ClassNotFoundException e){
                    e.printStackTrace();
                }
            }
        }
        return classes;
    }
}
