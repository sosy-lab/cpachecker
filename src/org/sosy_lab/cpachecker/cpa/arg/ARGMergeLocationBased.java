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
package org.sosy_lab.cpachecker.cpa.arg;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;


/**
 * MergeOperator for slicing abstractions.
 * To be used together with {@link ARGStopJoin}
 */
public class ARGMergeLocationBased implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    CFANode cfaNode1 = extractLocation(pState1);
    CFANode cfaNode2 = extractLocation(pState2);
    if (cfaNode1.equals(cfaNode2) && pState1 != pState2) {
      ARGState mergedElement = (ARGState) pState2;
      ARGState argElement1 = (ARGState) pState1;
      mergedElement.setForceMerge();

      // replace argElement1 with it
      for (ARGState parentOfElement1 : argElement1.getParents()) {
        mergedElement.addParent(parentOfElement1);
      }

      // argElement1 is the current successor, it does not have any children and covered nodes yet
      assert argElement1.getChildren().isEmpty();
      assert argElement1.getCoveredByThis().isEmpty();

      // ARGElement1 will only be removed from ARG if stop(e1, reached) returns true.
      // So we can't actually remove it now, but we need to remember this later.
      argElement1.setMergedWith(mergedElement);

    }
    // we always return pState2
    return pState2;
  }
}
