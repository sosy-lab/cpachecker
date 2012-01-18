#include "yices_YicesLite.h"
#include <yicesl_c.h>

/*
 * Class:     yices_YicesLite
 * Method:    yicesl_set_verbosity
 * Signature: (S)V
 */
JNIEXPORT void JNICALL Java_yices_YicesLite_yicesl_1set_1verbosity
  (JNIEnv * env, jobject jobj, jshort level)
{
	yicesl_set_verbosity(level);	
}


/*
 * Class:     yices_YicesLite
 * Method:    yicesl_version
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_yices_YicesLite_yicesl_1version
 (JNIEnv * env, jobject jobj)
{
	jstring version;
	char * c_version = yicesl_version();
	version =  (* env)->NewStringUTF(env, c_version);
	return version; 
}

/*
 * Class:     yices_YicesLite
 * Method:    yicesl_enable_type_checker
 * Signature: (S)V
 */
JNIEXPORT void JNICALL Java_yices_YicesLite_yicesl_1enable_1type_1checker
 (JNIEnv * env, jobject jobj, jshort flag)
{
	yicesl_enable_type_checker(flag);	
}

/*
 * Class:     yices_YicesLite
 * Method:    yicesl_enable_log_file
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_yices_YicesLite_yicesl_1enable_1log_1file
 (JNIEnv * env, jobject jobj, jstring filename)
{
	
	jboolean iscopy;
	char * fname = (* env)->GetStringUTFChars(env, filename, &iscopy);
	yicesl_enable_log_file(fname);
}
/*
 * Class:     yices_YicesLite
 * Method:    yicesl_mk_context
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_yices_YicesLite_yicesl_1mk_1context
  (JNIEnv * env, jobject jobj)
{
	yicesl_context ctx;
	ctx = yicesl_mk_context();
	
	return (jint) ctx;
}

/*
 * Class:     yices_YicesLite
 * Method:    yicesl_del_context
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_yices_YicesLite_yicesl_1del_1context
   (JNIEnv * env, jobject jobj, jint context)
{
	yicesl_context ctx = (yicesl_context) context;
	yicesl_del_context(ctx);
}

/*
 * Class:     yices_YicesLite
 * Method:    yicesl_read
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_yices_YicesLite_yicesl_1read
    (JNIEnv * env, jobject jobj, jint context, jstring cmd)
{
	yicesl_context ctx = (yicesl_context) context;
	jboolean iscopy;
	const char * command = (* env)->GetStringUTFChars(env, cmd, &iscopy);
	return (jint) yicesl_read(ctx,command);
}

/*
 * Class:     yices_YicesLite
 * Method:    yicesl_inconsistent
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_yices_YicesLite_yicesl_1inconsistent
  (JNIEnv * env, jobject jobj, jint context)
{
	yicesl_context ctx = (yicesl_context) context;
	return (jint) yicesl_inconsistent(ctx);
}

/*
 * Class:     yices_YicesLite
 * Method:    yicesl_get_last_error_message
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_yices_YicesLite_yicesl_1get_1last_1error_1message
  (JNIEnv * env, jobject jobj)
{
	
	jstring err_mess;
	char * c_err_mess = yicesl_get_last_error_message();
	err_mess =  (* env)->NewStringUTF(env, c_err_mess);
	return err_mess;
	 
}
/*
 * Class:     yices_YicesLite
 * Method:    yicesl_set_output_file
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_yices_YicesLite_yicesl_1set_1output_1file
 (JNIEnv * env, jobject jobj, jstring filename)
{
	jboolean iscopy;
	char * fname = (* env)->GetStringUTFChars(env, filename, &iscopy);
	yicesl_set_output_file(fname);
}

