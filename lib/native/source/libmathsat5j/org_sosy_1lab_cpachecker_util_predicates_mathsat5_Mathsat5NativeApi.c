#include "includes/defines.h"

/*
 * Copied from the Sun JNI Programmer's Guide and Specification
 */
void throwException(JNIEnv *env, const char *name, const char *msg) {
	jclass cls = (*env)->FindClass(env, name);
	if (cls != NULL) {
		(*env)->ThrowNew(env, cls, msg);
	}
}


// Now really define the functions.

struct msat_callback_info {
	JNIEnv *jenv;
	jmethodID callback_method;
	jobject obj;
};

static int call_java_callback(msat_term *model, int size, void *user_data) {
	int retval = 0; // 0 means "terminate search"
	struct msat_callback_info *helper =
			(struct msat_callback_info *) user_data;
	JNIEnv *jenv = helper->jenv;

	jlongArray jmodel = (*jenv)->NewLongArray(jenv, (size_t) size);
	if (jmodel == NULL) {
		goto out_jmodel;
	}
	jlong *jarr = malloc(sizeof(jlong) * size);
	if (jarr == NULL) {
		throwException(jenv, "java/lang/OutOfMemoryError",
				"Cannot allocate memory for allsat result");
		goto out_jarr;
	}

	int i;

	for (i = 0; i < size; ++i) {
		jarr[i] = (jlong)((size_t) model[i].repr);
	}
	(*jenv)->SetLongArrayRegion(jenv, jmodel, 0, (size_t) size, (jlong *) jarr);

	(*jenv)->CallVoidMethod(jenv, helper->obj, helper->callback_method, jmodel);

	if (!(*jenv)->ExceptionCheck(jenv)) {
		retval = 1; // everything successful, no exceptions: continue search
	}

	out_jarr: free(jarr);

	out_jmodel:
	// explicitly delete local reference because this is a long running computation
	(*jenv)->DeleteLocalRef(jenv, jmodel);
	return retval;
}

static int call_java_termination_test(void *user_data) {
	struct msat_callback_info *helper = (struct msat_callback_info *) user_data;
	JNIEnv *jenv = helper->jenv;

	if (helper->obj == NULL) {
		throwException(jenv, "java/lang/IllegalArgumentException", "Illegal termination test object");
		return 1;
	}

	jboolean result = (*jenv)->CallBooleanMethod(jenv, helper->obj, helper->callback_method);

	if ((*jenv)->ExceptionCheck(jenv)) {
		return 1;
	}

	return result;
}

/*
 * msat_config msat_create_config(void)
 */
DEFINE_FUNC(jconf, 1create_1config) WITHOUT_ARGS
CALL0(msat_config, create_config)
CONF_RETURN

/*
 * void msat_destroy_config(msat_config cfg);
 */
DEFINE_FUNC(void, 1destroy_1config) WITH_ONE_ARG(jconf)
CONF_ARG(1)
VOID_CALL1(destroy_config)

/*
 * msat_env msat_create_env(msat_config cfg);
 */
DEFINE_FUNC(jenv, 1create_1env) WITH_ONE_ARG(jconf)
CONF_ARG(1)
CALL1(msat_env, create_env)
ENV_RETURN

/*
 * msat_env msat_create_shared_env(msat_config cfg, msat_env sibling);
 */
DEFINE_FUNC(jenv, 1create_1shared_1env) WITH_TWO_ARGS(jconf, jenv)
CONF_ARG(1)
ENV_ARG(2)
CALL2(msat_env, create_shared_env)
ENV_RETURN

/*
 * void msat_destroy_env(msat_env e);
 */
DEFINE_FUNC(void, 1destroy_1env) WITH_ONE_ARG(jenv)
ENV_ARG(1)
VOID_CALL1(destroy_env)

/*
 * int msat_set_option(msat_config cfg, const char *option, const char *value);
 */
DEFINE_FUNC(int, 1set_1option) WITH_THREE_ARGS(jconf, string, string)
CONF_ARG(1)
STRING_ARG(2)
STRING_ARG(3)
CALL3(int, set_option)
FREE_STRING_ARG(3)
FREE_STRING_ARG(2)
INT_RETURN

/*
 * msat_type msat_get_bool_type(msat_env env);
 */
get_msat_type(bool)

/*
 * msat_type msat_get_rational_type(msat_env env);
 */
get_msat_type(rational)

/*
 * msat_type msat_get_integer_type(msat_env env);
 */
get_msat_type(integer)

/*
 * msat_type msat_get_bv_type(msat_env env, size_t width);
 */

DEFINE_FUNC(jtype, 1get_1bv_1type) WITH_TWO_ARGS(jenv, int)
ENV_ARG(1)
SIMPLE_ARG(int, 2)
CALL2(msat_type, get_bv_type)
TYPE_RETURN

/*
 * msat_type msat_get_simple_type(msat_env env, const char *name);
 */
DEFINE_FUNC(jtype, 1get_1simple_1type) WITH_TWO_ARGS(jenv, string)
ENV_ARG(1)
STRING_ARG(2)
CALL2(msat_type, get_simple_type)
FREE_STRING_ARG(2)
TYPE_RETURN

/*
 * msat_type msat_get_array_type(msat_env env, msat_type itp, msat_type etp);
 */
DEFINE_FUNC(jtype, 1get_1array_1type) WITH_THREE_ARGS(jenv, jtype, jtype)
ENV_ARG(1)
TYPE_ARG(2)
TYPE_ARG(3)
CALL3(msat_type, get_array_type)
TYPE_RETURN

/*
 * msat_type msat_get_fp_type(msat_env env, size_t exp_with, size_t mant_with);
 */
DEFINE_FUNC(jtype, 1get_1fp_1type) WITH_THREE_ARGS(jenv, int, int)
ENV_ARG(1)
SIMPLE_ARG(size_t, 2)
SIMPLE_ARG(size_t, 3)
CALL3(msat_type, get_fp_type)
TYPE_RETURN

/*
 * msat_type msat_get_fp_roundingmode_type(msat_env env);
 */
DEFINE_FUNC(jtype, 1get_1fp_1roundingmode_1type) WITH_ONE_ARG(jenv)
ENV_ARG(1)
CALL1(msat_type, get_fp_roundingmode_type)
TYPE_RETURN

/*
 * msat_type msat_get_function_type(msat_env env, msat_type *param_types,
 *                               size_t num_params, msat_type return_type);
 */
DEFINE_FUNC(jtype, 1get_1function_1type) WITH_FOUR_ARGS(jenv, jtypeArray, int, jtype)
ENV_ARG(1)
TYPE_ARRAY_ARG(2)
SIMPLE_ARG(int, 3)
TYPE_ARG(4)
CALL4(msat_type, get_function_type)
FREE_TYPE_ARRAY_ARG(2);
TYPE_RETURN

/*
 * int msat_is_bool_type(msat_env env, msat_type tp);
 */
DEFINE_FUNC(jboolean, 1is_1bool_1type) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
CALL2(int, is_bool_type)
BOOLEAN_RETURN

/*
 * int msat_is_rational_type(msat_env env, msat_type tp);
 */
DEFINE_FUNC(jboolean, 1is_1rational_1type) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
CALL2(int, is_rational_type)
BOOLEAN_RETURN

/*
 * int msat_is_integer_type(msat_env env, msat_type tp);
 */
DEFINE_FUNC(jboolean, 1is_1integer_1type) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
CALL2(int, is_integer_type)
BOOLEAN_RETURN

DEFINE_FUNC(jboolean, 1is_1bv_1type) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
NULL_ARG(size_t, 3);
CALL3(int, is_bv_type)
BOOLEAN_RETURN

DEFINE_FUNC(int, 1get_1bv_1type_1size) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
size_t r_arg3;
size_t *m_arg3 = &r_arg3;
CALL3(int, is_bv_type)
  if (retval != 1) { \
    throwException(jenv, "java/lang/IllegalArgumentException", "Cannot get size of non-bv term"); \
    return -1;
  } \
  return (jint)r_arg3; \
}


DEFINE_FUNC(jboolean, 1is_1array_1type) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
NULL_ARG(msat_type, 3)
NULL_ARG(msat_type, 4)
CALL4(int, is_array_type)
BOOLEAN_RETURN

DEFINE_FUNC(jboolean, 1is_1fp_1type) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
NULL_ARG(size_t, 3)
NULL_ARG(size_t, 4)
CALL4(int, is_fp_type)
BOOLEAN_RETURN

DEFINE_FUNC(int, 1get_1fp_1type_1exp_1width) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
size_t r_arg3;
size_t *m_arg3 = &r_arg3;
NULL_ARG(size_t, 4)
CALL4(int, is_fp_type)
  if (retval != 1) { \
    throwException(jenv, "java/lang/IllegalArgumentException", "Cannot get exponent width of non-fp term"); \
    return -1;
  } \
  return (jint)r_arg3; \
}

DEFINE_FUNC(int, 1get_1fp_1type_1mant_1width) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
NULL_ARG(size_t, 3)
size_t r_arg4;
size_t *m_arg4 = &r_arg4;
CALL4(int, is_fp_type)
  if (retval != 1) { \
    throwException(jenv, "java/lang/IllegalArgumentException", "Cannot get mantissa width of non-fp term"); \
    return -1;
  } \
  return (jint)r_arg4; \
}

DEFINE_FUNC(jboolean, 1is_1fp_1roundingmode_1type) WITH_TWO_ARGS(jenv, jtype)
ENV_ARG(1)
TYPE_ARG(2)
CALL2(int, is_fp_roundingmode_type)
BOOLEAN_RETURN

DEFINE_FUNC(jboolean, 1type_1equals) WITH_TWO_ARGS(jtype, jtype)
TYPE_ARG(1)
TYPE_ARG(2)
CALL2(int, type_equals)
BOOLEAN_RETURN

DEFINE_FUNC(string, 1type_1repr) WITH_ONE_ARG(jtype)
TYPE_ARG(1)
CALL1(char *, type_repr)
PLAIN_STRING_RETURN

/*
 * msat_decl msat_declare_function(msat_env e, const char *name, msat_type type);
 */
DEFINE_FUNC(jdecl, 1declare_1function) WITH_THREE_ARGS(jenv, string, jtype)
ENV_ARG(1)
STRING_ARG(2)
TYPE_ARG(3)
CALL3(msat_decl, declare_function)
FREE_STRING_ARG(2)
DECL_RETURN


make_term_constant(true, 1true)
make_term_constant(false, 1false)

DEFINE_FUNC(jterm, 1make_1not) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(msat_term, make_not)
STRUCT_RETURN_WITH_ENV

make_term_binary(iff)
make_term_binary(or)
make_term_binary(and)
make_term_binary(equal)
make_term_binary(leq)
make_term_binary(plus)
make_term_binary(times)

DEFINE_FUNC(jterm, 1make_1int_1modular_1congruence) WITH_FOUR_ARGS(jenv, long, jterm, jterm)
ENV_ARG(1)
MPZ_ARG(2)
TERM_ARG(3)
TERM_ARG(4)
CALL4(msat_term, make_int_modular_congruence)
FREE_MPZ_ARG(2)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1floor) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(msat_term, make_floor)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1number) WITH_TWO_ARGS(jenv, string)
ENV_ARG(1)
STRING_ARG(2)
CALL2(msat_term, make_number)
FREE_STRING_ARG(2)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1term_1ite) WITH_FOUR_ARGS(jenv, jterm, jterm, jterm)
ENV_ARG(1)
TERM_ARG(2)
TERM_ARG(3)
TERM_ARG(4)
CALL4(msat_term, make_term_ite)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1constant) WITH_TWO_ARGS(jenv, jdecl)
ENV_ARG(1)
DECL_ARG(2)
CALL2(msat_term, make_constant)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1uf) WITH_THREE_ARGS(jenv, jdecl, jtermArray)
ENV_ARG(1)
DECL_ARG(2)
TERM_ARRAY_ARG(3)
CALL3(msat_term, make_uf)
FREE_TERM_ARRAY_ARG(3)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1array_1read) WITH_THREE_ARGS(jenv, jterm, jterm)
ENV_ARG(1)
TERM_ARG(2)
TERM_ARG(3)
CALL3(msat_term, make_array_read)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1array_1write) WITH_FOUR_ARGS(jenv, jterm, jterm,jterm)
ENV_ARG(1)
TERM_ARG(2)
TERM_ARG(3)
TERM_ARG(4)
CALL4(msat_term, make_array_write)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1array_1const) WITH_THREE_ARGS(jenv, jtype, jterm)
ENV_ARG(1)
TYPE_ARG(2)
TERM_ARG(3)
CALL3(msat_term, make_array_const)
TERM_RETURN


DEFINE_FUNC(jterm, 1make_1int_1to_1bv) WITH_THREE_ARGS(jenv, int, jterm)
ENV_ARG(1)
SIMPLE_ARG(size_t, 2)
TERM_ARG(3)
CALL3(msat_term, make_int_to_bv)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1int_1from_1ubv) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(msat_term, make_int_from_ubv)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1int_1from_1sbv) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(msat_term, make_int_from_sbv)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1bv_1number) WITH_FOUR_ARGS(jenv, string, int, int)
ENV_ARG(1)
STRING_ARG(2)
SIMPLE_ARG(size_t, 3)
SIMPLE_ARG(size_t, 4)
CALL4(msat_term, make_bv_number)
FREE_STRING_ARG(2)
TERM_RETURN

#define make_term_bv_binary(name) \
  DEFINE_FUNC(jterm, 1make_1bv_1##name) WITH_THREE_ARGS(jenv, jterm, jterm) \
  ENV_ARG(1) \
  TERM_ARG(2) \
  TERM_ARG(3) \
  CALL3(msat_term, make_bv_##name) \
  TERM_RETURN

make_term_bv_binary(concat)

DEFINE_FUNC(jterm, 1make_1bv_1extract) WITH_FOUR_ARGS(jenv, int, int, jterm)
ENV_ARG(1)
SIMPLE_ARG(size_t, 2)
SIMPLE_ARG(size_t, 3)
TERM_ARG(4)
CALL4(msat_term, make_bv_extract)
TERM_RETURN

make_term_bv_binary(or)
make_term_bv_binary(xor)
make_term_bv_binary(and)

DEFINE_FUNC(jterm, 1make_1bv_1not) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(msat_term, make_bv_not)
TERM_RETURN

make_term_bv_binary(lshl)
make_term_bv_binary(lshr)
make_term_bv_binary(ashr)
make_term_bv_binary(plus)
make_term_bv_binary(minus)

DEFINE_FUNC(jterm, 1make_1bv_1neg) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(msat_term, make_bv_neg)
TERM_RETURN

make_term_bv_binary(times)
make_term_bv_binary(udiv)
make_term_bv_binary(urem)
make_term_bv_binary(sdiv)
make_term_bv_binary(srem)
make_term_bv_binary(ult)
make_term_bv_binary(uleq)
make_term_bv_binary(slt)
make_term_bv_binary(sleq)

DEFINE_FUNC(jterm, 1make_1bv_1rol) WITH_THREE_ARGS(jenv, int, jterm)
ENV_ARG(1)
SIMPLE_ARG(int, 2)
TERM_ARG(3)
CALL3(msat_term, make_bv_rol)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1bv_1ror) WITH_THREE_ARGS(jenv, int, jterm)
ENV_ARG(1)
SIMPLE_ARG(int, 2)
TERM_ARG(3)
CALL3(msat_term, make_bv_ror)
TERM_RETURN

make_term_bv_binary(comp)

DEFINE_FUNC(jterm, 1make_1bv_1sext) WITH_THREE_ARGS(jenv, int, jterm)
ENV_ARG(1)
SIMPLE_ARG(int, 2)
TERM_ARG(3)
CALL3(msat_term, make_bv_sext)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1bv_1zext) WITH_THREE_ARGS(jenv, int, jterm)
ENV_ARG(1)
SIMPLE_ARG(int, 2)
TERM_ARG(3)
CALL3(msat_term, make_bv_zext)
TERM_RETURN


#define make_term_fp_unary(name) \
  DEFINE_FUNC(jterm, 1make_1fp_1##name) WITH_TWO_ARGS(jenv, jterm) \
  ENV_ARG(1) \
  TERM_ARG(2) \
  CALL2(msat_term, make_fp_##name) \
  TERM_RETURN

#define make_term_fp_rounding_unary(name) \
  DEFINE_FUNC(jterm, 1make_1fp_1##name) WITH_THREE_ARGS(jenv, jterm, jterm) \
  ENV_ARG(1) \
  TERM_ARG(2) \
  TERM_ARG(3) \
  CALL3(msat_term, make_fp_##name) \
  TERM_RETURN

#define make_term_fp_binary(name) \
  DEFINE_FUNC(jterm, 1make_1fp_1##name) WITH_THREE_ARGS(jenv, jterm, jterm) \
  ENV_ARG(1) \
  TERM_ARG(2) \
  TERM_ARG(3) \
  CALL3(msat_term, make_fp_##name) \
  TERM_RETURN

#define make_term_fp_rounding_binary(name) \
  DEFINE_FUNC(jterm, 1make_1fp_1##name) WITH_FOUR_ARGS(jenv, jterm, jterm, jterm) \
  ENV_ARG(1) \
  TERM_ARG(2) \
  TERM_ARG(3) \
  TERM_ARG(4) \
  CALL4(msat_term, make_fp_##name) \
  TERM_RETURN

#define make_term_fp_rounding_cast(name, name_escaped) \
  DEFINE_FUNC(jterm, 1make_1fp_##name_escaped) WITH_FIVE_ARGS(jenv, int, int, jterm, jterm) \
  ENV_ARG(1) \
  SIMPLE_ARG(size_t, 2) \
  SIMPLE_ARG(size_t, 3) \
  TERM_ARG(4) \
  TERM_ARG(5) \
  CALL5(msat_term, make_fp_##name) \
  TERM_RETURN

make_term_fp_unary(neg)
make_term_fp_unary(abs)
make_term_fp_unary(isnan)
make_term_fp_unary(isinf)
make_term_fp_unary(iszero)
make_term_fp_unary(issubnormal)
make_term_fp_unary(isnormal)
make_term_fp_unary(isneg)
make_term_fp_unary(ispos)
make_term_fp_rounding_unary(sqrt)
make_term_fp_binary(equal)
make_term_fp_binary(lt)
make_term_fp_binary(leq)
make_term_fp_binary(max)
make_term_fp_binary(min)
make_term_fp_rounding_binary(plus)
make_term_fp_rounding_binary(minus)
make_term_fp_rounding_binary(times)
make_term_fp_rounding_binary(div)
make_term_fp_rounding_cast(cast, 1cast)
make_term_fp_rounding_cast(from_sbv, 1from_1sbv)
make_term_fp_rounding_cast(from_ubv, 1from_1ubv)

DEFINE_FUNC(jterm, 1make_1fp_1round_1to_1int) WITH_THREE_ARGS(jenv, jterm, jterm)
ENV_ARG(1)
TERM_ARG(2)
TERM_ARG(3)
CALL3(msat_term, make_fp_round_to_int)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1fp_1to_1bv) WITH_FOUR_ARGS(jenv, int, jterm, jterm)
ENV_ARG(1)
SIMPLE_ARG(size_t, 2)
TERM_ARG(3)
TERM_ARG(4)
CALL4(msat_term, make_fp_to_bv)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1fp_1as_1ieeebv) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(msat_term, make_fp_as_ieeebv)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1fp_1from_1ieeebv) WITH_FOUR_ARGS(jenv, int, int, jterm)
ENV_ARG(1)
SIMPLE_ARG(size_t, 2)
SIMPLE_ARG(size_t, 3)
TERM_ARG(4)
CALL4(msat_term, make_fp_from_ieeebv)
TERM_RETURN

#define make_term_fp_constant(name, name_escaped) \
  DEFINE_FUNC(jterm, 1make_1fp_##name_escaped) WITH_THREE_ARGS(jenv, int, int) \
  ENV_ARG(1) \
  SIMPLE_ARG(size_t, 2) \
  SIMPLE_ARG(size_t, 3) \
  CALL3(msat_term, make_fp_##name) \
  TERM_RETURN

make_term_fp_constant(plus_inf, 1plus_1inf)
make_term_fp_constant(minus_inf, 1minus_1inf)
make_term_fp_constant(nan, 1nan)

DEFINE_FUNC(jterm, 1make_1fp_1rat_1number) WITH_FIVE_ARGS(jenv, string, int, int, jterm)
ENV_ARG(1)
STRING_ARG(2)
SIMPLE_ARG(size_t, 3)
SIMPLE_ARG(size_t, 4)
TERM_ARG(5)
CALL5(msat_term, make_fp_rat_number)
FREE_STRING_ARG(2)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1fp_1bits_1number) WITH_FOUR_ARGS(jenv, string, int, int)
ENV_ARG(1)
STRING_ARG(2)
SIMPLE_ARG(size_t, 3)
SIMPLE_ARG(size_t, 4)
CALL4(msat_term, make_fp_bits_number)
FREE_STRING_ARG(2)
TERM_RETURN

make_term_constant(fp_roundingmode_nearest_even, 1fp_1roundingmode_1nearest_1even)
make_term_constant(fp_roundingmode_zero, 1fp_1roundingmode_1zero)
make_term_constant(fp_roundingmode_plus_inf, 1fp_1roundingmode_1plus_1inf)
make_term_constant(fp_roundingmode_minus_inf, 1fp_1roundingmode_1minus_1inf)


DEFINE_FUNC(jterm, 1make_1term) WITH_THREE_ARGS(jenv, jdecl, jtermArray)
ENV_ARG(1)
DECL_ARG(2)
TERM_ARRAY_ARG(3)
CALL3(msat_term, make_term)
FREE_TERM_ARRAY_ARG(3)
TERM_RETURN

DEFINE_FUNC(jterm, 1make_1copy_1from) WITH_THREE_ARGS(jenv, jterm, jenv)
ENV_ARG(1)
TERM_ARG(2)
ENV_ARG(3)
CALL3(msat_term, make_copy_from)
TERM_RETURN



i_func1s(term_id, 1term_1id, int, msat_term)
i_func1s(term_arity, 1term_1arity, int, msat_term)

DEFINE_FUNC(jterm, 1term_1get_1arg) WITH_TWO_ARGS(jterm, int)
TERM_ARG(1)
SIMPLE_ARG(int, 2)
CALL2(msat_term, term_get_arg)
STRUCT_RETURN

DEFINE_FUNC(jtype, 1term_1get_1type) WITH_ONE_ARG(jterm)
TERM_ARG(1)
CALL1(msat_type, term_get_type)
STRUCT_RETURN

#define func2_term_is(name, name_escaped) \
	DEFINE_FUNC(jboolean, 1term_1is_##name_escaped) WITH_TWO_ARGS(jenv, jterm) \
	ENV_ARG(1) \
	TERM_ARG(2) \
	CALL2(int, term_is_##name) \
	BOOLEAN_RETURN

func2_term_is(true, 1true)
func2_term_is(false, 1false)
func2_term_is(boolean_constant, 1boolean_1constant)
func2_term_is(atom, 1atom)
func2_term_is(number, 1number)

func2_term_is(and, 1and)
func2_term_is(or, 1or)
func2_term_is(not, 1not)
func2_term_is(iff, 1iff)
func2_term_is(term_ite, 1term_1ite)
func2_term_is(constant, 1constant)
func2_term_is(uf, 1uf)
func2_term_is(equal, 1equal)
func2_term_is(leq, 1leq)
func2_term_is(plus, 1plus)
func2_term_is(times, 1times)

func2_term_is(floor, 1floor)
func2_term_is(array_read, 1array_1read)
func2_term_is(array_write, 1array_1write)
func2_term_is(array_const, 1array_1const)
func2_term_is(int_to_bv, 1int_1to_1bv)
func2_term_is(int_from_ubv, 1int_1from_1ubv)
func2_term_is(int_from_sbv, 1int_1from_1sbv)

#define func_term_is_bv(name) \
	DEFINE_FUNC(jboolean, 1term_1is_1bv_1##name) WITH_TWO_ARGS(jenv, jterm) \
	ENV_ARG(1) \
	TERM_ARG(2) \
	CALL2(int, term_is_bv_##name) \
	BOOLEAN_RETURN
func_term_is_bv(concat)

DEFINE_FUNC(int, 1term_1is_1bv_extract) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
NULL_ARG(size_t, 3)
NULL_ARG(size_t, 4)
CALL4(int, term_is_bv_extract)
BOOLEAN_RETURN

func_term_is_bv(or)
func_term_is_bv(xor)
func_term_is_bv(and)
func_term_is_bv(not)
func_term_is_bv(plus)
func_term_is_bv(minus)
func_term_is_bv(times)
func_term_is_bv(neg)

func_term_is_bv(udiv)
func_term_is_bv(urem)
func_term_is_bv(sdiv)
func_term_is_bv(srem)
func_term_is_bv(ult)
func_term_is_bv(uleq)
func_term_is_bv(slt)
func_term_is_bv(sleq)
func_term_is_bv(lshl)
func_term_is_bv(lshr)
func_term_is_bv(ashr)

DEFINE_FUNC(jboolean, 1term_1is_1bv_1zext) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
NULL_ARG(size_t, 3)
CALL3(int, term_is_bv_zext)
BOOLEAN_RETURN

DEFINE_FUNC(jboolean, 1term_1is_1bv_1sext) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
NULL_ARG(size_t, 3)
CALL3(int, term_is_bv_sext)
BOOLEAN_RETURN

DEFINE_FUNC(jboolean, 1term_1is_1bv_1rol) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
NULL_ARG(size_t, 3)
CALL3(int, term_is_bv_rol)
BOOLEAN_RETURN

DEFINE_FUNC(jboolean, 1term_1is_1bv_1ror) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
NULL_ARG(size_t, 3)
CALL3(int, term_is_bv_ror)
BOOLEAN_RETURN

func_term_is_bv(comp)


#define func_term_is_fp(name) \
	DEFINE_FUNC(jboolean, 1term_1is_1fp_1##name) WITH_TWO_ARGS(jenv, jterm) \
	ENV_ARG(1) \
	TERM_ARG(2) \
	CALL2(int, term_is_fp_##name) \
	BOOLEAN_RETURN

func_term_is_fp(equal)
func_term_is_fp(lt)
func_term_is_fp(leq)
func_term_is_fp(neg)
func_term_is_fp(plus)
func_term_is_fp(minus)
func_term_is_fp(times)
func_term_is_fp(div)
func_term_is_fp(cast)
func_term_is_fp(to_bv)
func_term_is_fp(isnan)
func_term_is_fp(isinf)
func_term_is_fp(iszero)
func_term_is_fp(issubnormal)

func2_term_is(fp_roundingmode_nearest_even, 1fp_1roundingmode_1nearest_1even)
func2_term_is(fp_roundingmode_zero, 1fp_1roundingmode_1zero)
func2_term_is(fp_roundingmode_plus_inf, 1fp_1roundingmode_1plus_1inf)
func2_term_is(fp_roundingmode_minus_inf, 1fp_1roundingmode_1minus_1inf)
func2_term_is(fp_from_sbv, 1fp_1from_1sbv)
func2_term_is(fp_from_ubv, 1fp_1from_1ubv)
func2_term_is(fp_as_ieeebv, 1fp_1as_1ieeebv)
func2_term_is(fp_from_ieeebv, 1fp_1from_1ieeebv)


DEFINE_FUNC(jdecl, 1find_1decl) WITH_TWO_ARGS(jenv, string)
ENV_ARG(1)
STRING_ARG(2)
CALL2(msat_decl, find_decl)
FREE_STRING_ARG(2)
DECL_RETURN

DEFINE_FUNC(jdecl, 1term_1get_1decl) WITH_ONE_ARG(jterm)
TERM_ARG(1)
CALL1(msat_decl, term_get_decl)
STRUCT_RETURN

DEFINE_FUNC(jtype, 1decl_1get_1return_1type) WITH_ONE_ARG(jdecl)
DECL_ARG(1)
CALL1(msat_type, decl_get_return_type)
STRUCT_RETURN

DEFINE_FUNC(int, 1decl_1id) WITH_ONE_ARG(jdecl)
DECL_ARG(1)
CALL1(int, decl_id)
INT_RETURN

i_func1s(decl_get_arity, 1decl_1get_1arity, int, msat_decl)

DEFINE_FUNC(jtype, 1decl_1get_1arg_1type) WITH_TWO_ARGS(jdecl, int)
DECL_ARG(1)
SIMPLE_ARG(int, 2)
CALL2(msat_type, decl_get_arg_type)
STRUCT_RETURN

DEFINE_FUNC(string, 1decl_1get_1name) WITH_ONE_ARG(jdecl)
DECL_ARG(1)
CALL1(char *, decl_get_name)
PLAIN_STRING_RETURN

DEFINE_FUNC(string, 1decl_1repr) WITH_ONE_ARG(jdecl)
DECL_ARG(1)
CALL1(char *, decl_repr)
PLAIN_STRING_RETURN


DEFINE_FUNC(string, 1term_1repr) WITH_ONE_ARG(jterm)
TERM_ARG(1)
CALL1(char *, term_repr)
PLAIN_STRING_RETURN


#define term_to_string(func, func_escaped) \
  DEFINE_FUNC(string, func_escaped) WITH_TWO_ARGS(jenv, jterm) \
  ENV_ARG(1) \
  TERM_ARG(2) \
  CALL2(char *, func) \
  STRING_RETURN

#define make_term_from_string(func, func_escaped) \
  DEFINE_FUNC(jterm, func_escaped) WITH_TWO_ARGS(jenv, string) \
  ENV_ARG(1) \
  STRING_ARG(2) \
  CALL2(msat_term, func) \
  FREE_STRING_ARG(2) \
  TERM_RETURN

make_term_from_string(from_string, 1from_1string)

make_term_from_string(from_smtlib1, 1from_1smtlib1)
make_term_from_string(from_smtlib2, 1from_1smtlib2)
term_to_string(to_smtlib1, 1to_1smtlib1)
term_to_string(to_smtlib2, 1to_1smtlib2)
term_to_string(to_smtlib2_term, 1to_1smtlib2_term)


DEFINE_FUNC(jfailureCode, 1push_1backtrack_1point) WITH_ONE_ARG(jenv)
ENV_ARG(1)
CALL1(int, push_backtrack_point)
FAILURE_CODE_RETURN

DEFINE_FUNC(jfailureCode, 1pop_1backtrack_1point) WITH_ONE_ARG(jenv)
ENV_ARG(1)
CALL1(int, pop_backtrack_point)
FAILURE_CODE_RETURN

DEFINE_FUNC(void, 1reset_1env) WITH_ONE_ARG(jenv)
ENV_ARG(1)
VOID_CALL1(reset_env)

DEFINE_FUNC(jfailureCode, 1assert_1formula) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(int, assert_formula)
FAILURE_CODE_RETURN

i_func1s(solve, 1solve, msat_result, msat_env)

DEFINE_FUNC(int, 1solve_1with_1assumptions) WITH_THREE_ARGS(jenv, jtermArray, int)
ENV_ARG(1)
TERM_ARRAY_ARG(2)
SIMPLE_ARG(size_t, 3)
CALL3(msat_result, solve_with_assumptions)
FREE_TERM_ARRAY_ARG(2)
INT_RETURN


/*
 * msat_term* 	msat_get_asserted_formulas (msat_env e, size_t *num_asserted)
 */
DEFINE_FUNC(jtermArray, 1get_1asserted_1formulas) WITH_ONE_ARG(jenv)
	msat_env env;
	env.repr = (void *) ((size_t) arg1);
	size_t terms_size;
	size_t *terms_size_ptr = &terms_size;
	msat_term *retval = msat_get_asserted_formulas(env, terms_size_ptr);

	jlongArray jretval = ((void *) 0);
	jlong *jarr = malloc(sizeof(jlong) * (size_t) terms_size);
	if (jarr == ((void *) 0)) {
		throwException(
				jenv,
				"java/lang/OutOfMemoryError",
				"Cannot allocate native memory for passing return value from Mathsat");
		goto out;
	}
	size_t i;
	for (i = 0; i < terms_size; ++i) {
		jarr[i] = (jlong)((size_t)retval[i].repr);
	}
	jretval = (*jenv)->NewLongArray(jenv, (size_t) terms_size);
	if (jretval != ((void *) 0)) {
		(*jenv)->SetLongArrayRegion(jenv, jretval, 0, (size_t) terms_size, jarr);
	}
	free(jarr);
	out: msat_free(retval);
	return jretval;
}

DEFINE_FUNC(int, 1all_1sat) WITH_FOUR_ARGS(jenv, jtermArray, int, object)
ENV_ARG(1)
TERM_ARRAY_ARG(2)
SIMPLE_ARG(int, 3)

jclass cls = (*jenv)->FindClass(jenv,
		"org/sosy_lab/cpachecker/util/predicates/mathsat5/Mathsat5NativeApi$AllSatModelCallback");
if (cls == NULL) {
	goto out;
}
jmethodID mid = (*jenv)->GetMethodID(jenv, cls, "callback", "([J)V");
if (mid == NULL) {
	goto out;
}
struct msat_callback_info helper = {jenv, mid, arg4};
int retval = msat_all_sat(m_arg1, m_arg2, m_arg3, &call_java_callback, (void *)&helper);

out:
FREE_TERM_ARRAY_ARG(2)
INT_RETURN

DEFINE_FUNC(jterm, 1get_1model_1value) WITH_TWO_ARGS(jenv, jterm)
ENV_ARG(1)
TERM_ARG(2)
CALL2(msat_term, get_model_value)
STRUCT_RETURN_WITH_ENV

DEFINE_FUNC(jmodel_iterator, 1create_1model_1iterator) WITH_ONE_ARG(jenv)
ENV_ARG(1)
CALL1(msat_model_iterator, create_model_iterator)
MODEL_ITERATOR_RETURN


DEFINE_FUNC(jboolean, 1model_1iterator_1has_1next) WITH_ONE_ARG(jmodel_iterator)
MODEL_ITERATOR_ARG(1)
CALL1(int, model_iterator_has_next)
BOOLEAN_RETURN

DEFINE_FUNC(jboolean, 1model_1iterator_1next) WITH_THREE_ARGS(jmodel_iterator, jtermArray, jtermArray)
MODEL_ITERATOR_ARG(1)
TERM_POINTER_ARG(2)
TERM_POINTER_ARG(3)
CALL3(int, model_iterator_next)
PUT_TERM_POINTER_ARG(3)
PUT_TERM_POINTER_ARG(2)
BOOLEAN_RETURN

DEFINE_FUNC(void, 1destroy_1model_1iterator) WITH_ONE_ARG(jmodel_iterator)
MODEL_ITERATOR_ARG(1)
VOID_CALL1(destroy_model_iterator)


DEFINE_FUNC(jtermArray, 1get_1theory_1lemmas) WITH_ONE_ARG(jenv)
ENV_ARG(1)
TERM_ARRAY_OUTPUT_ARG(2)
CALL2(msat_term*, get_theory_lemmas)
RETURN_TERM_ARRAY(2)

DEFINE_FUNC(jtermArray, 1get_1unsat_1assumptions) WITH_ONE_ARG(jenv)
ENV_ARG(1)
TERM_ARRAY_OUTPUT_ARG(2)
CALL2(msat_term*, get_unsat_assumptions)
RETURN_TERM_ARRAY(2)


DEFINE_FUNC(jtermArray, 1get_1unsat_1core) WITH_ONE_ARG(jenv)
ENV_ARG(1)
TERM_ARRAY_OUTPUT_ARG(2)
CALL2(msat_term*, get_unsat_core)
RETURN_TERM_ARRAY(2)

i_func1s(create_itp_group, 1create_1itp_1group, int, msat_env)

DEFINE_FUNC(jfailureCode, 1set_1itp_1group) WITH_TWO_ARGS(jenv, int)
ENV_ARG(1)
SIMPLE_ARG(int, 2)
CALL2(int, set_itp_group)
FAILURE_CODE_RETURN

DEFINE_FUNC(jterm, 1get_1interpolant) WITH_THREE_ARGS(jenv, intArray, int)
ENV_ARG(1)
INT_ARRAY_ARG(2)
SIMPLE_ARG(size_t, 3)
CALL3(msat_term, get_interpolant)
FREE_INT_ARRAY_ARG(2)
TERM_RETURN

DEFINE_FUNC(long, 1set_1termination_1test) WITH_TWO_ARGS(jenv, object)
  ENV_ARG(1)

  jclass cls = (*jenv)->FindClass(jenv,
    "org/sosy_lab/cpachecker/util/predicates/mathsat5/Mathsat5NativeApi$TerminationTest");
  if (cls == NULL) {
    return;
  }
  jmethodID mid = (*jenv)->GetMethodID(jenv, cls, "shouldTerminate", "()Z");
  if (mid == NULL) {
    return;
  }
  if (arg2 == NULL) {
    throwException(jenv, "java/lang/NullPointerException", "TerminationTest may not be null");
    return;
  }

  // We can't use a struct on the stack here
  // because it needs to live longer than this method call.
  struct msat_callback_info *helper = malloc(sizeof(struct msat_callback_info));
  helper->jenv = jenv;
  helper->callback_method = mid;
  // Similarly we need a global ref to arg2 instance of a local ref.
  helper->obj = (*jenv)->NewGlobalRef(jenv, arg2);

  int retval = msat_set_termination_test(m_arg1, &call_java_termination_test, helper);
  if (retval != 0) {
    const char *msg = msat_last_error_message(m_arg1);
    throwException(jenv, "java/lang/IllegalArgumentException", msg);
    return;
  }
  // Ugly: return the struct's address so that it can be free'd later on.
  return (long)helper;
}

// This method is not defined by Mathsat,
// we need it to prevent a memory leak.
// This may be called only after the environment with this termination test has been destroyed.
DEFINE_FUNC(void, 1free_1termination_1test) WITH_ONE_ARG(long)
  struct msat_callback_info *helper = (struct msat_callback_info *)(long)arg1;
  if (helper == NULL) {
    throwException(jenv, "java/lang/NullPointerTest", "Argument to msat_free_termination_test may not be null");
    return;
  }

  (*jenv)->DeleteGlobalRef(jenv, helper->obj);
  helper->obj = NULL;
  free(helper);
}


DEFINE_FUNC(string, 1last_1error_1message) WITH_ONE_ARG(jenv)
ENV_ARG(1)
CALL1(const char *, last_error_message)
CONST_STRING_RETURN

DEFINE_FUNC(string, 1get_1version) WITHOUT_ARGS
CALL0(char *, get_version)
PLAIN_STRING_RETURN


DEFINE_FUNC(object, 1named_1list_1from_1smtlib2) WITH_TWO_ARGS(jenv, string)
    ENV_ARG(1)
    STRING_ARG(2)
    jobject ntw = NULL;
    const char *ERR = NULL;

    size_t n, i;
    char **names;
    msat_term *terms;

    size_t *m_arg3 = &n;
    char ***m_arg4 = &names;
    msat_term **m_arg5 = &terms;

    CALL5(int, named_list_from_smtlib2)
    FREE_STRING_ARG(2)

    if (retval != 0) {
        const char *msg = msat_last_error_message(m_arg1);
        throwException(jenv, "java/lang/IllegalArgumentException", msg);
        return NULL;
    }

    jclass cls = (*jenv)->FindClass(jenv,
        "org/sosy_lab/cpachecker/util/predicates/mathsat5/Mathsat5NativeApi$NamedTermsWrapper");
    if (cls == NULL) {
        ERR = "Could not find class NamedTermsWrapper.";
        goto ERROR;
    }

    jmethodID mid = (*jenv)->GetMethodID(jenv, cls, "<init>", "([J[Ljava/lang/String;)V");
    if (mid == NULL) {
        ERR = "Could not find constructor of class NamedTermsWrapper!";
        goto ERROR;
    }

    jobjectArray jnames = (jobjectArray)(*jenv)->NewObjectArray(jenv, n, (*jenv)->FindClass(jenv, "java/lang/String"), NULL);
    jlongArray jterms = (jlongArray)(*jenv)->NewLongArray(jenv, n);
    for (i = 0; i < n; i++) {
        char* name = names[i];
        msat_term term = terms[i];
        jlong termlong = (jlong)((size_t)(term.repr));
        jobject string = (*jenv)->NewStringUTF(jenv, name);
        msat_free(name);
        (*jenv)->SetLongArrayRegion(jenv, jterms, i, 1, &termlong);
        (*jenv)->SetObjectArrayElement(jenv, jnames, i, string);
    }

    ntw = (*jenv)->NewObject(jenv, cls, mid, jterms, jnames);
    if ((*jenv)->ExceptionCheck(jenv)) {
        return NULL;
    }
    if (ntw == NULL) {
        ERR = "Could not instantiate NamedTermsWrapper.";
        goto ERROR;
    }

    return ntw;

ERROR:
    throwException(jenv, "java/lang/IllegalArgumentException", ERR);
    return NULL;
}

DEFINE_FUNC(string, 1named_1list_1to_1smtlib2) WITH_TWO_ARGS(jenv, jnamedtermswrapper)
    ENV_ARG(1)

    const char *ERR = NULL;
    size_t m_arg2;
    const char **m_arg3;
    msat_term *m_arg4;
    jsize i = 0;

    // query and get fields
    jobject ntw = arg2;
    jclass cls = (*jenv)->GetObjectClass(jenv, ntw);

    jfieldID fid_terms = (*jenv)->GetFieldID(jenv, cls, "terms",  "[J");
    if (fid_terms == 0) {
        ERR = "Could not find the field 'terms'!";
        goto ERROR;
    }
    jfieldID fid_names = (*jenv)->GetFieldID(jenv, cls, "names",  "[Ljava/lang/String;");
    if (fid_names == 0) {
        ERR = "Could not find the field 'names'!";
        goto ERROR;
    }
    jlongArray terms = (jlongArray)((*jenv)->GetObjectField(jenv, ntw, fid_terms));
    if (terms == NULL) {
        ERR = "Could not get the field 'terms'!";
        goto ERROR;
    }

    jobjectArray names = (jobjectArray)((*jenv)->GetObjectField(jenv, ntw, fid_names));
    if (names == NULL) {
        ERR = "Could not get the field 'names'!";
        goto ERROR;
    }

    // check sizes
    jsize n_terms = (*jenv)->GetArrayLength(jenv, terms);
    jsize n = (*jenv)->GetArrayLength(jenv, names);
    if (n != n_terms) {
        ERR = "Invalid NamedTermsWrapper instance - should have same length in both arrays!";
        goto ERROR;
    }
    m_arg2 = n;

    // allocate target array
    jlong *terms_inner = (*jenv)->GetLongArrayElements(jenv, terms, NULL);
    if (terms_inner == NULL) {
        ERR = "Failed unpacking the terms array object.";
        goto ERROR;
    }
    m_arg3 = malloc(sizeof(const char*) * n);
    if (m_arg3 == NULL) {
        (*jenv)->ReleaseLongArrayElements(jenv, terms, terms_inner, JNI_ABORT);
        ERR = "Not enough memory for allocation of temporary array.";
        goto ERROR;
    }

    // extract long and string arrays into target arrays
    for (; i < n; i++) {
        jobject str = (*jenv)->GetObjectArrayElement(jenv, names, i);
        if ((*jenv)->ExceptionCheck(jenv)) {
            (*jenv)->ReleaseLongArrayElements(jenv, terms, terms_inner, JNI_ABORT);
            for (i--; i >= 0; i--) {
                jobject str = (*jenv)->GetObjectArrayElement(jenv, names, i);
                (*jenv)->ReleaseStringUTFChars(jenv, str, m_arg3[i]);
            }
            free(m_arg3);
            return NULL;
        }
        m_arg3[i] = (*jenv)->GetStringUTFChars(jenv, str, NULL);
        if ((*jenv)->ExceptionCheck(jenv)) {
            (*jenv)->ReleaseLongArrayElements(jenv, terms, terms_inner, JNI_ABORT);
            for (i--; i >= 0; i--) {
                jobject str = (*jenv)->GetObjectArrayElement(jenv, names, i);
                (*jenv)->ReleaseStringUTFChars(jenv, str, m_arg3[i]);
            }
            free(m_arg3);
            return NULL;
        }
    }

    // perform the call and clean up
    m_arg4 = (msat_term *) terms_inner;
    CALL4(char *, named_list_to_smtlib2)
    (*jenv)->ReleaseLongArrayElements(jenv, terms, terms_inner, JNI_ABORT);
    for (; i < n; i++) {
        jobject str = (*jenv)->GetObjectArrayElement(jenv, names, i);
        (*jenv)->ReleaseStringUTFChars(jenv, str, m_arg3[i]);
    }
    free(m_arg3);

    goto RETURN;
ERROR:
    throwException(jenv, "java/lang/IllegalArgumentException", ERR);
    return NULL;
RETURN:
STRING_RETURN

