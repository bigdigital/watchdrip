package com.thatguysservice.huami_xdrip.models;

import com.activeandroid.Model;
import com.activeandroid.util.SQLiteUtils;

public class PlusModel extends Model {

    protected synchronized static boolean fixUpTable(String[] schema, boolean patched) {
        if (patched) return true;

        for (String patch : schema) {
            try {
                SQLiteUtils.execSql(patch);
            } catch (Exception e) {
                //
            }
        }
        return true;
    }

}
