package com.thatguysservice.huami_xdrip.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
    public static boolean makeSureDirectoryExists( final String dir ) {
        final File file = new File( dir );
        return file.exists() || file.mkdirs();
    }

    public static void deleteFile(String filePath){
        File myFile = new File(filePath);
        if (myFile.exists()) {
            myFile.delete();
        }
    }

    public static String getExternalDir() {
        final StringBuilder sb = new StringBuilder();
        sb.append( Environment.getExternalStorageDirectory().getAbsolutePath() );
        sb.append( "/xdrip" );

        return sb.toString();
    }

    public static String getDownloadFolder(Context c) {
        return c.getCacheDir().getAbsolutePath();
    }

    public static void unzip(String zipFile, String location)
    {
        try
        {
            FileInputStream fin = new FileInputStream(zipFile);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null)
            {
                if(ze.isDirectory())
                {
                    dirChecker(location +  ze.getName());
                }
                else
                {
                    FileOutputStream fout = new FileOutputStream(location + ze.getName());
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zin.read(buffer)) != -1)
                    {
                        fout.write(buffer, 0, len);
                    }
                    fout.close();
                    zin.closeEntry();
                }
            }
            zin.close();
        }
        catch(Exception e)
        {
            throw new RuntimeException("Could not unzip file:", e);
        }
    }

    private static void dirChecker(String dir)
    {
        File f = new File( dir);
        if(!f.isDirectory())
        {
            f.mkdirs();
        }
    }
}
