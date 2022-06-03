// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * {@link Waitlist} implementation that is always empty and throws an exception if states are added.
 */
public enum AlwaysEmptyWaitlist implements Waitlist {
  INSTANCE;

  private enum Factory implements WaitlistFactory {
    FACTORY_INSTANCE;

    @Override
    public Waitlist createWaitlistInstance() {
      return AlwaysEmptyWaitlist.INSTANCE;
    }
  }

  public static WaitlistFactory factory() {
    return Factory.FACTORY_INSTANCE;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public void add(AbstractState pState) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    // not necessary
  }

  @Override
  public boolean contains(AbstractState pState) {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public AbstractState pop() {
    throw new NoSuchElementException();
  }

  @Override
  public boolean remove(AbstractState pState) {
    return false;
  }

  @Override
  public int size() {
    return 0;
  }
}
