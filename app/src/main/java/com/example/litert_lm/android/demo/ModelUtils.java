package com.example.litert_lm.android.demo;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ModelUtils {

    public static File copyModelIfNeeded(Context context, String assetName) {

        File outFile = new File(context.getFilesDir(), assetName);

        // 已存在就直接返回
        if (outFile.exists() && outFile.length() > 0) {
            return outFile;
        }

        try (InputStream is = context.getAssets().open(assetName);
            FileOutputStream os = new FileOutputStream(outFile)) {

            byte[] buffer = new byte[1024 * 1024];
            int len;

            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }

            os.flush();

        } catch (Exception e) {
            throw new RuntimeException("failed to copy model " + e.getMessage(), e);
        }

        return outFile;
    }

}
