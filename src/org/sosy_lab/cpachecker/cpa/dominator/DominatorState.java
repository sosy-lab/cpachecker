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
package org.sosy_lab.cpachecker.cpa.dominator;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

public class DominatorState extends ForwardingSet<CFANode>
    implements AbstractState, LatticeAbstractState<DominatorState>, Graphable {

  private Set<CFANode> dominators = new HashSet<>();

  public DominatorState() {}

  public DominatorState(Set<CFANode> pDominators) {
    checkNotNull(pDominators);
    dominators.addAll(pDominators);
  }

  @Override
  protected Set<CFANode> delegate() {
    return dominators;
  }

  @Override
  public boolean add(CFANode pDominator) {
    checkNotNull(pDominator);

    return dominators.add(pDominator);
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof DominatorState)) {
      return false;
    }

    DominatorState otherElement = (DominatorState) pOther;

    return dominators.equals(otherElement.dominators);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getSimpleName()).append(": ").append("{");

    boolean first = true;
    for (CFANode dominator : dominators) {
      if (first) {
        first = false;
      } else {
        builder.append(", ");
      }

      builder.append(dominator.toString());
    }

    builder.append("}");

    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(dominators);
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public DominatorState join(DominatorState pOther) {
    if (this.dominators.equals(pOther.dominators)) {
      return pOther;
    } else {
      return new DominatorState(Sets.intersection(this.dominators, pOther.dominators));
    }
  }

  /**
   * Checks whether the given state is less or equal to this state.
   * A dominator state d1 is less or equal than another dominator state d2,
   * if d1 contains all dominators that d2 contains.
   *
   * @param pOther the other state (d2) to check against this one (d1)
   * @return <code>true</code> if this state is less or equal to the given state,
   * <code>false</code> otherwise
   */
  @Override
  public boolean isLessOrEqual(DominatorState pOther) {
    for (CFANode dominator : pOther) {
      if (!contains(dominator)) {
        return false;
      }
    }
    return true;
  }
}
