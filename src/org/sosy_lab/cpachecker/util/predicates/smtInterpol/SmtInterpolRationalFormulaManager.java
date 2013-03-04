/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import static org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView.*;
import static org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolUtil.isNumber;

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractRationalFormulaManager;

import com.google.common.collect.ImmutableList;

import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;



public class SmtInterpolRationalFormulaManager extends AbstractRationalFormulaManager<Term> {
  private SmtInterpolEnvironment env;
  private SmtInterpolFormulaCreator creator;
  private SmtInterpolFunctionType<RationalFormula> multUfDecl;
  private SmtInterpolFunctionType<RationalFormula> divUfDecl;
  private SmtInterpolFunctionType<RationalFormula> modUfDecl;
  private SmtInterpolFunctionFormulaManager functionManager;

  public SmtInterpolRationalFormulaManager(
      SmtInterpolFormulaCreator pCreator,
      SmtInterpolFunctionFormulaManager functionManager) {
    super(
        pCreator);
    this.creator = pCreator;
    this.env = pCreator.getEnv();
    FormulaType<RationalFormula> formulaType = FormulaType.RationalType;
    this.functionManager = functionManager;
    multUfDecl = functionManager.createFunction(MultUfName, formulaType, formulaType, formulaType);
    divUfDecl = functionManager.createFunction(DivUfName, formulaType, formulaType, formulaType);
    modUfDecl = functionManager.createFunction(ModUfName, formulaType, formulaType, formulaType);

  }

  public static SmtInterpolRationalFormulaManager create(SmtInterpolFormulaCreator creator, SmtInterpolFunctionFormulaManager functionManager){
    return new SmtInterpolRationalFormulaManager(creator, functionManager);
  }

  @Override
  protected Term makeNumberImpl(long i) {
    return env.decimal(Long.toString(i));
  }

  @Override
  protected Term makeVariableImpl(String varName) {
    Sort t = creator.getNumberType();
    return creator.makeVariable(t, varName);
  }

  private Term makeUf(FunctionFormulaType<RationalFormula> decl, Term t1, Term t2) {
    return functionManager.createUninterpretedFunctionCallImpl(decl, ImmutableList.of(t1, t2));
  }

  private boolean isUf(SmtInterpolFunctionType<RationalFormula> funcDecl, Term pBits) {
    return functionManager.isUninterpretedFunctionCall(funcDecl, pBits);
  }

  @Override
  public Term negate(Term pNumber) {
    return env.term("*", env.numeral("-1"), pNumber);
  }

  @Override
  public boolean isNegate(Term pNumber) {
    boolean mult = isMultiply(pNumber);
    if (!mult) {
      return false;
    }
    Term arg = SmtInterpolUtil.getArg(pNumber, 0);
    if(SmtInterpolUtil.isNumber(arg)){
      // TODO: BUG: possible bug
      return SmtInterpolUtil.toNumber(arg) == -1;
    }
    return false;
  }

  @Override
  public Term add(Term pNumber1, Term pNumber2) {
    return env.term("+", pNumber1, pNumber2);
  }

  @Override
  public boolean isAdd(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, "+");
  }

  @Override
  public Term subtract(Term pNumber1, Term pNumber2) {
    return env.term("-", pNumber1, pNumber2);
  }

  @Override
  public boolean isSubtract(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, "-");
  }

  @Override
  public Term divide(Term pNumber1, Term pNumber2) {
    Term result;
    if (isNumber(pNumber2)) {
      result = env.term("/", pNumber1, pNumber2);
    } else {
      result = makeUf(divUfDecl, pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  public boolean isDivide(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, "/") || isUf(divUfDecl, pNumber);
  }

  @Override
  public Term modulo(Term pNumber1, Term pNumber2) {
    return makeUf(modUfDecl, pNumber1, pNumber2);
  }

  @Override
  public boolean isModulo(Term pNumber) {
    return isUf(modUfDecl, pNumber);
  }

  @Override
  public Term multiply(Term pNumber1, Term pNumber2) {
    Term result;
    if (isNumber(pNumber1) || isNumber(pNumber2)) { // TODO: both not numeral?
      result = env.term("*", pNumber1, pNumber2);
    } else {
      result = makeUf(multUfDecl, pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  public boolean isMultiply(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, "*") || isUf(multUfDecl, pNumber);
  }

  @Override
  public Term equal(Term pNumber1, Term pNumber2) {
    return env.term("=", pNumber1, pNumber2);
  }

  @Override
  public boolean isEqual(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, "=");
  }

  @Override
  public Term greaterThan(Term pNumber1, Term pNumber2) {
    return env.term(">", pNumber1, pNumber2);
  }

  @Override
  public boolean isGreaterThan(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, ">");
  }

  @Override
  public Term greaterOrEquals(Term pNumber1, Term pNumber2) {
    return env.term(">=", pNumber1, pNumber2);
  }

  @Override
  public boolean isGreaterOrEquals(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, ">=");
  }

  @Override
  public Term lessThan(Term pNumber1, Term pNumber2) {
    return env.term("<", pNumber1, pNumber2);
  }

  @Override
  public boolean isLessThan(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, "<");
  }

  @Override
  public Term lessOrEquals(Term pNumber1, Term pNumber2) {
    return env.term("<=", pNumber1, pNumber2);
  }

  @Override
  public boolean isLessOrEquals(Term pNumber) {
    return SmtInterpolUtil.isFunction(pNumber, "<=");
  }
}
