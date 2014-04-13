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
package org.sosy_lab.cpachecker.core.waitlist;

import java.util.LinkedList;
import java.util.Random;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist that implements DFS behavior with random selection of branching path.
 *
 * pop() removes the last added state of the path that is currently explored (DFS behavior).
 * If the last iteration added more than one state (branching case of successor computation) pop()
 * returns one of these successors at random.
 */
public class RandomPathWaitlist extends AbstractWaitlist<LinkedList<AbstractState>> {

  private final Random rand = new Random();
  private int successorsOfParent;
  private CFANode parent;

  protected RandomPathWaitlist() {
    super(new LinkedList<AbstractState>());
    successorsOfParent = 0;
  }

  @Override
  public void add(AbstractState pStat) {
    super.add(pStat);
    CFANode location = AbstractStates.extractLocation(pStat);
    if (parent == null || (!parent.hasEdgeTo(location)))
    {
      parent = location;
      successorsOfParent = 0;
    }
    else {
      successorsOfParent++;
    }
  }


  @Override
  public AbstractState pop() {
    AbstractState state;
    if (waitlist.size() < 2 || successorsOfParent < 2) {
      state = waitlist.getLast();
    } else //(successorsOnLevelCount >= 2)
    {
      int r = rand.nextInt(successorsOfParent) + 1;
      state = waitlist.get(waitlist.size() - r);
    }
    if (successorsOfParent > 0) {
      successorsOfParent--;
      parent = AbstractStates.extractLocation(state);
    } else {
      parent = null;//TODO not sure if a reset to no parent is correct.
    }
    return state;
  }
}
