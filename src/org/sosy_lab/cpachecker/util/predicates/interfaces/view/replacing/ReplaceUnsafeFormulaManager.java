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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view.replacing;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;

import com.google.common.base.Function;

class ReplaceUnsafeFormulaManager implements UnsafeFormulaManager {

  private final Function<FormulaType<?>, FormulaType<?>> unwrapTypes;
  private final UnsafeFormulaManager rawUnsafeManager;
  private final ReplacingFormulaManager replaceManager;

  public ReplaceUnsafeFormulaManager(
      ReplacingFormulaManager pReplacingFormulaManager,
      UnsafeFormulaManager pUnsafeFormulaManager,
      Function<FormulaType<?>, FormulaType<?>> unwrapTypes) {
    this.replaceManager = pReplacingFormulaManager;
    this.rawUnsafeManager = pUnsafeFormulaManager;
    this.unwrapTypes = unwrapTypes;
  }

  @Override
  public <T extends Formula> T typeFormula(FormulaType<T> pType, Formula pF) {
    FormulaType<?> unwrapped = unwrapTypes.apply(pType);
    return
       replaceManager.wrap(
           pType,
           rawUnsafeManager.typeFormula(unwrapped, replaceManager.unwrap(pF)));
  }

  private <T extends Formula> T encapsulateWithTypeOf(T wrapped, Formula unwrapped) {
    FormulaType<T> type = replaceManager.getFormulaType(wrapped);
    return replaceManager.wrap(type, unwrapped);
  }

  @Override
  public int getArity(Formula pF) {
    return rawUnsafeManager.getArity(replaceManager.unwrap(pF));
  }

  @Override
  public Formula getArg(Formula pF, int pN) {
    // TODO how to determine type of argument?
    // E.g., if it is a rational, it might either be a real rational
    // or a bitvector that was replaced with a rational.
    return rawUnsafeManager.getArg(replaceManager.unwrap(pF), pN);
  }

  @Override
  public boolean isAtom(Formula pF) {
    return rawUnsafeManager.isAtom(replaceManager.unwrap(pF));
  }

  @Override
  public boolean isVariable(Formula pF) {
    return rawUnsafeManager.isVariable(replaceManager.unwrap(pF));
  }

  @Override
  public boolean isNumber(Formula pTt) {
    return rawUnsafeManager.isNumber(replaceManager.unwrap(pTt));
  }

  @Override
  public boolean isUF(Formula pF) {
    return rawUnsafeManager.isUF(replaceManager.unwrap(pF));
  }

  @Override
  public String getName(Formula pF) {
    return rawUnsafeManager.getName(replaceManager.unwrap(pF));
  }

  private Formula[] unwrapArgs(Formula[] wrapped) {
    Formula[] unwrapped = new Formula[wrapped.length];
    for (int i = 0; i < unwrapped.length; i++) {
      unwrapped[i] = replaceManager.unwrap(wrapped[i]);
    }
    return unwrapped;
  }

  @Override
  public <T extends Formula> T replaceArgsAndName(T pF, String pNewName, Formula[] pArgs) {
    return encapsulateWithTypeOf(pF,
        rawUnsafeManager.replaceArgsAndName(
          replaceManager.unwrap(pF),
          pNewName,
          unwrapArgs(pArgs)));
  }

  @Override
  public <T extends Formula> T replaceArgs(T pF, Formula[] pArgs) {
    return encapsulateWithTypeOf(pF,
        rawUnsafeManager.replaceArgs(
          replaceManager.unwrap(pF),
          unwrapArgs(pArgs)));
  }

  @Override
  public <T extends Formula> T replaceName(T pF, String pNewName) {
    return encapsulateWithTypeOf(pF,
        rawUnsafeManager.replaceName(
          replaceManager.unwrap(pF),
          pNewName));
  }

  @Override
  public <T1 extends Formula, T2 extends Formula> T1
      substitute(T1 f, List<T2> changeFrom, List<T2> changeTo) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends Formula> T simplify(T pF) {
    return encapsulateWithTypeOf(pF,
        rawUnsafeManager.simplify(replaceManager.unwrap(pF)));
  }

}
