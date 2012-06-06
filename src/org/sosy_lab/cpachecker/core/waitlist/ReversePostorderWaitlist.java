/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.LinkedList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Waitlist implementation that sorts the abstract elements in reverse postorder
 * considering the location they belong to.
 * This implementation does not allow to choose a secondary strategy for elements
 * with the same reverse postorder id. Also, the method pop() has a complexity of O(n).
 * Therefore, this implementation should be considered as deprecated and
 * replaced by RevsersePostorderSortedWaitlist.
 */
@Deprecated
public class ReversePostorderWaitlist extends AbstractWaitlist<LinkedList<AbstractState>> {

  protected ReversePostorderWaitlist() {
    super(new LinkedList<AbstractState>());
  }

  @Override
  public AbstractState pop() {
    AbstractState result = null;
    int resultRpoId = Integer.MIN_VALUE;
    for (AbstractState currentElement : waitlist) {
      if ((result == null)
          || (extractLocation(currentElement).getReversePostorderId() >
          resultRpoId)) {
        result = currentElement;
        resultRpoId = extractLocation(result).getReversePostorderId();
      }
    }
    waitlist.remove(result);
    return result;
  }
}

