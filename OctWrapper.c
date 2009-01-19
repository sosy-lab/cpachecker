#include <stdio.h>
#include <math.h>
#include <oct.h>
#include <assert.h>
#include <oct_private.h>
#include "octagon_OctWrapper.h"

jobject convertToJOct (JNIEnv *env, jobject obj, oct_t* oct);
oct_t* convertToCOct (JNIEnv *env, jobject obj, jobject obj1);
num_t* convertToCArray(JNIEnv *env, jobject obj, jobject jarray);
long mylrand();
double mydrand();
oct_t* random_oct(int n, int m);
void printContent (oct_t* oct);
void destructNumT(num_t* array, JNIEnv *env, jobject jarray);
void freeOctC(oct_t* oct);

//var for random number generator
unsigned long long seed;

/*
 * Class:     OctWrapper
 * Method:    J_init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_octagon_OctWrapper_J_1init
  (JNIEnv *env, jobject obj){
	int i;
	jboolean b;
	i = oct_init ();
	if (i==0) b=0;
	else if (i==1) b=1;
	return b;
}

/*
 * Class:     OctWrapper
 * Method:    J_empty
 * Signature: (I)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1empty
  (JNIEnv *env, jobject obj, jint in){
	jobject	jobj;
	oct_t* oct =  oct_empty (in);
	jobj = convertToJOct (env, obj, oct);
	oct_free(oct);
	return jobj;
}

/*
 * Class:     OctWrapper
 * Method:    J_universe
 * Signature: (I)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1universe
  (JNIEnv *env , jobject obj, jint in){
	jobject	jobj;
	oct_t* oct =  oct_universe (in);
	jobj = convertToJOct (env, obj, oct);
	oct_free(oct);
	return jobj;
}

/*
 * Class:     OctWrapper
 * Method:    J_free
 * Signature: (LOctagon;)V
 */
// TODO
JNIEXPORT void JNICALL Java_octagon_OctWrapper_J_1free
  (JNIEnv *env , jobject obj1, jobject obj2){

}

/*
 * Class:     OctWrapper
 * Method:    J_dimension
 * Signature: (LOctagon;)I
 */
JNIEXPORT jint JNICALL Java_octagon_OctWrapper_J_1dimension
  (JNIEnv *env, jobject obj1, jobject obj2){
   oct_t* oct = convertToCOct(env, obj1, obj2);
   assert(oct != NULL);
   int dim = oct_dimension (oct);
   freeOctC(oct);
   oct_free(oct);
   return dim;
}

/*
 * Class:     OctWrapper
 * Method:    J_nbconstraints
 * Signature: (LOctagon;)I
 */
JNIEXPORT jint JNICALL Java_octagon_OctWrapper_J_1nbconstraints
  (JNIEnv *env, jobject obj1, jobject obj2){
   oct_t* oct = convertToCOct(env, obj1, obj2);
   assert(oct != NULL);
   int nb = oct_nbconstraints (oct);
   freeOctC(oct);
   oct_free(oct);
   return nb;
}

/*
 * Class:     OctWrapper
 * Method:    J_isEmpty
 * Signature: (LOctagon;)Z
 */
JNIEXPORT jboolean JNICALL Java_octagon_OctWrapper_J_1isEmpty
  (JNIEnv *env, jobject obj1, jobject obj2){
    bool ans;
    oct_t* oct = convertToCOct(env, obj1, obj2);
    assert(oct != NULL);
    ans = oct_is_empty(oct);
    freeOctC(oct);
    oct_free(oct);
    return ans;
}

/*
 * Class:     OctWrapper
 * Method:    J_isEmptyLazy
 * Signature: (LOctagon;)I
 */
JNIEXPORT jint JNICALL Java_octagon_OctWrapper_J_1isEmptyLazy
  (JNIEnv *env, jobject obj1, jobject obj2){
    oct_t* oct = convertToCOct(env, obj1, obj2);
    assert(oct != NULL);
    tbool tb = oct_is_empty_lazy (oct);
    freeOctC(oct);
    oct_free(oct);
    if (tb == tbool_true) return 1;
    else if (tb == tbool_false) return 2;
    else if (tb == tbool_bottom) return 0;
    else if (tb == tbool_top) return 3;
}

/*
 * Class:     OctWrapper
 * Method:    J_isUniverse
 * Signature: (LOctagon;)Z
 */
JNIEXPORT jboolean JNICALL Java_octagon_OctWrapper_J_1isUniverse
  (JNIEnv *env, jobject obj1, jobject obj2){
    bool ans;
    oct_t* oct = convertToCOct(env, obj1, obj2);
    assert(oct != NULL);
    ans = oct_is_universe(oct);
    freeOctC(oct);
    oct_free(oct);
    return ans;
}

/*
 * Class:     OctWrapper
 * Method:    J_isIncludedIn
 * Signature: (LOctagon;LOctagon;)Z
 */
JNIEXPORT jboolean JNICALL Java_octagon_OctWrapper_J_1isIncludedIn
  (JNIEnv *env, jobject obj1, jobject obj2, jobject obj3){
    bool ans;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    oct_t* oct2 = convertToCOct(env, obj1, obj3);
    assert(oct2 != NULL);
    ans = oct_is_included_in(oct1, oct2);
    freeOctC(oct1);
    freeOctC(oct2);
    oct_free(oct1);
    oct_free(oct2);
    return ans;
}

/*
 * Class:     OctWrapper
 * Method:    J_isIncludedInLazy
 * Signature: (LOctagon;LOctagon;)I
 */

JNIEXPORT jint JNICALL Java_octagon_OctWrapper_J_1isIncludedInLazy
  (JNIEnv *env, jobject obj1, jobject obj2, jobject obj3){
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    oct_t* oct2 = convertToCOct(env, obj1, obj3);
    assert(oct2 != NULL);
    tbool tb =  oct_is_included_in_lazy (oct1, oct2);
    freeOctC(oct1);
    freeOctC(oct2);
    oct_free(oct1);
    oct_free(oct2);
    if (tb == tbool_true) return 1;
    else if (tb == tbool_false) return 2;
    else if (tb == tbool_bottom) return 0;
    else if (tb == tbool_top) return 3;
}

/*
 * Class:     OctWrapper
 * Method:    J_isEqual
 * Signature: (LOctagon;LOctagon;)Z
 */
JNIEXPORT jboolean JNICALL Java_octagon_OctWrapper_J_1isEqual
  (JNIEnv *env, jobject obj1, jobject obj2, jobject obj3){
    bool ans;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    oct_t* oct2 = convertToCOct(env, obj1, obj3);
    assert(oct2 != NULL);
    ans = oct_is_equal(oct1, oct2);
    freeOctC(oct1);
    freeOctC(oct2);
    oct_free(oct1);
    oct_free(oct2);
    return ans;
}

/*
 * Class:     OctWrapper
 * Method:    J_isEqualLazy
 * Signature: (LOctagon;LOctagon;)I
 */
//TODO This function fails, figure this out
JNIEXPORT jint JNICALL Java_octagon_OctWrapper_J_1isEqualLazy
  (JNIEnv *env, jobject obj1, jobject obj2, jobject obj3){
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    oct_t* oct2 = convertToCOct(env, obj1, obj3);
    assert(oct2 != NULL);
    tbool tb = oct_is_equal_lazy (oct1, oct2);
    freeOctC(oct1);
    freeOctC(oct2);
    oct_free(oct1);
    oct_free(oct2);
    if (tb == tbool_true) return 1;
    else if (tb == tbool_false) return 2;
    else if (tb == tbool_bottom) return 0;
    else if (tb == tbool_top) return 3;
}

/*
 * Class:     OctWrapper
 * Method:    J_isIn
 * Signature: (LOctagon;[LNum;)Z
 */
// TODO Implement Later
JNIEXPORT jboolean JNICALL Java_octagon_OctWrapper_J_1isIn
  (JNIEnv *env, jobject obj1, jobject obj2, jobjectArray objArr){

}

/*
 * Class:     OctWrapper
 * Method:    J_intersection
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */

JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1intersection
  (JNIEnv *env, jobject obj1, jobject obj2, jobject obj3, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    oct_t* oct2 = convertToCOct(env, obj1, obj3);
    assert(oct2 != NULL);
    oct_t* res = oct_intersection(oct1, oct2, b);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    freeOctC(oct2);
    oct_free(oct1);
    oct_free(oct2);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_union
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1union
  (JNIEnv *env, jobject obj1, jobject obj2, jobject obj3, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    oct_t* oct2 = convertToCOct(env, obj1, obj3);
    assert(oct2 != NULL);
    oct_t* res = oct_convex_hull(oct1, oct2, b);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    freeOctC(oct2);
    oct_free(oct1);
    oct_free(oct2);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_widening
 * Signature: (LOctagon;LOctagon;ZI)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1widening
  (JNIEnv *env, jobject obj1, jobject obj2, jobject obj3, jboolean b, jint in){
    jobject ret;
    oct_widening_type type;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    oct_t* oct2 = convertToCOct(env, obj1, obj3);
    assert(oct2 != NULL);
    if(in == 0) {type =  OCT_WIDENING_FAST;}
    else if(in == 1) {type =  OCT_WIDENING_ZERO;}
    else if(in == 2) {type =  OCT_WIDENING_UNIT;}
    oct_t* res = oct_widening(oct1, oct2, b, type);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    freeOctC(oct2);
    oct_free(oct1);
    oct_free(oct2);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_narrowing
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1narrowing
  (JNIEnv *env, jobject obj1, jobject obj2, jobject obj3, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    oct_t* oct2 = convertToCOct(env, obj1, obj3);
    assert(oct2 != NULL);
    oct_t* res = oct_narrowing(oct1, oct2, b);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    freeOctC(oct2);
    oct_free(oct1);
    oct_free(oct2);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_forget
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1forget
  (JNIEnv *env, jobject obj1, jobject obj2, jint in, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t v;
    v = in;
    oct_t* res = oct_forget(oct1, v, b);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_assingVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */

JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1assingVar
  (JNIEnv *env, jobject obj1, jobject obj2, jint in, jobjectArray objArr, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t v;
    v = in;
    num_t* ar = convertToCArray(env, obj1, objArr);
    oct_t* res = oct_assign_variable(oct1, v, ar, b);
    assert(res != NULL);
    destructNumT(ar, env, objArr);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_substituteVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1substituteVar
  (JNIEnv *env, jobject obj1, jobject obj2, jint in, jobjectArray objArr, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t v;
    v = in;
    num_t* ar = convertToCArray(env, obj1, objArr);
    oct_t* res = oct_substitute_variable(oct1, v, ar, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    destructNumT(ar, env, objArr);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_addConstraint
 * Signature: (LOctagon;[LNum;Z)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1addConstraint
  (JNIEnv *env, jobject obj1, jobject obj2, jobjectArray objArr, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    num_t* ar = convertToCArray(env, obj1, objArr);
    oct_t* res = oct_add_constraint(oct1, ar, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    destructNumT(ar, env, objArr);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_intervAssingVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1intervAssingVar
  (JNIEnv *env, jobject obj1, jobject obj2, jint in, jobjectArray objArr, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t v;
    v = in;
    num_t* ar = convertToCArray(env, obj1, objArr);
    assert(ar != NULL);
    oct_t* res = oct_interv_assign_variable(oct1, v, ar, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    destructNumT(ar, env, objArr);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_intervSubstituteVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1intervSubstituteVar
  (JNIEnv *env, jobject obj1, jobject obj2, jint in, jobjectArray objArr, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t v;
    v = in;
    num_t* ar = convertToCArray(env, obj1, objArr);
    assert(ar != NULL);
    oct_t* res = oct_interv_substitute_variable(oct1, v, ar, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    destructNumT(ar, env, objArr);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_intervAddConstraint
 * Signature: (LOctagon;[LNum;Z)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1intervAddConstraint
  (JNIEnv *env, jobject obj1, jobject obj2, jobjectArray objArr, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    num_t* ar = convertToCArray(env, obj1, objArr);
    assert(ar != NULL);
    oct_t* res = oct_interv_add_constraint(oct1, ar, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    destructNumT(ar, env, objArr);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_addDimenensionAndEmbed
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1addDimenensionAndEmbed
  (JNIEnv *env , jobject obj1, jobject obj2, jint in, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t v;
    v = in;
    oct_t* res = oct_add_dimensions_and_embed(oct1, v, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;

}

/*
 * Class:     OctWrapper
 * Method:    J_addDimenensionAndProject
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1addDimenensionAndProject
  (JNIEnv *env , jobject obj1, jobject obj2, jint in, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t v;
    v = in;
    oct_t* res = oct_add_dimensions_and_project(oct1, v, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_removeDimension
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1removeDimension
  (JNIEnv *env , jobject obj1, jobject obj2, jint in, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t v;
    v = in;
    oct_t* res = oct_remove_dimensions(oct1, v, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     octagon_OctWrapper
 * Method:    J_removeDimensionAtPosition
 * Signature: (Loctagon/Octagon;IIZ)Loctagon/Octagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_J_1removeDimensionAtPosition
  (JNIEnv *env, jobject obj1, jobject obj2, jint pos, jint dimension, jboolean b){
    jobject ret;
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    var_t vpos;
    var_t vdim;
    vpos = pos;
    vdim = dimension;
    // TODO, for now we use this method only for one dimsup_t variable, later we can improve this
    dimsup_t array[1];
    array[0].pos = vpos;
    array[0].nbdims = vdim;
    oct_t* res =  oct_remove_dimensions_multi(oct1, array, 1, b);
    assert(res != NULL);
    ret = convertToJOct (env, obj1, res);
    freeOctC(oct1);
    oct_free(oct1);
    oct_free(res);
    return ret;
}

/*
 * Class:     OctWrapper
 * Method:    J_print
 * Signature: (LOctagon;)V
 */
JNIEXPORT void JNICALL Java_octagon_OctWrapper_J_1print
  (JNIEnv *env, jobject obj1, jobject obj2){
    oct_t* oct1 = convertToCOct(env, obj1, obj2);
    assert(oct1 != NULL);
    freeOctC(oct1);
    oct_print (oct1);
    oct_free(oct1);
}

/*
 * Class:     OctWrapper
 * Method:    getRandomOct
 * Signature: ()LOctagon;
 */
JNIEXPORT jobject JNICALL Java_octagon_OctWrapper_getRandomOct
  (JNIEnv *env, jobject obj){
  oct_t* oct = random_oct(3, 10);
  assert(oct != NULL);
  jobject res =  convertToJOct(env, obj, oct);
  assert(res != NULL);
  freeOctC(oct);
  oct_free(oct);
  return res;
}


/* This function takes a C octagon as argument and returns an object in form of a Java octagon (jobject) */
jobject convertToJOct (JNIEnv *env, jobject obj, oct_t* oct) {
	jclass octClass;
	jmethodID cid;
	jobject ret;
	
	jobjectArray numArray;
	jclass numClass;
	jmethodID nid;
	var_t dim;
	int jDim;
	int ref;
	oct_state state;
	int jState;
	int size;
	int i;

	dim = oct->n;
	ref = oct->ref;
	state = oct->state;
	size = matsize(dim);
	jDim = (long)dim;
	if(state == OCT_EMPTY) {jState = 0;}
	else if(state == OCT_NORMAL) {jState = 1;}
	else if(state == OCT_CLOSED) {jState = 2;}

	octClass = (*env)->FindClass(env, "octagon/Octagon");
	cid = (*env)->GetMethodID(env, octClass, "<init>", "(IIILoctagon/Octagon;[Loctagon/Num;)V");
	numClass = (*env)->FindClass(env, "octagon/Num");
	numArray = (*env)->NewObjectArray(env, size, numClass, NULL);
	if(oct->c != NULL){
	  for(i=0; i<size; i++){	
	     nid = (*env)->GetMethodID(env, numClass, "<init>", "(D)V");
	     jobject num = (*env)->NewObject(env, numClass, nid,  num_get_float(&(oct->c[i])));
	     (*env)->SetObjectArrayElement(env, numArray, i, num);
	     (*env)->DeleteLocalRef(env, num);
	  }
	}

	else {
	   numArray = NULL;
	}

	if(oct->closed==NULL){
	   ret = (*env)->NewObject(env, octClass, cid, jDim, ref, jState, NULL, numArray );
	}
	else {
	   printf("CLOSED IS NOT NULL");
	   ret = NULL;
	}
	//printf("Convert TO Java \n");
	//printContent(oct);
	return ret;
}

/* This function takes a J octagon as argument and returns an octagon in form of a oct_t */
oct_t* convertToCOct (JNIEnv *env, jobject obj, jobject obj1) {
	jfieldID fiddim, fidref, fidstate, fidclosed, fidarray;    /* store the field IDs */
	oct_t* oct;
	jobjectArray jarr;
	jobject closed;

  	int dim;
	int ref;
	int state;
	int size;
	int i,j;

	/* Get a reference to obj’s class */
	jclass cls = (*env)->GetObjectClass(env, obj1);

	/* Look for the instance field s in cls */
	fiddim = (*env)->GetFieldID(env, cls, "dimension","I");
	fidref = (*env)->GetFieldID(env, cls, "ref","I");
	fidstate = (*env)->GetFieldID(env, cls, "state","I");
	fidclosed = (*env)->GetFieldID(env, cls, "closed","Loctagon/Octagon;");
	fidarray = (*env)->GetFieldID(env, cls, "matrix","[Loctagon/Num;");

	/* Get fields */
	dim = (*env)-> GetIntField(env, obj1, fiddim);
	ref = (*env)-> GetIntField(env, obj1, fidref);
	state = (*env)-> GetIntField(env, obj1, fidstate);
	closed = (*env)-> GetObjectField(env, obj1, fidclosed);
	jarr = (*env)-> GetObjectField(env, obj1, fidarray);

	size = matsize(dim);

	num_t* matrix = new_n(num_t,size);
	num_init_n(matrix,size);

	if(jarr != NULL){
          jint len = (*env)->GetArrayLength(env, jarr);
	  assert(len==size);
	  for(i=0; i<size; i++){
	     jobject jArrayElem = (*env)->  GetObjectArrayElement(env, jarr, i);
	     assert(jArrayElem!=NULL);
	     jclass clsArr = (*env)->GetObjectClass(env, jArrayElem);
	     jfieldID fidArr = (*env)->GetFieldID(env, clsArr, "f","D");
	     double elem = (*env)->GetDoubleField(env, jArrayElem, fidArr);
	     num_set_float (matrix+i, elem);
	     (*env)->DeleteLocalRef(env, jArrayElem);
	  }
	}

	oct = oct_universe(dim);
	/* Set octagon's fields */
	oct->c = matrix;
	var_t v = dim;
	oct->n = v;
	oct->ref = ref;
	if(state == 0) {oct->state = OCT_EMPTY;}
	else if(state == 1) {oct->state = OCT_NORMAL;}
	else if(state == 2) {oct->state = OCT_CLOSED;}
	oct->closed = NULL;
	//printf("\nConvert TO C \n");
	//printContent(oct);
	return oct;
}

num_t* convertToCArray(JNIEnv *env, jobject obj, jobject jarray){

	int size, i;

	jint len = (*env)->GetArrayLength(env, jarray);
	size = len;

	num_t* matrix = new_n(num_t,size);
	num_init_n(matrix,size);
          
	for(i=0; i<size; i++){
	   jobject jArrayElem = (*env)->  GetObjectArrayElement(env, jarray, i);
	   assert(jArrayElem!=NULL);
	   jclass clsArr = (*env)->GetObjectClass(env, jArrayElem);
	   jfieldID fidArr = (*env)->GetFieldID(env, clsArr, "f","D");
	   double elem = (*env)->GetDoubleField(env, jArrayElem, fidArr);
	   num_set_float (matrix+i, elem);
	   (*env)->DeleteLocalRef(env, jArrayElem);
	}
	
	return matrix;
}

void destructNumT(num_t* array, JNIEnv *env, jobject jarray){
      int size;
      jint len = (*env)->GetArrayLength(env, jarray);
      size = len;
      num_clear_n(array,size);
      oct_mm_free(array);
}

void freeOctC(oct_t* oct){
      num_t* matrix = oct->c;
      int dim = oct->n;
      num_clear_n(matrix, dim);
      oct_mm_free(matrix);
}


long mylrand()
{
  seed = (0xfdeece66dULL * seed + 0xbULL) & 0x0000ffffffffffffULL;
  return (long)((seed>>5) & 0x7fffffffL);
}

double mydrand()
{
  return (double)(mylrand()%1000000UL) / 1000000.;
}

oct_t*
random_oct(int n, int m)
{
  oct_t* ar;
  int i;
  ar = oct_universe(n);
  for (i=0;i<m;i++) {
    int x = mylrand()%(ar->n*2);
    int y = mylrand()%(ar->n*2);
    double d = mydrand();
    if (x!=y) {
	num_set_float(oct_elem(ar,x,y), d);
	//printf("%d -> %f  \n",  matpos2(x,y), d);
    }
  }
  ar->state = OCT_NORMAL;
  return ar;
}

void printContent (oct_t* oct){
	
   int i;
   int size = matsize(oct->n);
   int size2;
   num_t* numarray = oct->c;
   if(numarray != NULL){
      for(i=0; i<size; i++){
        double d = num_get_float(&numarray[i]);
        printf("%lf ",  d);
      }
   }
}

