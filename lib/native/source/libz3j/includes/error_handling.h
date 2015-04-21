// TODO: as C doesn't have closures, we are using the global variable
// to communicate the information.
// This might cause issues in the presence of multiple threads.
JNIEnv* globalEnv;


void Z3JavaErrorHandler(Z3_context c, Z3_error_code e) {
    JNIEnv* env = globalEnv;
    if (e != Z3_OK) {
        char const * errMsg = Z3_get_error_msg_ex(c, e);

        // These errors normally should not occur and we don't wish them
        // to be caught.
        throwException(env, "org/sosy_lab/cpachecker/util/predicates/z3/Z3SolverException", errMsg);
    }
}

JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_predicates_z3_Z3NativeApi_setInternalErrorHandler(
    JNIEnv* env, jclass jcls, jJ_context a0) {

    globalEnv = env;
    Z3_set_error_handler((Z3_context)a0, Z3JavaErrorHandler);
}

