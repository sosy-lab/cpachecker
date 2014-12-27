/**
 * Temporary workaround to deal with the bug in python scripts for the bindings
 * generation:
 * they return a pointer-to-pointer instead of returning the pointer.
 * (E.g. in the binding generating code the write would go as (note the absence of the dereference operator compared to the version below):
 *      { jclass cls = (*jenv)->GetObjectClass(jenv, arg5); jfieldID fid = (*jenv)->GetFieldID(jenv, cls, "value", "J"); (*jenv)->SetLongField(jenv, arg5, fid, (jlong)z3_arg5); }
 *
 *
 */
__attribute__((visibility("default"))) jJ_lbool Java_org_sosy_1lab_cpachecker_util_predicates_z3_Z3NativeApi_compute_1interpolantFixed (JNIEnv *jenv, jclass jcls, jJ_context arg1, jJ_ast arg2, jJ_params arg3, jJ_ast_vector_pointer arg4, jJ_model_pointer arg5) {
Z3_context z3_arg1 = (void *)((size_t)arg1);
Z3_ast z3_arg2 = (void *)((size_t)arg2);
Z3_params z3_arg3 = (void *)((size_t)arg3);
Z3_ast_vector s_arg4; Z3_ast_vector * z3_arg4 = &s_arg4;
Z3_model s_arg5; Z3_model * z3_arg5 = &s_arg5;
Z3_lbool retval = Z3_compute_interpolant(z3_arg1, z3_arg2, z3_arg3, z3_arg4, z3_arg5);
{ jclass cls = (*jenv)->GetObjectClass(jenv, arg5); jfieldID fid = (*jenv)->GetFieldID(jenv, cls, "value", "J"); (*jenv)->SetLongField(jenv, arg5, fid, (jlong)*z3_arg5); }

{ jclass cls = (*jenv)->GetObjectClass(jenv, arg4); jfieldID fid = (*jenv)->GetFieldID(jenv, cls, "value", "J"); (*jenv)->SetLongField(jenv, arg4, fid, (jlong)*z3_arg4); }

out3:
out2:
out1:
return (jint)retval; };
