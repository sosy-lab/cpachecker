
// aliases for Z3 types

typedef void jvoid;
typedef unsigned int junsigned;
typedef unsigned long long junsigned_int64;
typedef long long jint64;
typedef long long int64;
//typedef long long jint64;

typedef jboolean jJ_bool;
typedef jint jJ_lbool;
typedef jstring jJ_string;

typedef jint jJ_ast_kind;
typedef jint jJ_decl_kind;
typedef jint jJ_error_code;
typedef jint jJ_param_kind;

typedef jint jJ_parameter_kind;
typedef jint jJ_sort_kind;
typedef jint jJ_symbol_kind;
typedef jint jJ_ast_print_mode;
typedef jint jJ_goal_prec;



#define UNSIGNED_ARG(num)       SIMPLE_ARG(junsigned, num)
#define UNSIGNED_INT64_ARG(num) SIMPLE_ARG(junsigned_int64, num)
#define DOUBLE_ARG(num)         SIMPLE_ARG(double, num)
#define BOOL_ARG(num)           SIMPLE_ARG(Z3_bool, num)
#define INT_ARG(num)            SIMPLE_ARG(jint, num)
#define INT64_ARG(num)          SIMPLE_ARG(jint64, num)
#define ERROR_CODE_ARG(num)     SIMPLE_ARG(jint, num)
#define AST_PRINT_MODE_ARG(num) SIMPLE_ARG(jint, num)


#define CLEAN_UNSIGNED_ARG(num)       CLEAN_SIMPLE_ARG(num)
#define CLEAN_UNSIGNED_INT64_ARG(num) CLEAN_SIMPLE_ARG(num)
#define CLEAN_DOUBLE_ARG(num)         CLEAN_SIMPLE_ARG(num)
#define CLEAN_BOOL_ARG(num)           CLEAN_SIMPLE_ARG(num)
#define CLEAN_INT_ARG(num)            CLEAN_SIMPLE_ARG(num)
#define CLEAN_INT64_ARG(num)          CLEAN_SIMPLE_ARG(num)
#define CLEAN_ERROR_CODE_ARG(num)     CLEAN_SIMPLE_ARG(num)
#define CLEAN_AST_PRINT_MODE_ARG(num) CLEAN_SIMPLE_ARG(num)


typedef jlong jJ_config;
#define CONFIG_ARG(num) STRUCT_ARG(Z3_config, num)
#define CLEAN_CONFIG_ARG(num) CLEAN_STRUCT_ARG(num)
#define CONFIG_RETURN STRUCT_RETURN

typedef jlong jJ_context;
#define CONTEXT_ARG(num) STRUCT_ARG(Z3_context, num)
#define CLEAN_CONTEXT_ARG(num) CLEAN_STRUCT_ARG(num)
#define CONTEXT_RETURN STRUCT_RETURN

typedef jlong jJ_app;
#define APP_ARG(num) STRUCT_ARG(Z3_app, num)
#define CLEAN_APP_ARG(num) CLEAN_STRUCT_ARG(num)
#define APP_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_ast;
#define AST_ARG(num) STRUCT_ARG(Z3_ast, num)
#define CLEAN_AST_ARG(num) CLEAN_STRUCT_ARG(num)
#define AST_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_constructor;
#define CONSTRUCTOR_ARG(num) STRUCT_ARG(Z3_constructor, num)
#define CLEAN_CONSTRUCTOR_ARG(num) CLEAN_STRUCT_ARG(num)
#define CONSTRUCTOR_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_func_decl;
#define FUNC_DECL_ARG(num) STRUCT_ARG(Z3_func_decl, num)
#define CLEAN_FUNC_DECL_ARG(num) CLEAN_STRUCT_ARG(num)
#define FUNC_DECL_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_param_descrs;
#define PARAM_DESCRS_ARG(num) STRUCT_ARG(Z3_param_descrs, num)
#define CLEAN_PARAM_DESCRS_ARG(num) CLEAN_STRUCT_ARG(num)
#define PARAM_DESCRS_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_params;
#define PARAMS_ARG(num) STRUCT_ARG(Z3_params, num)
#define CLEAN_PARAMS_ARG(num) CLEAN_STRUCT_ARG(num)
#define PARAMS_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_sort;
#define SORT_ARG(num) STRUCT_ARG(Z3_sort, num)
#define CLEAN_SORT_ARG(num) CLEAN_STRUCT_ARG(num)
#define SORT_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_symbol;
#define SYMBOL_ARG(num) STRUCT_ARG(Z3_symbol, num)
#define CLEAN_SYMBOL_ARG(num) CLEAN_STRUCT_ARG(num)
#define SYMBOL_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_pattern;
#define PATTERN_ARG(num) STRUCT_ARG(Z3_pattern, num)
#define CLEAN_PATTERN_ARG(num) CLEAN_STRUCT_ARG(num)
#define PATTERN_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_model;
#define MODEL_ARG(num) STRUCT_ARG(Z3_model, num)
#define CLEAN_MODEL_ARG(num) CLEAN_STRUCT_ARG(num)
#define MODEL_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_optimize;
#define OPTIMIZE_ARG(num) STRUCT_ARG(Z3_optimize, num)
#define CLEAN_OPTIMIZE_ARG(num) CLEAN_STRUCT_ARG(num)
#define OPTIMIZE_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_func_interp;
#define FUNC_INTERP_ARG(num) STRUCT_ARG(Z3_func_interp, num)
#define CLEAN_FUNC_INTERP_ARG(num) CLEAN_STRUCT_ARG(num)
#define FUNC_INTERP_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_ast_vector;
#define AST_VECTOR_ARG(num) STRUCT_ARG(Z3_ast_vector, num)
#define CLEAN_AST_VECTOR_ARG(num) CLEAN_STRUCT_ARG(num)
#define AST_VECTOR_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_func_entry;
#define FUNC_ENTRY_ARG(num) STRUCT_ARG(Z3_func_entry, num)
#define CLEAN_FUNC_ENTRY_ARG(num) CLEAN_STRUCT_ARG(num)
#define FUNC_ENTRY_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_theory;
#define THEORY_ARG(num) STRUCT_ARG(Z3_theory, num)
#define CLEAN_THEORY_ARG(num) CLEAN_STRUCT_ARG(num)
#define THEORY_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_theory_data;
#define THEORY_DATA_ARG(num) STRUCT_ARG(Z3_theory_data, num)
#define CLEAN_THEORY_DATA_ARG(num) CLEAN_STRUCT_ARG(num)

#define THEORY_DATA_RETURN STRUCT_RETURN

typedef jlong jJ_fixedpoint;
#define FIXEDPOINT_ARG(num) STRUCT_ARG(Z3_fixedpoint, num)
#define CLEAN_FIXEDPOINT_ARG(num) CLEAN_STRUCT_ARG(num)
#define FIXEDPOINT_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_ast_map;
#define AST_MAP_ARG(num) STRUCT_ARG(Z3_ast_map, num)
#define CLEAN_AST_MAP_ARG(num) CLEAN_STRUCT_ARG(num)
#define AST_MAP_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_goal;
#define GOAL_ARG(num) STRUCT_ARG(Z3_goal, num)
#define CLEAN_GOAL_ARG(num) CLEAN_STRUCT_ARG(num)
#define GOAL_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_tactic;
#define TACTIC_ARG(num) STRUCT_ARG(Z3_tactic, num)
#define CLEAN_TACTIC_ARG(num) CLEAN_STRUCT_ARG(num)
#define TACTIC_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_probe;
#define PROBE_ARG(num) STRUCT_ARG(Z3_probe, num)
#define CLEAN_PROBE_ARG(num) CLEAN_STRUCT_ARG(num)
#define PROBE_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_apply_result;
#define APPLY_RESULT_ARG(num) STRUCT_ARG(Z3_apply_result, num)
#define CLEAN_APPLY_RESULT_ARG(num) CLEAN_STRUCT_ARG(num)
#define APPLY_RESULT_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_solver;
#define SOLVER_ARG(num) STRUCT_ARG(Z3_solver, num)
#define CLEAN_SOLVER_ARG(num) CLEAN_STRUCT_ARG(num)
#define SOLVER_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_stats;
#define STATS_ARG(num) STRUCT_ARG(Z3_stats, num)
#define CLEAN_STATS_ARG(num) CLEAN_STRUCT_ARG(num)
#define STATS_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_literals;
#define LITERALS_ARG(num) STRUCT_ARG(Z3_literals, num)
#define CLEAN_LITERALS_ARG(num) CLEAN_STRUCT_ARG(num)
#define LITERALS_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT

typedef jlong jJ_constructor_list;
#define CONSTRUCTOR_LIST_ARG(num) STRUCT_ARG(Z3_constructor_list, num)
#define CLEAN_CONSTRUCTOR_LIST_ARG(num) CLEAN_STRUCT_ARG(num)



typedef jobject jJ_ast_pointer;
#define AST_POINTER_ARG(num) STRUCT_POINTER_ARG(Z3_ast, num)
#define SET_AST_POINTER_ARG(num) SET_LONG_POINTER_ARG(num)
#define CLEAN_AST_POINTER_ARG(num) CLEAN_LONG_POINTER_ARG(num)

typedef jobject jJ_func_decl_pointer;
#define FUNC_DECL_POINTER_ARG(num) STRUCT_POINTER_ARG(Z3_func_decl, num)
#define SET_FUNC_DECL_POINTER_ARG(num) SET_LONG_POINTER_ARG(num)
#define CLEAN_FUNC_DECL_POINTER_ARG(num) CLEAN_LONG_POINTER_ARG(num)

typedef jobject jJ_literals_pointer;
#define LITERALS_POINTER_ARG(num) STRUCT_POINTER_ARG(Z3_literals, num)
#define SET_LITERALS_POINTER_ARG(num) SET_LONG_POINTER_ARG(num)
#define CLEAN_LITERALS_POINTER_ARG(num) CLEAN_LONG_POINTER_ARG(num)

typedef jobject junsigned_pointer;
#define UNSIGNED_POINTER_ARG(num) STRUCT_POINTER_ARG(unsigned, num)
#define SET_UNSIGNED_POINTER_ARG(num) SET_INT_POINTER_ARG(num)
#define CLEAN_UNSIGNED_POINTER_ARG(num) CLEAN_INT_POINTER_ARG(num)

typedef jobject jint64_pointer;
#define INT64_POINTER_ARG(num) STRUCT_POINTER_ARG(jint64, num)
#define SET_INT64_POINTER_ARG(num) SET_LONG_POINTER_ARG(num)
#define CLEAN_INT64_POINTER_ARG(num) CLEAN_LONG_POINTER_ARG(num)

typedef jobject junsigned_int64_pointer;
#define UNSIGNED_INT64_POINTER_ARG(num) STRUCT_POINTER_ARG(junsigned_int64, num)
#define SET_UNSIGNED_INT64_POINTER_ARG(num) SET_LONG_POINTER_ARG(num)
#define CLEAN_UNSIGNED_INT64_POINTER_ARG(num) CLEAN_LONG_POINTER_ARG(num)

typedef jobject jJ_model_pointer;
#define MODEL_POINTER_ARG(num) STRUCT_POINTER_ARG(Z3_model, num)
#define SET_MODEL_POINTER_ARG(num) SET_LONG_POINTER_ARG(num)
#define CLEAN_MODEL_POINTER_ARG(num) CLEAN_LONG_POINTER_ARG(num)

#define CONSTRUCTOR_LIST_RETURN_WITH_CONTEXT STRUCT_RETURN_WITH_CONTEXT



typedef jintArray junsigned_array;
typedef jintArray jint_array;
#define UNSIGNED_ARRAY_ARG(num) INT_ARRAY_ARG(num)
#define CLEAN_UNSIGNED_ARRAY_ARG(num) CLEAN_INT_ARRAY_ARG(num)

typedef jlongArray jJ_ast_array;
#define AST_ARRAY_ARG(num) ARRAY_ARG(Z3_ast, num)
#define CLEAN_AST_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)
#define AST_OUT_ARRAY_ARG(num) AST_ARRAY_ARG(num)
#define CLEAN_AST_OUT_ARRAY_ARG(num) CLEAN_AST_ARRAY_ARG(num)
#define SET_AST_OUT_ARRAY_ARG(num) FILL_ARRAY_ARG(num)

typedef jlongArray jJ_func_decl_array;
#define FUNC_DECL_ARRAY_ARG(num) ARRAY_ARG(Z3_func_decl, num)
#define CLEAN_FUNC_DECL_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)
#define FUNC_DECL_OUT_ARRAY_ARG(num) FUNC_DECL_ARRAY_ARG(num)
#define CLEAN_FUNC_DECL_OUT_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)
#define SET_FUNC_DECL_OUT_ARRAY_ARG(num) FILL_ARRAY_ARG(num)

typedef jlongArray jJ_sort_array;
#define SORT_ARRAY_ARG(num) ARRAY_ARG(Z3_sort, num)
#define CLEAN_SORT_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)
#define SORT_OUT_ARRAY_ARG(num) SORT_ARRAY_ARG(num)
#define CLEAN_SORT_OUT_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)
#define SET_SORT_OUT_ARRAY_ARG(num) FILL_ARRAY_ARG(num)

typedef jlongArray jJ_sort_opt_array;
#define SORT_OPT_ARRAY_ARG(num) ARRAY_ARG(Z3_sort_opt, num)
#define CLEAN_SORT_OPT_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)

typedef jlongArray jJ_pattern_array;
#define PATTERN_ARRAY_ARG(num) ARRAY_ARG(Z3_pattern, num)
#define CLEAN_PATTERN_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)

typedef jlongArray jJ_symbol_array;
#define SYMBOL_ARRAY_ARG(num) ARRAY_ARG(Z3_symbol, num)
#define CLEAN_SYMBOL_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)

typedef jlongArray jJ_app_array;
#define APP_ARRAY_ARG(num) ARRAY_ARG(Z3_app, num)
#define CLEAN_APP_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)

typedef jlongArray jJ_tactic_array;
#define TACTIC_ARRAY_ARG(num) ARRAY_ARG(Z3_tactic, num)
#define CLEAN_TACTIC_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)

typedef jlongArray jJ_constructor_array;
#define CONSTRUCTOR_ARRAY_ARG(num) ARRAY_ARG(Z3_constructor, num)
#define CLEAN_CONSTRUCTOR_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)
#define CONSTRUCTOR_OUT_ARRAY_ARG(num) CONSTRUCTOR_ARRAY_ARG(num)
#define CLEAN_CONSTRUCTOR_OUT_ARRAY_ARG(num) CLEAN_CONSTRUCTOR_ARRAY_ARG(num)
#define SET_CONSTRUCTOR_OUT_ARRAY_ARG(num) FILL_ARRAY_ARG(num)

typedef jlongArray jJ_constructor_list_array;
#define CONSTRUCTOR_LIST_ARRAY_ARG(num) ARRAY_ARG(Z3_constructor_list, num)
#define CLEAN_CONSTRUCTOR_LIST_ARRAY_ARG(num) CLEAN_STRUCT_ARRAY_ARG(num)
#define CONSTRUCTOR_LIST_OUT_ARRAY_ARG(num) CONSTRUCTOR_LIST_ARRAY_ARG(num)
#define CLEAN_CONSTRUCTOR_LIST_OUT_ARRAY_ARG(num) CLEAN_CONSTRUCTOR_LIST_ARRAY_ARG(num)
#define SET_CONSTRUCTOR_LIST_OUT_ARRAY_ARG(num) FILL_ARRAY_ARG(num)




#define BOOL_RETURN           INT_RETURN;
#define BOOL_OPT_RETURN       INT_RETURN;
#define LBOOL_RETURN          INT_RETURN;
#define UNSIGNED_RETURN       INT_RETURN;
#define PARAM_KIND_RETURN     INT_RETURN;
#define SYMBOL_KIND_RETURN    INT_RETURN;
#define SORT_KIND_RETURN      INT_RETURN;
#define DECL_KIND_RETURN      INT_RETURN;
#define PARAMETER_KIND_RETURN INT_RETURN;
#define AST_KIND_RETURN       INT_RETURN;
#define ERROR_CODE_RETURN     INT_RETURN;
#define SEARCH_FAILURE_RETURN INT_RETURN;
#define GOAL_PREC_RETURN      INT_RETURN;


#define AST_OPT_RETURN_WITH_CONTEXT AST_RETURN_WITH_CONTEXT;
#define FUNC_INTERP_OPT_RETURN_WITH_CONTEXT FUNC_INTERP_RETURN_WITH_CONTEXT;


typedef jJ_bool jJ_bool_opt;
typedef jJ_ast jJ_ast_opt;
typedef jJ_func_interp jJ_func_interp_opt;

