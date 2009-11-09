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
package cpa.invariant.dump;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import exceptions.CPAException;

/**
 * @author g.theoduloz
 */
public class DumpInvariantDomain implements AbstractDomain {

  @Override
  public AbstractElement getBottomElement() {
    return DumpInvariantElement.BOTTOM;
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
          return DumpInvariantElement.TOP;
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
        return (el1 == el2)
          || (el1 == DumpInvariantElement.BOTTOM)
          || (el2 == DumpInvariantElement.TOP);
      }
    };
  }

  @Override
  public AbstractElement getTopElement() {
    return DumpInvariantElement.TOP;
  }
  
}
