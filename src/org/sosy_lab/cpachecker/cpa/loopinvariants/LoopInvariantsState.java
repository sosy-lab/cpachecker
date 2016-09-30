/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopinvariants;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.PolynomExpression;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class LoopInvariantsState
    implements AbstractState, LatticeAbstractState<LoopInvariantsState> {

  private static final LoopInvariantsState TOP = new LoopInvariantsState();

  private boolean inLoop;
  private boolean isLoopHead;

  private List<PolynomExpression> polynoms;



  private List<PolynomExpression> polynomsOutsideOfLoop;
  private HashMap<String, Double> variableValueMap;
  private List<Polynom> invariant;

  public LoopInvariantsState() {
    inLoop = false;
    polynoms = new LinkedList<>();
    polynomsOutsideOfLoop = new LinkedList<>();
    variableValueMap = new HashMap<>();
    invariant = null;
  }

  /**
   * @param pPolynoms the polynoms to set
   */
  public void setPolynoms(List<PolynomExpression> pPolynoms) {
    polynoms = pPolynoms;
  }

  /**
   * @param pIsLoopHead the isLoopHead to set
   */
  public void setLoopHead(boolean pIsLoopHead) {
    isLoopHead = pIsLoopHead;
  }

  @Override
  public boolean isLessOrEqual(LoopInvariantsState pOther)
      throws CPAException, InterruptedException {
    return this.equals(pOther);
  }

  public boolean getInLoop() {
    return inLoop;
  }

  public void setInLoop(boolean inloop) {
    this.inLoop = inloop;
  }


  /**
   * @return the isLoopHead
   */
  public boolean isLoopHead() {
    return isLoopHead;
  }

  public static LoopInvariantsState getTop() {
    return TOP;
  }

  //TODO überarbeiten
  @Override
  public LoopInvariantsState join(LoopInvariantsState pOther) {
    if (inLoop && pOther.inLoop) {
      LoopInvariantsState newState = new LoopInvariantsState();

      newState.polynoms.addAll(polynoms);
      newState.polynoms.addAll(pOther.polynoms);

      newState.variableValueMap.putAll(variableValueMap);
      newState.variableValueMap.putAll(pOther.variableValueMap);

      return newState;
    }

    return null;
  }

  public void setInvariant(List<Polynom> pPoly) {
    this.invariant = pPoly;
  }

  public List<Polynom> getInvariant() {
    return this.invariant;
  }

  public void addPolynom(PolynomExpression polynom) {
    polynoms.add(polynom);
  }

  public void addPolynomOutsideOfLoop(PolynomExpression polynom) {
    polynomsOutsideOfLoop.add(polynom);
  }

  public void addVariableValue(String quantifier, double value) {
    if (variableValueMap.get(quantifier) != null) {
      variableValueMap.replace(quantifier, value);
    } else {
      variableValueMap.put(quantifier, value);
    }
  }

  /**
   * @return the polynomies
   */
  public List<PolynomExpression> getPolynomies() {
    return polynoms;
  }

  /**
   * @return the variableValueMap
   */
  public HashMap<String, Double> getVariableValueMap() {
    return variableValueMap;
  }

  @SuppressWarnings("unchecked")
  public LoopInvariantsState copy() {
    LoopInvariantsState newState = new LoopInvariantsState();

    for (PolynomExpression polynom : polynoms) {
      newState.addPolynom(polynom);
    }

    for (PolynomExpression polynom : polynomsOutsideOfLoop) {
      newState.addPolynomOutsideOfLoop(polynom);
    }

    newState.variableValueMap = (HashMap<String, Double>) this.variableValueMap.clone();
    return newState;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LoopInvariantsState) {
      LoopInvariantsState oState = (LoopInvariantsState) obj;
      if (this.invariant != null && oState.invariant != null) {
        return this.invariant.toString().equals(oState.invariant.toString());
      }
    }
    return false;
  }
}
