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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A simple pair of a BooleanFormula representing a set of program states and a CFANode representing
 * the program location for those states.
 *
 * <p>Instances of this class are immutable.
 */
public class StatesWithLocation {

  private final BooleanFormula formula;
  private final CFANode location;

  public StatesWithLocation(BooleanFormula pFormula, CFANode pLocation) {
    this.formula = Objects.requireNonNull(pFormula);
    this.location = Objects.requireNonNull(pLocation);
  }

  public BooleanFormula getFormula() {
    return formula;
  }

  public CFANode getLocation() {
    return location;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (!(pObj instanceof StatesWithLocation)) {
      return false;
    }
    StatesWithLocation other = (StatesWithLocation) pObj;
    return this.formula.equals(other.formula) && this.location.equals(other.location);
  }

  @Override
  public int hashCode() {
    return Objects.hash(formula, location);
  }
}
