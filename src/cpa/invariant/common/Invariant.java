/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.invariant.common;

import symbpredabstraction.interfaces.SymbolicFormula;

/**
 * Representation of an invariant formula talking about data
 * (not location). Immutable data structure.
 * @author g.theoduloz
 */
public class Invariant {
 
  private final InvariantSymbolicFormulaManager manager; 
  private final SymbolicFormula dischargeableAssumption;
  private final SymbolicFormula otherAssumption;
  
  public Invariant(SymbolicFormula dischargeable, SymbolicFormula rest)
  {
    manager = MathsatInvariantSymbolicFormulaManager.getInstance();
    dischargeableAssumption = dischargeable;
    otherAssumption = rest;
  }
  
  public Invariant(SymbolicFormula assumption, boolean isDischargeable)
  {
    manager = MathsatInvariantSymbolicFormulaManager.getInstance();
    dischargeableAssumption = isDischargeable ? assumption : manager.makeTrue();
    otherAssumption = isDischargeable ? manager.makeTrue() : assumption;
  }
  
  /** Constructs an invariant corresponding to true */
  public Invariant()
  {
    manager = MathsatInvariantSymbolicFormulaManager.getInstance();
    dischargeableAssumption = manager.makeTrue();
    otherAssumption = manager.makeTrue();
  }

  public SymbolicFormula getDischargeableAssumption() {
    return dischargeableAssumption;
  }
  
  public SymbolicFormula getOtherAssumption() {
    return otherAssumption;
  }
  
  /**
   * Return a formula representing all assumptions
   * contained in this invariant
   */
  public SymbolicFormula getAllAssumptions() {
    return manager.makeAnd(dischargeableAssumption, otherAssumption);
  }
  
  /**
   * Conjunct this invariant with an other invariant and
   * return the result
   */
  public Invariant and(Invariant other)
  {
    // shortcut
    if (this == TRUE)
      return other;
    else if (other == TRUE)
      return this;
      
    SymbolicFormula newDischargeable = manager.makeAnd(dischargeableAssumption, other.dischargeableAssumption);
    SymbolicFormula newOther = manager.makeAnd(dischargeableAssumption, other.dischargeableAssumption);
    return new Invariant(newDischargeable, newOther);
  }
  
  /**
   * Check whether an invariant is true
   */
  public boolean isTrue() {
    // shortcut
    if (this == TRUE)
      return true;
    
    return dischargeableAssumption.isTrue()
        && otherAssumption.isTrue();
  }
  
  public static final Invariant TRUE = new Invariant();
 
}
