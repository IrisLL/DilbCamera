package com.example.dlibtest;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Utility class for manipulating images.
 **/
public class ImageUtils {
    private static final String TAG = ImageUtils.class.getSimpleName();
    public static int getYUVByteSize(final int width, final int height) {
        // The luminance plane requires 1 byte per pixel.
        final int ySize = width * height;

        // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
        // Each 2x2 block takes 2 bytes to encode, one each for U and V.
        final int uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2;

        return ySize + uvSize;
    }

    public static void saveBitmap(final Bitmap bitmap) {
        final String root =
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "dlib";
        Log.d(TAG,String.format("Saving %dx%d bitmap to %s.", bitmap.getWidth(), bitmap.getHeight(), root));
        final File myDir = new File(root);

        if (!myDir.mkdirs()) {
            Log.e(TAG,"Make dir failed");
        }

        final String fname = "preview.png";
        final File file = new File(myDir, fname);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            Log.e(TAG,"Exception!", e);
        }
    }
    /**
     * Converts YUV420 semi-planar data to ARGB 8888 data using the supplied width
     * and height. The input and output must already be allocated and non-null.
     * For efficiency, no error checking is performed.
     *
     * @param y
     * @param u
     * @param v
     * @param uvPixelStride
     * @param width         The width of the input image.
     * @param height        The height of the input image.
     * @param halfSize      If true, downsample to 50% in each dimension, otherwise not.
     * @param output        A pre-allocated array for the ARGB 8:8:8:8 output data.
     */



    public static native void convertYUV420ToARGB8888(
            byte[] y,
            byte[] u,
            byte[] v,
            int[] output,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            boolean halfSize);


     public static native void convertYUV420SPToARGB8888(
            byte[] input, int[] output, int width, int height, boolean halfSize);



   /*
    public static native void convertARGB8888ToYUV420SP(
            int[] input, byte[] output, int width, int height);



 public static native void convertYUV420SPToRGB565(
            byte[] input, byte[] output, int width, int height);

    public static native void convertRGB565ToYUV420SP(
            byte[] input, byte[] output, int width, int height);*/
            
}
