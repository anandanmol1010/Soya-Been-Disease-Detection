package com.example.soyabeendiseasedetection;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class TFLiteClassifier {
    private Interpreter interpreter;
    private List<String> labels;
    private final int IMAGE_SIZE = 224;

    public TFLiteClassifier(Context context) throws IOException {
        interpreter = new Interpreter(FileUtil.loadMappedFile(context, "model.tflite"));
        labels = FileUtil.loadLabels(context, "labels.txt");
    }

    public String classify(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);
        ByteBuffer input = convertBitmapToByteBuffer(resized);

        float[][] output = new float[1][labels.size()];
        interpreter.run(input, output);

        int maxIndex = 0;
        float maxConfidence = 0;
        for (int i = 0; i < output[0].length; i++) {
            if (output[0][i] > maxConfidence) {
                maxConfidence = output[0][i];
                maxIndex = i;
            }
        }

        return labels.get(maxIndex);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
        bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int pixel : pixels) {
            buffer.putFloat(((pixel >> 16) & 0xFF) / 255.f);  // R
            buffer.putFloat(((pixel >> 8) & 0xFF) / 255.f);   // G
            buffer.putFloat((pixel & 0xFF) / 255.f);          // B
        }

        return buffer;
    }
}
