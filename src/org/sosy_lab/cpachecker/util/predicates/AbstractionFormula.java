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

import java.io.Serializable;

import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegion;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

/**
 * Instances of this class should hold a state formula (the result of an
 * abstraction computation) in several representations:
 * First, as an abstract region (usually this would be a BDD).
 * Second, as a symbolic formula.
 * Third, again as a symbolic formula, but this time all variables have names
 * which include their SSA index at the time of the abstraction computation.
 *
 * Additionally the formula for the block immediately before the abstraction
 * computation is stored (this also has SSA indices as it is a path formula,
 * even if it is not of the type PathFormula).
 *
 * Abstractions are not considered equal even if they have the same formula.
 */
public class AbstractionFormula implements Serializable {

  private static final long serialVersionUID = -7756517128231447936L;
  private transient final Region region;
  private final Formula formula;
  private final Formula instantiatedFormula;

  /**
   * The formula of the block directly before this abstraction.
   * (This formula was used to create this abstraction).
   */
  private final Formula blockFormula;

  private static int nextId = 0;
  private final int id = nextId++;

  public AbstractionFormula(Region pRegion, Formula pFormula,
      Formula pInstantiatedFormula, Formula pBlockFormula) {
    this.region = pRegion;
    this.formula = pFormula;
    this.instantiatedFormula = pInstantiatedFormula;
    this.blockFormula = pBlockFormula;
  }

  public boolean isTrue() {
    return formula.isTrue();
  }

  public boolean isFalse() {
    return formula.isFalse();
  }

  public Region asRegion() {
    return region;
  }

  /**
   * Returns the formula representation where all variables do not have SSA indices.
   */
  public Formula asFormula() {
    return formula;
  }

  /**
   * Returns the formula representation where all variables DO have SSA indices.
   */
  public Formula asInstantiatedFormula() {
    return instantiatedFormula;
  }

  public Formula getBlockFormula() {
    return blockFormula;
  }

  @Override
  public String toString() {
    // we print the formula only when using BDDs because it might be very very large otherwise
    return "ABS" + id + (region instanceof BDDRegion ? ": " + formula : "");
  }
}
