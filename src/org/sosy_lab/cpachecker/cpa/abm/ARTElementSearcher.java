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
package org.sosy_lab.cpachecker.cpa.abm;

import static org.sosy_lab.cpachecker.util.AbstractElements.filterLocation;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;


/**
 * Helper class to search for <code>ARTElement</code>s in a <code>ReachedSet</code> that have the same abstraction as the given <code>ARTElement</code> after reduction to the relevant predicates.
 */
public class ARTElementSearcher {
  final static Timer searchForARTElementTimer = new Timer();

  public static ARTElement searchForARTElement(UnmodifiableReachedSet reached, ARTElement targetElement, Reducer reducer, BlockPartitioning manager) {
    assert reached != null;
    CFANode targetNode = targetElement.retrieveLocationElement().getLocationNode();
    searchForARTElementTimer.start();
    try {
      if (targetElement.isTarget()) {
        //shortcut
        ARTElement result = (ARTElement)reached.getLastElement();
        assert result != null;
        return result;
      }
      CFANode callNode = ((ARTElement)reached.getFirstElement()).retrieveLocationElement().getLocationNode();
      Block context = manager.getBlockForCallNode(callNode);
      AbstractElement reducedTarget = reducer.getVariableReducedElement(targetElement, context, callNode);

      Iterable<AbstractElement> localReached = filterLocation(reached, targetNode);

      for(AbstractElement element : localReached) {
        if (reducer.isEqual(reducedTarget, element)) {
          return (ARTElement)element;
        }
      }

      return null;
    }
    finally {
      searchForARTElementTimer.stop();
    }
  }
}
