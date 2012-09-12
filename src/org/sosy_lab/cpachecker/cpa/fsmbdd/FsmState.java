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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.VariableDeclarationException;
import org.sosy_lab.cpachecker.cpa.fsmbdd.interfaces.DomainIntervalProvider;

public class FsmState implements AbstractState {

  private final BDDFactory bddFactory;
  private final DomainIntervalProvider domainIntervalProvider;

  private BDD stateBdd;
  private List<CAssumeEdge> unencodedConditionSequence;

  private static Map<String, BDDDomain> declaredVariables = new HashMap<String, BDDDomain>();

  public FsmState(BDDFactory pBddFactory, DomainIntervalProvider pDomainIntervalProvider) {
    this.domainIntervalProvider = pDomainIntervalProvider;
    this.bddFactory = pBddFactory;
    this.stateBdd = bddFactory.one();
    this.unencodedConditionSequence = null;
  }

  public void setStateBdd(BDD bdd) {
    this.stateBdd = bdd;
  }

  public BDD getStateBdd() {
    return stateBdd;
  }

  public List<CAssumeEdge> getUnencodedConditions() {
    return unencodedConditionSequence;
  }

  public void addUnencodedCondition(CAssumeEdge assume) {
    if (unencodedConditionSequence == null) {
      unencodedConditionSequence = new ArrayList<CAssumeEdge>();
    }
    unencodedConditionSequence.add(assume);
  }

  public void clearUnencodedConditions() {
    this.unencodedConditionSequence = null;
  }

  public BDDDomain declareGlobal(String pVariableName, int pDomainSize) throws VariableDeclarationException {

    BDDDomain varDomain = declaredVariables.get(pVariableName);
    if (varDomain != null) {
      // TODO: Check if there is optimization potential.
      stateBdd = stateBdd.exist(varDomain.set());
    } else {
      varDomain = bddFactory.extDomain(pDomainSize);
      varDomain.setName(pVariableName);
      declaredVariables.put(pVariableName, varDomain);
    }

    return varDomain;
  }

  public void undefineVariable(String pScopedVariableName) {
    BDDDomain varDomain = declaredVariables.get(pScopedVariableName);
    stateBdd = stateBdd.exist(varDomain.set());
  }

  public BDDDomain getGlobalVariableDomain(String pVariableName) throws VariableDeclarationException {
    BDDDomain varDomain = declaredVariables.get(pVariableName);
    if (varDomain == null) {
      throw new VariableDeclarationException("Variable " + pVariableName + " not declared.");
    } else {
      return varDomain;
    }
  }

  public void addConjunctionWith(BDD bdd) {
    stateBdd = stateBdd.and(bdd);
  }

  public void doVariableAssignment(String pVariableName, CExpression pValue) throws VariableDeclarationException {
    if (unencodedConditionSequence != null) {
      throw new RuntimeException("UnencodedConditions must be encoded first!");
    }

    BDDDomain variableDomain = getGlobalVariableDomain(pVariableName);
    int literalIndex = domainIntervalProvider.mapLiteralToIndex(pValue);

    stateBdd = stateBdd.exist(bddFactory.makeSet(new BDDDomain[]{variableDomain}))
        .and(variableDomain.ithVar(literalIndex));
  }

  public FsmState cloneState() {
    FsmState result = new FsmState(bddFactory, domainIntervalProvider);

    result.stateBdd = this.stateBdd;

    if (unencodedConditionSequence != null) {
      result.unencodedConditionSequence = new ArrayList<CAssumeEdge>();
      result.unencodedConditionSequence.addAll(this.unencodedConditionSequence);
    }

    return result;
  }

  @Override
  public String toString() {
    int bddNodes = stateBdd.nodeCount();
    if (bddNodes > 200) {
      return String.format("BDD with %d nodes.", bddNodes);
    } else {
      return stateBdd.toStringWithDomains();
    }
  }


}
