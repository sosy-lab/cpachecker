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
package cpa.invariant.controller;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import exceptions.CPAException;

/**
 * Domain for the analysis controller
 * @author g.theoduloz
 */
public class AnalysisControllerDomain implements AbstractDomain {

  private final AnalysisControllerElement bottom;
  private final AnalysisControllerElement top;
  
  public AnalysisControllerDomain(AnalysisControllerCPA a) {
    bottom = AnalysisControllerElement.getBottom(a);
    top = AnalysisControllerElement.getTop(a);
  }
  
  // Join is not supported
  private final JoinOperator joinOperator = new JoinOperator() {
    @Override
    public AbstractElement join(AbstractElement el1, AbstractElement el2) throws CPAException {
      return null;
    }
  };
  
  // Partial order: flat
  private final PartialOrder partialOrder = new PartialOrder() {
    @Override
    public boolean satisfiesPartialOrder(AbstractElement el1, AbstractElement el2) throws CPAException {
      return (el1.equals(bottom))
          || (((AnalysisControllerElement)el1).isLessThan((AnalysisControllerElement) el2));
    }
  };
  
  @Override
  public AbstractElement getBottomElement() {
    return bottom;
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
    return top;
  }

}
