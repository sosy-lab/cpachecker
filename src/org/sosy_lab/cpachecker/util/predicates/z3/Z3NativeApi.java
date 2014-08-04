/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.z3;

import org.sosy_lab.cpachecker.util.NativeLibraries;

/** This class contains the native calls for Z3.
 *
 * Z3 uses reference-counting for all objects. If a function returns an
 * object and the user wants to use it only once, the reference-counter
 * should not be incremented. The object will be destroyed after next usage.
 * If the user wants to use the object several times, he has to increment
 * the reference (only once!), so that the object remains valid. */
public final class Z3NativeApi {

  static {
    NativeLibraries.loadLibrary("z3j");
  }

  // Helper Classes,
  // they are used during the native operations
  // and can be accessed later to get the values.
  public static class PointerToInt {
    public int value;
  }

  public static class PointerToLong {
    public long value;
  }

  public static class PointerToString {
    public String value;
  }

  // OPTIMIZATION
  public static native int solver_check_opti(
      long context, long solver, PointerToInt unbounded, long var, int maximize);

  // CREATE CONFIGURATION
  public static native void global_param_set(String param_id, String param_value);
  public static native void global_param_reset_all();
  public static native boolean global_param_get(String param_id, PointerToString param_value);

  public static native long mk_config();
  public static native void del_config(long context);
  public static native void set_param_value(long z3_config, String param_id, String param_value);


  // CREATE CONTEXT
  public static native long mk_context(long config);
  public static native long mk_context_rc(long context);
  public static native void del_context(long context);
  public static native void inc_ref(long context, long ast);
  public static native void dec_ref(long context, long ast);
  public static native void update_param_value(long context, String param_id, String param_value);
  public static native boolean get_param_value(long context, String param_id, PointerToString param_value);
  public static native void interrupt(long context);


  // PARAMETERS
  public static native long mk_params(long context);
  public static native void params_inc_ref(long context, long params);
  public static native void params_dec_ref(long context, long params);
  public static native void params_set_bool(long context, long params, long symbol, boolean value);
  public static native void params_set_uint(long context, long params, long symbol, int value);
  public static native void params_set_double(long context, long params, long symbol, double value);
  public static native void params_set_symbol(long context, long params, long symbol, long symbol_value);
  public static native String params_to_string(long context, long params);
  public static native void params_validate(long context, long params, long params_descrs);


  // PARAMETER DESCRIPTIONS
  public static native void param_descrs_inc_ref(long context, long params_descrs);
  public static native void param_descrs_dec_ref(long context, long params_descrs);
  public static native int param_descrs_get_kind(long context, long params_descrs, long symbol);
  public static native int param_descrs_size(long context, long params_descrs);
  public static native long param_descrs_get_name(long context, long params_descrs, int index);
  public static native String param_descrs_to_string(long context, long params_descrs);


  // SYMBOLS
  public static native long mk_int_symbol(long context, int number);
  public static native long mk_string_symbol(long context, String name);


  // SORTS
  public static native long mk_uninterpreted_sort(long context, long symbol);
  public static native long mk_bool_sort(long context);
  public static native long mk_int_sort(long context);
  public static native long mk_real_sort(long context);
  public static native long mk_bv_sort(long context, int size);
  public static native long mk_finite_domain_sort(long context, long symbol, long size);
  public static native long mk_array_sort(long context, long sort1, long sort2);
  public static native long mk_tuple_sort(long context, long symbol, int len, long[] symbols, long[] sorts, PointerToLong tupl_decl, long[] proj_decl);
  public static native long mk_enumeration_sort(long context, long symbol, int len, long[] symbols, long[] enum_consts, long[] enum_testers);
  public static native long mk_list_sort(long context, long symbol, long sort, PointerToLong nil_decl, PointerToLong is_nil_decl, PointerToLong cons_decl, PointerToLong is_cons_decl, PointerToLong head_decl, PointerToLong tail_decl);
  public static native long mk_constructor(long context, long symbol, long recognizer, int len, long[] symbols, long[] sorts, int[] sort_refs);
  public static native void del_constructor(long context, long constructor);
  public static native long mk_datatype(long context, long symbol, int len, long[] constructors);
  public static native long mk_constructor_list(long context, int len, long[] constructors);
  public static native void del_constructor_list(long context, long constructor);
  public static native void mk_datatypes(long context, int len, long[] symbols, long[] sorts, long[] constructor_lists);
  public static native void query_constructor(long context, long constructor, int len, PointerToLong constructor2, PointerToLong tester, long[] accessors);


  // CONSTANTS AND APPLICATIONS
  private static native long mk_func_decl(long context, long symbol, int len, long[] sorts, long sort);
  private static native long mk_app(long context, long decl, int len, long[] args);
  public static native long mk_const(long context, long symbol, long sort);
  public static native long mk_fresh_func_decl(long context, String prefix, int len, long[] sorts, long sort);
  public static native long mk_fresh_const(long context, String prefix, long sort);

  public static long mk_func_decl(long context, long symbol, long[] sorts, long sort) {
    return mk_func_decl(context, symbol, sorts.length, sorts, sort);
  }

  public static long mk_app(long context, long decl, long... args) {
    return mk_app(context, decl, args.length, args);
  }


  // PROPOSITIONAL LOGIC AND EQUALITY
  public static native long mk_true(long context);
  public static native long mk_false(long context);
  public static native long mk_eq(long context, long a1, long a2);
  private static native long mk_distinct(long context, int len, long[] as);
  public static native long mk_not(long context, long a1);
  public static native long mk_interp(long context, long a1);
  public static native long mk_ite(long context, long a1, long a2, long a3);
  public static native long mk_iff(long context, long a1, long a2);
  public static native long mk_implies(long context, long a1, long a2);
  public static native long mk_xor(long context, long a1, long a2);
  private static native long mk_and(long context, int len, long[] as);
  private static native long mk_or(long context, int len, long[] as);

  public static long mk_distinct(long context, long... as) {
    return mk_distinct(context, as.length, as);
  }

  public static long mk_and(long context, long... as) {
    return mk_and(context, as.length, as);
  }

  public static long mk_or(long context, long... as) {
    return mk_or(context, as.length, as);
  }

  // ARITHMETIC: INTEGERS AND REALS
  private static native long mk_add(long context, int len, long[] as);
  private static native long mk_mul(long context, int len, long[] as);
  private static native long mk_sub(long context, int len, long[] as);
  public static native long mk_unary_minus(long context, long a1);
  public static native long mk_div(long context, long a1, long a2);
  public static native long mk_mod(long context, long a1, long a2);
  public static native long mk_rem(long context, long a1, long a2);
  public static native long mk_power(long context, long a1, long a2);

  public static native long mk_lt(long context, long a1, long a2);
  public static native long mk_le(long context, long a1, long a2);
  public static native long mk_gt(long context, long a1, long a2);
  public static native long mk_ge(long context, long a1, long a2);
  public static native long mk_int2real(long context, long a1);
  public static native long mk_real2int(long context, long a1);
  public static native long mk_is_int(long context, long a1);

  public static long mk_add(long context, long... as) {
    return mk_add(context, as.length, as);
  }

  public static long mk_mul(long context, long... as) {
    return mk_mul(context, as.length, as);
  }

  public static long mk_sub(long context, long... as) {
    return mk_sub(context, as.length, as);
  }

  // BIT-VECTORS
  public static native long mk_bvnot(long context, long a1);
  public static native long mk_bvredand(long context, long a1);
  public static native long mk_bvredor(long context, long a1);
  public static native long mk_bvand(long context, long a1, long a2);
  public static native long mk_bvor(long context, long a1, long a2);
  public static native long mk_bvxor(long context, long a1, long a2);
  public static native long mk_bvnand(long context, long a1, long a2);
  public static native long mk_bvnor(long context, long a1, long a2);
  public static native long mk_bvxnor(long context, long a1, long a2);
  public static native long mk_bvneg(long context, long a1);
  public static native long mk_bvadd(long context, long a1, long a2);
  public static native long mk_bvsub(long context, long a1, long a2);
  public static native long mk_bvmul(long context, long a1, long a2);
  public static native long mk_bvudiv(long context, long a1, long a2);
  public static native long mk_bvsdiv(long context, long a1, long a2);
  public static native long mk_bvurem(long context, long a1, long a2);
  public static native long mk_bvsrem(long context, long a1, long a2);
  public static native long mk_bvsmod(long context, long a1, long a2);

  public static native long mk_bvult(long context, long a1, long a2);
  public static native long mk_bvslt(long context, long a1, long a2);
  public static native long mk_bvule(long context, long a1, long a2);
  public static native long mk_bvsle(long context, long a1, long a2);
  public static native long mk_bvuge(long context, long a1, long a2);
  public static native long mk_bvsge(long context, long a1, long a2);
  public static native long mk_bvugt(long context, long a1, long a2);
  public static native long mk_bvsgt(long context, long a1, long a2);
  public static native long mk_concat(long context, long a1, long a2);
  public static native long mk_extract(long context, int high, int low, long a1);
  public static native long mk_sign_ext(long context, int i, long a1);
  public static native long mk_zero_ext(long context, int i, long a1);
  public static native long mk_repeat(long context, int i, long a1);
  public static native long mk_bvshl(long context, long a1, long a2);
  public static native long mk_bvlshr(long context, long a1, long a2);
  public static native long mk_bvashr(long context, long a1, long a2);
  public static native long mk_rotate_left(long context, int i, long a1);
  public static native long mk_rotate_right(long context, int i, long a1);
  public static native long mk_ext_rotate_left(long context, long a1, long a2);
  public static native long mk_ext_rotate_right(long context, long a1, long a2);

  public static native long mk_bv2int(long context, long a1, boolean is_signed);
  public static native long mk_int2bv(long context, int len, long a1);

  public static native long mk_bvadd_no_overflow(long context, long a1, long a2, boolean is_signed);
  public static native long mk_bvadd_no_underflow(long context, long a1, long a2);
  public static native long mk_bvsub_no_overflow(long context, long a1, long a2);
  public static native long mk_bvsub_no_underflow(long context, long a1, long a2, boolean is_signed);
  public static native long mk_bvsdiv_no_overflow(long context, long a1, long a2);
  public static native long mk_bvneg_no_overflow(long context, long a1);
  public static native long mk_bvmul_no_overflow(long context, long a1, long a2, boolean is_signed);
  public static native long mk_bvmul_no_underflow(long context, long a1, long a2);


  // ARRAYS
  public static native long mk_select(long context, long a, long i);
  public static native long mk_store(long context, long a, long i, long v);
  public static native long mk_const_array(long context, long sort, long v);
  public static native long mk_map(long context, long func_decl, int n, long[] args);
  public static native long mk_array_default(long context, long a1);


  // SETS
  public static native long mk_set_sort(long context, long sort);
  public static native long mk_empty_set(long context, long sort);
  public static native long mk_full_set(long context, long sort);
  public static native long mk_set_add(long context, long a1, long a2);
  public static native long mk_set_del(long context, long a1, long a2);
  public static native long mk_set_union(long context, int len, long[] a1);
  public static native long mk_set_intersect(long context, int len, long[] a1);
  public static native long mk_set_difference(long context, long a1, long a2);
  public static native long mk_set_complement(long context, long a1);
  public static native long mk_set_member(long context, long a1, long a2);
  public static native long mk_set_subset(long context, long a1, long a2);


  // NUMERALS
  public static native long mk_numeral(long context, String s, long sort);
  public static native long mk_real(long context, int num, int den);
  public static native long mk_int(long context, int v, long sort);
  public static native long mk_unsigned_int(long context, int v, long sort);
  public static native long mk_int64(long context, long v, long sort);
  public static native long mk_unsigned_int64(long context, long v, long sort);


  // QUANTIFIERS
  public static native long mk_pattern(long context, int a1, long[] a2);
  public static native long mk_bound(long context, int a1, long a2);
  public static native long mk_forall(long context, int a1, int a2, long[] a3, int a4, long[] a5, long[] a6, long a7);
  public static native long mk_exists(long context, int a1, int a2, long[] a3, int a4, long[] a5, long[] a6, long a7);
  public static native long mk_quantifier(long context, boolean a1, int a2, int a3, long[] a4, int a5, long[] a6, long[] a7, long a8);
  public static native long mk_quantifier_ex(long context, boolean a1, int a2, long a3, long a4, int a5, long[] a6, int a7, long[] a8, int a9, long[] a10, long[] a11, long a12);
  public static native long mk_forall_const(long context, int a1, int a2, long[] a3, int a4, long[] a5, long a6);
  public static native long mk_exists_const(long context, int a1, int a2, long[] a3, int a4, long[] a5, long a6);
  public static native long mk_quantifier_const(long context, boolean a1, int a2, int a3, long[] a4, int a5, long[] a6, long a7);
  public static native long mk_quantifier_const_ex(long context, boolean a1, int a2, long a3, long a4, int a5, long[] a6, int a7, long[] a8, int a9, long[] a10, long a11);


  // ACCESSORS
  public static native int get_symbol_kind(long context, long a1);
  public static native int get_symbol_int(long context, long a1);
  public static native String get_symbol_string(long context, long a1);
  public static native long get_sort_name(long context, long a1);
  public static native int get_sort_id(long context, long a1);
  public static native long sort_to_ast(long context, long a1);
  public static native boolean is_eq_sort(long context, long a1, long a2);
  public static native int get_sort_kind(long context, long a1);

  public static native int get_bv_sort_size(long context, long sort);
  public static native boolean get_finite_domain_sort_size(long context, long a1, PointerToLong a2);
  public static native long get_array_sort_domain(long context, long a1);
  public static native long get_array_sort_range(long context, long a1);
  public static native long get_tuple_sort_mk_decl(long context, long a1);
  public static native int get_tuple_sort_num_fields(long context, long a1);
  public static native long get_tuple_sort_field_decl(long context, long a1, int a2);
  public static native int get_datatype_sort_num_constructors(long context, long a1);
  public static native long get_datatype_sort_constructor(long context, long a1, int a2);
  public static native long get_datatype_sort_recognizer(long context, long a1, int a2);
  public static native long get_datatype_sort_constructor_accessor(long context, long a1, int a2, int a3);
  public static native int get_relation_arity(long context, long a1);
  public static native long get_relation_column(long context, long a1, int a2);

  public static native long func_decl_to_ast(long context, long a1);
  public static native boolean is_eq_func_decl(long context, long a1, long a2);
  public static native int get_func_decl_id(long context, long a1);
  public static native long get_decl_name(long context, long a1);
  public static native int get_decl_kind(long context, long a1);
  public static native int get_domain_size(long context, long a1);
  public static native int get_arity(long context, long a1);
  public static native long get_domain(long context, long a1, int a2);

  public static native long get_range(long context, long a1);
  public static native int get_decl_num_parameters(long context, long a1);
  public static native int get_decl_parameter_kind(long context, long a1, int a2);
  public static native int get_decl_int_parameter(long context, long a1, int a2);
  public static native double get_decl_double_parameter(long context, long a1, int a2);
  public static native long get_decl_symbol_parameter(long context, long a1, int a2);
  public static native long get_decl_sort_parameter(long context, long a1, int a2);
  public static native long get_decl_ast_parameter(long context, long a1, int a2);
  public static native long get_decl_func_decl_parameter(long context, long a1, int a2);
  public static native String get_decl_rational_parameter(long context, long a1, int a2);

  public static native long app_to_ast(long context, long a1);
  public static native long get_app_decl(long context, long a1);
  public static native int get_app_num_args(long context, long a1);
  public static native long get_app_arg(long context, long a1, int index);
  public static native boolean is_eq_ast(long context, long a1, long a2);
  public static native int get_ast_id(long context, long a1);
  public static native int get_ast_hash(long context, long a1);
  public static native long get_sort(long context, long a1);
  public static native boolean is_well_sorted(long context, long a1);
  public static native int get_bool_value(long context, long a1);
  public static native int get_ast_kind(long context, long a1);
  public static native boolean is_app(long context, long a1);
  public static native boolean is_numeral_ast(long context, long a1);
  public static native boolean is_algebraic_number(long context, long a1);

  public static native long to_app(long context, long a1);
  public static native long to_func_decl(long context, long a1);

  public static native String get_numeral_string(long context, long a1);
  public static native String get_numeral_decimal_string(long context, long a1, int a2);
  public static native long get_numerator(long context, long a1);
  public static native long get_denominator(long context, long a1);

  public static native boolean get_numeral_small(long context, long a1, PointerToLong a2, PointerToLong a3);
  public static native boolean get_numeral_int(long context, long a1, PointerToInt a2);
  public static native boolean get_numeral_uint(long context, long a1, PointerToInt a2);
  public static native boolean get_numeral_uint64(long context, long a1, PointerToLong a2);
  public static native boolean get_numeral_int64(long context, long a1, PointerToLong a2);
  public static native boolean get_numeral_rational_int64(long context, long a1, PointerToLong a2, PointerToLong a3);
  public static native long get_algebraic_number_lower(long context, long a1, int a2);
  public static native long get_algebraic_number_upper(long context, long a1, int a2);

  public static native long pattern_to_ast(long context, long a1);
  public static native int get_pattern_num_terms(long context, long a1);
  public static native long get_pattern(long context, long a1, int a2);
  public static native int get_index_value(long context, long a1);
  public static native boolean is_quantifier_forall(long context, long a1);
  public static native int get_quantifier_weight(long context, long a1);
  public static native int get_quantifier_num_patterns(long context, long a1);
  public static native long get_quantifier_pattern_ast(long context, long a1, int a2);
  public static native int get_quantifier_num_no_patterns(long context, long a1);
  public static native long get_quantifier_no_pattern_ast(long context, long a1, int a2);
  public static native int get_quantifier_num_bound(long context, long a1);
  public static native long get_quantifier_bound_name(long context, long a1, int a2);
  public static native long get_quantifier_bound_sort(long context, long a1, int a2);
  public static native long get_quantifier_body(long context, long a1);

  public static native long simplify(long context, long a1);
  public static native long simplify_ex(long context, long a1, long a2);
  public static native String simplify_get_help(long context);
  public static native long simplify_get_param_descrs(long context);


  // MODIFIERS
  public static native long update_term(long context, long a1, int a2, long[] a3);
  public static native long substitute(long context, long a1, int a2, long[] a3, long[] a4);
  public static native long substitute_vars(long context, long a1, int a2, long[] a3);
  public static native long translate(long context, long a1, long a2);


  // MODELS
  public static native void model_inc_ref(long context, long model);
  public static native void model_dec_ref(long context, long model);
  public static native boolean model_eval(long context, long model, long a1, boolean model_completion, PointerToLong a2);
  public static native long model_get_const_interp(long context, long model, long funcDecl);
  public static native long model_get_func_interp(long context, long model, long funcDecl);
  public static native int model_get_num_consts(long context, long model);
  public static native long model_get_const_decl(long context, long model, int i);
  public static native int model_get_num_funcs(long context, long model);
  public static native long model_get_func_decl(long context, long model, int i);
  public static native int model_get_num_sorts(long context, long model);
  public static native long model_get_sort(long context, long model, int i);
  public static native long model_get_sort_universe(long context, long model, long sort);
  public static native boolean is_as_array(long context, long a1);
  public static native long get_as_array_func_decl(long context, long a1);

  public static native void func_interp_inc_ref(long context, long a1);
  public static native void func_interp_dec_ref(long context, long a1);
  public static native int func_interp_get_num_entries(long context, long a1);
  public static native long func_interp_get_entry(long context, long a1, int a2);
  public static native long func_interp_get_else(long context, long a1);
  public static native int func_interp_get_arity(long context, long a1);

  public static native void func_entry_inc_ref(long context, long a1);
  public static native void func_entry_dec_ref(long context, long a1);
  public static native long func_entry_get_value(long context, long a1);
  public static native int func_entry_get_num_args(long context, long a1);
  public static native long func_entry_get_arg(long context, long a1, int a2);


    // INTERACTION LOGGING
    public static native boolean open_log(String filename);
    public static native void append_log(String s);
    public static native void close_log();
    public static native void toggle_warning_messages(boolean enabled);


    // STRING CONVERSION
    public static native void set_ast_print_mode(long context, int mode);
    public static native String ast_to_string(long context, long ast);
    public static native String pattern_to_string(long context, long pattern);
    public static native String sort_to_string(long context, long sort);
    public static native String func_decl_to_string(long context, long func_decl);
    public static native String model_to_string(long context, long model);
    public static native String benchmark_to_smtlib_string(long context, String name, String logic, String status, String attributes, int len, long[] assumptions, long ast);


    // PARSER INTERFACE
    private static native long parse_smtlib2_string(long context, String str, int len1, long[] sort_symbols, long[] sorts, int len2, long[] decl_symbols, long[] decls);
    private static native long parse_smtlib2_file(long context, String filename, int len1, long[] sort_symbols, long[] sorts, int len2, long[] decl_symbols, long[] decls);
    private static native void parse_smtlib_string(long context, String str, int len1, long[] sort_symbols, long[] sorts, int len2, long[] decl_symbols, long[] decls);
    private static native void parse_smtlib_file(long context, String filename, int len1, long[] sort_symbols, long[] sorts, int len2, long[] decl_symbols, long[] decls);

    public static long parse_smtlib2_string(long context, String str, long[] sort_symbols, long[] sorts, long[] decl_symbols, long[] decls) {
      return parse_smtlib2_string(context, str, sort_symbols.length, sort_symbols, sorts, decl_symbols.length, decl_symbols, decls);
    }

    public static long parse_smtlib2_file(long context, String filename, long[] sort_symbols, long[] sorts, long[] decl_symbols, long[] decls) {
      return parse_smtlib2_file(context, filename, sort_symbols.length, sort_symbols, sorts, decl_symbols.length, decl_symbols, decls);
    }

    public static void parse_smtlib_string(long context, String str, long[] sort_symbols, long[] sorts, long[] decl_symbols, long[] decls) {
      parse_smtlib_string(context, str, sort_symbols.length, sort_symbols, sorts, decl_symbols.length, decl_symbols, decls);
    }

    public static void parse_smtlib_file(long context, String filename, long[] sort_symbols, long[] sorts, long[] decl_symbols, long[] decls) {
      parse_smtlib_string(context, filename, sort_symbols.length, sort_symbols, sorts, decl_symbols.length, decl_symbols, decls);
    }

    public static native int get_smtlib_num_formulas(long context);
    public static native long get_smtlib_formula(long context, int i);
    public static native int get_smtlib_num_assumptions(long context);
    public static native long get_smtlib_assumption(long context, int i);
    public static native int get_smtlib_num_decls(long context);
    public static native long get_smtlib_decl(long context, int i);
    public static native int get_smtlib_num_sorts(long context);
    public static native long get_smtlib_sort(long context, int i);

    public static native String get_smtlib_error(long context);


    // ERROR HANDLING
    // TODO set_error_handler()
    public static native int get_error_code(long context);
    public static native void set_error(long context, int error_code);
    public static native String get_error_msg(int error_code);
    public static native String get_error_msg_ex(long context, int error_code);


    // MISC
    public static native void get_version(PointerToInt major, PointerToInt minor, PointerToInt build, PointerToInt revision);
    public static native void enable_trace(String context);
    public static native void disable_trace(String context);
    public static native void reset_memory();


    // EXTERNAL
    // TODO do we need external functions?


    // FIXPOINT FACILITIES
    // TODO callbacks missing, do we need them?
    // TODO missing function: fp_init?
    public static native long mk_fixedpoint(long context);
    public static native void fixedpoint_inc_ref(long context, long a1);
    public static native void fixedpoint_dec_ref(long context, long a1);
    public static native void fixedpoint_add_rule(long context, long a1, long a2, long a3);
    public static native void fixedpoint_add_fact(long context, long a1, long a2, int a3, int[] a4);
    public static native void fixedpoint_assert(long context, long a1, long a2);
    public static native int fixedpoint_query(long context, long a1, long a2);
    public static native int fixedpoint_query_relations(long context, long a1, int a2, long[] a3);
    public static native long fixedpoint_get_answer(long context, long a1);
    public static native String fixedpoint_get_reason_unknown(long context, long a1);
    public static native void fixedpoint_update_rule(long context, long a1, long a2, long a3);
    public static native int fixedpoint_get_num_levels(long context, long a1, long a2);
    public static native long fixedpoint_get_cover_delta(long context, long a1, int a2, long a3);
    public static native void fixedpoint_add_cover(long context, long a1, int a2, long a3, long a4);
    public static native long fixedpoint_get_statistics(long context, long a1);
    public static native void fixedpoint_register_relation(long context, long a1, long a2);
    public static native void fixedpoint_set_predicate_representation(long context, long a1, long a2, int a3, long[] a4);
    public static native long fixedpoint_get_rules(long context, long a1);
    public static native long fixedpoint_get_assertions(long context, long a1);
    public static native void fixedpoint_set_params(long context, long a1, long a2);
    public static native String fixedpoint_get_help(long context, long a1);
    public static native long fixedpoint_get_param_descrs(long context, long a1);
    public static native String fixedpoint_to_string(long context, long a1, int a2, long[] a3);
    public static native long fixedpoint_from_string(long context, long a1, String a2);
    public static native long fixedpoint_from_file(long context, long a1, String a2);
    public static native void fixedpoint_push(long context, long a1);
    public static native void fixedpoint_pop(long context, long a1);


    // AST VECTORS
    public static native long mk_ast_vector(long context);
    public static native void ast_vector_inc_ref(long context, long ast_vector);
    public static native void ast_vector_dec_ref(long context, long ast_vector);
    public static native int ast_vector_size(long context, long ast_vector);
    public static native long ast_vector_get(long context, long ast_vector, int i);
    public static native void ast_vector_set(long context, long ast_vector, int i, long ast);
    public static native void ast_vector_resize(long context, long ast_vector, int num);
    public static native void ast_vector_push(long context, long ast_vector, long ast);
    public static native long ast_vector_translate(long context_source, long ast_vector, long context_target);
    public static native String ast_vector_to_string(long context, long ast_vector);


    // AST MAPS
    public static native long mk_ast_map(long context);
    public static native void ast_map_inc_ref(long context, long ast_map);
    public static native void ast_map_dec_ref(long context, long ast_map);
    public static native boolean ast_map_contains(long context, long ast_map, long ast);
    public static native long ast_map_find(long context, long ast_map, long ast);
    public static native void ast_map_insert(long context, long ast_map, long ast_key, long ast_value);
    public static native void ast_map_erase(long context, long ast_map, long ast_key);
    public static native void ast_map_reset(long context, long ast_map);
    public static native int ast_map_size(long context, long ast_map);
    public static native long ast_map_keys(long context, long ast_map);
    public static native String ast_map_to_string(long context, long ast_map);


    // GOALS
    public static native long mk_goal(long context, boolean models, boolean unsat_cores, boolean proofs);
    public static native void goal_inc_ref(long context, long goal);
    public static native void goal_dec_ref(long context, long goal);
    public static native int goal_precision(long context, long goal);
    public static native void goal_assert(long context, long goal, long ast);
    public static native boolean goal_inconsistent(long context, long goal);
    public static native int goal_depth(long context, long goal);
    public static native void goal_reset(long context, long goal);
    public static native int goal_size(long context, long goal);
    public static native long goal_formula(long context, long goal, int index);
    public static native int goal_num_exprs(long context, long goal);
    public static native boolean goal_is_decided_sat(long context, long goal);
    public static native boolean goal_is_decided_unsat(long context, long goal);
    public static native long goal_translate(long context_source, long goal, long context_target);
    public static native String goal_to_string(long context, long goal);


    // TACTICS AND PROBES
    public static native long mk_tactic(long context, String name);
    public static native void tactic_inc_ref(long context, long tactic);
    public static native void tactic_dec_ref(long context, long tactic);

    public static native long mk_probe(long context, String name);
    public static native void probe_inc_ref(long context, long probe);
  public static native void probe_dec_ref(long context, long probe);

  public static native long tactic_and_then(long context, long tactic1, long tactic2);
  public static native long tactic_or_else(long context, long tactic1, long tactic2);
  public static native long tactic_par_or(long context, int len, long[] tactics);
  public static native long tactic_par_and_then(long context, long tactic1, long tactic2);
  public static native long tactic_try_for(long context, long tactic, int milliseconds);
  public static native long tactic_when(long context, long probe, long tactic);
  public static native long tactic_cond(long context, long probe, long tactic1, long tactic2);
  public static native long tactic_repeat(long context, long tactic, int num_max);
  public static native long tactic_skip(long context);
  public static native long tactic_fail(long context);
  public static native long tactic_fail_if(long context, long probe);
  public static native long tactic_fail_if_not_decided(long context);
  public static native long tactic_using_params(long context, long probe, long params);

  public static native long probe_const(long context, double v);
  public static native long probe_lt(long context, long probe1, long probe2);
  public static native long probe_gt(long context, long probe1, long probe2);
  public static native long probe_le(long context, long probe1, long probe2);
  public static native long probe_ge(long context, long probe1, long probe2);
  public static native long probe_eq(long context, long probe1, long probe2);
  public static native long probe_and(long context, long probe1, long probe2);
  public static native long probe_or(long context, long probe1, long probe2);
  public static native long probe_not(long context, long probe1);

  public static native int get_num_tactics(long context);
  public static native String get_tactic_name(long context, int index);
  public static native int get_num_probes(long context);
  public static native String get_probe_name(long context, int index);

  public static native String tactic_get_help(long context, long tactic);
  public static native long tactic_get_param_descrs(long context, long tactic);
  public static native String tactic_get_descr(long context, String name);
  public static native String probe_get_descr(long context, String name);
  public static native double probe_apply(long context, long probe, long goal);
  public static native long tactic_apply(long context, long probe, long goal);
  public static native long tactic_apply_ex(long context, long tactic, long goal, long params);
  public static native void apply_result_inc_ref(long context, long apply_result);
  public static native void apply_result_dec_ref(long context, long apply_result);
  public static native String apply_result_to_string(long context, long apply_result);
  public static native int apply_result_get_num_subgoals(long context, long apply_result);
  public static native long apply_result_get_subgoal(long context, long apply_result, int i);
  public static native long apply_result_convert_model(long context, long apply_result, int i, long model);

  // SOLVERS
  public static native long mk_solver(long context);
  public static native long mk_simple_solver(long context);
  public static native long mk_solver_for_logic(long context, long logic);
  public static native long mk_solver_from_tactic(long context, long tactic);
  public static native String solver_get_help(long context, long solver);
  public static native long solver_get_param_descrs(long context, long solver);
  public static native void solver_set_params(long context, long solver, long params);
  public static native void solver_inc_ref(long context, long solver);
  public static native void solver_dec_ref(long context, long solver);
  public static native void solver_push(long context, long solver);
  public static native void solver_pop(long context, long solver, int number);
  public static native void solver_reset(long context, long solver);
  public static native int solver_get_num_scopes(long context, long solver);
  public static native void solver_assert(long context, long solver, long ast);
  public static native void solver_assert_and_track(long context, long solver, long ast, long p);
  public static native long solver_get_assertions(long context, long solver);
  public static native int solver_check(long context, long solver);
  public static native int solver_check_assumptions(long context, long solver, int len, long[] assumptions);
  public static native long solver_get_model(long context, long solver);
  public static native long solver_get_proof(long context, long solver);
  public static native long solver_get_unsat_core(long context, long solver);
  public static native String solver_get_reason_unknown(long context, long solver);
  public static native long solver_get_statistics(long context, long solver);
  public static native String solver_to_string(long context, long solver);


  // STATISTICS
  public static native String stats_to_string(long context, long stats);
  public static native void stats_inc_ref(long context, long stats);
  public static native void stats_dec_ref(long context, long stats);
  public static native int stats_size(long context, long stats);
  public static native String stats_get_key(long context, long stats, int i);
  public static native boolean stats_is_uint(long context, long stats, int i);
  public static native boolean stats_is_double(long context, long stats, int i);
  public static native int stats_get_uint_value(long context, long stats, int i);
  public static native double stats_get_double_value(long context, long stats, int i);

  // INTERPOLATION
  public static native long mk_interpolation_context(long config);
  private static native int interpolate(long context, int num, long[] cnsts, int[] parents, long options, long[] interps, PointerToLong model, PointerToLong labels, int incremental, int num_theory, long[] theory);
  private static native int interpolateSeq(long context, int num, long[] cnsts, long[] interps, PointerToLong model, PointerToLong labels, int incremental, int num_theory, long[] theory);

  public static native String interpolation_profile(long context);

  public static int interpolate(long context, long[] cnsts, int[] parents, long options, long[] interps, PointerToLong model, PointerToLong labels, int incremental, long[] theory) {
    return interpolate(context, cnsts.length, cnsts, parents, options, interps, model, labels, incremental, theory.length, theory);
  }

  public static int interpolateSeq(long context, long[] cnsts, long[] interps, PointerToLong model, PointerToLong labels, int incremental, long[] theory) {
    return interpolateSeq(context, cnsts.length, cnsts, interps, model, labels, incremental, theory.length, theory);
  }

}