package com.example.proyecto_vison;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImagePickerActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_SELECT_IMAGE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String TAG = "ImagePickerActivity";
    private JSONObject modelParams;
    private ImageView imageView;
    private ImageView imageViewGrayscale;
    private TextView textPredictedNumber;

    static {
        System.loadLibrary("proyecto_vison");
    }

    public native float[] extractHOG(long matAddrInput, long matAddrOutput);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_picker);

        imageView = findViewById(R.id.image_view);
        imageViewGrayscale = findViewById(R.id.image_view_grayscale); // Inicializar la ImageView para la imagen en escala de grises
        textPredictedNumber = findViewById(R.id.text_predicted_number);  // Inicializar el TextView
        Button btnTakePhoto = findViewById(R.id.btn_take_photo);
        Button btnSelectPhoto = findViewById(R.id.btn_select_photo);
        Button btnProcesarHOG = findViewById(R.id.btn_procesar_imagenes_hog);

        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                clearImages();
                takePhoto();
            }
        });

        btnSelectPhoto.setOnClickListener(v -> {
            clearImages();
            selectPhoto();
        });

        btnProcesarHOG.setOnClickListener(v -> {
            procesarImagenHOG(); // Llamada al método para procesar la imagen HOG
        });

        // Cargar el modelo desde los assets al iniciar la actividad
        modelParams = loadModelFromAssets(getAssets(), "model_params.json");
    }

    private void clearImages() {
        imageView.setImageDrawable(null);
        imageViewGrayscale.setImageDrawable(null); // Limpiar la ImageView para la imagen en escala de grises
        textPredictedNumber.setText("");
    }

    private JSONObject loadModelFromAssets(AssetManager assetManager, String fileName) {
        try {
            InputStream is = assetManager.open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            Log.d(TAG, "Modelo cargado correctamente desde los assets.");
            return new JSONObject(json);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error al cargar el modelo desde los assets.");
            return null;
        }
    }

    private int predict(float[] hogFeatures, JSONObject modelParams) {
        try {
            JSONArray weightsArray = modelParams.getJSONArray("weights");
            JSONArray biasesArray = modelParams.getJSONArray("biases");

            List<Float> inputFeatures = new ArrayList<>();
            for (float feature : hogFeatures) {
                inputFeatures.add(feature);
            }

            int outputLayerSize = biasesArray.getJSONArray(biasesArray.length() - 1).length();
            float[] output = new float[outputLayerSize];
            Log.d(TAG, "output length: " + output.length);

            for (int layer = 0; layer < weightsArray.length(); layer++) {
                JSONArray weightsLayer = weightsArray.getJSONArray(layer);
                JSONArray biasesLayer = biasesArray.getJSONArray(layer);
                int layerSize = biasesLayer.length();
                float[] newOutput = new float[layerSize];

                for (int i = 0; i < layerSize; i++) {
                    newOutput[i] = (float) biasesLayer.getDouble(i);
                }

                for (int i = 0; i < layerSize; i++) {
                    JSONArray weightsNeuron = weightsLayer.getJSONArray(i);
                    for (int j = 0; j < weightsNeuron.length(); j++) {
                        if (j < inputFeatures.size()) {
                            newOutput[i] += inputFeatures.get(j) * (float) weightsNeuron.getDouble(j);
                        }
                    }
                    if (layer < weightsArray.length() - 1) {
                        newOutput[i] = Math.max(0, newOutput[i]); // ReLU activation
                    }
                }

                inputFeatures.clear();
                for (float v : newOutput) {
                    inputFeatures.add(v);
                }
                output = newOutput;
            }

            // Softmax for the output layer
            float sumExp = 0;
            for (float v : output) {
                sumExp += Math.exp(v);
            }
            for (int i = 0; i < output.length; i++) {
                output[i] = (float) Math.exp(output[i]) / sumExp;
            }

            Log.d(TAG, "Output array: " + Arrays.toString(output));

            int predictedLabel = 0;
            float max = output[0];
            for (int i = 1; i < output.length; i++) {
                if (output[i] > max) {
                    max = output[i];
                    predictedLabel = i;
                }
            }

            Log.d(TAG, "Predicted Label: " + predictedLabel);

            // Imprimir la probabilidad y características HOG
            Log.d(TAG, "Probabilities: " + Arrays.toString(output));
            Log.d(TAG, "HOG Features Shape: (" + 1 + ", " + hogFeatures.length + ")");
            Log.d(TAG, "HOG Features: " + Arrays.toString(hogFeatures));

            return predictedLabel;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error en la predicción.");
            return -1;
        }
    }

    private void procesarImagenHOG() {
        new Thread(() -> {
            try {
                // Obtener el bitmap de la ImageView
                Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                Mat matOriginal = new Mat();
                Utils.bitmapToMat(bitmap, matOriginal);

                Log.d(TAG, "Original Mat size: " + matOriginal.size());

                // Convertir a escala de grises
                Mat matGrayscale = new Mat();
                Imgproc.cvtColor(matOriginal, matGrayscale, Imgproc.COLOR_BGR2GRAY);
                Log.d(TAG, "Grayscale Mat size: " + matGrayscale.size());

                // Crear una Mat para la imagen HOG procesada
                Mat matHOG = new Mat();

                // Extraer características HOG y obtener la imagen HOG
                float[] hogFeatures = extractHOG(matGrayscale.getNativeObjAddr(), matHOG.getNativeObjAddr());

                // Verificar el tamaño de matHOG
                Log.d(TAG, "HOG Mat size: " + matHOG.size());

                // Asegurarse de que matHOG tenga un tamaño válido
                if (matHOG.cols() > 0 && matHOG.rows() > 0) {
                    // Convertir la imagen procesada a Bitmap
                    Bitmap bitmapGrayscale = Bitmap.createBitmap(matHOG.cols(), matHOG.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(matHOG, bitmapGrayscale);
                    Log.d(TAG, "Bitmap Grayscale size: " + bitmapGrayscale.getWidth() + "x" + bitmapGrayscale.getHeight());

                    // Realizar predicción
                    int predictedNumber = predict(hogFeatures, modelParams);

                    runOnUiThread(() -> {
                        textPredictedNumber.setText(String.format("Predicción: %d", predictedNumber));
                        imageViewGrayscale.setImageBitmap(bitmapGrayscale); // Mostrar la imagen en escala de grises
                        Toast.makeText(this, "Imagen HOG procesada.", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e(TAG, "matHOG tiene tamaño inválido: " + matHOG.size());
                    runOnUiThread(() -> Toast.makeText(this, "Error: matHOG tiene tamaño inválido.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error al procesar la imagen HOG.", e);
                runOnUiThread(() -> Toast.makeText(this, "Error al procesar la imagen HOG.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(this, "Camera permission is required to take photo", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void takePhoto() {
        Log.d(TAG, "Attempting to take a photo");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (activities.size() > 0) {
            ResolveInfo resolved = activities.get(0); // Toma la primera aplicación de cámara disponible
            takePictureIntent.setClassName(resolved.activityInfo.packageName, resolved.activityInfo.name);
            Log.d(TAG, "Using camera app: " + resolved.activityInfo.packageName);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Log.e(TAG, "No camera app found");
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectPhoto() {
        Log.d(TAG, "Selecting photo from gallery");
        Intent selectPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(selectPictureIntent, REQUEST_SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imageView.setImageBitmap(imageBitmap);
            } else if (requestCode == REQUEST_SELECT_IMAGE && data != null) {
                Uri imageUri = data.getData();
                try {
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    imageView.setImageBitmap(selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
