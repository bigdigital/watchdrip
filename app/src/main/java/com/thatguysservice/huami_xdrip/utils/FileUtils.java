package com.thatguysservice.huami_xdrip.utils;

import android.os.Environment;

import java.io.File;

public class FileUtils {
    public static boolean makeSureDirectoryExists( final String dir ) {
        final File file = new File( dir );
        return file.exists() || file.mkdirs();
    }

    public static String getExternalDir() {
        final StringBuilder sb = new StringBuilder();
        sb.append( Environment.getExternalStorageDirectory().getAbsolutePath() );
        sb.append( "/xdrip" );

        final String dir = sb.toString();
        return dir;
    }
}
