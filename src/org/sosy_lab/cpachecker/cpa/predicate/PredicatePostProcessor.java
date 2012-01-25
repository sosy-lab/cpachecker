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

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.PostProcessor;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.AbstractElements;


public class PredicatePostProcessor implements PostProcessor {

  @Override
  public void postProcess(ReachedSet pReached) {
    System.out.println("Called post processing for predicate cpa :-)");
    removeUnsatPaths(pReached);
  }

  private void removeUnsatPaths(ReachedSet pReached) {
    //idea: non-abstraction elements from which a non-false abstraction element is not reachable can get removed

    Deque<ARTElement> leaves = new ArrayDeque<ARTElement>();
    for (AbstractElement e : pReached) {
      ARTElement artEle = (ARTElement) e;
      if (artEle.getChildren().size() == 0 && (!AbstractElements.extractElementByType(artEle, PredicateAbstractElement.class).isAbstractionElement() || AbstractElements.extractElementByType(artEle, PredicateAbstractElement.class).getAbstractionFormula().isFalse()) && !artEle.isTarget()) {
        leaves.push(artEle);
      }
    }

    while (!leaves.isEmpty()) {
      ARTElement leaf = leaves.pop();

      assert (!AbstractElements.extractElementByType(leaf, PredicateAbstractElement.class).isAbstractionElement() || AbstractElements.extractElementByType(leaf, PredicateAbstractElement.class).getAbstractionFormula().isFalse());

      Collection<ARTElement> parents = new ArrayList<ARTElement>();
      parents.addAll(leaf.getParents());

      pReached.remove(leaf);
      leaf.removeFromART();

      for (ARTElement parent : parents) {
        if (parent.getChildren().size() == 0) {
          leaves.push(parent);
        }
      }
    }
  }

}
