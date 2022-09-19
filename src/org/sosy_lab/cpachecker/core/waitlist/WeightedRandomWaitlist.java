// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.OrderStatisticMap;
import org.sosy_lab.cpachecker.util.OrderStatisticMap.OrderStatisticsMapProxy;

public class WeightedRandomWaitlist implements Waitlist {

  @Options(prefix = "analysis.traversal.random")
  public static class WaitlistOptions {
    @Option(
        secure = true,
        description =
            "Exponent of random function."
                + "This value influences the probability distribution over the waitlist elements"
                + "when choosing the next element."
                + "Has to be a double in the range [0, INF)")
    private double exponent = 1;

    @Option(secure = true, description = "Seed for random values.")
    private int seed = 0;

    public WaitlistOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
      if (exponent < 0) {
        throw new InvalidConfigurationException(
            "analysis.traversal.random.exponent has to be " + "a double greater or equal to 0");
      }
    }
  }

  private final double exponent;
  private OrderStatisticMap<AbstractState, Waitlist> states;
  private WaitlistFactory waitlistFactory;
  private Comparator<AbstractState> comparator;
  private Random random;

  public WeightedRandomWaitlist(
      Comparator<AbstractState> pComparator, WaitlistFactory pFactory, WaitlistOptions pConfig) {
    exponent = pConfig.exponent;
    random = new Random(pConfig.seed);
    comparator = pComparator;
    states = new OrderStatisticsMapProxy<>(new TreeMap<>(comparator));

    waitlistFactory = pFactory;
  }

  @Override
  public void add(AbstractState state) {
    if (!states.containsKey(state)) {
      states.put(state, waitlistFactory.createWaitlistInstance());
    }
    Waitlist w = states.get(state);
    w.add(state);
  }

  @Override
  public void clear() {
    states.clear();
  }

  @Override
  public boolean contains(AbstractState state) {
    return states.containsKey(state) && states.get(state).contains(state);
  }

  @Override
  public boolean isEmpty() {
    return states.isEmpty();
  }

  /**
   * Return a get level between 0 and the size of the waitlist - 1. The probability distribution is
   * logarithmic (i.e., higher values are less likely).
   */
  private int getRandomIndex() {
    double r = random.nextDouble();
    double x = Math.pow(r, exponent);
    int s = states.size() - 1;
    return ((int) Math.round(s * x));
  }

  @Override
  public AbstractState pop() {
    assert size() > 0;
    int idx = getRandomIndex();
    Preconditions.checkElementIndex(idx, states.size());
    Waitlist chosenWaitlist = states.getEntryByRank(idx).getValue();
    AbstractState poppedState = chosenWaitlist.pop();
    if (chosenWaitlist.isEmpty()) {
      states.removeByRank(idx);
    }
    return poppedState;
  }

  @Override
  public boolean remove(AbstractState state) {
    if (states.containsKey(state)) {
      Waitlist containingWaitlist = states.get(state);
      boolean removed = containingWaitlist.remove(state);
      if (containingWaitlist.isEmpty()) {
        states.remove(state);
      }
      return removed;
    } else {
      return false;
    }
  }

  @Override
  public int size() {
    int sum = 0;
    for (Waitlist w : states.values()) {
      sum += w.size();
    }
    return sum;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    if (states.isEmpty()) {
      return new Iterator<>() {

        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public AbstractState next() {
          throw new NoSuchElementException();
        }
      };
    }
    return new Iterator<>() {

      private Iterator<AbstractState> currIt = states.firstEntry().getValue().iterator();
      private int currRank = 0;
      private int maxRank = states.size() - 1;

      @Override
      public boolean hasNext() {
        return currIt.hasNext() || currRank < maxRank;
      }

      @Override
      public AbstractState next() {
        if (!currIt.hasNext()) {
          currRank++;
          currIt = states.getEntryByRank(currRank).getValue().iterator();
        }
        return currIt.next();
      }

      @Override
      public void remove() {
        currIt.remove();
      }
    };
  }
}
