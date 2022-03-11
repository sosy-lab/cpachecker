// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import java.util.Collection;
import java.util.Iterator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Default implementation for a waitlist that uses any collection as the backing data structure. All
 * methods except pop() are implemented by delegating them to the correspondent Collection method.
 *
 * <p>Sub-classes may choose their own collection implementation (e.g. a LinkedList or an
 * ArrayDeque) depending on their needs for pop().
 */
public abstract class AbstractWaitlist<T extends Collection<AbstractState>> implements Waitlist {

  protected final T waitlist;

  protected AbstractWaitlist(T pWaitlist) {
    waitlist = pWaitlist;
  }

  @Override
  public void add(AbstractState pStat) {
    waitlist.add(pStat);
  }

  @Override
  public void clear() {
    waitlist.clear();
  }

  @Override
  public boolean contains(AbstractState pState) {
    return waitlist.contains(pState);
  }

  @Override
  public boolean isEmpty() {
    return waitlist.isEmpty();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return waitlist.iterator();
  }

  @Override
  public boolean remove(AbstractState pState) {
    return waitlist.remove(pState);
  }

  @Override
  public int size() {
    return waitlist.size();
  }

  @Override
  public String toString() {
    return waitlist.toString();
  }
}
