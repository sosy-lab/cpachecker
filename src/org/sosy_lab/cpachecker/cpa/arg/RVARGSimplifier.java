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
package org.sosy_lab.cpachecker.cpa.arg;

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
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PostProcessor;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix="cpa.arg")
public class RVARGSimplifier implements PostProcessor {
  //Note: this post processor requires a very specific configuration designed for runtime verification
  private final static String MONITOR_STATE_NAME = "Monitor";

  private final ARGCPA cpa;
  private final ShutdownNotifier shutdownNotifier;

  public RVARGSimplifier(Configuration config, ARGCPA cpa, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);
    this.cpa = cpa;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public void postProcess(ReachedSet pReached) throws InterruptedException {
    ARGState artRoot = (ARGState) pReached.getFirstState();
    AbstractState lastElement = pReached.getLastState();
    if (!AbstractStates.isTargetState(lastElement)) {
      removeMonitorTransitions(artRoot, pReached);
    }
  }

  public void removeMonitorTransitions(ARGState artRoot, ReachedSet pReached) throws InterruptedException {
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Set<ARGState> seen = new HashSet<>();
    waitlist.add(artRoot);

    while(!waitlist.isEmpty()) {
      ARGState currentElement = waitlist.pop();

      Collection<ARGState> toProcess = new ArrayList<>();

      Collection<ARGState> children = new ArrayList<>();
      children.addAll(currentElement.getChildren());
      for(ARGState child : children) {
        toProcess.addAll(removeMonitorComponent(child, pReached)); //may alter currentElement.getChildren()
      }

      shutdownNotifier.shutdownIfNecessary();

      for(ARGState child : toProcess) {
        if(!seen.contains(child)) {
          waitlist.add(child);
          seen.add(child);
        }
      }
    }
  }

  private Collection<ARGState> removeMonitorComponent(ARGState pRootElement, ReachedSet pReached) throws InterruptedException {
    AutomatonState automatonElement = AbstractStates.extractStateByType(pRootElement, AutomatonState.class);
    if(!automatonElement.getInternalStateName().equals(MONITOR_STATE_NAME)) {
      return Collections.singleton(pRootElement);
    }

    //find reachable non-monitor elements
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Set<ARGState> seen = new HashSet<>();
    waitlist.add(pRootElement);
    seen.add(pRootElement);

    List<ARGState> newChildren = new ArrayList<>();
    List<ARGState> toDelete = new ArrayList<>();


    while(!waitlist.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      ARGState currentElement = waitlist.pop();

      AutomatonState currentAutomatonElement = AbstractStates.extractStateByType(currentElement, AutomatonState.class);
      if(!currentAutomatonElement.getInternalStateName().equals(MONITOR_STATE_NAME)) {
        newChildren.add(currentElement);
        continue;
      }

      for(ARGState child : currentElement.getChildren()) {
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

    shutdownNotifier.shutdownIfNecessary();

    for(ARGState newChild : newChildren) {
      for(ARGState rootParents : pRootElement.getParents()) {
        newChild.addParent(rootParents);
      }
    }

    shutdownNotifier.shutdownIfNecessary();

    for(ARGState e : toDelete) {
      pReached.remove(e);
      e.removeFromARG();
    }

    return newChildren;
  }

}
