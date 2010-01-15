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
package cpa.invariant.assumptions;

import symbpredabstraction.interfaces.SymbolicFormula;
import cpa.common.interfaces.AbstractElement;
import cpa.invariant.common.InvariantSymbolicFormulaManager;
import cpa.invariant.dump.InvariantReportingElement;

/**
 * Abstract element for the assumption collector CPA;
 * encapsulate a symbolic formula that represents the
 * assumption.
 * 
 * @author g.theoduloz
 */
public class AssumptionCollectorElement implements AbstractElement, InvariantReportingElement {

  // The inner representation is a formula.
  private SymbolicFormula formula;
  private final InvariantSymbolicFormulaManager manager; 
  
  public AssumptionCollectorElement(
      InvariantSymbolicFormulaManager aManager,
      SymbolicFormula aFormula)
  {
    manager = aManager;
    formula = aFormula;
  }

  public SymbolicFormula getFormula()
  {
    return formula;
  }
  
  @Override
  public SymbolicFormula getInvariant()
  {
    return formula;
  }
  
  /**
   * @param other an other abstract element <b>with the same manager</b>
   * @return an abstract element representing the conjunction of
   *         the formula of this element with the other element
   */
  public AssumptionCollectorElement makeAnd(AssumptionCollectorElement other)
  {
    assert manager == other.manager;
    SymbolicFormula newFormula = manager.makeAnd(formula, other.formula);
    return new AssumptionCollectorElement(manager, newFormula);
  }
  
  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof AssumptionCollectorElement)
      return formula.equals(((AssumptionCollectorElement)pObj).formula);
    else
      return false;
  }
  
  @Override
  public String toString() {
    return formula.toString();
  }

}
