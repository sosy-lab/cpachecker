 
// Macros for defining JNI functions which call Z3
// Use them as follows:
//
// DEFINE_FUNC(java_return_type, escaped_name_without_Z3) WITH_X_ARGS(java_arg_types)
// for each arg a definition like STRUCT_ARG(Z3_arg_type, position)
// CALLX(Z3_return_type, function_name_without_Z3)
// return definition like STRUCT_RETURN depending on the return type

#define DEFINE_FUNC(jreturn, func_escaped) \
  JNIEXPORT j##jreturn JNICALL Java_org_sosy_1lab_cpachecker_util_predicates_z3_Z3NativeApi_##func_escaped

#define WITHOUT_ARGS \
  (JNIEnv *jenv, jclass jcls) {

#define WITH_1_ARGS(jtype) \
  (JNIEnv *jenv, jclass jcls, j##jtype arg1) {

#define WITH_2_ARGS(jtype1, jtype2) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2) {

#define WITH_3_ARGS(jtype1, jtype2, jtype3) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3) {

#define WITH_4_ARGS(jtype1, jtype2, jtype3, jtype4) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4) {

#define WITH_5_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5) {

#define WITH_6_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5, jtype6) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5, j##jtype6 arg6) {

#define WITH_7_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5, jtype6, jtype7) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5, j##jtype6 arg6, j##jtype7 arg7) {

#define WITH_8_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5, jtype6, jtype7, jtype8) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5, j##jtype6 arg6, j##jtype7 arg7, j##jtype8 arg8) {

#define WITH_9_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5, jtype6, jtype7, jtype8, jtype9) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5, j##jtype6 arg6, j##jtype7 arg7, j##jtype8 arg8, j##jtype9 arg9) {

#define WITH_10_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5, jtype6, jtype7, jtype8, jtype9, jtype10) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5, j##jtype6 arg6, j##jtype7 arg7, j##jtype8 arg8, j##jtype9 arg9, j##jtype10 arg10) {

#define WITH_11_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5, jtype6, jtype7, jtype8, jtype9, jtype10, jtype11) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5, j##jtype6 arg6, j##jtype7 arg7, j##jtype8 arg8, j##jtype9 arg9, j##jtype10 arg10, j##jtype11 arg11) {

#define WITH_12_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5, jtype6, jtype7, jtype8, jtype9, jtype10, jtype11, jtype12) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5, j##jtype6 arg6, j##jtype7 arg7, j##jtype8 arg8, j##jtype9 arg9, j##jtype10 arg10, j##jtype11 arg11, j##jtype12 arg12 ) {

#define WITH_13_ARGS(jtype1, jtype2, jtype3, jtype4, jtype5, jtype6, jtype7, jtype8, jtype9, jtype10, jtype11, jtype12, jtype13) \
  (JNIEnv *jenv, jclass jcls, j##jtype1 arg1, j##jtype2 arg2, j##jtype3 arg3, j##jtype4 arg4, j##jtype5 arg5, j##jtype6 arg6, j##jtype7 arg7, j##jtype8 arg8, j##jtype9 arg9, j##jtype10 arg10, j##jtype11 arg11, j##jtype12 arg12, j##jtype13 arg13 ) {


#define CALL0(z3return, func) z3return retval = Z3_##func();
#define CALL1(z3return, func) z3return retval = Z3_##func(z3_arg1);
#define CALL2(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2);
#define CALL3(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3);
#define CALL4(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4);
#define CALL5(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5);
#define CALL6(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6);
#define CALL7(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7);
#define CALL8(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7, z3_arg8);
#define CALL9(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7, z3_arg8, z3_arg9);
#define CALL10(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7, z3_arg8, z3_arg9, z3_arg10);
#define CALL11(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7, z3_arg8, z3_arg9, z3_arg10, z3_arg11);
#define CALL12(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7, z3_arg8, z3_arg9, z3_arg10, z3_arg11, z3_arg12);
#define CALL13(z3return, func) z3return retval = Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7, z3_arg8, z3_arg9, z3_arg10, z3_arg11, z3_arg12, z3_arg13);


#define VOID_CALL0(func) Z3_##func();
#define VOID_CALL1(func) Z3_##func(z3_arg1);
#define VOID_CALL2(func) Z3_##func(z3_arg1, z3_arg2);
#define VOID_CALL3(func) Z3_##func(z3_arg1, z3_arg2, z3_arg3);
#define VOID_CALL4(func) Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4);
#define VOID_CALL5(func) Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5);
#define VOID_CALL6(func) Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6);
#define VOID_CALL7(func) Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7);
#define VOID_CALL8(func) Z3_##func(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5, z3_arg6, z3_arg7, z3_arg8);

