// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include"CFloatNativeAPI.h"

#include<stdlib.h>
#include<math.h>
#include<locale.h>

#include<stdio.h>

#define min(x, y) (x < y ? x : y)
#define max(x, y) (x > y ? x : y)

#define chooseOf2(x, y) (x == 0 ? y.f_value : y.d_value)
#define chooseOf3(x, y) (x == 0 ? y.f_value : (x == 1 ? y.d_value : y.ld_value))

const char* WRAPPER = "org/sosy_lab/cpachecker/util/floatingpoint/CFloatWrapper";
const char* NUMBER = "Number";
const char* EXCEPTION = "org/sosy_lab/cpachecker/util/floatingpoint/NativeComputationException";

const char* EX_TEXT = "Type invalid or not supported.";

typedef struct {long long mantissa; long long exp_sig_pad; } t_ld_bits;
typedef union {t_ld_bits bitmask; long double ld_value; double d_value; float f_value; } t_ld;

/**
 * Utility function to throw Java exceptions
 * as error handling.
 */
void throwNativeException(JNIEnv* env, const char* message) {
	jthrowable ex = (*env)->FindClass(env, EXCEPTION);
	(*env)->ThrowNew(env, ex, message);
}

/**
 * Utility function to get the floating point
 * representation of the java wrapper object.
 */
t_ld transformWrapperFromJava(JNIEnv* env, jobject wrapper, jint type) {
	jobject cls = (*env)->FindClass(env, WRAPPER);
	
	jmethodID getE = (*env)->GetMethodID(env, cls, "getExponent", "()J");
	jmethodID getM = (*env)->GetMethodID(env, cls, "getMantissa", "()J");

	jlong exponent = (*env)->CallLongMethod(env, wrapper, getE);
	jlong mantissa = (*env)->CallLongMethod(env, wrapper, getM);

	t_ld fp_obj = { .ld_value = 0.0L };
	switch (type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			fp_obj.bitmask.mantissa ^= mantissa + (exponent << 23);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			fp_obj.bitmask.mantissa ^= mantissa + (exponent << 52);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			fp_obj.bitmask.exp_sig_pad ^= exponent;
			fp_obj.bitmask.mantissa ^= mantissa;
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return fp_obj;
}

/**
 * Utility function to encapsulate a
 * floating point number into a java wrapper.
 */
jobject transformWrapperToJava(JNIEnv* env, t_ld fp_obj, jint type) {
	jobject cls = (*env)->FindClass(env, WRAPPER);
	jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "()V");

	jobject wrapper = (*env)->NewObject(env, cls, constructor);

	jmethodID setE = (*env)->GetMethodID(env, cls, "setExponent", "(J)V");
	jmethodID setM = (*env)->GetMethodID(env, cls, "setMantissa", "(J)V");

	switch (type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			(*env)->CallVoidMethod(env, wrapper, setE, ((jlong)((fp_obj.bitmask.mantissa & (511L << 23)) >> 23)));
			(*env)->CallVoidMethod(env, wrapper, setM, ((jlong)fp_obj.bitmask.mantissa & 8388607L));
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			(*env)->CallVoidMethod(env, wrapper, setE, ((jlong)((fp_obj.bitmask.mantissa & (4095L << 52)) >> 52)));
			(*env)->CallVoidMethod(env, wrapper, setM, ((jlong)fp_obj.bitmask.mantissa & 4503599627370495L));
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			(*env)->CallVoidMethod(env, wrapper, setE, ((jlong)fp_obj.bitmask.exp_sig_pad));
			(*env)->CallVoidMethod(env, wrapper, setM, ((jlong)fp_obj.bitmask.mantissa));
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return wrapper;
}

/**
 * Function to create an instance of a wrapped 
 * floating point number with a value as given
 * by 'valString'.
 *
 * The caller has to make sure himself, that
 * he provides a string representation of
 * his floating point value, that fits
 * the requirements of the strto* implementation
 * corresponding to the type he declares via
 * the 'type' parameter.
 */
JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_createFp(JNIEnv* env, jclass cl, jstring valString, jint type) {
	setlocale(LC_ALL, "C");
	const char* string = (*env)->GetStringUTFChars(env, valString, NULL);
	
	t_ld fp_obj = { .ld_value = 0.0L };
	float valf = 0.0;
	double vald = 0.0;
	long double val = 0.0L;

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			valf = strtof(string, NULL);
			fp_obj.f_value = valf;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			vald = strtod(string, NULL);
			fp_obj.d_value = vald;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			val = strtold(string, NULL);
			fp_obj.ld_value = val;
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}
	jobject wrapper = transformWrapperToJava(env, fp_obj, type);
	(*env)->ReleaseStringUTFChars(env, valString, string);

	return wrapper;
}

/**
 * Function to create a string representation of a
 * floating point type, to be able to print it correctly
 * in java. 
 */
JNIEXPORT jstring JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_printFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp_obj = transformWrapperFromJava(env, wrapper, type);
	char s[35000];

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			snprintf(s, 35000, "%.2000f", fp_obj.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			snprintf(s, 35000, "%.10000f", fp_obj.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			snprintf(s, 35000, "%.16445Lf", fp_obj.ld_value);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return (*env)->NewStringUTF(env, s);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_addFp(JNIEnv* env, jclass cl, jobject wrapper1, jint type1, jobject wrapper2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, wrapper1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, wrapper2, type2);

	t_ld result = { .ld_value = 0.0L };

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = fp_1.f_value + fp_2.f_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = chooseOf2(type1, fp_1) + chooseOf3(type2, fp_2);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = chooseOf3(type1, fp_1) + chooseOf3(type2, fp_2);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_subtractFp(JNIEnv* env, jclass cl, jobject wrapper1, jint type1, jobject wrapper2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, wrapper1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, wrapper2, type2);

	t_ld result = { .ld_value = 0.0L };

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = fp_1.f_value - fp_2.f_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = chooseOf2(type1, fp_1) - chooseOf2(type2, fp_2);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = chooseOf3(type1, fp_1) - chooseOf3(type2, fp_2);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_multiplyFp(JNIEnv* env, jclass cl, jobject wrapper1, jint type1, jobject wrapper2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, wrapper1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, wrapper2, type2);

	t_ld result = { .ld_value = 0.0L };

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = fp_1.f_value * fp_2.f_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = chooseOf2(type1, fp_1) * chooseOf2(type2, fp_2);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = chooseOf3(type1, fp_1) * chooseOf3(type2, fp_2);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_divideFp(JNIEnv* env, jclass cl, jobject wrapper1, jint type1, jobject wrapper2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, wrapper1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, wrapper2, type2);

	t_ld result = { .ld_value = 0.0L };

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = fp_1.f_value / fp_2.f_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = chooseOf2(type1, fp_1) / chooseOf2(type2, fp_2);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = chooseOf3(type1, fp_1) / chooseOf3(type2, fp_2);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_addManyFp(JNIEnv* env, jclass cl, jobject wrapper, jintArray type, jobjectArray summands) {
	jint* types = (*env)->GetIntArrayElements(env, type, 0);
	t_ld fp = transformWrapperFromJava(env, wrapper, types[0]);
	jsize size = (*env)->GetArrayLength(env, summands);

	float tmpf;
	double tmp;

	jint maxType = types[0];

	for (long long i = 0; i < size; i++) {
		maxType = max(maxType, types[i+1]);
	}

	switch(maxType) {
		// lowest one does need no handling
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			if (types[0] == 0) {
				tmpf = fp.f_value;
				fp.ld_value = 0.0L;
				fp.d_value = tmpf;
			}
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			if (types[0] == 0) {
				tmpf = fp.f_value;
				fp.ld_value = 0.0L;
				fp.ld_value = tmpf;
			}
			if (types[0] == 1) {
				tmp = fp.d_value;
				fp.ld_value = 0.0L;
				fp.ld_value = tmp;
			}
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	for (long long i = 0; i < size; i++) {
		t_ld cur_fp = transformWrapperFromJava(env, (*env)->GetObjectArrayElement(env, summands, i), types[i + 1]);

		switch(maxType) {
			case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
				fp.f_value = fp.f_value + cur_fp.f_value;
				break;
			case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
				fp.d_value = fp.d_value + chooseOf2(types[i+1], cur_fp);
				break;
			case  org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
				fp.ld_value = fp.ld_value + chooseOf3(types[i+1], cur_fp);
				break;
			default:
				throwNativeException(env, EX_TEXT);
		}
	}

	return transformWrapperToJava(env, fp, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_multiplyManyFp(JNIEnv* env, jclass cl, jobject wrapper, jintArray type, jobjectArray factors) {
	jint* types = (*env)->GetIntArrayElements(env, type, 0);
	t_ld fp = transformWrapperFromJava(env, wrapper, types[0]);
	jsize size = (*env)->GetArrayLength(env, factors);

	float tmpf;
	double tmp;

	jint maxType = types[0];

	for (long long i = 0; i < size; i++) {
		maxType = max(maxType, types[i+1]);
	}

	switch(maxType) {
		// lowest one does need no handling
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			if (types[0] == 0) {
				tmpf = fp.f_value;
				fp.ld_value = 0.0L;
				fp.d_value = tmpf;
			}
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			if (types[0] == 0) {
				tmpf = fp.f_value;
				fp.ld_value = 0.0L;
				fp.ld_value = tmpf;
			}
			if (types[0] == 1) {
				tmp = fp.d_value;
				fp.ld_value = 0.0L;
				fp.ld_value = tmp;
			}
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	for (long long i = 0; i < size; i++) {
		t_ld cur_fp = transformWrapperFromJava(env, (*env)->GetObjectArrayElement(env, factors, 0), types[i + 1]);
		switch(maxType) {
			case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
				fp.f_value = fp.f_value * cur_fp.f_value;
				break;
			case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
				fp.d_value = fp.d_value * chooseOf2(types[i+1], cur_fp);
				break;
			case  org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
				fp.ld_value = fp.ld_value * chooseOf3(types[i+1], cur_fp);
				break;
			default:
				throwNativeException(env, EX_TEXT);
		}
	}

	return transformWrapperToJava(env, fp, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_powFp(JNIEnv* env, jclass cl, jobject fp1, jint type1, jobject fp2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type2);

	t_ld result = { .ld_value = 0.0L };

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = powf(fp_1.f_value, fp_2.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = pow(chooseOf2(type1, fp_1), chooseOf2(type2, fp_2));
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = powl(chooseOf3(type1, fp_1), chooseOf3(type2, fp_2));
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_powIntegralFp(JNIEnv* env, jclass cl, jobject fp, jint exp, jint type) {
	t_ld fp_base = transformWrapperFromJava(env, fp, type);

	t_ld result = { .ld_value = 0.0L };

	// TODO: negative exponent has to be implemented somehow, too
	if (exp > -1) {
		// initialization according to actual type
		switch(type) {
			case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
				result.f_value = 1.0;
				break;
			case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
				result.d_value = 1.0;
				break;
			case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
				result.ld_value = 1.0L;
				break;
			default:
				throwNativeException(env, EX_TEXT);
		}

		// actual computation
		while (exp) {
			switch(type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					if (exp & 1) {
						result.f_value *= fp_base.f_value;
					}
					fp_base.f_value *= fp_base.f_value;
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					if (exp & 1) {
						result.d_value *= fp_base.d_value;
					}
					fp_base.d_value *= fp_base.d_value;
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					if (exp & 1) {
						result.ld_value *= fp_base.ld_value;
					}
					fp_base.ld_value *= fp_base.ld_value;
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			exp >>= 1;
		}
	}

	return transformWrapperToJava(env, result, type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_sqrtFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = sqrtf(fp.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = sqrt(fp.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = sqrtl(fp.ld_value);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_roundFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = roundf(fp.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = round(fp.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = roundl(fp.ld_value);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_truncFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = truncf(fp.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = trunc(fp.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = truncl(fp.ld_value);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_ceilFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = ceilf(fp.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = ceil(fp.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = ceill(fp.ld_value);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_floorFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = floorf(fp.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = floor(fp.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = floorl(fp.ld_value);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_absFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = fabsf(fp.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = fabs(fp.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = fabsl(fp.ld_value);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isZeroFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return fp.f_value == 0.0 || fp.f_value == -0.0;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return fp.d_value == 0.0 || fp.d_value == -0.0;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return fp.ld_value == 0.0L || fp.ld_value == -0.0L;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isOneFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return fp.f_value == 1.0;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return fp.d_value == 1.0;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return fp.ld_value == 1.0L;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isNanFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return isnan(fp.f_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return isnan(fp.d_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return isnan(fp.ld_value);
		default:
			throwNativeException(env, EX_TEXT);
	}

	return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isInfinityFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return isinf(fp.f_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return isinf(fp.d_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return isinf(fp.ld_value);
		default:
			throwNativeException(env, EX_TEXT);
	}

	return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isNegativeFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return 0 != signbit(fp.f_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return 0 != signbit(fp.d_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return 0 != signbit(fp.ld_value);
		default:
			throwNativeException(env, EX_TEXT);
	}

	return 0;
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_copySignFp(JNIEnv* env, jclass cl, jobject fp1, jobject fp2, jint type) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type);

	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = copysignf(fp_1.f_value, fp_2.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = copysign(fp_1.d_value, fp_2.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = copysign(fp_1.ld_value, fp_2.ld_value);
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castFpFromTo(JNIEnv* env, jclass cl, jobject wrapper, jint from_type, jint to_type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, from_type);

	float castedf;
	double castedd;
	long double casted;

	switch(to_type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			castedf = ((float)(chooseOf3(from_type, fp)));
			fp.ld_value = 0.0L;
			fp.f_value = castedf;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			castedd = ((double)(chooseOf3(from_type, fp)));
			fp.ld_value = 0.0L;
			fp.d_value = castedd;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			casted = ((long double)(chooseOf3(from_type, fp)));
			fp.ld_value = 0.0L;
			fp.ld_value = casted;
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, fp, to_type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castOtherToFp(JNIEnv* env, jclass cl, jobject number, jint from_type, jint to_fp_type) {
	jclass number_clazz = (*env)->FindClass(env, NUMBER);

	jmethodID getValue;
	t_ld fp = { .ld_value = 0.0L };

	switch(from_type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_CHAR:
			getValue = (*env)->GetMethodID(env, number_clazz, "byteValue", "()B");
			switch(to_fp_type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					fp.f_value = (float)((*env)->CallByteMethod(env, number, getValue));
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					fp.d_value = (double)((*env)->CallByteMethod(env, number, getValue));
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					fp.ld_value = (long double)((*env)->CallByteMethod(env, number, getValue));
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_SHORT:
			getValue = (*env)->GetMethodID(env, number_clazz, "shortValue", "()S");
			switch(to_fp_type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					fp.f_value = (float)((*env)->CallShortMethod(env, number, getValue));
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					fp.d_value = (double)((*env)->CallShortMethod(env, number, getValue));
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					fp.ld_value = (long double)((*env)->CallShortMethod(env, number, getValue));
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_INT:
			getValue = (*env)->GetMethodID(env, number_clazz, "intValue", "()I");
			switch(to_fp_type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					fp.f_value = (float)((*env)->CallIntMethod(env, number, getValue));
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					fp.d_value = (double)((*env)->CallIntMethod(env, number, getValue));
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					fp.ld_value = (long double)((*env)->CallIntMethod(env, number, getValue));
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			break;
		// this has to be adjusted according to architecture
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_LONG:
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_LONG_LONG:
			getValue = (*env)->GetMethodID(env, number_clazz, "longValue", "()J");
			switch(to_fp_type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					fp.f_value = (float)((*env)->CallLongMethod(env, number, getValue));
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					fp.d_value = (double)((*env)->CallLongMethod(env, number, getValue));
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					fp.ld_value = (long double)((*env)->CallLongMethod(env, number, getValue));
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, fp, to_fp_type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castFpToOther(JNIEnv* env, jclass cl, jobject wrapper, jint fp_from_type, jint to_type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, fp_from_type);

	jobject number_obj;
	jclass cls;
	jmethodID constructor;

	switch(to_type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_CHAR:
			cls = (*env)->FindClass(env, "Byte");
			constructor = (*env)->GetMethodID(env, cls, "<init>", "(B)V");
			switch(fp_from_type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (char)(fp.f_value)); 
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (char)(fp.d_value)); 
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (char)(fp.ld_value)); 
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_SHORT:
			cls = (*env)->FindClass(env, "Short");
			constructor = (*env)->GetMethodID(env, cls, "<init>", "(S)V");
			switch(fp_from_type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (short)(fp.f_value)); 
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (short)(fp.d_value)); 
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (short)(fp.ld_value)); 
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_INT:
			cls = (*env)->FindClass(env, "Integer");
			constructor = (*env)->GetMethodID(env, cls, "<init>", "(I)V");
			switch(fp_from_type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (int)(fp.f_value)); 
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (int)(fp.d_value)); 
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (int)(fp.ld_value)); 
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			break;
		// adjust according to architecture
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_LONG:
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_TYPE_LONG_LONG:
			cls = (*env)->FindClass(env, "Long");
			constructor = (*env)->GetMethodID(env, cls, "<init>", "(J)V");
			switch(fp_from_type) {
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (long long)(fp.f_value)); 
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (long long)(fp.d_value)); 
					break;
				case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
					number_obj = (*env)->NewObject(env, cls, constructor, (long long)(fp.ld_value)); 
					break;
				default:
					throwNativeException(env, EX_TEXT);
			}
			break;
		default:
			throwNativeException(env, EX_TEXT);
	}

	return number_obj;
}
