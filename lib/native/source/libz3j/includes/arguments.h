
// WARNING: if an exception occures while a functioncall, the returnvalue is undefined!


// GET ARGUMENT OF FUNCTION WITH CORRECT TYPE

#define SIMPLE_ARG(z3type, num) \
  z3type z3_arg##num = arg##num;


#define STRUCT_POINTER_ARG(z3type, num) \
  z3type s_arg##num; \
  z3type * z3_arg##num = &s_arg##num;


#define STRUCT_ARG(z3type, num) \
  if (arg##num == 0) { \
    throwException(jenv, "java/lang/IllegalArgumentException", "Null passed to Z3"); \
    goto out##num; \
  } \
  z3type z3_arg##num = (void *)((size_t)arg##num);


#define STRING_ARG(num) \
  char * z3_arg##num = (char *)(*jenv)->GetStringUTFChars(jenv, arg##num, NULL); \
  if (z3_arg##num == NULL) { \
    goto out##num; \
  }


#define INT_ARRAY_ARG(num) \
  int * z3_arg##num = (int *)((*jenv)->GetIntArrayElements(jenv, arg##num, NULL)); \
  if (z3_arg##num == NULL) { \
    goto out##num; \
  }


// array input from Java -> copy values into new array for C
#define STRUCT_ARRAY_ARG(z3type, num) \
  z3type * z3_arg##num; \
  { \
    size_t sz = (size_t)((*jenv)->GetArrayLength(jenv, arg##num)); \
    z3_arg##num = (z3type *)malloc(sizeof(z3type) * sz); \
    if (z3_arg##num == NULL) { \
      throwException(jenv, "java/lang/OutOfMemoryError", "Cannot allocate native memory for calling Z3"); \
      goto out##num##a; \
    } \
    \
    jlong *tmp = (jlong *)((*jenv)->GetLongArrayElements(jenv, arg##num, NULL)); \
    if (tmp == NULL) { \
      goto out##num##b; \
    } \
    \
    size_t i; \
    for (i = 0; i < sz; ++i) { \
      z3_arg##num[i] = (void *)((size_t)tmp[i]); \
      if (z3_arg##num[i] == NULL) { \
        throwException(jenv, "java/lang/IllegalArgumentException", "Null passed to Z3"); \
        goto out##num##b; \
      } \
    } \
    (*jenv)->ReleaseLongArrayElements(jenv, arg##num, tmp, 0); \
  }




  
// CLEAN AFTER FUNCTIONCALL
// for every GET_ARG, there must be a CLEAN_ARG,
// declare all jump-labels and free some memory


#define NOTHING_TO_DO 
#define CLEAN_SIMPLE_ARG(num) NOTHING_TO_DO
#define CLEAN_STRUCT_POINTER_ARG(num) NOTHING_TO_DO

#define CLEAN_STRUCT_ARG(num) \
  out##num:

#define CLEAN_STRING_ARG(num) \
  (*jenv)->ReleaseStringUTFChars(jenv, arg##num, z3_arg##num); \
  out##num:

#define CLEAN_INT_ARRAY_ARG(num) \
  (*jenv)->ReleaseIntArrayElements(jenv, arg##num, z3_arg##num, 0); \
  out##num:

#define CLEAN_STRUCT_ARRAY_ARG(num) \
  out##num##b: \
  free(z3_arg##num); \
  out##num##a:



// fill Java-array with values from C-array,
// the array has to be given as input-arg with ARRAY_ARG
// all elements are casted to jlong,
#define FILL_ARRAY_ARG(num) \
  if (!(*jenv)->ExceptionCheck(jenv)) { \
    size_t sz = (size_t)((*jenv)->GetArrayLength(jenv, arg##num)); \
    (*jenv)->SetLongArrayRegion(jenv, arg##num, 0, sz, (jlong *)(z3_arg##num)); \
  }


// POINTER TO SOMETHING
// there are special classes for this in NativeApi.java

typedef jobject jJ_string_pointer;
typedef jobject jint_pointer;
typedef jobject jlong_pointer;

typedef jobject jvoid_pointer;

#define ARRAY_POINTER_ARG(z3type, num) STRUCT_POINTER_ARG(z3type, num)
#define ARRAY_ARG(z3type, num) STRUCT_ARRAY_ARG(z3type, num)

#define STRING_POINTER_ARG(num) STRUCT_POINTER_ARG(Z3_string, num)
#define LONG_POINTER_ARG(num) STRUCT_POINTER_ARG(long, num)
#define VOID_POINTER_ARG(num) STRUCT_POINTER_ARG(long, num)
#define INT_POINTER_ARG(num) STRUCT_POINTER_ARG(int, num)
  
#define CLEAN_STRING_POINTER_ARG(num) CLEAN_STRUCT_POINTER_ARG(num)
#define CLEAN_LONG_POINTER_ARG(num) CLEAN_STRUCT_POINTER_ARG(num)
#define CLEAN_VOID_POINTER_ARG(num) CLEAN_STRUCT_POINTER_ARG(num)
#define CLEAN_INT_POINTER_ARG(num) CLEAN_STRUCT_POINTER_ARG(num)


#define SET_STRING_POINTER_ARG(num) \
  { \
    jclass cls    = (*jenv)->GetObjectClass(jenv, arg##num); \
    jfieldID fid = (*jenv)->GetFieldID(jenv, cls, "value", "Ljava/lang/String;"); \
    jstring str = (*jenv)->NewStringUTF(jenv, *z3_arg##num); \
    (*jenv)->SetObjectField(jenv, arg##num, fid, str); \
    (*jenv)->ReleaseStringUTFChars(jenv, str, *z3_arg##num); \
  }

#define SET_INT_POINTER_ARG(num) \
  { \
    jclass cls    = (*jenv)->GetObjectClass(jenv, arg##num); \
    jfieldID fid = (*jenv)->GetFieldID(jenv, cls, "value", "I"); \
    (*jenv)->SetIntField(jenv, arg##num, fid, *z3_arg##num); \
  }

#define SET_LONG_POINTER_ARG(num) \
  { \
    jclass cls    = (*jenv)->GetObjectClass(jenv, arg##num); \
    jfieldID fid = (*jenv)->GetFieldID(jenv, cls, "value", "J"); \
    (*jenv)->SetLongField(jenv, arg##num, fid, (jlong)z3_arg##num); \
  }




// RETURN SOMETHING

#define VOID_RETURN \
  return; \
}

#define STRUCT_RETURN \
  if (retval == NULL) { \
    throwException(jenv, "java/lang/IllegalArgumentException", "Z3 returned null"); \
  } \
  return (jlong)((size_t)(retval)); \
}
  
#define STRUCT_RETURN_WITH_CONTEXT \
  if (retval == NULL) { \
    const char *msg = Z3_get_error_msg(Z3_get_error_code(z3_arg1)); \
    throwException(jenv, "java/lang/IllegalArgumentException", msg); \
  } \
  return (jlong)((size_t)(retval)); \
}

#define INT_RETURN \
  return (jint)retval; \
}

#define DOUBLE_RETURN \
  return (jdouble)retval; \
}

#define STRING_RETURN \
  jstring jretval = NULL; \
  if (!(*jenv)->ExceptionCheck(jenv)) { \
    jretval = (*jenv)->NewStringUTF(jenv, retval); \
  } \
  return jretval; \
}
  //TODO why is there an warning with "free(retval);"?

#define STRING_RETURN_WITH_CONTEXT \
  if (retval == NULL) { \
    const char *msg = Z3_get_error_msg(Z3_get_error_code(z3_arg1)); \
    throwException(jenv, "java/lang/IllegalArgumentException", msg); \
    return NULL; \
  } \
  STRING_RETURN

#define CONST_STRING_RETURN \
  if (retval == NULL) { \
    const char *msg = Z3_get_error_msg(Z3_get_error_code(z3_arg1)); \
    throwException(jenv, "java/lang/IllegalArgumentException", msg); \
    return NULL; \
  } \
  jstring jretval = NULL; \
  if (!(*jenv)->ExceptionCheck(jenv)) { \
    jretval = (*jenv)->NewStringUTF(jenv, retval); \
  } \
  return jretval; \
}

#define FAILURE_CODE_RETURN \
  if (retval != 0) { \
    const char *msg = Z3_get_error_msg(Z3_get_error_code(z3_arg1)); \
    throwException(jenv, "java/lang/IllegalArgumentException", msg); \
  } \
}


/*
 * Copied from the Sun JNI Programmer's Guide and Specification
 */
void throwException(JNIEnv *env, const char *name, const char *msg) {
        jclass cls = (*env)->FindClass(env, name);
        if (cls != NULL) {
                (*env)->ThrowNew(env, cls, msg);
        }
}
