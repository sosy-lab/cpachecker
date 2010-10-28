/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.assumptions;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;

/**
 * Representation of an assumption formula talking about data
 * (not location). Immutable data structure.
 * @author g.theoduloz
 */
public class Assumption {

  private static SymbolicFormulaManager manager = AssumptionSymbolicFormulaManagerImpl.getSymbolicFormulaManager();
  {
    // hopefully we have an instance already
    assert manager != null;
  }

  public static final Assumption TRUE = new Assumption();
  public static final Assumption FALSE = new Assumption(manager.makeFalse(), false);

  private final SymbolicFormula dischargeableAssumption;
  private final SymbolicFormula otherAssumption;

  public Assumption(SymbolicFormula dischargeable, SymbolicFormula rest)
  {
    dischargeableAssumption = dischargeable;
    otherAssumption = rest;
  }

  public Assumption(SymbolicFormula assumption, boolean isDischargeable)
  {
    dischargeableAssumption = isDischargeable ? assumption : manager.makeTrue();
    otherAssumption = isDischargeable ? manager.makeTrue() : assumption;
  }

  /** Constructs an invariant corresponding to true */
  public Assumption()
  {
    dischargeableAssumption = manager.makeTrue();
    otherAssumption = manager.makeTrue();
  }

  public SymbolicFormula getDischargeableFormula() {
    return dischargeableAssumption;
  }

  public SymbolicFormula getOtherFormula() {
    return otherAssumption;
  }

  /**
   * Return a formula representing all assumptions
   * contained in this invariant
   */
  public SymbolicFormula getAllFormula() {
    return manager.makeAnd(dischargeableAssumption, otherAssumption);
  }

  /**
   * Conjunct this invariant with an other invariant and
   * return the result
   */
  public Assumption and(Assumption other)
  {
    // shortcut
    if (this == TRUE)
      return other;
    else if (other == TRUE)
      return this;
    
    SymbolicFormula newDischargeable = manager.makeAnd(dischargeableAssumption, other.dischargeableAssumption);
    SymbolicFormula newOther = manager.makeAnd(otherAssumption, other.otherAssumption);
    return new Assumption(newDischargeable, newOther);
  }

  /**
   * Check whether an invariant is true
   */
  public boolean isTrue() {
    // shortcut
    if (this == TRUE)
      return true;
    else
      return dischargeableAssumption.isTrue()
      && otherAssumption.isTrue();
  }

  public boolean isFalse() {
    if (this == FALSE)
      return true;
    else if (this == TRUE)
      return false;
    else
      return dischargeableAssumption.isFalse()
      || otherAssumption.isFalse();
  }
  
  @Override
  public String toString() {
    return "Formula: " + dischargeableAssumption;
  }
  
  @Override
  public int hashCode() {
    return (31 + dischargeableAssumption.hashCode()) * 31 + otherAssumption.hashCode();
  }  
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof Assumption)) {
      return false;
    } else {
      Assumption other = (Assumption)obj;
      return dischargeableAssumption.equals(other.dischargeableAssumption) && 
              otherAssumption.equals(other.otherAssumption);
    }
  }
  
}
