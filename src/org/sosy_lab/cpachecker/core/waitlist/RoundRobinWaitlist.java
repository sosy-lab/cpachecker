/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class RoundRobinWaitlist implements Waitlist {

  private final ImmutableList<Waitlist> waitlistStrategies;
  private int nextStrategy = 0;

  protected RoundRobinWaitlist() {
    waitlistStrategies =
        ImmutableList.<Waitlist>of(
            Waitlist.TraversalMethod.DFS.createWaitlistInstance(),
            Waitlist.TraversalMethod.BFS.createWaitlistInstance());
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return waitlistStrategies.get(nextStrategy).iterator();
  }

  @Override
  public void add(AbstractState pState) {
    for (Waitlist waitlist : waitlistStrategies) {
      waitlist.add(pState);
    }
  }

  @Override
  public void clear() {
    for (Waitlist waitlist : waitlistStrategies) {
      waitlist.clear();
    }

    nextStrategy = 0;
  }

  @Override
  public boolean contains(AbstractState pState) {
    return waitlistStrategies.get(nextStrategy).contains(pState);
  }

  @Override
  public boolean isEmpty() {
    return waitlistStrategies.get(nextStrategy).isEmpty();
  }

  @Override
  public AbstractState pop() {
    AbstractState poppedElem = waitlistStrategies.get(nextStrategy).pop();
    for (Waitlist waitlist : waitlistStrategies) {
      waitlist.remove(poppedElem);
    }

    nextStrategy = (nextStrategy + 1) % waitlistStrategies.size();

    return poppedElem;
  }

  @Override
  public boolean remove(AbstractState pState) {
    boolean removed = false;
    for (Waitlist waitlist : waitlistStrategies) {
      removed = waitlist.remove(pState);
    }
    return removed;
  }

  @Override
  public int size() {
    return waitlistStrategies.get(nextStrategy).size();
  }
}
