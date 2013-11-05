/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PostProcessor;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;


public class PredicatePostProcessor implements PostProcessor {

  @Override
  public void postProcess(ReachedSet pReached) {
    AbstractState lastElement = pReached.getLastState();
    if (!AbstractStates.isTargetState(lastElement)) {
      removeUnsatPaths(pReached);
    }
  }

  private void removeUnsatPaths(ReachedSet pReached) {
    //idea: non-abstraction elements from which a non-false abstraction element is not reachable can get removed

    Deque<ARGState> leaves = new ArrayDeque<>();
    for (AbstractState e : pReached) {
      ARGState artEle = (ARGState) e;
      if (artEle.getChildren().size() == 0 && (!AbstractStates.extractStateByType(artEle, PredicateAbstractState.class).isAbstractionState() || AbstractStates.extractStateByType(artEle, PredicateAbstractState.class).getAbstractionFormula().isFalse()) && !artEle.isTarget()) {
        leaves.push(artEle);
      }
    }

    while (!leaves.isEmpty()) {
      ARGState leaf = leaves.pop();

      assert (!AbstractStates.extractStateByType(leaf, PredicateAbstractState.class).isAbstractionState() || AbstractStates.extractStateByType(leaf, PredicateAbstractState.class).getAbstractionFormula().isFalse());

      Collection<ARGState> parents = new ArrayList<>();
      parents.addAll(leaf.getParents());

      pReached.remove(leaf);
      leaf.removeFromARG();

      for (ARGState parent : parents) {
        if (parent.getChildren().size() == 0) {
          leaves.push(parent);
        }
      }
    }
  }

}
