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

import java.util.Collection;
import java.util.Iterator;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Default implementation for a waitlist that uses any collection as the backing
 * data structure. All methods except pop() are implemented by delegating them
 * to the correspondent Collection method.
 *
 * Sub-classes may choose their own collection implementation (e.g. a LinkedList
 * or an ArrayDeque) depending on their needs for pop().
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
