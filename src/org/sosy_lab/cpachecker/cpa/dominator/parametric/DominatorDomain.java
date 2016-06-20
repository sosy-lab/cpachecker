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
package org.sosy_lab.cpachecker.cpa.dominator.parametric;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DominatorDomain implements AbstractDomain<DominatorState> {

  private final ConfigurableProgramAnalysis cpa;

  public DominatorDomain(ConfigurableProgramAnalysis<?> cpa) {
    this.cpa = cpa;
  }

  private static class DominatorTopState extends DominatorState  {

    @Override
    public String toString() {
      return "\\bot";
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof DominatorTopState);
    }

    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }

    @Override
    public CFANode getLocationNode() {
      // TODO Auto-generated method stub
      return null;
    }
  }

  private final static DominatorTopState topState = new DominatorTopState();

  @Override
  public boolean isLessOrEqual(DominatorState dominatorState1, DominatorState dominatorState2) throws
                                                                   CPAException, InterruptedException {
    if (dominatorState1.equals(dominatorState2)) {
      return true;
    }

    if (dominatorState2.equals(topState)) {
      return true;
    }

    if (this.cpa.getAbstractDomain().isLessOrEqual(dominatorState1.getDominatedState(), dominatorState2.getDominatedState())) {
      Iterator<AbstractState> dominatorIterator = dominatorState2.getIterator();

      while (dominatorIterator.hasNext()) {
        AbstractState dominator = dominatorIterator.next();

        if (!dominatorState1.isDominatedBy(dominator)) {
          return false;
        }
      }

      return true;
    }
    return false;
  }

  @Override
  public DominatorState join(
      DominatorState dominatorState1, DominatorState dominatorState2) {
    if (dominatorState1.equals(topState)) {
      return dominatorState1;
    }

    if (dominatorState2.equals(topState)) {
      return dominatorState2;
    }

    if (!dominatorState1.getDominatedState().equals(dominatorState2.getDominatedState())) {
      return topState;
    }

    Set<AbstractState> intersectingDominators = new HashSet<>();

    Iterator<AbstractState> dominatorIterator = dominatorState1.getIterator();

    while (dominatorIterator.hasNext()) {
      AbstractState dominator = dominatorIterator.next();

      if (dominatorState2.isDominatedBy(dominator)) {
        intersectingDominators.add(dominator);
      }
    }

    DominatorState result = new DominatorState(dominatorState1.getDominatedState(), intersectingDominators);

    result.update(dominatorState1.getDominatedState());

    return result;
  }
}
