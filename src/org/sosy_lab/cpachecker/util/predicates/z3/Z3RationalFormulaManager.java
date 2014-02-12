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

import static org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractRationalFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.PointerToInt;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class Z3RationalFormulaManager extends AbstractRationalFormulaManager<Long> {

  private final long z3context;
  private final Z3FormulaCreator creator;
  private final Z3FunctionType<RationalFormula> multUfDecl;
  private final Z3FunctionType<RationalFormula> divUfDecl;
  private final Z3FunctionType<RationalFormula> modUfDecl;
  private final Z3FunctionFormulaManager functionManager;

  public Z3RationalFormulaManager(
      Z3FormulaCreator pCreator,
      Z3FunctionFormulaManager functionManager) {
    super(pCreator);
    this.creator = pCreator;
    this.z3context = pCreator.getEnv();
    FormulaType<RationalFormula> formulaType = FormulaType.RationalType;
    this.functionManager = functionManager;
    multUfDecl = functionManager.createFunction(MultUfName, formulaType, formulaType, formulaType);
    divUfDecl = functionManager.createFunction(DivUfName, formulaType, formulaType, formulaType);
    modUfDecl = functionManager.createFunction(ModUfName, formulaType, formulaType, formulaType);
  }

  @Override
  protected Long makeNumberImpl(long i) {
    long sort = creator.getNumberType();
    return mk_int64(z3context, i, sort);
  }

  @Override
  protected Long makeNumberImpl(BigInteger pI) {
    return makeNumberImpl(pI.toString());
  }

  @Override
  protected Long makeNumberImpl(String pI) {
    long sort = creator.getNumberType();
    return mk_numeral(z3context, pI, sort);
  }

  @Override
  protected Long makeVariableImpl(String varName) {
    long type = creator.getNumberType();
    return creator.makeVariable(type, varName);
  }

  private Long makeUf(FunctionFormulaType<RationalFormula> decl, Long t1, Long t2) {
    return functionManager.createUninterpretedFunctionCallImpl(decl, ImmutableList.of(t1, t2));
  }

  private boolean isUf(Z3FunctionType<RationalFormula> funcDecl, Long pBits) {
    return functionManager.isUninterpretedFunctionCall(funcDecl, pBits);
  }

  @Override
  public Long negate(Long pNumber) {
    long sort = get_sort(z3context, pNumber);
    long minusOne = mk_int(z3context, -1, sort);
    return mk_mul(z3context, minusOne, pNumber);
  }

  @Override
  public boolean isNegate(Long pNumber) {
    boolean mult = isMultiply(pNumber);
    if (!mult) { return false; }
    long arg = get_app_arg(z3context, pNumber, 0);
    if (is_numeral_ast(z3context, arg)) {
      long sort = get_sort(z3context, arg);
      int sortKind = get_sort_kind(z3context, sort);
      switch (sortKind) {
      case Z3_INT_SORT: {
        PointerToInt p = new PointerToInt();
        boolean check = get_numeral_int(z3context, arg, p);
        Preconditions.checkState(check);
        return p.value == -1;
      }
      case Z3_REAL_SORT: {
        long numerator = get_numerator(z3context, arg);
        long denominator = get_denominator(z3context, arg);
        return (numerator == -denominator); // (a/b==-1) <--> (a==-b)
      }
      default:
        return false;
      }
    }
    return false;
  }

  @Override
  public Long add(Long pNumber1, Long pNumber2) {
    return mk_add(z3context, pNumber1, pNumber2);
  }

  @Override
  public boolean isAdd(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_ADD);
  }

  @Override
  public Long subtract(Long pNumber1, Long pNumber2) {
    return mk_sub(z3context, pNumber1, pNumber2);
  }

  @Override
  public boolean isSubtract(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_SUB);
  }

  @Override
  public Long divide(Long pNumber1, Long pNumber2) {
    long result;
    if (is_numeral_ast(z3context, pNumber2)) {
      result = mk_div(z3context, pNumber1, pNumber2);
    } else {
      result = makeUf(divUfDecl, pNumber1, pNumber2);
    }
    return result;
  }

  @Override
  public boolean isDivide(Long pNumber) {
    if (isOP(z3context, pNumber, Z3_OP_DIV)) { return true; }
    long decl = get_app_decl(z3context, pNumber);
    return is_eq_func_decl(z3context, decl, divUfDecl.getFuncDecl());
  }

  @Override
  public Long modulo(Long pNumber1, Long pNumber2) {
    return makeUf(modUfDecl, pNumber1, pNumber2);
  }

  @Override
  public boolean isModulo(Long pNumber) {
    return isUf(modUfDecl, pNumber);
  }

  @Override
  public Long multiply(Long pNumber1, Long pNumber2) {
    long result;
    if (is_numeral_ast(z3context, pNumber1) || is_numeral_ast(z3context, pNumber2)) {
      result = mk_mul(z3context, pNumber1, pNumber2);
    } else {
      result = makeUf(multUfDecl, pNumber1, pNumber2);
    }
    return result;
  }

  @Override
  public boolean isMultiply(Long pNumber) {
    if (isOP(z3context, pNumber, Z3_OP_MUL)) { return true; }
    long decl = get_app_decl(z3context, pNumber);
    return is_eq_func_decl(z3context, decl, multUfDecl.getFuncDecl());
  }

  @Override
  public Long equal(Long pNumber1, Long pNumber2) {
    return mk_eq(z3context, pNumber1, pNumber2);
  }

  @Override
  public boolean isEqual(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_EQ);
  }

  @Override
  public Long greaterThan(Long pNumber1, Long pNumber2) {
    return mk_gt(z3context, pNumber1, pNumber2);
  }

  @Override
  public boolean isGreaterThan(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_GT);
  }

  @Override
  public Long greaterOrEquals(Long pNumber1, Long pNumber2) {
    return mk_ge(z3context, pNumber1, pNumber2);
  }

  @Override
  public boolean isGreaterOrEquals(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_GE);
  }

  @Override
  public Long lessThan(Long pNumber1, Long pNumber2) {
    return mk_lt(z3context, pNumber1, pNumber2);
  }

  @Override
  public boolean isLessThan(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_LT);
  }

  @Override
  public Long lessOrEquals(Long pNumber1, Long pNumber2) {
    return mk_le(z3context, pNumber1, pNumber2);
  }

  @Override
  public boolean isLessOrEquals(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_LE);
  }
}
