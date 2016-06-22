/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class CompositeDomain implements AbstractDomain {

  private final ImmutableList<AbstractDomain> domains;

  public CompositeDomain(ImmutableList<AbstractDomain> domains) {
      this.domains = domains;
  }

  @Override
  public AbstractState join(AbstractState pElement1,
      AbstractState pElement2) throws CPAException {
    // a simple join is here not possible, because it would over-approximate,
    // but join needs to return the least upper bound
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) throws CPAException, InterruptedException {
    CompositeState comp1 = (CompositeState)pElement1;
    CompositeState comp2 = (CompositeState)pElement2;

    List<AbstractState> comp1Elements = comp1.getWrappedStates();
    List<AbstractState> comp2Elements = comp2.getWrappedStates();

    Preconditions.checkState(comp1Elements.size() == comp2Elements.size());
    Preconditions.checkState(comp1Elements.size() == domains.size());

    for (int idx = 0; idx < comp1Elements.size(); idx++) {
      AbstractDomain domain = domains.get(idx);
      if (!domain.isLessOrEqual(comp1Elements.get(idx), comp2Elements.get(idx))) {
        return false;
      }
    }

    return true;
  }
}
