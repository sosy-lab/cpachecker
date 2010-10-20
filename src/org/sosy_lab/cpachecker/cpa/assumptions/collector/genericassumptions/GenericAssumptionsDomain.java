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

package org.sosy_lab.cpachecker.cpa.assumptions.collector.genericassumptions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class GenericAssumptionsDomain implements AbstractDomain {

  private final JoinOperator joinOperator =
    new JoinOperator() {
      @Override
      public AbstractElement join(AbstractElement el1, AbstractElement el2)
        throws CPAException {
        GenericAssumptionsElement iel1 = (GenericAssumptionsElement)el1;
        GenericAssumptionsElement iel2 = (GenericAssumptionsElement)el2;
        return iel1.makeAnd(iel2);
      }
    };

  private final PartialOrder partialOrder =
    new PartialOrder() {
      @Override
      public boolean satisfiesPartialOrder(AbstractElement el1, AbstractElement el2)
        throws CPAException
      {
        if (GenericAssumptionsElement.TOP.equals(el2)) return true;
        return el1.equals(el2);
      }
    };

  public GenericAssumptionsDomain(GenericAssumptionsCPA aCPA)
  {
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
    return GenericAssumptionsElement.TOP;
  }

}
