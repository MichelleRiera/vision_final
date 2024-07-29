#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>
#include <cmath>

#define LOG_TAG "PROYECTO_VISION"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

void computeGradients(const Mat& img, Mat& gradX, Mat& gradY) {
    Sobel(img, gradX, CV_32F, 1, 0, 3); // Kernel size 3
    Sobel(img, gradY, CV_32F, 0, 1, 3); // Kernel size 3
}

void computeMagnitudeAndAngle(const Mat& gradX, const Mat& gradY, Mat& magnitude, Mat& angle) {
    cartToPolar(gradX, gradY, magnitude, angle, true);
}

vector<float> extractHOGFeaturesManual(const Mat& img, int orientations, Size cellSize, Size blockSize, Size blockStride, Mat& visualImage) {
    Mat gradX, gradY;
    computeGradients(img, gradX, gradY);

    Mat magnitude, angle;
    computeMagnitudeAndAngle(gradX, gradY, magnitude, angle);

    int cellsX = img.cols / cellSize.width;
    int cellsY = img.rows / cellSize.height;

    vector<vector<vector<float>>> histograms(cellsY, vector<vector<float>>(cellsX, vector<float>(orientations, 0.0f)));

    for (int y = 0; y < img.rows; ++y) {
        for (int x = 0; x < img.cols; ++x) {
            int cellX = x / cellSize.width;
            int cellY = y / cellSize.height;

            float mag = magnitude.at<float>(y, x);
            float ang = angle.at<float>(y, x);

            int bin = static_cast<int>(ang / (180.0 / orientations)) % orientations;
            histograms[cellY][cellX][bin] += mag;
        }
    }

    vector<float> features;
    for (int y = 0; y < cellsY - blockSize.height / cellSize.height + 1; y += blockStride.height / cellSize.height) {
        for (int x = 0; x < cellsX - blockSize.width / cellSize.width + 1; x += blockStride.width / cellSize.width) {
            vector<float> blockHist;
            for (int dy = 0; dy < blockSize.height / cellSize.height; ++dy) {
                for (int dx = 0; dx < blockSize.width / cellSize.width; ++dx) {
                    int cellX = x + dx;
                    int cellY = y + dy;
                    blockHist.insert(blockHist.end(), histograms[cellY][cellX].begin(), histograms[cellY][cellX].end());
                }
            }

            float normFactor = 0.0f;
            for (const auto& val : blockHist) {
                normFactor += val * val;
            }
            normFactor = sqrt(normFactor + 1e-6);

            for (auto& val : blockHist) {
                val /= normFactor;
            }

            features.insert(features.end(), blockHist.begin(), blockHist.end());
        }
    }

    // Visualización del gradiente
    visualImage = Mat::zeros(img.size(), CV_8UC3);
    for (int y = 0; y < cellsY; ++y) {
        for (int x = 0; x < cellsX; ++x) {
            Point cellCenter(x * cellSize.width + cellSize.width / 2, y * cellSize.height + cellSize.height / 2);
            for (int o = 0; o < orientations; ++o) {
                float magnitude = histograms[y][x][o];
                float angle = o * (180.0 / orientations);
                Point start(cellCenter.x + magnitude * cos(angle), cellCenter.y + magnitude * sin(angle));
                Point end(cellCenter.x - magnitude * cos(angle), cellCenter.y - magnitude * sin(angle));
                line(visualImage, start, end, Scalar(0, 255, 0), 1);
            }
        }
    }

    return features;
}

vector<float> extraerCaracteristicasHOG(const Mat& imagen, Mat& visualImage) {
    Mat imagenGris, imagenResized;
    if (imagen.channels() > 1) {
        cvtColor(imagen, imagenGris, COLOR_BGR2GRAY);
    } else {
        imagenGris = imagen;
    }
    resize(imagenGris, imagenResized, Size(28, 28));

    int orientations = 9;
    Size cellSize(4, 4);
    Size blockSize(12, 12); // 3x3 cells
    Size blockStride(4, 4);

    vector<float> descriptores = extractHOGFeaturesManual(imagenResized, orientations, cellSize, blockSize, blockStride, visualImage);

    if (descriptores.empty()) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error: los descriptores HOG no fueron extraídos correctamente.");
    }

    return descriptores;
}

