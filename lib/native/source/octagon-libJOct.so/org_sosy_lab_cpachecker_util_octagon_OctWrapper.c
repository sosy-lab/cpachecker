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
	return oct_init();
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
	num_t* num1 = n1;
	num_t* num2 = n2;
	num_set(num1, num2);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_set_int
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1set_1int
(JNIEnv *env, jobject obj, jlong n, jint pos, jint i){
	num_t* num = n;
	num_set_int(num + pos, i);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_set_float
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1set_1float
(JNIEnv *env, jobject obj, jlong n, jint pos, jdouble d){
	num_t* num = n;
	num_set_float(num + pos, d);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_set_inf
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1set_1inf
(JNIEnv *env, jobject obj, jlong n, jint pos){
	num_t* num = n;
	num_set_infty(num + pos);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_get_int
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1get_1int
(JNIEnv *env, jobject obj, jlong n, jint pos){
	num_t* num = n;
	return num_get_int(num + pos);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_get_float
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1get_1float
(JNIEnv *env, jobject obj, jlong n, jint pos){
	num_t* num = n;
	return num_get_float(num + pos);
}

/*
 * Class:     org_sosy_lab_cpachecker_util_octagon_OctWrapper
 * Method:    J_num_infty
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1infty
(JNIEnv *env, jobject obj, jlong n, jint pos){
	num_t* num = n;
	return num_infty(num + pos);
}

JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1num_1clear_1n
(JNIEnv *env, jobject obj, jlong n, jint size){
	num_t *num = n;
	num_clear_n(num, size);
	oct_mm_free(num);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1empty
(JNIEnv *env, jobject obj, jint in){
	return  oct_empty (in);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1universe
(JNIEnv *env, jobject obj, jint in){
	return  oct_universe (in);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1copy
(JNIEnv *env, jobject obj, jlong oct1){
	return oct_copy(oct1);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1full_1copy
(JNIEnv *env, jobject obj, jlong oct1){
	return oct_full_copy(oct1);
}
JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1free
(JNIEnv *env, jobject obj, jlong oct1){
	oct_free(oct1);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1dimension
(JNIEnv *env, jobject obj, jlong oct1){
	return oct_dimension (oct1);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1nbconstraints
(JNIEnv *env, jobject obj, jlong oct1){
	return oct_nbconstraints (oct1);
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isEmpty
(JNIEnv *env, jobject obj, jlong oct){
	return oct_is_empty(oct);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isEmptyLazy
(JNIEnv *env, jobject obj, jlong oct1){
	tbool tb = oct_is_empty_lazy (oct1);
	//    //freeOctC(oct);
	//    oct_free(oct);
	if (tb == tbool_true) return 1;
	else if (tb == tbool_false) return 2;
	else if (tb == tbool_top) return 3;
	else if (tb == tbool_bottom) return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isUniverse
(JNIEnv *env, jobject obj, jlong oct1){
	return oct_is_universe(oct1);
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isIncludedIn
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2){
	return oct_is_included_in(oct1, oct2);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isIncludedInLazy
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2){
	tbool tb = oct_is_included_in_lazy(oct1, oct2);
	if (tb == tbool_true) return 1;
	else if (tb == tbool_false) return 2;
	else if (tb == tbool_top) return 3;
	else if (tb == tbool_bottom) return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isEqual
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2){
	return oct_is_equal(oct1, oct2);
}

JNIEXPORT jint JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isEqualLazy
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2){
	tbool tb = oct_is_equal_lazy(oct1, oct2);
	if (tb == tbool_true) return 1;
	else if (tb == tbool_false) return 2;
	else if (tb == tbool_top) return 3;
	else if (tb == tbool_bottom) return 0;
}

JNIEXPORT jboolean JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1isIn
(JNIEnv *env, jobject obj, jlong oct1, jlong arr){
	num_t* v = arr;
	return oct_is_in(oct1, v);
}

/*
 * Class:     OctWrapper
 * Method:    J_intersection
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1intersection
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2, jboolean b){
	return oct_intersection(oct1, oct2, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_union
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1union
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2, jboolean b){
	return  oct_convex_hull(oct1, oct2, b);
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
	return oct_widening(oct1, oct2, b, type);
}

/*
 * Class:     OctWrapper
 * Method:    J_narrowing
 * Signature: (LOctagon;LOctagon;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1narrowing
(JNIEnv *env, jobject obj, jlong oct1, jlong oct2, jboolean b){
	return oct_narrowing(oct1, oct2, b);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1forget
(JNIEnv *env, jobject obj, jlong oct1, jint in, jboolean b){
	return oct_forget(oct1, in, b);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1assingVar
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* tab = arr;
	return  oct_assign_variable (oct1, in, tab, b);
}

JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1addBinConstraints
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* numarray = arr;
	oct_cons oc;
	oc.type = numarray[0];
	oc.x = numarray[1];
	oc.y = numarray[2];
	oc.c = numarray[3];
	return oct_add_bin_constraints(oct1, in, &oc, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_substituteVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1substituteVar
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* tab = arr;
	return oct_substitute_variable (oct1, in, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_addConstraint
 * Signature: (LOctagon;[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1addConstraint
(JNIEnv *env, jobject obj, jlong oct1, jlong arr, jboolean b){
	num_t* tab = arr;
	return oct_add_constraint (oct1, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_intervAssingVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1intervAssingVar
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* tab = arr;
	return oct_interv_assign_variable (oct1, in, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_intervSubstituteVar
 * Signature: (LOctagon;I[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1intervSubstituteVar
(JNIEnv *env, jobject obj, jlong oct1, jint in, jlong arr, jboolean b){
	num_t* tab = arr;
	return oct_interv_substitute_variable (oct1, in, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_intervAddConstraint
 * Signature: (LOctagon;[LNum;Z)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1intervAddConstraint
(JNIEnv *env, jobject obj, jlong oct1, jlong arr, jboolean b){
	num_t* tab = arr;
	return oct_interv_add_constraint (oct1, tab, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_addDimenensionAndEmbed
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1addDimenensionAndEmbed
(JNIEnv *env , jobject obj, jlong oct1, jint i, jboolean b){
	return oct_add_dimensions_and_embed (oct1, i, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_addDimenensionAndProject
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1addDimenensionAndProject
(JNIEnv *env , jobject obj, jlong oct1, jint i, jboolean b){
	return oct_add_dimensions_and_project (oct1, i, b);
}

/*
 * Class:     OctWrapper
 * Method:    J_removeDimension
 * Signature: (LOctagon;IZ)LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1removeDimension
(JNIEnv *env, jobject obj, jlong oct1, jint in, jboolean b){
	return oct_remove_dimensions (oct1, in, b);
}

JNIEXPORT void JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_J_1print
(JNIEnv *env, jobject obj, jlong  oct1){
	oct_print (oct1);
}

/*
 * Class:     OctWrapper
 * Method:    getRandomOct
 * Signature: ()LOctagon;
 */
JNIEXPORT jlong JNICALL Java_org_sosy_1lab_cpachecker_util_octagon_OctWrapper_getRandomOct
(JNIEnv *env, jobject obj){
	return random_oct(3, 10);
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

//JNIEXPORT jstring JNICALL Java_J_toString(JNIEnv* env, jobject obj, jlong  oct1)
//{
//	oct_t* m = oct1;
//	var_t i, j;
//	num_t w;
//	num_init(&w);
//	char *buf = (char*) malloc(2000);
//	if (m->state==OCT_EMPTY) {
//		strcat(buf, "[ empty ]\n");
//		return;
//	}
//	printf("[");
//	if (m->state==OCT_CLOSED) printf(" (closed)");
//	for (i=0;i<m->n;i++) {
//		if (num_cmp_zero(m->c+matpos(2*i,2*i))) {
//			strcat(buf, "\n   v%02i-v%02i <= ",i,i);
//			strcat(buf, "%d", m->c+matpos(2*i,2*i));
//		}
//		if (num_cmp_zero(m->c+matpos(2*i+1,2*i+1))) {
//			printf("\n  -v%02i+v%02i <= ",i,i);
//			num_print(m->c+matpos(2*i+1,2*i+1));
//		}
//		if (!num_infty(m->c+matpos(2*i+1,2*i))) {
//			printf("\n   v%02i     <= ",i);
//			num_div_by_2(&w,m->c+matpos(2*i+1,2*i));
//			num_print(&w);
//		}
//		if (!num_infty(m->c+matpos(2*i,2*i+1))) {
//			printf("\n  -v%02i     <= ",i);
//			num_div_by_2(&w,m->c+matpos(2*i,2*i+1));
//			num_print(&w);
//		}
//	}
//
//	for (i=0;i<m->n;i++)
//		for (j=i+1;j<m->n;j++) {
//			if (!num_infty(m->c+matpos(2*j,2*i))) {
//				printf("\n   v%02i-v%02i <= ",i,j);
//				num_print(m->c+matpos(2*j,2*i));
//			}
//			if (!num_infty(m->c+matpos(2*j,2*i+1))) {
//				printf("\n  -v%02i-v%02i <= ",i,j);
//				num_print(m->c+matpos(2*j,2*i+1));
//			}
//			if (!num_infty(m->c+matpos(2*j+1,2*i))) {
//				printf("\n   v%02i+v%02i <= ",i,j);
//				num_print(m->c+matpos(2*j+1,2*i));
//			}
//			if (!num_infty(m->c+matpos(2*j+1,2*i+1)))	{
//				printf("\n   v%02i-v%02i <= ",j,i);
//				num_print(m->c+matpos(2*j+1,2*i+1));
//			}
//
//		}
//	printf("  ]\n");
//	num_clear(&w);
//	OCT_EXIT("oct_print",37);
//}

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
