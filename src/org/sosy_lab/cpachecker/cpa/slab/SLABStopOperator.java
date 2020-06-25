/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slab;

import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class SLABStopOperator implements StopOperator {

  private final AbstractDomain domain;

  public SLABStopOperator(AbstractDomain pDomain) {
    this.domain = pDomain;
  }
  @Override
  public boolean stop(AbstractState pState, Collection<AbstractState> pReached,
      Precision pPrecision) throws CPAException, InterruptedException {
    // Check if the argElement has only one parent and remember it for later:
    ARGState parent = null;
    if (((ARGState) pState).getParents().size() == 1) {
      parent = Iterables.get(((ARGState) pState).getParents(), 0);
    }

    for (AbstractState reachedState : pReached) {
      if (pState != reachedState && domain.isLessOrEqual(pState, reachedState)) {
        if (parent != null && ((ARGState) reachedState).getParents().contains(parent)) {
          ((ARGState) pState).removeFromARG();
        }
        return true;
      }
    }
    return false;
  }

}
