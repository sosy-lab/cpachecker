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
package org.sosy_lab.cpachecker.cpa.assumptions.collector;

import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * @author g.theoduloz
 */
public class AssumptionCollectorDomain implements AbstractDomain {
  
  private final AbstractElement top;
  private final AbstractElement bottom;
  
  public AssumptionCollectorDomain(AbstractDomain wrappedDomain) {
    top = new AssumptionCollectorElement(wrappedDomain.getTopElement(), AssumptionWithLocation.TRUE, false);
    bottom = new AssumptionCollectorElement(wrappedDomain.getBottomElement(), AssumptionWithLocation.TRUE, true);
  }
  
  @Override
  public AbstractElement getBottomElement() {
    return bottom;
  }

  @Override
  public JoinOperator getJoinOperator() {
    return new JoinOperator() {
      @Override
      public AbstractElement join(AbstractElement el1, AbstractElement el2)
        throws CPAException
      {
        if (el1 == el2)
          return el1;
        else
          return top;
      }
    };
  }

  @Override
  public PartialOrder getPartialOrder() {
    return new PartialOrder() {
      @Override
      public boolean satisfiesPartialOrder(AbstractElement el1, AbstractElement el2)
        throws CPAException
      {
        return (el1.equals(el2))
          || (top.equals(el2))
          || (bottom.equals(el1));
      }
    };
  }

  @Override
  public AbstractElement getTopElement() {
    return top;
  }
  
}
