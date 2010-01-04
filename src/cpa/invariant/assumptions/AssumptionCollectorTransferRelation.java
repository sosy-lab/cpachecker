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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import symbpredabstraction.interfaces.SymbolicFormula;
import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.invariant.util.InvariantSymbolicFormulaManager;
import exceptions.CPATransferException;

/**
 * Transfer relation for the assumption collector. 
 * @author g.theoduloz
 */
public class AssumptionCollectorTransferRelation implements TransferRelation {

  /**
   * List of interfaces used to build the default
   * assumptions made by the model checker for
   * program operations.
   */ 
  protected final List<AssumptionBuilder> assumptionBuilders;
  
  /**
   * Register the default set of assumption builders.
   * Modify this method to register new kind of assumptions. 
   */
  private void registerDefaultAssumptionBuilders()
  {
    // arithmetic overflows
    assumptionBuilders.add(new ArithmeticOverflowAssumptionBuilder());
  }
  
  private InvariantSymbolicFormulaManager manager;
  
  /**
   * Constructor
   */
  public AssumptionCollectorTransferRelation(AssumptionCollectorCPA cpa)
  {
    manager = cpa.getSymbolicFormulaManager();
    assumptionBuilders = new LinkedList<AssumptionBuilder>();
    registerDefaultAssumptionBuilders();
  }
  
  private AbstractElement getAbstractSuccessor(AbstractElement el, CFAEdge edge, Precision p)
    throws CPATransferException
  {
    SymbolicFormula assumptions = manager.makeTrue();
    for (AssumptionBuilder b : assumptionBuilders)
    {
      assumptions = manager.makeAnd(assumptions, b.assumptionsForEdge(edge));
    }
    return new AssumptionCollectorElement(manager, assumptions);
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge cfaEdge)
    throws CPATransferException
  {
    return Collections.singleton(getAbstractSuccessor(pElement, cfaEdge, pPrecision));
  }

  @Override
  public AbstractElement strengthen(
      AbstractElement el, List<AbstractElement> otherElements,
      CFAEdge edge, Precision p)
    throws CPATransferException
  {
    // TODO Improve strengthening for assumptions so that they
    //      may be discharged online
    return null;
  }

}
