#ifndef PROYECTO_VISION_PARTE2_H
#define PROYECTO_VISION_PARTE2_H

#include <opencv2/opencv.hpp>
#include <vector>
#include <jni.h>

using namespace cv;
using namespace std;

extern "C" {
JNIEXPORT jfloatArray JNICALL Java_com_example_proyecto_1vison_ImagePickerActivity_extractHOG(JNIEnv* env, jobject obj, jlong addrInput, jlong addrOutput);
JNIEXPORT void JNICALL Java_com_example_proyecto_1vison_ImagePickerActivity_procesarFrame(JNIEnv* env, jobject obj, jlong direccionMatRgba, jboolean modoDibujar);
}

vector<float> extraerCaracteristicasHOG(const Mat& imagen, Mat& visualImage);

#endif //PROYECTO_VISION_PARTE2_H
