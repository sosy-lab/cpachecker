/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import org.sosy_lab.common.collect.SkipList;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class WeightedRandomWaitlist implements Waitlist {

  public SkipList<AbstractState> states;

  public static Random random = new Random();

  public WeightedRandomWaitlist(final Comparator<AbstractState> pComparator) {
    states = new SkipList<>(pComparator);
  }

  @Override
  public void add(AbstractState state) {
    boolean added = states.add(state);
    Preconditions.checkState(added);
  }

  @Override
  public void clear() {
    states.clear();
  }

  @Override
  public boolean contains(AbstractState state) {
    return states.contains(state);
  }

  @Override
  public boolean isEmpty() {
    return states.isEmpty();
  }

  /**
   * Return a random level between 0 and the size of the waitlist. The probability distribution is
   * logarithmic (i.e., higher values are less likely).
   */
  private int getRandomIndex() {
    double r = random.nextDouble();
    return ((int) Math.round(Math.pow(size(), r))) - 1;
  }

  @Override
  public AbstractState pop() {
    assert size() > 0;
    int idx = getRandomIndex();
    assert idx >= 0;
    return states.removeByRank(idx);
  }

  @Override
  public boolean remove(AbstractState state) {
    return states.remove(state);
  }

  @Override
  public int size() {
    return states.size();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return states.iterator();
  }


}

