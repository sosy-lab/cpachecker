#include <stdio.h>
#include <math.h>
#include <oct.h>
#include <assert.h>
#include <oct_private.h>
#include "org_sosy_lab_cpachecker_util_octagon_OctWrapper.h"

long mylrand();
double mydrand();
oct_t* random_oct(int n, int m);

//var for random number generator
unsigned long long seed;

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1init
(JNIEnv *env, jobject obj){
	return (jboolean) oct_init();
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_init_n
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1init_1n
(JNIEnv *env, jobject obj, jint i){
	num_t* mm = new_n(num_t, i);
	num_init_n(mm, i);
	return (jlong) mm;
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_set
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1set
(JNIEnv *env, jobject obj, jlong n1, jlong n2){
	num_t* num1 = (num_t*) n1;
	num_t* num2 = (num_t*) n2;
	num_set(num1, num2);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_set_int
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1set_1int
(JNIEnv *env, jobject obj, jlong n, jint pos, jint i){
	num_t* num = (num_t*) n;
	num_set_int(num + pos, i);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_set_float
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1set_1float
(JNIEnv *env, jobject obj, jlong n, jint pos, jdouble d){
	num_t* num = (num_t*) n;
	num_set_float(num + pos, d);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_set_inf
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1set_1inf
(JNIEnv *env, jobject obj, jlong n, jint pos){
	num_t* num = (num_t*) n;
	num_set_infty(num + pos);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_get_int
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1get_1int
(JNIEnv *env, jobject obj, jlong n, jint pos){
	num_t* num = (num_t*) n;
	return (jlong) num_get_int(num + pos);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_get_float
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1get_1float
(JNIEnv *env, jobject obj, jlong n, jint pos){
	num_t* num = (num_t*) n;
	return (jdouble) num_get_float(num + pos);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_infty
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1infty
(JNIEnv *env, jobject obj, jlong n, jint pos){
	num_t* num = (num_t*) n;
	return (jboolean) num_infty(num + pos);
}

JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1clear_1n
(JNIEnv *env, jobject obj, jlong n, jint size){
	num_t *num = (num_t*) n;
	num_clear_n(num, size);
	oct_mm_free(num);
}

JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1get_1bounds
(JNIEnv *env, jobject obj, jlong octl, jint pos, jlong upper, jlong lower){
    oct_t *oct = (oct_t*) octl;
    oct_get_bounds(oct, pos, (num_t *) upper, (num_t *) lower);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1set_1bounds
(JNIEnv *env, jobject obj, jlong octl, jint pos, jlong upper, jlong lower, jboolean dest){
    oct_t *oct = (oct_t*) octl;
    return (jlong) oct_set_bounds(oct, pos, (const num_t *) upper, (const num_t *) lower, dest);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1empty
(JNIEnv *env, jobject obj, jint in){
	return  (jlong) oct_empty ((var_t)in);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1universe
(JNIEnv *env, jobject obj, jint in){
	return  (jlong) oct_universe ((var_t)in);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1copy
(JNIEnv *env, jobject obj, jlong oct1){
	return (jlong) oct_copy((oct_t *)oct1);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1full_1copy
(JNIEnv *env, jobject obj, jlong oct1){
	return (jlong) oct_full_copy((oct_t *)oct1);
}
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1free
(JNIEnv *env, jobject obj, jlong oct1){
	oct_free((oct_t *)oct1);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1dimension
(JNIEnv *env, jobject obj, jlong oct1){
	return  (jint) oct_dimension ((oct_t *)oct1);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1nbconstraints
(JNIEnv *env, jobject obj, jlong oct1){
	return (jint) oct_nbconstraints ((oct_t *)oct1);
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isEmpty
(JNIEnv *env, jobject obj, jlong oct){
	return (jboolean) oct_is_empty((oct_t *)oct);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isEmptyLazy
(JNIEnv *env, jobject obj, jlong oct1){
	tbool tb = oct_is_empty_lazy ((oct_t *)oct1);
	//    //freeOctC(oct);
	//    oct_free(oct);
	if (tb == tbool_true) return 1;
	else if (tb == tbool_false) return 2;
	else if (tb == tbool_top) return 3;
	else if (tb == tbool_bottom) return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isUniverse
(JNIEnv *env, jobject obj, jlong oct1){
	return (jboolean) oct_is_universe((oct_t *)oct1);
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isIncludedIn
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2){
	return (jboolean) oct_is_included_in((oct_t *)oct1, (oct_t *)oct2);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isIncludedInLazy
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2){
	tbool tb = oct_is_included_in_lazy((oct_t *)oct1, (oct_t *)oct2);
	if (tb == tbool_true) return 1;
	else if (tb == tbool_false) return 2;
	else if (tb == tbool_top) return 3;
	else if (tb == tbool_bottom) return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isEqual
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2){
	return (jboolean) oct_is_equal((oct_t *)oct1, (oct_t *)oct2);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isEqualLazy
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2){
	tbool tb = oct_is_equal_lazy((oct_t *)oct1, (oct_t *)oct2);
	if (tb == tbool_true) return 1;
	else if (tb == tbool_false) return 2;
	else if (tb == tbool_top) return 3;
	else if (tb == tbool_bottom) return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isIn
(JNIEnv *env, jobject obj, jlong oct1, jlong arr){
	num_t* v = (num_t *)arr;
	return (jboolean) oct_is_in((oct_t *)oct1, v);
}

/*
 * Class:     OctWrapper
 * Method:    J_intersection
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1intersection
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2, jboolean b){
	return (jlong) oct_intersection((oct_t *)oct1, (oct_t *)oct2, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_union
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1union
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2, jboolean b){
	return  (jlong) oct_convex_hull((oct_t *)oct1, (oct_t *)oct2, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_widening
 * Signature: (LOctagon;LOctagon;ZI)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1widening
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2, jboolean b, jint in){
	oct_widening_type type;
	if(in == 0) {type =  OCT_WIDENING_FAST;}
	else if(in == 1) {type =  OCT_WIDENING_ZERO;}
	else if(in == 2) {type =  OCT_WIDENING_UNIT;}
	return (jlong) oct_widening((oct_t *)oct1, (oct_t *)oct2, b, type);
}

/*
 * Class:     OctWrapper
 * Method:    J_narrowing
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1narrowing
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2, jboolean b){
	return (jlong) oct_narrowing((oct_t *)oct1, (oct_t *)oct2, b);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1forget
(JNIEnv *env, jobject obj, jlong oct1, jint in, jboolean b){
	return (jlong) oct_forget((oct_t *)oct1, in, b);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1assingVar
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* tab = (num_t *) arr;
	return  (jlong) oct_assign_variable ((oct_t *)oct1, in, tab, b);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1addBinConstraints
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* numarray = (num_t *)arr;
	oct_cons oc;
	oc.type = numarray[0];
	oc.x = numarray[1];
	oc.y = numarray[2];
	oc.c = numarray[3];
	return (jlong) oct_add_bin_constraints((oct_t *)oct1, in, &oc, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_substituteVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1substituteVar
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* tab = (num_t *) arr;
	return (jlong) oct_substitute_variable ((oct_t *)oct1, in, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_addConstraint
 * Signature: (LOctagon;[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1addConstraint
(JNIEnv *env, jobject obj, jlong oct1, jlong arr, jboolean b){
	num_t* tab = (num_t *)arr;
	return (jlong) oct_add_constraint ((oct_t *)oct1, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_intervAssingVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1intervAssingVar
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* tab = (num_t *) arr;
	return (jlong) oct_interv_assign_variable ((oct_t *)oct1, in, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_intervSubstituteVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1intervSubstituteVar
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* tab = (num_t *)arr;
	return (jlong) oct_interv_substitute_variable ((oct_t *)oct1, in, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_intervAddConstraint
 * Signature: (LOctagon;[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1intervAddConstraint
(JNIEnv *env, jobject obj, jlong oct1, jlong arr, jboolean b){
	num_t* tab = (num_t *) arr;
	return (jlong) oct_interv_add_constraint ((oct_t *)oct1, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_addDimenensionAndEmbed
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1addDimenensionAndEmbed
(JNIEnv *env , jobject obj, jlong oct1, jint i, jboolean b){
	return (jlong) oct_add_dimensions_and_embed ((oct_t *)oct1, i, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_addDimenensionAndProject
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1addDimenensionAndProject
(JNIEnv *env , jobject obj, jlong oct1, jint i, jboolean b){
	return (jlong) oct_add_dimensions_and_project ((oct_t *)oct1, i, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_removeDimension
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1removeDimension
(JNIEnv *env, jobject obj, jlong oct1, jint in, jboolean b){
	return (jlong) oct_remove_dimensions ((oct_t *)oct1, in, b);
}

JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1print
(JNIEnv *env, jobject obj, jlong  oct1){
	oct_print ((oct_t *)oct1);
}

/*
 * Class:     OctWrapper
 * Method:    getRandomOct
 * Signature: ()LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_getRandomOct
(JNIEnv *env, jobject obj){
	return (jlong) random_oct(3, 10);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    getValueFor
 * Signature: (JJJ)J
 */
JNIEXPORT jdouble JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1getValueFor
  (JNIEnv *env, jobject obj, jlong oct, jlong valI, jlong valJ) {
       return (jdouble) num_get_float(oct_elem((oct_t *)oct, (var_t)valI, (var_t)valJ));
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    printNum
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1printNum
  (JNIEnv *env, jobject obj, jlong num, jint size) {
       int i = 0;
       for (i; i < size; i++) {
          num_print(((num_t *) num) + i);
       }
       printf("\n");
}

long mylrand() {
	seed = (0xfdeece66dULL * seed + 0xbULL) & 0x0000ffffffffffffULL;
	return (long)((seed>>5) & 0x7fffffffL);
}

double mydrand() {
	return (double)(mylrand()%1000000UL) / 1000000.;
}

oct_t* random_oct(int n, int m) {
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
