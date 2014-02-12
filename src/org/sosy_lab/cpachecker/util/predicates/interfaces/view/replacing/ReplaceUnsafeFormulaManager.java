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

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;

import com.google.common.base.Function;


public class ReplaceUnsafeFormulaManager implements UnsafeFormulaManager {


  private Function<FormulaType<?>, FormulaType<?>> unwrapTypes;
  private UnsafeFormulaManager rawUnsafeManager;
  private ReplacingFormulaManager replaceManager;

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

  @Override
  public int getArity(Formula pF) {
    return rawUnsafeManager.getArity(replaceManager.unwrap(pF));
  }

  @Override
  public Formula getArg(Formula pF, int pN) {
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
  public Formula replaceArgsAndName(Formula pF, String pNewName, Formula[] pArgs) {
    return rawUnsafeManager.replaceArgsAndName(
        replaceManager.unwrap(pF),
        pNewName,
        unwrapArgs(pArgs));
  }

  @Override
  public Formula replaceArgs(Formula pF, Formula[] pArgs) {
    return rawUnsafeManager.replaceArgs(
        replaceManager.unwrap(pF),
        unwrapArgs(pArgs));
  }

  @Override
  public Formula replaceName(Formula pF, String pNewName) {
    return rawUnsafeManager.replaceName(
        replaceManager.unwrap(pF),
        pNewName);
  }
}
