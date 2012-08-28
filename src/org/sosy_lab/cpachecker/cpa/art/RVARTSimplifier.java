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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.PostProcessor;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractElements;

@Options(prefix="cpa.art")
public class RVARTSimplifier implements PostProcessor {
  //Note: this post processor requires a very specific configuration designed for runtime verification
  private final static String MONITOR_STATE_NAME = "Monitor";

  private final ARTCPA cpa;

  public RVARTSimplifier(Configuration config, ARTCPA cpa) throws InvalidConfigurationException {
    config.inject(this);
    this.cpa = cpa;
  }

  @Override
  public void postProcess(ReachedSet pReached) {
    ARTElement artRoot = (ARTElement) pReached.getFirstElement();
    AbstractElement lastElement = pReached.getLastElement();
    if (!AbstractElements.isTargetElement(lastElement)) {
      removeMonitorTransitions(artRoot, pReached);
    }
  }

  public void removeMonitorTransitions(ARTElement artRoot, ReachedSet pReached) {
    Deque<ARTElement> waitlist = new ArrayDeque<ARTElement>();
    Set<ARTElement> seen = new HashSet<ARTElement>();
    waitlist.add(artRoot);

    while(!waitlist.isEmpty()) {
      ARTElement currentElement = waitlist.pop();

      Collection<ARTElement> toProcess = new ArrayList<ARTElement>();

      Collection<ARTElement> children = new ArrayList<ARTElement>();
      children.addAll(currentElement.getChildren());
      for(ARTElement child : children) {
        toProcess.addAll(removeMonitorComponent(child, pReached)); //may alter currentElement.getChildren()
      }

      for(ARTElement child : toProcess) {
        if(!seen.contains(child)) {
          waitlist.add(child);
          seen.add(child);
        }
      }
    }
  }

  private Collection<ARTElement> removeMonitorComponent(ARTElement pRootElement, ReachedSet pReached) {
    AutomatonState automatonElement = AbstractElements.extractElementByType(pRootElement, AutomatonState.class);
    if(!automatonElement.getInternalStateName().equals(MONITOR_STATE_NAME)) {
      return Collections.singleton(pRootElement);
    }

    //find reachable non-monitor elements
    Deque<ARTElement> waitlist = new ArrayDeque<ARTElement>();
    Set<ARTElement> seen = new HashSet<ARTElement>();
    waitlist.add(pRootElement);
    seen.add(pRootElement);

    List<ARTElement> newChildren = new ArrayList<ARTElement>();
    List<ARTElement> toDelete = new ArrayList<ARTElement>();


    while(!waitlist.isEmpty()) {
      ARTElement currentElement = waitlist.pop();

      AutomatonState currentAutomatonElement = AbstractElements.extractElementByType(currentElement, AutomatonState.class);
      if(!currentAutomatonElement.getInternalStateName().equals(MONITOR_STATE_NAME)) {
        newChildren.add(currentElement);
        continue;
      }

      for(ARTElement child : currentElement.getChildren()) {
        if(!seen.contains(child)) {
          waitlist.add(child);
          seen.add(child);
        }
      }

      //this element is not needed anymore
      toDelete.add(currentElement);
    }

    assert newChildren.size() != 0;

    if(newChildren.size() > 1) {
      //better leave the transition code inside
      //TODO: but we should remove __MONITOR_STATE assignments?
      return newChildren;
    }

    for(ARTElement newChild : newChildren) {
      for(ARTElement rootParents : pRootElement.getParents()) {
        newChild.addParent(rootParents);
      }
    }

    for(ARTElement e : toDelete) {
      pReached.remove(e);
      e.removeFromART();
    }

    return newChildren;
  }

}
