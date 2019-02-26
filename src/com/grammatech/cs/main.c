#include "csurf/src/csurf_config.h"
#include "csurf/src/api/java/csurf_java.h"
#include "gtr/src/file_funcs/file_funcs.h"
#include "gtr/src/gthome/gthome.h"
#include <jni.h>
#include "third-party/libtool/inst/include/ltdl.h"
#include "csurf/src/api/java/jni_module_name.h"
 
#define PATH_SEPARATOR PATHSEP

int csurf_java_main(const char *_user_classpath, 
                    const char *user_class, 
                    const char *jvm_dll,
                    int argc, const char * const * argv )
{
    JNIEnv *env = NULL;
    JavaVM *jvm = NULL;
    jint res;
    jclass cls;
    jmethodID mid;
    jstring jstr;
    jclass stringClass;
    jobjectArray args;
    int rv = 1;
    char *csurf_classpath = gthome_expand_gthome(
        "$GTHOME"SDIRSEP"csurf"SDIRSEP
        "classes");
    char *csurf_jni_module_path = gthome_expand_gthome(
        "$GTHOME"SDIRSEP"csurf"SDIRSEP
        "bin"SDIRSEP JNI_MODULE_NAME);
    lt_dlhandle dll = NULL;
    char *user_classpath_exp = gthome_expand_gthome(_user_classpath);
    const char *user_classpath = user_classpath_exp ? user_classpath_exp : _user_classpath;
#ifdef JNI_VERSION_1_2
    JavaVMInitArgs vm_args;
    JavaVMOption options[1];
    typedef jint (JNICALL *JNI_CreateJavaVM_fn_t)(JavaVM **pvm, void **penv, void *args);
    JNI_CreateJavaVM_fn_t create_vm;
    options[0].optionString = cs_malloc(strlen("-Djava.class.path=")
                                        +strlen(csurf_classpath)
                                        +1
                                        +strlen(user_classpath)
                                        +1);
    strcpy( options[0].optionString, "-Djava.class.path=" );
    strcat( options[0].optionString, csurf_classpath );
    strcat( options[0].optionString, SPATHSEP );
    strcat( options[0].optionString, user_classpath );
    vm_args.version = 0x00010002;
    vm_args.options = options;
    vm_args.nOptions = 1;
    vm_args.ignoreUnrecognized = JNI_TRUE;
#define JVM_VOID_CAST (void**)
#else
    typedef jint (JNICALL *JNI_CreateJavaVM_fn_t)(JavaVM **pvm, JNIEnv **penv, void *args);
    JNI_CreateJavaVM_fn_t create_vm;
    JDK1_1InitArgs vm_args;
    char *classpath = cs_malloc(strlen(csurf_classpath)
                                +strlen(user_classpath)
                                +strlen(vm_args.classpath)
                                +3);
    vm_args.version = 0x00010001;
    JNI_GetDefaultJavaVMInitArgs(&vm_args);
    sprintf(classpath, "%s%c%s%c%s",
            vm_args.classpath, 
            PATH_SEPARATOR, 
            csurf_classpath,
            PATH_SEPARATOR, 
            user_classpath );
    vm_args.classpath = classpath;
#define JVM_VOID_CAST
#endif /* JNI_VERSION_1_2 */
    lt_dlinit();
    
    if( !jvm_dll )
    {
        jvm_dll = 
#ifdef WIN32
            "jvm.dll"
#else
            "libjvm"
#endif
            ;
    }
    dll = lt_dlopenext(jvm_dll);
    if( !dll )
    {
        fprintf( stderr, "lt_dlopenext(%s): %s\n", 
                 jvm_dll,
                 lt_dlerror() );
        goto destroy;
    }
    create_vm = (JNI_CreateJavaVM_fn_t)lt_dlsym( 
        dll, "JNI_CreateJavaVM" );
    if( !create_vm )
    {
        fprintf( stderr, "lt_dlsym(JNI_CreateJavaVM): %s\n", lt_dlerror() );
        goto destroy;
    }
    res = create_vm(&jvm, JVM_VOID_CAST &env, &vm_args);
 
    if (res < 0) {
        fprintf(stderr, "Can't create Java VM\n");
        goto destroy;
    }

    /* For some reason, if we change this to java/lang/System, it
     * doesn't work. 
     */
    cls = (*env)->FindClass(env, "com/grammatech/cs/csLoader");
    if (cls == NULL) {
        goto destroy;
    }

    mid = (*env)->GetStaticMethodID(env, cls, "load",
                                    "(Ljava/lang/String;)V");
    if (mid == NULL) {
        goto destroy;
    }
    jstr = (*env)->NewStringUTF(env, csurf_jni_module_path);
    if (jstr == NULL) {
        goto destroy;
    }

    (*env)->CallStaticVoidMethod(env, cls, mid, jstr);
    if ((*env)->ExceptionOccurred(env)) {
        goto destroy;
    }
    
    cls = (*env)->FindClass(env, user_class);
    if (cls == NULL) {
        goto destroy;
    }
 
    mid = (*env)->GetStaticMethodID(env, cls, "main",
                                    "([Ljava/lang/String;)V");
    if (mid == NULL) {
        goto destroy;
    }
    jstr = (*env)->NewStringUTF(env, "this string will not survive");
    if (jstr == NULL) {
        goto destroy;
    }
    stringClass = (*env)->FindClass(env, "java/lang/String");
    args = (*env)->NewObjectArray(env, argc, stringClass, jstr);
    if (args == NULL) {
        goto destroy;
    }
    while( argc-- > 0 )
    {
        jstr = (*env)->NewStringUTF(env, argv[argc]);
        if (jstr == NULL) {
            goto destroy;
        }
        (*env)->SetObjectArrayElement(env, args, argc, jstr);
    }
    (*env)->CallStaticVoidMethod(env, cls, mid, args);
    rv = 0;
destroy:
    if( env )
    {
        if ((*env)->ExceptionOccurred(env)) {
            rv = 2;
            (*env)->ExceptionDescribe(env);
        }
    }
    if( jvm )
        (*jvm)->DestroyJavaVM(jvm);
    if( user_classpath_exp )
        cs_free( user_classpath_exp );
    if( dll )
        lt_dlclose(dll);
    lt_dlexit();
    return rv;
}
