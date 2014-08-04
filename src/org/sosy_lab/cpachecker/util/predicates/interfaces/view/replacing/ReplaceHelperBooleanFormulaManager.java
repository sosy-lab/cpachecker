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

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

class ReplaceHelperBooleanFormulaManager implements BooleanFormulaManager {

  private final ReplacingFormulaManager replaceManager;
  private final BooleanFormulaManager rawBooleanManager;

  public ReplaceHelperBooleanFormulaManager(
      ReplacingFormulaManager pReplacingFormulaManager,
      BooleanFormulaManager pBooleanFormulaManager) {
    this.replaceManager = pReplacingFormulaManager;
    this.rawBooleanManager = pBooleanFormulaManager;
  }

  @Override
  public boolean isBoolean(Formula pF) {
    return rawBooleanManager.isBoolean(replaceManager.unwrap(pF));
  }

  @Override
  public FormulaType<BooleanFormula> getFormulaType() {
    return rawBooleanManager.getFormulaType();
  }

  @Override
  public BooleanFormula makeBoolean(boolean pValue) {
    return rawBooleanManager.makeBoolean(pValue);
  }

  @Override
  public BooleanFormula makeVariable(String pVar) {
    return rawBooleanManager.makeVariable(pVar);
  }

  @Override
  public BooleanFormula equivalence(BooleanFormula pFormula1, BooleanFormula pFormula2) {
    return rawBooleanManager.equivalence(pFormula1, pFormula2);
  }

  @Override
  public boolean isTrue(BooleanFormula pFormula) {
    return rawBooleanManager.isTrue(pFormula);
  }

  @Override
  public boolean isFalse(BooleanFormula pFormula) {
    return rawBooleanManager.isFalse(pFormula);
  }

  @Override
  public <T extends Formula> T ifThenElse(BooleanFormula pCond, T pF1, T pF2) {
    Formula f1 = replaceManager.unwrap(pF1);
    Formula f2 = replaceManager.unwrap(pF2);

    return replaceManager.wrap(replaceManager.getFormulaType(pF1), rawBooleanManager.ifThenElse(pCond, f1, f2));
  }

  @Override
  public BooleanFormula not(BooleanFormula pBits) {
    return rawBooleanManager.not(pBits);
  }

  @Override
  public BooleanFormula and(BooleanFormula pBits1, BooleanFormula pBits2) {
    return rawBooleanManager.and(pBits1, pBits2);
  }

  @Override
  public BooleanFormula and(List<BooleanFormula> pBits) {
    return rawBooleanManager.and(pBits);
  }

  @Override
  public BooleanFormula or(BooleanFormula pBits1, BooleanFormula pBits2) {
    return rawBooleanManager.or(pBits1, pBits2);
  }

  @Override
  public BooleanFormula xor(BooleanFormula pBits1, BooleanFormula pBits2) {
    return rawBooleanManager.xor(pBits1, pBits2);
  }

  @Override
  public boolean isNot(BooleanFormula pBits) {
    return rawBooleanManager.isNot(pBits);
  }

  @Override
  public boolean isAnd(BooleanFormula pBits) {
    return rawBooleanManager.isAnd(pBits);
  }

  @Override
  public boolean isOr(BooleanFormula pBits) {
    return rawBooleanManager.isOr(pBits);
  }

  @Override
  public boolean isXor(BooleanFormula pBits) {
    return rawBooleanManager.isXor(pBits);
  }

  @Override
  public boolean isEquivalence(BooleanFormula pFormula) {
    return rawBooleanManager.isEquivalence(pFormula);
  }

  @Override
  public boolean isImplication(BooleanFormula pFormula) {
    return rawBooleanManager.isImplication(pFormula);
  }

  @Override
  public <T extends Formula> boolean isIfThenElse(T pF) {
    Formula f = replaceManager.unwrap(pF);
    return rawBooleanManager.isIfThenElse(f);
  }

}
