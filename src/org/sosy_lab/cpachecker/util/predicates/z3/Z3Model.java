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

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.counterexample.Model.Constant;
import org.sosy_lab.cpachecker.core.counterexample.Model.Function;
import org.sosy_lab.cpachecker.core.counterexample.Model.TermType;
import org.sosy_lab.cpachecker.core.counterexample.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.PointerToInt;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;

public class Z3Model {

  private final Z3FormulaManager mgr;
  private final long z3context;
  private final long z3solver;

  public Z3Model(Z3FormulaManager mgr, long z3context, long z3solver) {
    this.mgr = mgr;
    this.z3context = z3context;
    this.z3solver = z3solver;
    Preconditions.checkArgument(mgr.getEnvironment() == z3context);
  }

  private TermType toZ3Type(long sort) {
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

  private Constant toVariable(long expr) {
    long decl = get_app_decl(z3context, expr);
    long symbol = get_decl_name(z3context, decl);

    Preconditions.checkArgument(get_symbol_kind(z3context, symbol) == Z3_STRING_SYMBOL,
        "Given symbol of expression is no stringSymbol! (%s)", new LazyString(expr));

    String lName = get_symbol_string(z3context, symbol);
    long sort = get_sort(z3context, expr);
    TermType lType = toZ3Type(sort);

    Pair<String, Integer> lSplitName = FormulaManagerView.parseName(lName);
    if (lSplitName.getSecond() != null) {
      return new Variable(lSplitName.getFirst(), lSplitName.getSecond(), lType);
    } else {
      return new Constant(lSplitName.getFirst(), lType);
    }
  }


  private Function toFunction(long expr) {
    long decl = get_app_decl(z3context, expr);
    long symbol = get_decl_name(z3context, decl);

    Preconditions.checkArgument(get_symbol_kind(z3context, symbol) == Z3_STRING_SYMBOL,
        "Given symbol of expression is no stringSymbol! (%s)", new LazyString(expr));

    String lName = get_symbol_string(z3context, symbol);
    long sort = get_sort(z3context, expr);
    TermType lType = toZ3Type(sort);

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
        PointerToInt p = new PointerToInt();
        boolean check = get_numeral_int(z3context, arg, p);
        Preconditions.checkState(check);
        lValue = p.value;
        break;
      }
      case Z3_REAL_SORT: {
        long numerator = get_numerator(z3context, arg);
        long denominator = get_denominator(z3context, arg);
        BigInteger num = BigInteger.valueOf(numerator);
        BigInteger den = BigInteger.valueOf(denominator);
        lValue = num.divide(den);
        break;
      }
      case Z3_BV_SORT: {
        lValue = interpreteBitvector(arg);
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


  private AssignableTerm toAssignable(long expr) {
    Preconditions.checkArgument(is_app(z3context, expr),
        "Given expr is no application! (%s)", new LazyString(expr));

    if (get_app_num_args(z3context, expr) == 0) {
      return toVariable(expr);
    } else {
      return toFunction(expr);
    }
  }

  public Model createZ3Model() {
    // Preconditions.checkArgument(solver_check(z3context, z3solver) != Z3_L_FALSE,
    // "model is not available for UNSAT"); // TODO expensive check?

    long z3model = solver_get_model(z3context, z3solver);
    model_inc_ref(z3context, z3model);

    mgr.getSmtLogger().logGetModel();

    ImmutableMap.Builder<AssignableTerm, Object> model = ImmutableMap.builder();

    // TODO increment all ref-counters and decrement them later?
    long modelFormula = mk_true(z3context);
    inc_ref(z3context, modelFormula);

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

      long equivalence = mk_eq(z3context, var, value);
      inc_ref(z3context, equivalence);

      long newModelFormula = mk_and(z3context, modelFormula, equivalence);
      inc_ref(z3context, newModelFormula);

      AssignableTerm lAssignable = toAssignable(var);

      Object lValue;
      switch (lAssignable.getType()) {
      case Boolean:
        lValue = isOP(z3context, value, Z3_OP_TRUE); // if IS_TRUE, true, else false. TODO IS_UNKNOWN ?
        break;

      case Integer:
        PointerToInt p = new PointerToInt();
        boolean check = get_numeral_int(z3context, value, p);
        Preconditions.checkState(check);
        lValue = p.value;
        break;

      case Real:
        String s = get_numeral_string(z3context, value);
        lValue = ExtendedRational.ofString(s);
        break;

      case Bitvector:
        lValue = interpreteBitvector(value);
        break;

      default:
        throw new IllegalArgumentException("Z3 expr with unhandled type " + lAssignable.getType());
      }

      model.put(lAssignable, lValue);

      // cleanup outdated data
      dec_ref(z3context, keyDecl);
      dec_ref(z3context, value);
      dec_ref(z3context, var);
      dec_ref(z3context, equivalence);
      dec_ref(z3context, modelFormula);

      modelFormula = newModelFormula;
    }

    // TODO unused, remove and cleanup
    mgr.encapsulate(BooleanFormula.class, modelFormula);

    // cleanup
    model_dec_ref(z3context, z3model);
    return new Model(model.build());
  }

  /* INFO:
   * There are 2 representations for BVs, depending on the length:
   * (display (_ bv10 6)) -> #b001010, length=6
   * (display (_ bv10 8)) -> #x0a, length=8, 8 modulo 4 == 0
   */
  private Object interpreteBitvector(long bv) {
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
  private class LazyString {

    final long value;

    LazyString(long v) {
      value = v;
    }

    @Override
    public String toString() {
      return ast_to_string(z3context, value); // this could be an expensive operation!
    }
  }
}
