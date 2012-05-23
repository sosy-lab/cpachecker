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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class BDDElement implements AbstractElement {

  private Region currentState;
  private final NamedRegionManager manager;
  private Set<String> currentVars;
  private BDDElement functionCallElement;
  private String functionName;

  public BDDElement(NamedRegionManager mgr, BDDElement functionCallElement,
      Region state, Set<String> vars, String functionName) {
    this.currentState = state;
    this.currentVars = vars;
    this.functionCallElement = functionCallElement;
    this.functionName = functionName;
    this.manager = mgr;
  }

  public Region getRegion() {
    return currentState;
  }

  public Set<String> getVars() {
    return currentVars;
  }

  public BDDElement getFunctionCallElement() {
    return functionCallElement;
  }

  public String getFunctionName() {
    return functionName;
  }

  public boolean isLessOrEqual(BDDElement other) {
    assert this.functionName.equals(other.functionName) : "same function needed: "
        + this.functionName + " vs " + other.functionName;

    return manager.entails(this.currentState, other.currentState);
  }

  public BDDElement join(BDDElement other) {
    assert this.functionName.equals(other.functionName) : "same function needed: "
        + this.functionName + " vs " + other.functionName;
    this.currentVars.addAll(other.currentVars); // some vars more make no difference

    Region result = manager.makeOr(this.currentState, other.currentState);
    if (result.equals(this.currentState)) {
      return this;
    } else if (result.equals(other.currentState)) {
      return other;
    } else {
      return new BDDElement(this.manager, this.functionCallElement, result,
          this.currentVars, this.functionName);
    }
  }

  @Override
  public String toString() {
    return manager.dumpRegion(currentState);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BDDElement) {
      BDDElement other = (BDDElement) o;
      return this.functionName.equals(other.functionName) &&
          this.currentState.equals(other.currentState);
    }
    return false;
  }
}