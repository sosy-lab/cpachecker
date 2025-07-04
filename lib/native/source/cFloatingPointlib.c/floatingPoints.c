// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#define __STDC_WANT_IEC_60559_EXT__

#include"CFloatNativeAPI.h"

#include<stdlib.h>
#include<math.h>
#include<locale.h>

#include<stdio.h>
#include<stdint.h>

#define min(x, y) (x < y ? x : y)
#define max(x, y) (x > y ? x : y)

#define chooseOf2(x, y) (x == 0 ? y.f_value : y.d_value)
#define chooseOf3(x, y) (x == 0 ? y.f_value : (x == 1 ? y.d_value : y.ld_value))

const char* WRAPPER = "org/sosy_lab/cpachecker/util/floatingpoint/CFloatWrapper";
const char* NUMBER = "Number";
const char* EXCEPTION = "java/lang/IllegalArgumentException";

const char* EX_TEXT = "Type invalid or not supported.";

typedef struct {unsigned long long lower; unsigned long long upper; } t_ld_bits;
typedef union {t_ld_bits bitmask; long double ld_value; double d_value; float f_value; } t_ld;

/**
 * Utility function to throw Java exceptions
 * as error handling.
 */
void throwException(JNIEnv* env, const char* message) {
	jthrowable ex = (*env)->FindClass(env, EXCEPTION);
	(*env)->ThrowNew(env, ex, message);
}

#define FLOAT_SIGNIFICAND_WIDTH 23
#define FLOAT_SIGN_AND_EXPONENT_BITMASK 0xFF800000L
#define FLOAT_SIGNIFICAND_BITMASK 0x007FFFFFL

#define DOUBLE_SIGNIFICAND_WIDTH 52
#define DOUBLE_SIGN_AND_EXPONENT_BITMASK 0xFFF0000000000000L
#define DOUBLE_SIGNIFICAND_BITMASK 0xFFFFFFFFFFFFFL

#define LONGDOUBLE_SIGN_AND_EXPONENT_BITMASK 0xFFFFL

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
			fp_obj.bitmask.lower = mantissa + (exponent << FLOAT_SIGNIFICAND_WIDTH);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			fp_obj.bitmask.lower = mantissa + (exponent << DOUBLE_SIGNIFICAND_WIDTH);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			fp_obj.bitmask.upper = exponent;
			fp_obj.bitmask.lower = mantissa;
			break;
		default:
			throwException(env, EX_TEXT);
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
			(*env)->CallVoidMethod(env, wrapper, setE, (jlong) ((fp_obj.bitmask.lower & FLOAT_SIGN_AND_EXPONENT_BITMASK) >> FLOAT_SIGNIFICAND_WIDTH));
			(*env)->CallVoidMethod(env, wrapper, setM, (jlong) (fp_obj.bitmask.lower & FLOAT_SIGNIFICAND_BITMASK));
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			(*env)->CallVoidMethod(env, wrapper, setE, (jlong) ((fp_obj.bitmask.lower & DOUBLE_SIGN_AND_EXPONENT_BITMASK) >> DOUBLE_SIGNIFICAND_WIDTH));
			(*env)->CallVoidMethod(env, wrapper, setM, (jlong) (fp_obj.bitmask.lower & DOUBLE_SIGNIFICAND_BITMASK));
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			(*env)->CallVoidMethod(env, wrapper, setE, (jlong) (fp_obj.bitmask.upper & LONGDOUBLE_SIGN_AND_EXPONENT_BITMASK));
			(*env)->CallVoidMethod(env, wrapper, setM, (jlong) fp_obj.bitmask.lower);
			break;
		default:
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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

	const int buffersize = 100; // always enough as we never print more than 20 digits for the significand
	char s[buffersize];

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
		        snprintf(s, buffersize, "%.*e", 8, fp_obj.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			snprintf(s, buffersize, "%.*e", 16, fp_obj.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			snprintf(s, buffersize, "%.*Le", 20, fp_obj.ld_value);
			break;
		default:
			throwException(env, EX_TEXT);
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
			result.d_value = chooseOf2(type1, fp_1) + chooseOf2(type2, fp_2);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = chooseOf3(type1, fp_1) + chooseOf3(type2, fp_2);
			break;
		default:
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_moduloFp(JNIEnv* env, jclass cl, jobject wrapper1, jint type1, jobject wrapper2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, wrapper1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, wrapper2, type2);

	t_ld result = { .ld_value = 0.0L };

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = fmodf(fp_1.f_value, fp_2.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = fmod(chooseOf2(type1, fp_1), chooseOf2(type2, fp_2));
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = fmodl(chooseOf3(type1, fp_1), chooseOf3(type2, fp_2));
			break;
		default:
			throwException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_remainderFp(JNIEnv* env, jclass cl, jobject wrapper1, jint type1, jobject wrapper2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, wrapper1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, wrapper2, type2);

	t_ld result = { .ld_value = 0.0L };

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = remainderf(fp_1.f_value, fp_2.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = remainder(chooseOf2(type1, fp_1), chooseOf2(type2, fp_2));
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = remainderl(chooseOf3(type1, fp_1), chooseOf3(type2, fp_2));
			break;
		default:
			throwException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, maxType);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_logFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);
	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = logf(fp.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = log(fp.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = logl(fp.ld_value);
			break;
		default:
			throwException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
}


JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_expFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);
	t_ld result = { .ld_value = 0.0L };

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			result.f_value = expf(fp.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			result.d_value = exp(fp.d_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			result.ld_value = expl(fp.ld_value);
			break;
		default:
			throwException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, result, type);
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
			throwException(env, EX_TEXT);
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
				throwException(env, EX_TEXT);
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
					throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
	}

	return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isOneFp(JNIEnv* env, jclass cl, jobject wrapper, jint type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, type);

	switch(type) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return fp.f_value == 1.0 || fp.f_value == -1.0;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return fp.d_value == 1.0 || fp.d_value == -1.0;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return fp.ld_value == 1.0L || fp.ld_value == -1.0L;
		default:
			throwException(env, EX_TEXT);
	}

	return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isEqualFp(JNIEnv* env, jclass cl, jobject fp1, jint type1, jobject fp2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type2);

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return fp_1.f_value == fp_2.f_value;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return chooseOf2(type1, fp_1) == chooseOf2(type2, fp_2);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return chooseOf3(type1, fp_1) == chooseOf3(type2, fp_2);
		default:
			throwException(env, EX_TEXT);
	}
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isNotEqualFp(JNIEnv* env, jclass cl, jobject fp1, jint type1, jobject fp2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type2);

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return islessgreater(fp_1.f_value, fp_2.f_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return islessgreater(chooseOf2(type1, fp_1), chooseOf2(type2, fp_2));
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return islessgreater(chooseOf3(type1, fp_1), chooseOf3(type2, fp_2));
		default:
			throwException(env, EX_TEXT);
	}
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isGreaterFp(JNIEnv* env, jclass cl, jobject fp1, jint type1, jobject fp2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type2);

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return isgreater(fp_1.f_value, fp_2.f_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return isgreater(chooseOf2(type1, fp_1), chooseOf2(type2, fp_2));
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return isgreater(chooseOf3(type1, fp_1), chooseOf3(type2, fp_2));
		default:
			throwException(env, EX_TEXT);
	}
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isGreaterEqualFp(JNIEnv* env, jclass cl, jobject fp1, jint type1, jobject fp2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type2);

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return isgreaterequal(fp_1.f_value, fp_2.f_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return isgreaterequal(chooseOf2(type1, fp_1), chooseOf2(type2, fp_2));
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return isgreaterequal(chooseOf3(type1, fp_1), chooseOf3(type2, fp_2));
		default:
			throwException(env, EX_TEXT);
	}
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isLessFp(JNIEnv* env, jclass cl, jobject fp1, jint type1, jobject fp2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type2);

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return isless(fp_1.f_value, fp_2.f_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return isless(chooseOf2(type1, fp_1), chooseOf2(type2, fp_2));
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return isless(chooseOf3(type1, fp_1), chooseOf3(type2, fp_2));
		default:
			throwException(env, EX_TEXT);
	}
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_isLessEqualFp(JNIEnv* env, jclass cl, jobject fp1, jint type1, jobject fp2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type2);

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			return islessequal(fp_1.f_value, fp_2.f_value);
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			return islessequal(chooseOf2(type1, fp_1), chooseOf2(type2, fp_2));
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			return islessequal(chooseOf3(type1, fp_1), chooseOf3(type2, fp_2));
		default:
			throwException(env, EX_TEXT);
	}
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_totalOrderFp(JNIEnv* env, jclass cl, jobject fp1, jint type1, jobject fp2, jint type2) {
	t_ld fp_1 = transformWrapperFromJava(env, fp1, type1);
	t_ld fp_2 = transformWrapperFromJava(env, fp2, type2);

        int leq, gte;

	jint maxType = max(type1, type2);
	switch(maxType) {
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			leq = totalorderf(&fp_1.f_value, &fp_2.f_value);
			gte = totalorderf(&fp_2.f_value, &fp_1.f_value);
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			leq = totalorder(&fp_1.d_value, &fp_2.d_value);
                        gte = totalorder(&fp_2.d_value, &fp_1.d_value);
                        break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			leq = totalorderl(&fp_1.ld_value, &fp_2.ld_value);
                        gte = totalorderl(&fp_2.ld_value, &fp_1.ld_value);
                        break;
		default:
			throwException(env, EX_TEXT);
	}

	if (leq && gte) {
	  return 0;
	} else {
	  return leq ? -1 : 1;
	}
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
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
			result.ld_value = copysignl(fp_1.ld_value, fp_2.ld_value);
			break;
		default:
			throwException(env, EX_TEXT);
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
			throwException(env, EX_TEXT);
	}

	return transformWrapperToJava(env, fp, to_type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castByteToFp(JNIEnv* env, jclass cl, jbyte number, jint to_fp_type) {
	t_ld fp = { .ld_value = 0.0L };
        switch(to_fp_type) {
                case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
        	        fp.f_value = (float) number;
        		break;
        	case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
        	        fp.d_value = (double) number;
        		break;
       		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
        		fp.ld_value = (long double) number;
        	        break;
        	default:
        	        throwException(env, EX_TEXT);
        }
        return transformWrapperToJava(env, fp, to_fp_type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castShortToFp(JNIEnv* env, jclass cl, jshort number, jint to_fp_type) {
	t_ld fp = { .ld_value = 0.0L };
        switch(to_fp_type) {
                case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
        	        fp.f_value = (float) number;
        		break;
        	case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
        	        fp.d_value = (double) number;
        		break;
       		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
        		fp.ld_value = (long double) number;
        	        break;
        	default:
        	        throwException(env, EX_TEXT);
        }
        return transformWrapperToJava(env, fp, to_fp_type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castIntToFp(JNIEnv* env, jclass cl, jint number, jint to_fp_type) {
	t_ld fp = { .ld_value = 0.0L };
        switch(to_fp_type) {
                case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
        	        fp.f_value = (float) number;
        		break;
        	case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
        	        fp.d_value = (double) number;
        		break;
       		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
        		fp.ld_value = (long double) number;
        	        break;
        	default:
        	        throwException(env, EX_TEXT);
        }
        return transformWrapperToJava(env, fp, to_fp_type);
}

JNIEXPORT jobject JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castLongToFp(JNIEnv* env, jclass cl, jlong number, jint to_fp_type) {
	t_ld fp = { .ld_value = 0.0L };
        switch(to_fp_type) {
                case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
        	        fp.f_value = (float) number;
        		break;
        	case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
        	        fp.d_value = (double) number;
        		break;
       		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
        		fp.ld_value = (long double) number;
        	        break;
        	default:
        	        throwException(env, EX_TEXT);
        }
        return transformWrapperToJava(env, fp, to_fp_type);
}

JNIEXPORT jbyte JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castFpToByte(JNIEnv* env, jclass cl, jobject wrapper, jint fp_from_type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, fp_from_type);
        jbyte r;
        switch(fp_from_type) {
        	case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			r = (int8_t) fp.f_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			r = (int8_t) fp.d_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			r = (int8_t) fp.ld_value;
			break;
		default:
			throwException(env, EX_TEXT);
	}
	return r;
}

JNIEXPORT jshort JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castFpToShort(JNIEnv* env, jclass cl, jobject wrapper, jint fp_from_type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, fp_from_type);
        jshort r;
        switch(fp_from_type) {
        	case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			r = (int16_t) fp.f_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			r = (int16_t) fp.d_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			r = (int16_t) fp.ld_value;
			break;
		default:
			throwException(env, EX_TEXT);
	}
	return r;
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castFpToInt(JNIEnv* env, jclass cl, jobject wrapper, jint fp_from_type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, fp_from_type);
        jint r;
        switch(fp_from_type) {
        	case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			r = (int32_t) fp.f_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			r = (int32_t) fp.d_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
			r = (int32_t) fp.ld_value;
			break;
		default:
			throwException(env, EX_TEXT);
	}
	return r;
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_floatingpoint_CFloatNativeAPI_castFpToLong(JNIEnv* env, jclass cl, jobject wrapper, jint fp_from_type) {
	t_ld fp = transformWrapperFromJava(env, wrapper, fp_from_type);
        jlong r;
        switch(fp_from_type) {
        	case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_SINGLE:
			r = (int64_t) fp.f_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_DOUBLE:
			r = (int64_t) fp.d_value;
			break;
		case org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI_FP_TYPE_LONG_DOUBLE:
		 	r = (int64_t) fp.ld_value;
			break;
		default:
			throwException(env, EX_TEXT);
	}
	return r;
}
