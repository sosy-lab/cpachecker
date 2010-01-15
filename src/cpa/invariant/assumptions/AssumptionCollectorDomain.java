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

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.invariant.common.InvariantSymbolicFormulaManager;
import exceptions.CPAException;

public class AssumptionCollectorDomain implements AbstractDomain {
  
  private final AssumptionCollectorElement bottomElement;
  private final AssumptionCollectorElement topElement;
  private final InvariantSymbolicFormulaManager manager;
  
  private final JoinOperator joinOperator =
    new JoinOperator() {
      @Override
      public AbstractElement join(AbstractElement el1, AbstractElement el2)
        throws CPAException {
        AssumptionCollectorElement iel1 = (AssumptionCollectorElement)el1;
        AssumptionCollectorElement iel2 = (AssumptionCollectorElement)el2;
        return iel1.makeAnd(iel2);
      }
    };
    
  private final PartialOrder partialOrder = 
    new PartialOrder() {
      @Override
      public boolean satisfiesPartialOrder(AbstractElement el1, AbstractElement el2)
        throws CPAException
      {
        if (el1.equals(bottomElement)) return true;
        if (el2.equals(topElement)) return true;
        return el1.equals(el2);
      }
    };
  
  public AssumptionCollectorDomain(AssumptionCollectorCPA aCPA)
  {
    manager = aCPA.getSymbolicFormulaManager();
    bottomElement = new AssumptionCollectorElement(manager, manager.makeFalse());
    topElement = new AssumptionCollectorElement(manager, manager.makeTrue());
  }
  
  @Override
  public AbstractElement getBottomElement() {
    return bottomElement;
  }

  @Override
  public JoinOperator getJoinOperator() {
    return joinOperator;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return partialOrder;
  }

  @Override
  public AbstractElement getTopElement() {
    return topElement;
  }

}
