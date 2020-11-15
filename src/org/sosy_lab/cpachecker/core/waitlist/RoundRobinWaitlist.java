// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class RoundRobinWaitlist implements Waitlist {

  private final ImmutableList<Waitlist> waitlistStrategies;
  private int nextStrategy = 0;

  protected RoundRobinWaitlist() {
    waitlistStrategies =
        ImmutableList.of(
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
