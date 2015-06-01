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

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

import java.math.BigInteger;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.core.counterexample.Model.Function;
import org.sosy_lab.cpachecker.util.predicates.TermType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

class Z3Model {
  private static TermType toZ3Type(long z3context, long sort) {
    int sortKind = get_sort_kind(z3context, sort);
    switch (sortKind) {
    case Z3_BOOL_SORT:
      return TermType.Boolean;
    case Z3_INT_SORT:
      return TermType.Integer;
    case Z3_REAL_SORT:
      return TermType.Real;
    case Z3_BV_SORT:
      return TermType.Bitvector;
    default:
      // TODO Uninterpreted;
      throw new IllegalArgumentException("Given parameter cannot be converted to a TermType!");
    }
  }

  private static Variable toVariable(long z3context, long expr) {
    long decl = get_app_decl(z3context, expr);
    long symbol = get_decl_name(z3context, decl);

    Preconditions.checkArgument(get_symbol_kind(z3context, symbol) == Z3_STRING_SYMBOL,
        "Given symbol of expression is no stringSymbol! (%s)", new LazyString(expr, z3context));

    String lName = get_symbol_string(z3context, symbol);
    long sort = get_sort(z3context, expr);
    TermType lType = toZ3Type(z3context, sort);
    return new Variable(lName, lType);
  }


  private static Function toFunction(long z3context, long expr) {
    long decl = get_app_decl(z3context, expr);
    long symbol = get_decl_name(z3context, decl);

    Preconditions.checkArgument(get_symbol_kind(z3context, symbol) == Z3_STRING_SYMBOL,
        "Given symbol of expression is no stringSymbol! (%s)", new LazyString(expr, z3context));

    String lName = get_symbol_string(z3context, symbol);
    long sort = get_sort(z3context, expr);
    TermType lType = toZ3Type(z3context, sort);

    int lArity = get_app_num_args(z3context, expr);

    // TODO we assume only constants (int/real) as parameters for now
    Object[] lArguments = new Object[lArity];
    for (int i = 0; i < lArity; i++) {
      long arg = get_app_arg(z3context, expr, i);
      inc_ref(z3context, arg);

      Object lValue;
      long argSort = get_sort(z3context, arg);
      int sortKind = get_sort_kind(z3context, argSort);
      switch (sortKind) {
      case Z3_INT_SORT: {
        lValue = new BigInteger(get_numeral_string(z3context, arg));
        break;
      }
      case Z3_REAL_SORT: {
        String s = get_numeral_string(z3context, arg);
        lValue = Rational.ofString(s);
        break;
      }
      case Z3_BV_SORT: {
        lValue = interpretBitvector(z3context, arg);
        break;
      }
      default:
        throw new IllegalArgumentException(
            "function " + ast_to_string(z3context, expr) + " with unhandled arg "
                + ast_to_string(z3context, arg));
      }

      dec_ref(z3context, arg);

      lArguments[i] = lValue;
    }

    return new Function(lName, lType, lArguments);
  }


  private static AssignableTerm toAssignable(long z3context, long expr) {
    Preconditions.checkArgument(is_app(z3context, expr),
        "Given expr is no application! (%s)", new LazyString(expr, z3context));

    if (get_app_num_args(z3context, expr) == 0) {
      return toVariable(z3context, expr);
    } else {
      return toFunction(z3context, expr);
    }
  }

  public static Model createZ3Model(Z3FormulaManager mgr, long z3context, long z3solver) {
    long z3model = solver_get_model(z3context, z3solver);
    return parseZ3Model(mgr, z3context, z3model);
  }

  public static Model parseZ3Model(
      Z3FormulaManager mgr,
      long z3context,
      long z3model) {
    mgr.getSmtLogger().logGetModel();
    return new Model(parseMapFromModel(z3context, z3model));
  }

  private static ImmutableMap<AssignableTerm, Object> parseMapFromModel(
      long z3context, long z3model
  ) {
    model_inc_ref(z3context, z3model);
    ImmutableMap.Builder<AssignableTerm, Object> model = ImmutableMap.builder();

    int n = model_get_num_consts(z3context, z3model);
    for (int i = 0; i < n; i++) {
      long keyDecl = model_get_const_decl(z3context, z3model, i);
      inc_ref(z3context, keyDecl);

      Preconditions.checkArgument(get_arity(z3context, keyDecl) == 0,
          "declaration is no constant");

      long var = mk_app(z3context, keyDecl);
      inc_ref(z3context, var);

      long value = model_get_const_interp(z3context, z3model, keyDecl);
      inc_ref(z3context, value);

      AssignableTerm lAssignable = toAssignable(z3context, var);

      Object lValue;
      switch (lAssignable.getType()) {
        case Boolean:
          lValue = isOP(z3context, value, Z3_OP_TRUE); // if IS_TRUE, true, else false. TODO IS_UNKNOWN ?
          break;

        case Integer:
          lValue = new BigInteger(get_numeral_string(z3context, value));
          break;

        case Real:
          String s = get_numeral_string(z3context, value);
          lValue = Rational.ofString(s);
          break;

        case Bitvector:
          lValue = interpretBitvector(z3context, value);
          break;

        default:
          throw new IllegalArgumentException("Z3 expr with unhandled type " + lAssignable.getType());
      }

      model.put(lAssignable, lValue);

      // cleanup outdated data
      dec_ref(z3context, keyDecl);
      dec_ref(z3context, value);
      dec_ref(z3context, var);
    }
    model_dec_ref(z3context, z3model);
    return model.build();
  }

  /* INFO:
   * There are 2 representations for BVs, depending on the length:
   * (display (_ bv10 6)) -> #b001010, length=6
   * (display (_ bv10 8)) -> #x0a, length=8, 8 modulo 4 == 0
   */
  private static Object interpretBitvector(long z3context, long bv) {
    long argSort = get_sort(z3context, bv);
    int sortKind = get_sort_kind(z3context, argSort);
    Preconditions.checkArgument(sortKind == Z3_BV_SORT);
    //    int size = get_bv_sort_size(z3context, argSort);

    return ast_to_string(z3context, bv);

    // TODO make BigInteger from BV? signed/unsigned?

    // next lines are not working, mk_bv2int can only handle short BVs (<31 bit)
    //    boolean isSigned = false;
    //    long numExpr = mk_bv2int(z3context, bv, isSigned);
    //    PointerToInt p = new PointerToInt();
    //    boolean check = get_numeral_int(z3context, numExpr, p);
    //    Preconditions.checkState(check);
    //    return p.value;
  }

  /** just a wrapper around a value,
   * this class allows to call the toString-method later. */
  private static class LazyString {

    final long value;
    final long z3context;

    LazyString(long v, long pZ3context) {
      value = v;
      z3context = pZ3context;
    }

    @Override
    public String toString() {
      return ast_to_string(z3context, value); // this could be an expensive operation!
    }
  }
}
