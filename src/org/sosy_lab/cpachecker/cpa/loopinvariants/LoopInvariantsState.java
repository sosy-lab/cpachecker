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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.PolynomExpression;

public class LoopInvariantsState implements AbstractState {

  private static final LoopInvariantsState TOP = new LoopInvariantsState();

  private boolean inLoop;
  private boolean isLoopHead;
  private List<PolynomExpression> polynomials;
  private List<PolynomExpression> polynomialsOutsideOfLoop;
  private Map<String, Double> variableValueMap;
  private List<Polynom> invariants;

  public LoopInvariantsState() {
    isLoopHead = false;
    inLoop = false;
    polynomials = new LinkedList<>();
    polynomialsOutsideOfLoop = new LinkedList<>();
    variableValueMap = new HashMap<>();
    invariants = null;
  }

  public LoopInvariantsState(boolean isInLoop, boolean isLoopHead,
      List<PolynomExpression> polynomials, List<PolynomExpression> polynomialsOutsideOfLoop,
      Map<String, Double> variableValueMap, List<Polynom> invariantList) {
    this.inLoop = isInLoop;
    this.isLoopHead = isLoopHead;
    this.polynomials = polynomials;
    this.polynomialsOutsideOfLoop = polynomialsOutsideOfLoop;
    this.variableValueMap = variableValueMap;
    this.invariants = invariantList;
  }

  /**
   * @return the polynomialsOutsideOfLoop
   */
  public List<PolynomExpression> getPolynomialsOutsideOfLoop() {
    return polynomialsOutsideOfLoop;
  }

  /**
   * @return if the state is part of a loop
   */
  public boolean getInLoop() {
    return inLoop;
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

  /**
   * @return invariants or null, if no invariant is available
   */
  public List<Polynom> getInvariant() {
    return this.invariants;
  }

  /**
   * @return the polynomies
   */
  public List<PolynomExpression> getPolynomials() {
    return polynomials;
  }

  /**
   * @return the variableValueMap
   */
  public Map<String, Double> getVariableValueMap() {
    return variableValueMap;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof LoopInvariantsState) {
      LoopInvariantsState oState = (LoopInvariantsState) obj;
      if (this.invariants != null && oState.invariants != null
          && this.invariants.toString().equals(oState.invariants.toString())
          && this.inLoop == oState.inLoop && this.isLoopHead == oState.isLoopHead
          && this.polynomials.equals(oState.polynomials)
          && this.polynomialsOutsideOfLoop.equals(oState.polynomialsOutsideOfLoop)
          && this.variableValueMap.equals(oState.variableValueMap)) { return true; }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.inLoop, this.isLoopHead, this.invariants, this.polynomials,
        this.polynomialsOutsideOfLoop, this.variableValueMap);
  }
}
