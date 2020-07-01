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
package org.sosy_lab.cpachecker.cpa.statistics.provider;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.statistics.StatisticsDataProvider;
import org.sosy_lab.cpachecker.cpa.statistics.StatisticsProvider;


/**
 * The SimpleIntProvider is a basic implementation for StatisticsProvider
 * where the state is basically a single Integer.
 * To use this Api you only have to define how to calculate the next state given the current state
 * and an CFAEdge. (implementing SimpleIntProviderImplementation)
 * And defining your merge strategy by implementing IntMerger.
 * The interfaces are separated so you can implement multiple merge strategies for a calculateNext-strategy.
 *
 */
public class SimpleIntProvider implements StatisticsProvider {
  public static interface SimpleIntProviderImplementation {
    String getPropertyName();
    /**
     * Calculate the next state given the current state and an CFAEdge
     */
    int calculateNext(int current, CFAEdge edge);
  }

  /**
   * To also name your merge strategy override the toString method.
   */
  public static interface IntMerger {
    /**
     * Merge two states.
     */
    int merge(int s1, int s2);
    @Override
    String toString();
  }

  public static class SimpleIntMergeProvider {
    private IntMerger merger;
    SimpleIntMergeProvider(IntMerger merger) {
      assert merger != null;
      this.merger = merger;
    }

    public int mergePath(StatisticsDataProvider left, StatisticsDataProvider right) {
      return merger.merge(((SimpleIntDataProvider)left).data, ((SimpleIntDataProvider)right).data);
    }

    @Override
    public String toString() {
      return merger.toString();
    }
  }

  public static class SimpleIntDataProvider implements StatisticsDataProvider {
    private int data;
    private SimpleIntProviderImplementation calculator;
    private SimpleIntMergeProvider merger;

    SimpleIntDataProvider(SimpleIntProviderImplementation calculator, SimpleIntMergeProvider merger, int data) {
      assert calculator != null;
      assert merger != null;
      this.data = data;
      this.merger = merger;
      this.calculator = calculator;
    }

    @Override
    public Object getPropertyValue() {
      return data;
    }

    @Override
    public StatisticsDataProvider calculateNext(CFAEdge edge) {
      return new SimpleIntDataProvider(calculator, merger, calculator.calculateNext(data, edge));
    }
    @Override
    public StatisticsDataProvider mergePath(StatisticsDataProvider other) {
      return new SimpleIntDataProvider(calculator, merger, merger.mergePath(this, other));
    }
  }

  private SimpleIntProviderImplementation calculator;
  private SimpleIntMergeProvider merger;
  private int defValue;
  public SimpleIntProvider(SimpleIntProviderImplementation calculator, IntMerger merger, int defValue) {
    assert calculator != null;
    this.calculator = calculator;
    this.merger = new SimpleIntMergeProvider(merger);
    this.defValue = defValue;
  }

  @Override
  public String getPropertyName() {
    return calculator.getPropertyName();
  }

  @Override
  public String getMergeType() {
    return merger.toString();
  }

  @Override
  public StatisticsDataProvider createDataProvider() {
    return new SimpleIntDataProvider(calculator, merger, defValue);
  }
}
