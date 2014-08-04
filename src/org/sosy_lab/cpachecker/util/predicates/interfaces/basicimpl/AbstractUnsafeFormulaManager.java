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

import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;

import com.google.common.base.Function;


public abstract class AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv> implements UnsafeFormulaManager {

  protected AbstractUnsafeFormulaManager(AbstractFormulaCreator<TFormulaInfo, TType, TEnv> creator) {
    super(creator);
  }

  private TFormulaInfo getTerm(Formula f) {
    return getFormulaCreator().extractInfo(f);
  }

  @Override
  public <T extends Formula> T typeFormula(FormulaType<T> type, Formula f) {
    TFormulaInfo formulaInfo = getTerm(f);

    return typeFormula(type, formulaInfo);
  }

  protected <T extends Formula> T typeFormula(FormulaType<T> type, TFormulaInfo formulaInfo) {
    FormulaCreator<TFormulaInfo> creator = getFormulaCreator();
    Class<T> clazz = type.getInterfaceType();
    return creator.encapsulate(clazz, formulaInfo);
  }

  private List<TFormulaInfo> toFormulaInfo(Formula[] formulas) {
    List<TFormulaInfo> returns = from(Arrays.asList(formulas))
        .transform(new Function<Formula, TFormulaInfo>() {
          @Override
          public TFormulaInfo apply(Formula pArg0) {
            return getTerm(pArg0);
          }}).toList();
    return returns;
  }

  protected Formula encapsulateUnsafe(TFormulaInfo pL) {
    return new AbstractFormula<>(pL);
  }

  protected List<TFormulaInfo> getArguments(TFormulaInfo pT) {
    int arity = getArity(pT);
    List<TFormulaInfo> rets = new ArrayList<>(arity);
    for (int i = 0; i < arity; i++) {
      rets.add(getArg(pT, i));
    }
    return rets;
  }

  @Override
  public boolean isAtom(Formula pF) {
    TFormulaInfo t = getTerm(pF);
    return isAtom(t);
  }

  protected abstract boolean isAtom(TFormulaInfo pT) ;

  @Override
  public int getArity(Formula pF) {
    TFormulaInfo t = getTerm(pF);
    return getArity(t);
  }
  protected abstract int getArity(TFormulaInfo pT) ;

  @Override
  public Formula getArg(Formula pF, int pN) {
    TFormulaInfo t = getTerm(pF);
    return encapsulateUnsafe(getArg(t, pN));
  }

  protected abstract TFormulaInfo getArg(TFormulaInfo pT, int n);

  @Override
  public boolean isVariable(Formula pF) {
    TFormulaInfo t = getTerm(pF);
    return isVariable(t);
  }

  protected abstract boolean isVariable(TFormulaInfo pT);

  @Override
  public boolean isNumber(Formula pF) {
    TFormulaInfo t = getTerm(pF);
    return isNumber(t);
  }

  protected abstract boolean isNumber(TFormulaInfo pT);

  @Override
  public boolean isUF(Formula pF) {
    TFormulaInfo t = getTerm(pF);
    return isUF(t);
  }

  protected abstract boolean isUF(TFormulaInfo pT);

  @Override
  public String getName(Formula pF) {

    TFormulaInfo t = getTerm(pF);
    return getName(t);
  }

  protected abstract String getName(TFormulaInfo pT);

  @Override
  public Formula replaceArgsAndName(Formula f, String newName, Formula[] args) {
    return encapsulateUnsafe(replaceArgsAndName(getTerm(f), newName, toFormulaInfo(args)));
  }

  protected TFormulaInfo replaceArgsAndName(TFormulaInfo pTerm, String pNewName, List<TFormulaInfo> newArgs) {
    TFormulaInfo withNewArgs = replaceArgs(pTerm, newArgs);
    return replaceName(withNewArgs, pNewName);
  }

  @Override
  public Formula replaceArgs(Formula pF, Formula[] pArgs) {
    return encapsulateUnsafe(replaceArgs(getTerm(pF), toFormulaInfo(pArgs)));
  }

  protected abstract TFormulaInfo replaceArgs(TFormulaInfo pT, List<TFormulaInfo> newArgs);

  @Override
  public Formula replaceName(Formula pF, String pNewName) {
    return encapsulateUnsafe(replaceName(getTerm(pF), pNewName));
  }

  protected abstract TFormulaInfo replaceName(TFormulaInfo pT, String newName);
}
