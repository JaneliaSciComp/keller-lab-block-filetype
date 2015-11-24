#include "org_janelia_simview_klb_KLBJNI.h"
#include <string>
#include "common.h"
#include "klb_imageIO.h"
#include "klb_Cwrapper.h"

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadHeader
(JNIEnv* env, jobject obj, jstring filePath, jlongArray imageSize, jlongArray blockSize, jfloatArray pixelSpacing, jintArray dataAndCompressionType, jbyteArray metadata)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cImageSize = env->GetLongArrayElements(imageSize, 0);
	jlong* cBlockSize = env->GetLongArrayElements(blockSize, 0);
	jfloat* cPixelSpacing = env->GetFloatArrayElements(pixelSpacing, 0);
	jint* cDataAndCompressionType = env->GetIntArrayElements(dataAndCompressionType, 0);
	jbyte* cMetadata = env->GetByteArrayElements(metadata, 0);

	int datatype, compressiontype;
	uint32_t imgsize[5], blksize[5];
	const int errid = readKLBheader(cFilePath, imgsize, (KLB_DATA_TYPE*) &datatype, cPixelSpacing, blksize, (KLB_COMPRESSION_TYPE*) &compressiontype, (char*) cMetadata);
	cDataAndCompressionType[0] = datatype;
	cDataAndCompressionType[1] = compressiontype;
	for (int i = 0; i < 5; ++i) {
		cImageSize[i] = imgsize[i];
		cBlockSize[i] = blksize[i];
	}

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(imageSize, cImageSize, 0);
	env->ReleaseLongArrayElements(blockSize, cBlockSize, 0);
	env->ReleaseFloatArrayElements(pixelSpacing, cPixelSpacing, 0);
	env->ReleaseIntArrayElements(dataAndCompressionType, cDataAndCompressionType, 0);
	env->ReleaseByteArrayElements(metadata, cMetadata, 0);

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadFull__Ljava_lang_String_2I_3B
(JNIEnv* env, jobject obj, jstring filePath, jint numThreads, jbyteArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jboolean isCopy;
	jbyte* cBuffer = env->GetByteArrayElements(buffer, &isCopy);

	int datatype; // placeholder, overwritten by function call below
	const int errid = readKLBstackInPlace(cFilePath, cBuffer, (KLB_DATA_TYPE*)&datatype, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	if (isCopy == JNI_TRUE) {
		env->ReleaseByteArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseByteArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadFull__Ljava_lang_String_2I_3S
(JNIEnv* env, jobject obj, jstring filePath, jint numThreads, jshortArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jboolean isCopy;
	jshort* cBuffer = env->GetShortArrayElements(buffer, &isCopy);

	int datatype; // placeholder, overwritten by function call below
	const int errid = readKLBstackInPlace(cFilePath, cBuffer, (KLB_DATA_TYPE*)&datatype, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	if (isCopy == JNI_TRUE) {
		env->ReleaseShortArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseShortArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadFull__Ljava_lang_String_2I_3I
(JNIEnv* env, jobject obj, jstring filePath, jint numThreads, jintArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jboolean isCopy;
	jint* cBuffer = env->GetIntArrayElements(buffer, &isCopy);

	int datatype; // placeholder, overwritten by function call below
	const int errid = readKLBstackInPlace(cFilePath, cBuffer, (KLB_DATA_TYPE*)&datatype, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	if (isCopy == JNI_TRUE) {
		env->ReleaseIntArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseIntArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadFull__Ljava_lang_String_2I_3J
(JNIEnv* env, jobject obj, jstring filePath, jint numThreads, jlongArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jboolean isCopy;
	jlong* cBuffer = env->GetLongArrayElements(buffer, &isCopy);

	int datatype; // placeholder, overwritten by function call below
	const int errid = readKLBstackInPlace(cFilePath, cBuffer, (KLB_DATA_TYPE*)&datatype, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	if (isCopy == JNI_TRUE) {
		env->ReleaseLongArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseLongArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadFull__Ljava_lang_String_2I_3F
(JNIEnv* env, jobject obj, jstring filePath, jint numThreads, jfloatArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jboolean isCopy;
	jfloat* cBuffer = env->GetFloatArrayElements(buffer, &isCopy);

	int datatype; // placeholder, overwritten by function call below
	const int errid = readKLBstackInPlace(cFilePath, cBuffer, (KLB_DATA_TYPE*)&datatype, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	if (isCopy == JNI_TRUE) {
		env->ReleaseFloatArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseFloatArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadFull__Ljava_lang_String_2I_3D
(JNIEnv* env, jobject obj, jstring filePath, jint numThreads, jdoubleArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jboolean isCopy;
	jdouble* cBuffer = env->GetDoubleArrayElements(buffer, &isCopy);

	int datatype; // placeholder, overwritten by function call below
	const int errid = readKLBstackInPlace(cFilePath, cBuffer, (KLB_DATA_TYPE*)&datatype, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	if (isCopy == JNI_TRUE) {
		env->ReleaseDoubleArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseDoubleArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadFull__Ljava_lang_String_2ILjava_nio_Buffer_2
(JNIEnv* env, jobject obj, jstring filePath, jint numThreads, jobject buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	void* cBuffer = env->GetDirectBufferAddress(buffer);

	int datatype; // placeholder, overwritten by function call below
	const int errid = readKLBstackInPlace(cFilePath, cBuffer, (KLB_DATA_TYPE*)&datatype, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);

	return (jint)errid;
}

// variant of readKLBroiInPlace that converts the LB and UB arguments from jlong to uint32_t
inline int readKLBroiInPlaceConvertJlong(const char* filename, void* im, jlong xyzctLB[KLB_DATA_DIMS], jlong xyzctUB[KLB_DATA_DIMS], int numThreads)
{
	std::string filenameOut(filename);

	klb_imageIO img(filenameOut);

	klb_ROI roi;
	for (int d = 0; d < KLB_DATA_DIMS; d++)
	{
		roi.xyzctLB[d] = (uint32_t)xyzctLB[d];
		roi.xyzctUB[d] = (uint32_t)xyzctUB[d];
	}

	return img.readImage((char*)im, &roi, numThreads);
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadROI__Ljava_lang_String_2_3J_3JI_3B
(JNIEnv* env, jobject obj, jstring filePath, jlongArray xyzctMin, jlongArray xyzctMax, jint numThreads, jbyteArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cXyzctMin = env->GetLongArrayElements(xyzctMin, 0);
	jlong* cXyzctMax = env->GetLongArrayElements(xyzctMax, 0);
	jboolean isCopy;
	jbyte* cBuffer = env->GetByteArrayElements(buffer, &isCopy);

	const int errid = readKLBroiInPlaceConvertJlong(cFilePath, cBuffer, cXyzctMin, cXyzctMax, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(xyzctMin, cXyzctMin, JNI_ABORT);
	env->ReleaseLongArrayElements(xyzctMax, cXyzctMax, JNI_ABORT);
	if (isCopy == JNI_TRUE) {
		env->ReleaseByteArrayElements(buffer, cBuffer, 0);
	} else {
		env->ReleaseByteArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadROI__Ljava_lang_String_2_3J_3JI_3S
(JNIEnv* env, jobject obj, jstring filePath, jlongArray xyzctMin, jlongArray xyzctMax, jint numThreads, jshortArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cXyzctMin = env->GetLongArrayElements(xyzctMin, 0);
	jlong* cXyzctMax = env->GetLongArrayElements(xyzctMax, 0);
	jboolean isCopy;
	jshort* cBuffer = env->GetShortArrayElements(buffer, &isCopy);

	const int errid = readKLBroiInPlaceConvertJlong(cFilePath, cBuffer, cXyzctMin, cXyzctMax, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(xyzctMin, cXyzctMin, JNI_ABORT);
	env->ReleaseLongArrayElements(xyzctMax, cXyzctMax, JNI_ABORT);
	if (isCopy == JNI_TRUE) {
		env->ReleaseShortArrayElements(buffer, cBuffer, 0);
	} else {
		env->ReleaseShortArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadROI__Ljava_lang_String_2_3J_3JI_3I
(JNIEnv* env, jobject obj, jstring filePath, jlongArray xyzctMin, jlongArray xyzctMax, jint numThreads, jintArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cXyzctMin = env->GetLongArrayElements(xyzctMin, 0);
	jlong* cXyzctMax = env->GetLongArrayElements(xyzctMax, 0);
	jboolean isCopy;
	jint* cBuffer = env->GetIntArrayElements(buffer, &isCopy);

	const int errid = readKLBroiInPlaceConvertJlong(cFilePath, cBuffer, cXyzctMin, cXyzctMax, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(xyzctMin, cXyzctMin, JNI_ABORT);
	env->ReleaseLongArrayElements(xyzctMax, cXyzctMax, JNI_ABORT);
	if (isCopy == JNI_TRUE) {
		env->ReleaseIntArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseIntArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadROI__Ljava_lang_String_2_3J_3JI_3J
(JNIEnv* env, jobject obj, jstring filePath, jlongArray xyzctMin, jlongArray xyzctMax, jint numThreads, jlongArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cXyzctMin = env->GetLongArrayElements(xyzctMin, 0);
	jlong* cXyzctMax = env->GetLongArrayElements(xyzctMax, 0);
	jboolean isCopy;
	jlong* cBuffer = env->GetLongArrayElements(buffer, &isCopy);

	const int errid = readKLBroiInPlaceConvertJlong(cFilePath, cBuffer, cXyzctMin, cXyzctMax, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(xyzctMin, cXyzctMin, JNI_ABORT);
	env->ReleaseLongArrayElements(xyzctMax, cXyzctMax, JNI_ABORT);
	if (isCopy == JNI_TRUE) {
		env->ReleaseLongArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseLongArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadROI__Ljava_lang_String_2_3J_3JI_3F
(JNIEnv* env, jobject obj, jstring filePath, jlongArray xyzctMin, jlongArray xyzctMax, jint numThreads, jfloatArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cXyzctMin = env->GetLongArrayElements(xyzctMin, 0);
	jlong* cXyzctMax = env->GetLongArrayElements(xyzctMax, 0);
	jboolean isCopy;
	jfloat* cBuffer = env->GetFloatArrayElements(buffer, &isCopy);

	const int errid = readKLBroiInPlaceConvertJlong(cFilePath, cBuffer, cXyzctMin, cXyzctMax, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(xyzctMin, cXyzctMin, JNI_ABORT);
	env->ReleaseLongArrayElements(xyzctMax, cXyzctMax, JNI_ABORT);
	if (isCopy == JNI_TRUE) {
		env->ReleaseFloatArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseFloatArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadROI__Ljava_lang_String_2_3J_3JI_3D
(JNIEnv* env, jobject obj, jstring filePath, jlongArray xyzctMin, jlongArray xyzctMax, jint numThreads, jdoubleArray buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cXyzctMin = env->GetLongArrayElements(xyzctMin, 0);
	jlong* cXyzctMax = env->GetLongArrayElements(xyzctMax, 0);
	jboolean isCopy;
	jdouble* cBuffer = env->GetDoubleArrayElements(buffer, &isCopy);

	const int errid = readKLBroiInPlaceConvertJlong(cFilePath, cBuffer, cXyzctMin, cXyzctMax, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(xyzctMin, cXyzctMin, JNI_ABORT);
	env->ReleaseLongArrayElements(xyzctMax, cXyzctMax, JNI_ABORT);
	if (isCopy == JNI_TRUE) {
		env->ReleaseDoubleArrayElements(buffer, cBuffer, 0);
	}
	else {
		env->ReleaseDoubleArrayElements(buffer, cBuffer, JNI_ABORT);
	}

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniReadROI__Ljava_lang_String_2_3J_3JILjava_nio_Buffer_2
(JNIEnv* env, jobject obj, jstring filePath, jlongArray xyzctMin, jlongArray xyzctMax, jint numThreads, jobject buffer)
{
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cXyzctMin = env->GetLongArrayElements(xyzctMin, 0);
	jlong* cXyzctMax = env->GetLongArrayElements(xyzctMax, 0);
	void* cBuffer = env->GetDirectBufferAddress(buffer);

	const int errid = readKLBroiInPlaceConvertJlong(cFilePath, cBuffer, cXyzctMin, cXyzctMax, numThreads);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(xyzctMin, cXyzctMin, JNI_ABORT);
	env->ReleaseLongArrayElements(xyzctMax, cXyzctMax, JNI_ABORT);

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniWriteFull___3BLjava_lang_String_2_3JII_3F_3JI_3B
(JNIEnv* env, jobject obj, jbyteArray buffer, jstring filePath, jlongArray imageSize, jint dataType, jint numThreads, jfloatArray pixelSpacing, jlongArray blockSize, jint compressionType, jbyteArray metadata)
{
	jbyte* cBuffer = env->GetByteArrayElements(buffer, 0);
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cImageSize = env->GetLongArrayElements(imageSize, 0);
	jfloat* cPixelSpacing = pixelSpacing == NULL ? NULL : env->GetFloatArrayElements(pixelSpacing, 0);

	uint32_t* blksize = NULL;
	if (blockSize != NULL) {
		jlong* cBlockSize = env->GetLongArrayElements(blockSize, 0);
		uint32_t tmp[5];
		for (int d = 0; d < 5; ++d)
			tmp[d] = (uint32_t)cImageSize[d];
		blksize = tmp;
		env->ReleaseLongArrayElements(blockSize, cBlockSize, JNI_ABORT);
	}

	uint32_t imgsize[5];
	for (int d = 0; d < 5; ++d)
		imgsize[d] = (uint32_t)cImageSize[d];

    const int errid = writeKLBstack(cBuffer, cFilePath, imgsize, (KLB_DATA_TYPE)dataType, numThreads, cPixelSpacing, blksize, (KLB_COMPRESSION_TYPE)compressionType, NULL);

	env->ReleaseByteArrayElements(buffer, cBuffer, JNI_ABORT);
	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(imageSize, cImageSize, JNI_ABORT);
	if (cPixelSpacing != NULL)
		env->ReleaseFloatArrayElements(pixelSpacing, cPixelSpacing, JNI_ABORT);

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniWriteFull___3SLjava_lang_String_2_3JII_3F_3JI_3B
(JNIEnv* env, jobject obj, jshortArray buffer, jstring filePath, jlongArray imageSize, jint dataType, jint numThreads, jfloatArray pixelSpacing, jlongArray blockSize, jint compressionType, jbyteArray metadata)
{
	jshort* cBuffer = env->GetShortArrayElements(buffer, 0);
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cImageSize = env->GetLongArrayElements(imageSize, 0);
	jfloat* cPixelSpacing = pixelSpacing == NULL ? NULL : env->GetFloatArrayElements(pixelSpacing, 0);

	uint32_t* blksize = NULL;
	if (blockSize != NULL) {
		jlong* cBlockSize = env->GetLongArrayElements(blockSize, 0);
		uint32_t tmp[5];
		for (int d = 0; d < 5; ++d)
			tmp[d] = (uint32_t)cImageSize[d];
		blksize = tmp;
		env->ReleaseLongArrayElements(blockSize, cBlockSize, JNI_ABORT);
	}

	uint32_t imgsize[5];
	for (int d = 0; d < 5; ++d)
		imgsize[d] = (uint32_t)cImageSize[d];

	const int errid = writeKLBstack(cBuffer, cFilePath, imgsize, (KLB_DATA_TYPE)dataType, numThreads, cPixelSpacing, blksize, (KLB_COMPRESSION_TYPE)compressionType, NULL);

	env->ReleaseShortArrayElements(buffer, cBuffer, JNI_ABORT);
	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(imageSize, cImageSize, JNI_ABORT);
	if (cPixelSpacing != NULL)
		env->ReleaseFloatArrayElements(pixelSpacing, cPixelSpacing, JNI_ABORT);

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniWriteFull___3ILjava_lang_String_2_3JII_3F_3JI_3B
(JNIEnv* env, jobject obj, jintArray buffer, jstring filePath, jlongArray imageSize, jint dataType, jint numThreads, jfloatArray pixelSpacing, jlongArray blockSize, jint compressionType, jbyteArray metadata)
{
	jint* cBuffer = env->GetIntArrayElements(buffer, 0);
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cImageSize = env->GetLongArrayElements(imageSize, 0);
	jfloat* cPixelSpacing = pixelSpacing == NULL ? NULL : env->GetFloatArrayElements(pixelSpacing, 0);

	uint32_t* blksize = NULL;
	if (blockSize != NULL) {
		jlong* cBlockSize = env->GetLongArrayElements(blockSize, 0);
		uint32_t tmp[5];
		for (int d = 0; d < 5; ++d)
			tmp[d] = (uint32_t)cImageSize[d];
		blksize = tmp;
		env->ReleaseLongArrayElements(blockSize, cBlockSize, JNI_ABORT);
	}

	uint32_t imgsize[5];
	for (int d = 0; d < 5; ++d)
		imgsize[d] = (uint32_t)cImageSize[d];

	const int errid = writeKLBstack(cBuffer, cFilePath, imgsize, (KLB_DATA_TYPE)dataType, numThreads, cPixelSpacing, blksize, (KLB_COMPRESSION_TYPE)compressionType, NULL);

	env->ReleaseIntArrayElements(buffer, cBuffer, JNI_ABORT);
	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(imageSize, cImageSize, JNI_ABORT);
	if (cPixelSpacing != NULL)
		env->ReleaseFloatArrayElements(pixelSpacing, cPixelSpacing, JNI_ABORT);

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniWriteFull___3JLjava_lang_String_2_3JII_3F_3JI_3B
(JNIEnv* env, jobject obj, jlongArray buffer, jstring filePath, jlongArray imageSize, jint dataType, jint numThreads, jfloatArray pixelSpacing, jlongArray blockSize, jint compressionType, jbyteArray metadata)
{
	jlong* cBuffer = env->GetLongArrayElements(buffer, 0);
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cImageSize = env->GetLongArrayElements(imageSize, 0);
	jfloat* cPixelSpacing = pixelSpacing == NULL ? NULL : env->GetFloatArrayElements(pixelSpacing, 0);

	uint32_t* blksize = NULL;
	if (blockSize != NULL) {
		jlong* cBlockSize = env->GetLongArrayElements(blockSize, 0);
		uint32_t tmp[5];
		for (int d = 0; d < 5; ++d)
			tmp[d] = (uint32_t)cImageSize[d];
		blksize = tmp;
		env->ReleaseLongArrayElements(blockSize, cBlockSize, JNI_ABORT);
	}

	uint32_t imgsize[5];
	for (int d = 0; d < 5; ++d)
		imgsize[d] = (uint32_t)cImageSize[d];

	const int errid = writeKLBstack(cBuffer, cFilePath, imgsize, (KLB_DATA_TYPE)dataType, numThreads, cPixelSpacing, blksize, (KLB_COMPRESSION_TYPE)compressionType, NULL);

	env->ReleaseLongArrayElements(buffer, cBuffer, JNI_ABORT);
	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(imageSize, cImageSize, JNI_ABORT);
	if (cPixelSpacing != NULL)
		env->ReleaseFloatArrayElements(pixelSpacing, cPixelSpacing, JNI_ABORT);

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniWriteFull___3FLjava_lang_String_2_3JII_3F_3JI_3B
(JNIEnv* env, jobject obj, jfloatArray buffer, jstring filePath, jlongArray imageSize, jint dataType, jint numThreads, jfloatArray pixelSpacing, jlongArray blockSize, jint compressionType, jbyteArray metadata)
{
	jfloat* cBuffer = env->GetFloatArrayElements(buffer, 0);
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cImageSize = env->GetLongArrayElements(imageSize, 0);
	jfloat* cPixelSpacing = pixelSpacing == NULL ? NULL : env->GetFloatArrayElements(pixelSpacing, 0);

	uint32_t* blksize = NULL;
	if (blockSize != NULL) {
		jlong* cBlockSize = env->GetLongArrayElements(blockSize, 0);
		uint32_t tmp[5];
		for (int d = 0; d < 5; ++d)
			tmp[d] = (uint32_t)cImageSize[d];
		blksize = tmp;
		env->ReleaseLongArrayElements(blockSize, cBlockSize, JNI_ABORT);
	}

	uint32_t imgsize[5];
	for (int d = 0; d < 5; ++d)
		imgsize[d] = (uint32_t)cImageSize[d];

	const int errid = writeKLBstack(cBuffer, cFilePath, imgsize, (KLB_DATA_TYPE)dataType, numThreads, cPixelSpacing, blksize, (KLB_COMPRESSION_TYPE)compressionType, NULL);

	env->ReleaseFloatArrayElements(buffer, cBuffer, JNI_ABORT);
	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(imageSize, cImageSize, JNI_ABORT);
	if (cPixelSpacing != NULL)
		env->ReleaseFloatArrayElements(pixelSpacing, cPixelSpacing, JNI_ABORT);

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniWriteFull___3DLjava_lang_String_2_3JII_3F_3JI_3B
(JNIEnv* env, jobject obj, jdoubleArray buffer, jstring filePath, jlongArray imageSize, jint dataType, jint numThreads, jfloatArray pixelSpacing, jlongArray blockSize, jint compressionType, jbyteArray metadata)
{
	jdouble* cBuffer = env->GetDoubleArrayElements(buffer, 0);
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cImageSize = env->GetLongArrayElements(imageSize, 0);
	jfloat* cPixelSpacing = pixelSpacing == NULL ? NULL : env->GetFloatArrayElements(pixelSpacing, 0);

	uint32_t* blksize = NULL;
	if (blockSize != NULL) {
		jlong* cBlockSize = env->GetLongArrayElements(blockSize, 0);
		uint32_t tmp[5];
		for (int d = 0; d < 5; ++d)
			tmp[d] = (uint32_t)cImageSize[d];
		blksize = tmp;
		env->ReleaseLongArrayElements(blockSize, cBlockSize, JNI_ABORT);
	}

	uint32_t imgsize[5];
	for (int d = 0; d < 5; ++d)
		imgsize[d] = (uint32_t)cImageSize[d];

	const int errid = writeKLBstack(cBuffer, cFilePath, imgsize, (KLB_DATA_TYPE)dataType, numThreads, cPixelSpacing, blksize, (KLB_COMPRESSION_TYPE)compressionType, NULL);

	env->ReleaseDoubleArrayElements(buffer, cBuffer, JNI_ABORT);
	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(imageSize, cImageSize, JNI_ABORT);
	if (cPixelSpacing != NULL)
		env->ReleaseFloatArrayElements(pixelSpacing, cPixelSpacing, JNI_ABORT);

	return (jint)errid;
}

JNIEXPORT jint JNICALL Java_org_janelia_simview_klb_KLBJNI_jniWriteFull__Ljava_nio_Buffer_2Ljava_lang_String_2_3JII_3F_3JI_3B
(JNIEnv* env, jobject obj, jobject buffer, jstring filePath, jlongArray imageSize, jint dataType, jint numThreads, jfloatArray pixelSpacing, jlongArray blockSize, jint compressionType, jbyteArray metadata)
{
	void* cBuffer = env->GetDirectBufferAddress(buffer);
	const char* cFilePath = env->GetStringUTFChars(filePath, 0);
	jlong* cImageSize = env->GetLongArrayElements(imageSize, 0);
	jfloat* cPixelSpacing = pixelSpacing == NULL ? NULL : env->GetFloatArrayElements(pixelSpacing, 0);

	uint32_t* blksize = NULL;
	if (blockSize != NULL) {
		jlong* cBlockSize = env->GetLongArrayElements(blockSize, 0);
		uint32_t tmp[5];
		for (int d = 0; d < 5; ++d)
			tmp[d] = (uint32_t)cImageSize[d];
		blksize = tmp;
		env->ReleaseLongArrayElements(blockSize, cBlockSize, JNI_ABORT);
	}

	uint32_t imgsize[5];
	for (int d = 0; d < 5; ++d)
		imgsize[d] = (uint32_t)cImageSize[d];

	const int errid = writeKLBstack(cBuffer, cFilePath, imgsize, (KLB_DATA_TYPE)dataType, numThreads, cPixelSpacing, blksize, (KLB_COMPRESSION_TYPE)compressionType, NULL);

	env->ReleaseStringUTFChars(filePath, cFilePath);
	env->ReleaseLongArrayElements(imageSize, cImageSize, JNI_ABORT);
	if (cPixelSpacing != NULL)
		env->ReleaseFloatArrayElements(pixelSpacing, cPixelSpacing, JNI_ABORT);

	return (jint)errid;
}
