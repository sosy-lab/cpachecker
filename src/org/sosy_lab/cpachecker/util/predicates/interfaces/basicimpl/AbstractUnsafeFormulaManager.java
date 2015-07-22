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
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;


public abstract class AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv> implements UnsafeFormulaManager {

  protected static class QuantifiedVariable {
    final FormulaType<?> variableType;
    final String nameInFormula;
    final int deBruijnIndex;

    public QuantifiedVariable(FormulaType<?> pVariableType, String pNameInFormula, int pDeBruijnIndex) {
      variableType = pVariableType;
      nameInFormula = pNameInFormula;
      deBruijnIndex = pDeBruijnIndex;
    }

    public String getDeBruijnName() {
      return "?" + deBruijnIndex;
    }
  }

  protected AbstractUnsafeFormulaManager(FormulaCreator<TFormulaInfo, TType, TEnv> creator) {
    super(creator);
  }

  private <T extends Formula> T encapsulateWithTypeOf(T f, TFormulaInfo e) {
    FormulaType<T> type = getFormulaCreator().getFormulaType(f);
    return typeFormula(type, e);
  }

  @Override
  public <T extends Formula> T typeFormula(FormulaType<T> type, Formula f) {
    TFormulaInfo formulaInfo = extractInfo(f);

    return typeFormula(type, formulaInfo);
  }

  final <T extends Formula> T typeFormula(FormulaType<T> type, TFormulaInfo formulaInfo) {
    return getFormulaCreator().encapsulate(type, formulaInfo);
  }

  @Override
  public boolean isAtom(Formula pF) {
    TFormulaInfo t = extractInfo(pF);
    return isAtom(t);
  }
  protected abstract boolean isAtom(TFormulaInfo pT) ;

  @Override
  public int getArity(Formula pF) {
    TFormulaInfo t = extractInfo(pF);
    return getArity(t);
  }
  protected abstract int getArity(TFormulaInfo pT) ;

  @Override
  public Formula getArg(Formula pF, int pN) {
    assert 0 <= pN && pN < getArity(pF) : String.format("index %d out of bounds %d", pN, getArity(pF));
    TFormulaInfo t = extractInfo(pF);
    TFormulaInfo arg = getArg(t, pN);
    return typeFormula(getFormulaCreator().getFormulaType(arg), arg);
  }

  protected abstract TFormulaInfo getArg(TFormulaInfo pT, int n);

  @Override
  public boolean isVariable(Formula pF) {
    TFormulaInfo t = extractInfo(pF);
    return isVariable(t);
  }

  protected abstract boolean isVariable(TFormulaInfo pT);

  @Override
  public boolean isFreeVariable(Formula pF) {
    TFormulaInfo t = extractInfo(pF);
    return isFreeVariable(t);
  }

  protected abstract boolean isFreeVariable(TFormulaInfo pT);

  @Override
  public boolean isBoundVariable(Formula pF) {
    TFormulaInfo t = extractInfo(pF);
    return isBoundVariable(t);
  }

  protected abstract boolean isBoundVariable(TFormulaInfo pT);

  @Override
  public boolean isQuantification(Formula pF) {
    TFormulaInfo t = extractInfo(pF);
    return isQuantification(t);
  }

  protected abstract boolean isQuantification(TFormulaInfo pT);

  @Override
  public BooleanFormula getQuantifiedBody(Formula pQuantifiedFormula) {
    Preconditions.checkArgument(isQuantification(pQuantifiedFormula));

    TFormulaInfo t = extractInfo(pQuantifiedFormula);
    TFormulaInfo result = getQuantifiedBody(t);

    return getFormulaCreator().encapsulateBoolean(result);
  }

  protected abstract TFormulaInfo getQuantifiedBody(TFormulaInfo pT);

  @Override
  public boolean isNumber(Formula pF) {
    TFormulaInfo t = extractInfo(pF);
    return isNumber(t);
  }

  @Override
  public BooleanFormula replaceQuantifiedBody(BooleanFormula pF, BooleanFormula pNewBody) {
    Preconditions.checkArgument(isQuantification(pF));

    TFormulaInfo f = extractInfo(pF);
    TFormulaInfo body = extractInfo(pNewBody);
    TFormulaInfo result = replaceQuantifiedBody(f, body);

    return getFormulaCreator().encapsulateBoolean(result);
  }

  protected abstract TFormulaInfo replaceQuantifiedBody(TFormulaInfo pF, TFormulaInfo pBody);

  protected abstract boolean isNumber(TFormulaInfo pT);

  @Override
  public boolean isUF(Formula pF) {
    TFormulaInfo t = extractInfo(pF);
    return isUF(t);
  }

  protected abstract boolean isUF(TFormulaInfo pT);

  @Override
  public String getName(Formula pF) {

    TFormulaInfo t = extractInfo(pF);
    return getName(t);
  }

  protected abstract String getName(TFormulaInfo pT);

  @Override
  public <T extends Formula> T replaceArgsAndName(T f, String newName, List<Formula> args) {
    return encapsulateWithTypeOf(f,
        replaceArgsAndName(extractInfo(f), newName, Lists.transform(args, extractor)));
  }

  protected abstract TFormulaInfo replaceArgsAndName(TFormulaInfo pT, String newName, List<TFormulaInfo> newArgs);

  @Override
  public <T extends Formula> T replaceArgs(T pF, List<Formula> pArgs) {
    assert pArgs.size() == getArity(pF) : "number of args must match arity.";
    return encapsulateWithTypeOf(pF, replaceArgs(extractInfo(pF), Lists.transform(pArgs, extractor)));
  }

  protected abstract TFormulaInfo replaceArgs(TFormulaInfo pT, List<TFormulaInfo> newArgs);

  @Override
  public <ResultFormulaType extends Formula, ParamFormulaType extends Formula>
    ResultFormulaType
    substitute(
      ResultFormulaType f,
      List<ParamFormulaType> changeFrom,
      List<ParamFormulaType> changeTo) {

    TFormulaInfo newExpression = substitute(
        getFormulaCreator().extractInfo(f),
        Lists.transform(changeFrom, extractor),
        Lists.transform(changeTo, extractor)
    );

    FormulaType<ResultFormulaType> type = getFormulaCreator().getFormulaType(f);
    return getFormulaCreator().encapsulate(type, newExpression);
  }

  @Override
  public <T extends Formula> List<T> splitNumeralEqualityIfPossible(final T pF) {
    return Lists.transform(
        splitNumeralEqualityIfPossible(extractInfo(pF)),
        new Function<TFormulaInfo, T>() {
          @Override
          public T apply(TFormulaInfo input) {
            return encapsulateWithTypeOf(pF, input);
          }
        }
    );
  }

  protected abstract List<? extends TFormulaInfo> splitNumeralEqualityIfPossible(TFormulaInfo pF);


  @Override
  public <T1 extends Formula, T2 extends Formula> T1 substitute(T1 pF, Map<T2, T2> pFromToMapping) {
    List<T2> fromList = Lists.newArrayList(pFromToMapping.keySet());
    List<T2> toList = Lists.newArrayList(pFromToMapping.values());

    return substitute(pF, fromList, toList);
  }

  protected abstract TFormulaInfo substitute(
      TFormulaInfo expr,
      List<TFormulaInfo> substituteFrom,
      List<TFormulaInfo> substituteTo);

  @Override
  public <T extends Formula> T simplify(T f) {
    return encapsulateWithTypeOf(f, simplify(extractInfo(f)));
  }

  protected TFormulaInfo simplify(TFormulaInfo f) {
    return f;
  }
}
