/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Delay expanding nodes which have edges to leaf states.
 * Attempts to delay target reachability checking, as error states are very
 * often leaf nodes of the CFA.
 */
public class LeafSortedWaitlist extends AbstractSortedWaitlist<Integer>{
  private LeafSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    CFANode location = AbstractStates.extractLocation(pState);
    for (CFAEdge edge : CFAUtils.leavingEdges(location)) {
      if (edge.getSuccessor().getNumLeavingEdges() == 0) {

        // Delay nodes which have edges to leaf nodes.
        return 0;
      }
    }
    return 1;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return new WaitlistFactory() {
      @Override
      public Waitlist createWaitlistInstance() {
        return new LeafSortedWaitlist(pSecondaryStrategy);
      }
    };
  }
}
