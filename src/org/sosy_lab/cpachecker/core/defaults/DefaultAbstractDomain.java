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
package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DefaultAbstractDomain implements AbstractDomain {

  private final JoinOperator mJoinOperator;
  private final PartialOrder mPartialOrder;

  public DefaultAbstractDomain(JoinOperator pJoinOperator,
      PartialOrder pPartialOrder) {
    mJoinOperator = checkNotNull(pJoinOperator);
    mPartialOrder = checkNotNull(pPartialOrder);
  }

  @Override
  public AbstractElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    return mJoinOperator.join(pElement1, pElement2);
  }

  @Override
  public boolean satisfiesPartialOrder(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    return mPartialOrder.satisfiesPartialOrder(pElement1, pElement2);
  }
}
