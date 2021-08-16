package com.boymask.edocs;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Util {
    public static String convertDateToFull(String input) {
        if (input == null) {
            return null;
        }
        try {
    //        return new SimpleDateFormat("yyMMdd", Locale.US)
     //               .format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(input));

            return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(new SimpleDateFormat("yyMMdd", Locale.US).parse(input));
        } catch (ParseException e) {
            Log.w("WW", e);
            return null;
        }
    }

}
