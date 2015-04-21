#include "includes/defines.h"

DEFINE_FUNC(jfailureCode, 1push_1minimize) WITH_FOUR_ARGS(jenv, jterm, string, string)
ENV_ARG(1)
TERM_ARG(2)
OPTIONAL_STRING_ARG(3)
OPTIONAL_STRING_ARG(4)
CALL4(int, push_minimize)
FREE_STRING_OPTIONAL_ARG(3)
FREE_STRING_OPTIONAL_ARG(4)
FAILURE_CODE_RETURN

DEFINE_FUNC(jfailureCode, 1push_1maximize) WITH_FOUR_ARGS(jenv, jterm, string, string)
ENV_ARG(1)
TERM_ARG(2)
OPTIONAL_STRING_ARG(3)
OPTIONAL_STRING_ARG(4)
CALL4(int, push_maximize)
FREE_STRING_OPTIONAL_ARG(3)
FREE_STRING_OPTIONAL_ARG(4)
FAILURE_CODE_RETURN

DEFINE_FUNC(jfailureCode, 1push_1maxmin) WITH_FIVE_ARGS(jenv, int, jtermArray, string, string)
ENV_ARG(1)
SIMPLE_ARG(size_t, 2)
TERM_ARRAY_ARG(3)
OPTIONAL_STRING_ARG(4)
OPTIONAL_STRING_ARG(5)
CALL5(int, push_maxmin)
FREE_TERM_ARRAY_ARG(3)
FREE_STRING_OPTIONAL_ARG(4)
FREE_STRING_OPTIONAL_ARG(5)
FAILURE_CODE_RETURN

DEFINE_FUNC(jfailureCode, 1assert_1soft_1formula) WITH_FOUR_ARGS(jenv, jterm, jterm, string)
ENV_ARG(1)
TERM_ARG(2)
TERM_ARG(3)
STRING_ARG(4)
CALL4(int, assert_soft_formula)
FREE_STRING_ARG(4)
FAILURE_CODE_RETURN

DEFINE_FUNC(jobjective_iterator, 1create_1objective_1iterator) WITH_ONE_ARG(jenv)
ENV_ARG(1)
CALL1(msat_objective_iterator, create_objective_iterator)
OBJECTIVE_ITERATOR_RETURN

DEFINE_FUNC(int, 1objective_1iterator_1has_1next) WITH_ONE_ARG(jobjective_iterator)
OBJECTIVE_ITERATOR_ARG(1)
CALL1(int, objective_iterator_has_next)
INT_RETURN

DEFINE_FUNC(int, 1objective_1iterator_1next) WITH_TWO_ARGS(jobjective_iterator, jobjectiveArray)
OBJECTIVE_ITERATOR_ARG(1)
OBJECTIVE_POINTER_ARG(2)
CALL2(int, objective_iterator_next)
PUT_OBJECTIVE_POINTER_ARG(2)
INT_RETURN

DEFINE_FUNC(void, 1destroy_1objective_1iterator) WITH_ONE_ARG(jobjective_iterator)
OBJECTIVE_ITERATOR_ARG(1)
VOID_CALL1(destroy_objective_iterator)

DEFINE_FUNC(int, 1objective_1result) WITH_TWO_ARGS(jenv, jobjective)
ENV_ARG(1)
OBJECTIVE_ARG(2)
CALL2(msat_result, objective_result)
INT_RETURN

DEFINE_FUNC(jterm, 1objective_1get_1term) WITH_TWO_ARGS(jenv, jobjective)
ENV_ARG(1)
OBJECTIVE_ARG(2)
CALL2(msat_term, objective_get_term)
TERM_RETURN

DEFINE_FUNC(int, 1objective_1get_1type) WITH_TWO_ARGS(jenv, jobjective)
ENV_ARG(1)
OBJECTIVE_ARG(2)
CALL2(msat_objective_type, objective_get_type)
INT_RETURN

DEFINE_FUNC(jfailureCode, 1set_1model) WITH_TWO_ARGS(jenv, jobjective)
ENV_ARG(1)
OBJECTIVE_ARG(2)
CALL2(int, set_model)
FAILURE_CODE_RETURN

DEFINE_FUNC(string, 1objective_1get_1search_1stats) WITH_TWO_ARGS(jenv, jobjective)
ENV_ARG(1)
OBJECTIVE_ARG(2)
CALL2(const char *, objective_get_search_stats)
CONST_STRING_RETURN

DEFINE_FUNC(int, 1objective_1value_1is_1unbounded) WITH_THREE_ARGS(jenv, jobjective, int)
ENV_ARG(1)
OBJECTIVE_ARG(2)
SIMPLE_ARG(int, 3)
CALL3(int, objective_value_is_unbounded)
INT_RETURN

DEFINE_FUNC(int, 1objective_1value_1is_1plus_1inf) WITH_THREE_ARGS(jenv, jobjective, int)
ENV_ARG(1)
OBJECTIVE_ARG(2)
SIMPLE_ARG(int, 3)
CALL3(int, objective_value_is_plus_inf)
INT_RETURN

DEFINE_FUNC(int, 1objective_1value_1is_1minus_1inf) WITH_THREE_ARGS(jenv, jobjective, int)
ENV_ARG(1)
OBJECTIVE_ARG(2)
SIMPLE_ARG(int, 3)
CALL3(int, objective_value_is_minus_inf)
INT_RETURN

DEFINE_FUNC(int, 1objective_1value_1is_1strict) WITH_THREE_ARGS(jenv, jobjective, int)
ENV_ARG(1)
OBJECTIVE_ARG(2)
SIMPLE_ARG(int, 3)
CALL3(int, objective_value_is_strict)
INT_RETURN

DEFINE_FUNC(jterm, 1objective_1value_1term) WITH_THREE_ARGS(jenv, jobjective, int)
ENV_ARG(1)
OBJECTIVE_ARG(2)
SIMPLE_ARG(int, 3)
CALL3(msat_term, objective_value_term)
TERM_RETURN

DEFINE_FUNC(string, 1objective_1value_1repr) WITH_THREE_ARGS(jenv, jobjective, int)
ENV_ARG(1)
OBJECTIVE_ARG(2)
SIMPLE_ARG(int, 3)
CALL3(char *, objective_value_repr)
STRING_RETURN
