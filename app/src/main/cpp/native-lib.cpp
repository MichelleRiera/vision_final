#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "parte1.h"
#include "parte2.h"

#define LOG_TAG "PROYECTO_VISION"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace cv;

extern "C" JNIEXPORT void JNICALL
Java_com_example_proyecto_1vison_MainActivity_procesarFrame(
        JNIEnv* env, jobject obj, jlong direccionMatRgba, jboolean modoDibujar) {
    Mat& frame = *(Mat*)direccionMatRgba;
    cvtColor(frame, frame, COLOR_RGBA2BGR);

    if (modoDibujar) {
        detectar(frame);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_proyecto_1vison_MainActivity_calcularMomentosHuYzernike(JNIEnv* env, jobject obj, jobject assetManager) {
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    std::string rutaBaseCarpeta = "images";
    std::vector<std::string> subcarpetas = {"circle", "square", "triangle"};

    momentosHuZernikeCirculos.clear();
    momentosHuZernikeCuadrados.clear();
    momentosHuZernikeTriangulos.clear();

    for (const auto& subcarpeta : subcarpetas) {
        std::string rutaCarpeta = rutaBaseCarpeta + "/" + subcarpeta;
        LOGD("Procesando carpeta: %s", rutaCarpeta.c_str());
        if (subcarpeta == "circle") {
            procesarImagenesEnCarpeta(mgr, rutaCarpeta, momentosHuZernikeCirculos);
        } else if (subcarpeta == "square") {
            procesarImagenesEnCarpeta(mgr, rutaCarpeta, momentosHuZernikeCuadrados);
        } else if (subcarpeta == "triangle") {
            procesarImagenesEnCarpeta(mgr, rutaCarpeta, momentosHuZernikeTriangulos);
        }
    }

    LOGD("Momentos de Hu y Zernike calculados para todas las carpetas");
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_proyecto_1vison_ImagePickerActivity_procesarFrame(
        JNIEnv* env, jobject obj, jlong direccionMatRgba, jboolean modoDibujar) {
    Mat& frame = *(Mat*)direccionMatRgba;
    cvtColor(frame, frame, COLOR_RGBA2BGR);

    if (modoDibujar) {
        detectar(frame);
    }
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_proyecto_1vison_ImagePickerActivity_extractHOG(JNIEnv *env, jobject instance, jlong matAddrInput, jlong matAddrOutput) {
    Mat &inputMat = *(Mat *) matAddrInput;
    Mat &outputMat = *(Mat *) matAddrOutput;

    // Extraer características HOG y obtener la imagen de visualización
    std::vector<float> descriptors = extraerCaracteristicasHOG(inputMat, outputMat);

    if (descriptors.empty()) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error: los descriptores HOG no fueron extraídos correctamente.");
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Descriptores HOG extraídos correctamente.");
    }

    jfloatArray hogFeatures = env->NewFloatArray(descriptors.size());
    env->SetFloatArrayRegion(hogFeatures, 0, descriptors.size(), &descriptors[0]);

    return hogFeatures;
}