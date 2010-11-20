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
  public static final Assumption FALSE = new Assumption(manager.makeFalse());

  private final SymbolicFormula assumption;

  public Assumption(SymbolicFormula pAssumption) {
    assumption = pAssumption;
  }

  /** Constructs an invariant corresponding to true */
  public Assumption() {
    assumption = manager.makeTrue();
  }

  public SymbolicFormula getFormula() {
    return assumption;
  }

  /**
   * Conjunct this invariant with an other invariant and
   * return the result
   */
  public static Assumption and(Assumption one, Assumption other, SymbolicFormulaManager manager)
  {
    // shortcut
    if (one == TRUE)
      return other;
    else if (other == TRUE)
      return one;
    
    SymbolicFormula newFormula = manager.makeAnd(one.assumption, other.assumption);
    return new Assumption(newFormula);
  }

  /**
   * Check whether an invariant is true
   */
  public boolean isTrue() {
    // shortcut
    if (this == TRUE)
      return true;
    else
      return assumption.isTrue();
  }

  public boolean isFalse() {
    if (this == FALSE)
      return true;
    else if (this == TRUE)
      return false;
    else
      return assumption.isFalse();
  }
  
  @Override
  public String toString() {
    return assumption.toString();
  }
  
  @Override
  public int hashCode() {
    return assumption.hashCode();
  }  
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof Assumption)) {
      return false;
    } else {
      Assumption other = (Assumption)obj;
      return assumption.equals(other.assumption);
    }
  }
  
}
