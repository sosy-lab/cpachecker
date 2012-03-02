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

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.LinkedList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

/**
 * Waitlist implementation that sorts the abstract elements topologically
 * considering the location they belong to.
 * This implementation does not allow to choose a secondary strategy for elements
 * with the same topsort id. Also, the method pop() has a complexity of O(n).
 * Therefore, this implementation should be considered as deprecated and
 * replaced by TopologiallySortedWaitlist.
 */
@Deprecated
public class TopsortWaitlist extends AbstractWaitlist<LinkedList<AbstractElement>> {

  protected TopsortWaitlist() {
    super(new LinkedList<AbstractElement>());
  }

  @Override
  public AbstractElement pop() {
    AbstractElement result = null;
    int resultTopSortId = Integer.MIN_VALUE;
    for (AbstractElement currentElement : waitlist) {
      if ((result == null)
          || (extractLocation(currentElement).getStrictTopoSortId() >
              resultTopSortId)) {
        result = currentElement;
        resultTopSortId = extractLocation(result).getStrictTopoSortId();
      }
    }
    waitlist.remove(result);
    return result;
  }
}

