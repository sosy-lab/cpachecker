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
package org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.CoverageSpecificationEvaluator;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Coverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Sequence;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.FilterMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.PathMonitor;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Identity;

public class CoverageSequence implements Iterable<Pair<Automaton, Set<? extends TestGoal>>> {
  private Automaton mFinalMonitor;
  private LinkedList<Pair<Automaton, Set<? extends TestGoal>>> mSequence;

  private CoverageSequence() {
    mSequence = new LinkedList<Pair<Automaton, Set<? extends TestGoal>>>();
  }

  private CoverageSequence(Automaton pInitialAutomaton, Set<? extends TestGoal> pTestGoals, Automaton pFinalAutomaton) {
    assert(pInitialAutomaton != null);
    assert(pTestGoals != null);
    assert(pFinalAutomaton != null);

    mSequence = new LinkedList<Pair<Automaton, Set<? extends TestGoal>>>();

    mSequence.add(new Pair<Automaton, Set<? extends TestGoal>>(pInitialAutomaton, pTestGoals));

    mFinalMonitor = pFinalAutomaton;
  }

  public Automaton getFinalMonitor() {
    return mFinalMonitor;
  }

  public int size() {
    return mSequence.size();
  }

  public Pair<Automaton, Set<? extends TestGoal>> get(int pIndex) {
    assert(0 <= pIndex);
    assert(pIndex < mSequence.size());

    return mSequence.get(pIndex);
  }

  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();

    lBuffer.append("<");

    boolean isFirst = true;

    for (Pair<Automaton, Set<? extends TestGoal>> lPair : mSequence) {
      Automaton lMonitor = lPair.getFirst();
      Set<? extends TestGoal> lTestGoal = lPair.getSecond();

      if (isFirst) {
        isFirst = false;
      }
      else {
        lBuffer.append(",");
      }

      lBuffer.append(lMonitor.toString());
      lBuffer.append(",");
      lBuffer.append(lTestGoal.toString());
    }

    lBuffer.append(",");
    lBuffer.append(mFinalMonitor.toString());
    lBuffer.append(">");

    return lBuffer.toString();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() == getClass()) {
      CoverageSequence lSequence = (CoverageSequence)pOther;

      return lSequence.mSequence.equals(mSequence)
              && lSequence.mFinalMonitor.equals(mFinalMonitor);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 398231 + mSequence.hashCode() + mFinalMonitor.hashCode();
  }

  @Override
  public Iterator<Pair<Automaton, Set<? extends TestGoal>>> iterator() {
    return mSequence.iterator();
  }

  public static CoverageSequence create(Coverage pCoverageSpecification, TargetGraph pTargetGraph) {
    assert(pTargetGraph != null);
    assert(pCoverageSpecification != null);

    if (pCoverageSpecification instanceof Sequence) {
      CoverageSequence lCoverageSequence = new CoverageSequence();

      Sequence lSequenceSpecification = (Sequence)pCoverageSpecification;

      for (Pair<PathMonitor, Coverage> lPair : lSequenceSpecification) {
        PathMonitor lMonitor = lPair.getFirst();
        Coverage lCoverageSpecification = lPair.getSecond();

        Automaton lAutomaton = Automaton.create(lMonitor, pTargetGraph);

        CoverageSpecificationEvaluator lEvaluator = new CoverageSpecificationEvaluator(pTargetGraph);
        
        Set<? extends TestGoal> lTestGoals = lEvaluator.evaluate(lCoverageSpecification);

        lCoverageSequence.mSequence.add(new Pair<Automaton, Set<? extends TestGoal>>(lAutomaton, lTestGoals));
      }

      Automaton lFinalAutomaton = Automaton.create(lSequenceSpecification.getFinalMonitor(), pTargetGraph);

      lCoverageSequence.mFinalMonitor = lFinalAutomaton;

      return lCoverageSequence;
    }
    else {
      // simple coverage specification
      CoverageSpecificationEvaluator lEvaluator = new CoverageSpecificationEvaluator(pTargetGraph);
      
      Set<? extends TestGoal> lTestGoals = lEvaluator.evaluate(pCoverageSpecification);

      PathMonitor lIdStarMonitor = new LowerBound(new FilterMonitor(Identity.getInstance()), 0);

      Automaton lAutomaton = Automaton.create(lIdStarMonitor, pTargetGraph);

      return new CoverageSequence(lAutomaton, lTestGoals, lAutomaton);
    }
  }
}

