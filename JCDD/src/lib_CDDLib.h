/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class lib_CDDLib */

#ifndef _Included_lib_CDDLib
#define _Included_lib_CDDLib
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     lib_CDDLib
 * Method:    cddInit
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_lib_CDDLib_cddInit
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     lib_CDDLib
 * Method:    cddDone
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lib_CDDLib_cddDone
  (JNIEnv *, jclass);

/*
 * Class:     lib_CDDLib
 * Method:    allocateCdd
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_allocateCdd
  (JNIEnv *, jclass);

/*
 * Class:     lib_CDDLib
 * Method:    freeCdd
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_lib_CDDLib_freeCdd
  (JNIEnv *, jclass, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    conjunction
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_conjunction
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    disjunction
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_disjunction
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    negation
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_negation
  (JNIEnv *, jclass, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    reduce
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_reduce
  (JNIEnv *, jclass, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    interval
 * Signature: (IIII)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_interval
  (JNIEnv *, jclass, jint, jint, jint, jint);

/*
 * Class:     lib_CDDLib
 * Method:    lower
 * Signature: (III)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_lower
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     lib_CDDLib
 * Method:    upper
 * Signature: (III)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_upper
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     lib_CDDLib
 * Method:    cddNodeCount
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_lib_CDDLib_cddNodeCount
  (JNIEnv *, jclass, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    cddAddClocks
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lib_CDDLib_cddAddClocks
  (JNIEnv *, jclass, jint);

/*
 * Class:     lib_CDDLib
 * Method:    getRootNode
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_getRootNode
  (JNIEnv *, jclass, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    getNodeLevel
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_lib_CDDLib_getNodeLevel
  (JNIEnv *, jclass, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    isElemArrayNullTerminator
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_lib_CDDLib_isElemArrayNullTerminator
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     lib_CDDLib
 * Method:    getChildFromElemArray
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_getChildFromElemArray
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     lib_CDDLib
 * Method:    getBoundFromElemArray
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_lib_CDDLib_getBoundFromElemArray
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     lib_CDDLib
 * Method:    cddFromDbm
 * Signature: ([II)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_cddFromDbm
  (JNIEnv *, jclass, jintArray, jint);

/*
 * Class:     lib_CDDLib
 * Method:    cddPrintDot
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_lib_CDDLib_cddPrintDot__J
  (JNIEnv *, jclass, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    cddPrintDot
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lib_CDDLib_cddPrintDot__JLjava_lang_String_2
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     lib_CDDLib
 * Method:    addBddvar
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_lib_CDDLib_addBddvar
  (JNIEnv *, jclass, jint);

/*
 * Class:     lib_CDDLib
 * Method:    cddBddvar
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_cddBddvar
  (JNIEnv *, jclass, jint);

/*
 * Class:     lib_CDDLib
 * Method:    cddNBddvar
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_lib_CDDLib_cddNBddvar
  (JNIEnv *, jclass, jint);

/*
 * Class:     lib_CDDLib
 * Method:    isTrue
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_lib_CDDLib_isTrue
  (JNIEnv *, jclass, jlong);

/*
 * Class:     lib_CDDLib
 * Method:    isFalse
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_lib_CDDLib_isFalse
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
