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
/**
 *
 */
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * @author holzera
 *
 * This class implements a join operator according to the partial order
 * implemented in EqualityPartialOrder (flat lattice).
 */
public class EqualityJoinOperator implements JoinOperator {

  private final PartialOrder mPartialOrder;
  private final AbstractElement mTopElement;

  public EqualityJoinOperator(AbstractDomain pDomain, AbstractElement pTopElement) {
    assert(pDomain != null);

    this.mPartialOrder = pDomain.getPartialOrder();
    this.mTopElement = pTopElement;
  }

  @Override
  public AbstractElement join(AbstractElement element1, AbstractElement element2) throws CPAException {
    if (this.mPartialOrder.satisfiesPartialOrder(element1, element2)) {
      return element2;
    }

    if (this.mPartialOrder.satisfiesPartialOrder(element2, element1)) {
      return element1;
    }

    return this.mTopElement;
  }

}
