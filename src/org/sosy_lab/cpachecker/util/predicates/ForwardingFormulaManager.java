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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

public class ForwardingFormulaManager implements FormulaManager {

  private final FormulaManager delegate;

  public ForwardingFormulaManager(FormulaManager pDelegate) {
    delegate = pDelegate;
  }

  protected FormulaManager getDelegate() {
    return delegate;
  }

  @Override
  public boolean isBoolean(Formula pF) {
    return delegate.isBoolean(pF);
  }

  @Override
  public Formula makeTrue() {
    return delegate.makeTrue();
  }

  @Override
  public Formula makeFalse() {
    return delegate.makeFalse();
  }

  @Override
  public Formula makeNot(Formula pF) {
    return delegate.makeNot(pF);
  }

  @Override
  public Formula makeAnd(Formula pF1, Formula pF2) {
    return delegate.makeAnd(pF1, pF2);
  }

  @Override
  public Formula makeOr(Formula pF1, Formula pF2) {
    return delegate.makeOr(pF1, pF2);
  }

  @Override
  public Formula makeEquivalence(Formula pF1, Formula pF2) {
    return delegate.makeEquivalence(pF1, pF2);
  }

  @Override
  public Formula makeIfThenElse(Formula pCond, Formula pF1, Formula pF2) {
    return delegate.makeIfThenElse(pCond, pF1, pF2);
  }

  @Override
  public Formula makeNumber(int pI) {
    return delegate.makeNumber(pI);
  }

  @Override
  public Formula makeNumber(String pI) {
    return delegate.makeNumber(pI);
  }

  @Override
  public Formula makeNegate(Formula pF) {
    return delegate.makeNegate(pF);
  }

  @Override
  public Formula makePlus(Formula pF1, Formula pF2) {
    return delegate.makePlus(pF1, pF2);
  }

  @Override
  public Formula makeMinus(Formula pF1, Formula pF2) {
    return delegate.makeMinus(pF1, pF2);
  }

  @Override
  public Formula makeDivide(Formula pF1, Formula pF2) {
    return delegate.makeDivide(pF1, pF2);
  }

  @Override
  public Formula makeModulo(Formula pF1, Formula pF2) {
    return delegate.makeModulo(pF1, pF2);
  }

  @Override
  public Formula makeMultiply(Formula pF1, Formula pF2) {
    return delegate.makeMultiply(pF1, pF2);
  }

  @Override
  public Formula makeEqual(Formula pF1, Formula pF2) {
    return delegate.makeEqual(pF1, pF2);
  }

  @Override
  public Formula makeGt(Formula pF1, Formula pF2) {
    return delegate.makeGt(pF1, pF2);
  }

  @Override
  public Formula makeGeq(Formula pF1, Formula pF2) {
    return delegate.makeGeq(pF1, pF2);
  }

  @Override
  public Formula makeLt(Formula pF1, Formula pF2) {
    return delegate.makeLt(pF1, pF2);
  }

  @Override
  public Formula makeLeq(Formula pF1, Formula pF2) {
    return delegate.makeLeq(pF1, pF2);
  }

  @Override
  public Formula makeBitwiseNot(Formula pF) {
    return delegate.makeBitwiseNot(pF);
  }

  @Override
  public Formula makeBitwiseAnd(Formula pF1, Formula pF2) {
    return delegate.makeBitwiseAnd(pF1, pF2);
  }

  @Override
  public Formula makeBitwiseOr(Formula pF1, Formula pF2) {
    return delegate.makeBitwiseOr(pF1, pF2);
  }

  @Override
  public Formula makeBitwiseXor(Formula pF1, Formula pF2) {
    return delegate.makeBitwiseXor(pF1, pF2);
  }

  @Override
  public Formula makeShiftLeft(Formula pF1, Formula pF2) {
    return delegate.makeShiftLeft(pF1, pF2);
  }

  @Override
  public Formula makeShiftRight(Formula pF1, Formula pF2) {
    return delegate.makeShiftRight(pF1, pF2);
  }

  @Override
  public Formula makeUIF(String pName, FormulaList pArgs) {
    return delegate.makeUIF(pName, pArgs);
  }

  @Override
  public Formula makeUIF(String pName, FormulaList pArgs, int pIdx) {
    return delegate.makeUIF(pName, pArgs, pIdx);
  }

  @Override
  public Formula makeString(int pI) {
    return delegate.makeString(pI);
  }

  @Override
  public Formula makeVariable(String pVar, int pIdx) {
    return delegate.makeVariable(pVar, pIdx);
  }

  @Override
  public Formula makeVariable(String pVar) {
    return delegate.makeVariable(pVar);
  }

  @Override
  public Formula makePredicateVariable(String pVar, int pIdx) {
    return delegate.makePredicateVariable(pVar, pIdx);
  }

  @Override
  public Formula makeAssignment(Formula pF1, Formula pF2) {
    return delegate.makeAssignment(pF1, pF2);
  }

  @Override
  public FormulaList makeList(Formula pF) {
    return delegate.makeList(pF);
  }

  @Override
  public FormulaList makeList(Formula pF1, Formula pF2) {
    return delegate.makeList(pF1, pF2);
  }

  @Override
  public FormulaList makeList(List<Formula> pFs) {
    return delegate.makeList(pFs);
  }

  @Override
  public Formula parseInfix(String pS) throws IllegalArgumentException {
    return delegate.parseInfix(pS);
  }

  @Override
  public Formula parse(String pS) throws IllegalArgumentException {
    return delegate.parse(pS);
  }

  @Override
  public Formula instantiate(Formula pF, SSAMap pSsa) {
    return delegate.instantiate(pF, pSsa);
  }

  @Override
  @Deprecated
  public Formula uninstantiate(Formula pF) {
    return delegate.uninstantiate(pF);
  }

  @Override
  public Collection<Formula> extractAtoms(Formula pF,
      boolean pSplitArithEqualities, boolean pConjunctionsOnly) {
    return delegate.extractAtoms(pF, pSplitArithEqualities, pConjunctionsOnly);
  }

  @Override
  public Set<String> extractVariables(Formula pF) {
    return delegate.extractVariables(pF);
  }

  @Override
  public String dumpFormula(Formula pT) {
    return delegate.dumpFormula(pT);
  }

  @Override
  public Formula getBitwiseAxioms(Formula pF) {
    return delegate.getBitwiseAxioms(pF);
  }

  @Override
  public Formula createPredicateVariable(Formula pAtom) {
    return delegate.createPredicateVariable(pAtom);
  }

  @Override
  public Pair<Formula, Formula> splitBinOp(Formula pF) {
    return delegate.splitBinOp(pF);
  }

  @Override
  public boolean checkSyntacticEntails(Formula pLeftFormula, Formula pRightFormula) {
    return delegate.checkSyntacticEntails(pLeftFormula, pRightFormula);
  }

  @Override
  public Formula[] getArguments(Formula pF) {
    return delegate.getArguments(pF);
  }

  @Override
  public Formula makeUIP(String pName, FormulaList pArgs) {
    return delegate.makeUIP(pName, pArgs);
  }

  @Override
  public void declareUIP(String pName, int pArgCount) {
    delegate.declareUIP(pName, pArgCount);
  }

}
