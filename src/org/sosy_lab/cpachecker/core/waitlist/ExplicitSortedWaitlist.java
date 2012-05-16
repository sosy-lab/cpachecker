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

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitElement;
import org.sosy_lab.cpachecker.util.AbstractElements;

/**
 * Waitlist implementation that sorts the abstract elements depending on the
 * content of the ExplicitElement (if there is any).
 * Elements where less variables have a value assigned are considered first.
 * This elements are expected to cover a bigger part of the state space,
 * so elements with more variables will probably be covered later.
 */
public class ExplicitSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ExplicitSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractElement pElement) {
    ExplicitElement explicitElement =
      AbstractElements.extractElementByType(pElement, ExplicitElement.class);

    // negate size so that the highest key corresponds to the smallest map
    return (explicitElement != null) ? -explicitElement.getSize() : 0;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return new WaitlistFactory() {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ExplicitSortedWaitlist(pSecondaryStrategy);
      }
    };
  }
}
