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
package org.sosy_lab.cpachecker.cpa.statistics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A simple wrapper around the management of a Map<StatisticsProvider, StatisticsDataProvider> field.
 * All instances of this class are immutable.
 */
public class StatisticsData implements Iterable<Entry<StatisticsProvider, StatisticsDataProvider>> {
  private Map<StatisticsProvider, StatisticsDataProvider> data;

  public StatisticsData(Set<StatisticsProvider> propertyProviders) {
    Map<StatisticsProvider, StatisticsDataProvider> dataProvider = new HashMap<>(propertyProviders.size());
    for (StatisticsProvider providerEntry : propertyProviders) {
      dataProvider.put(providerEntry, providerEntry.createDataProvider());
    }
    data = dataProvider;
  }

  private StatisticsData(Map<StatisticsProvider, StatisticsDataProvider> data) {
    this.data = data;
  }

  public StatisticsDataProvider getProperty(String pProperty) {
    throw new UnsupportedOperationException("Querying is currently not implemented.");
  }

  public StatisticsData mergeState(StatisticsData state2) {
    assert data.size() == state2.data.size() : "sized and properties have to match";
    Map<StatisticsProvider, StatisticsDataProvider> merged = new HashMap<>(data.size());
    for (Entry<StatisticsProvider, StatisticsDataProvider> providerEntry : data.entrySet()) {
      merged.put(providerEntry.getKey(), providerEntry.getValue().mergePath(state2.data.get(providerEntry.getKey())));
    }
    return new StatisticsData(merged);
  }

  public StatisticsData getNextState(CFAEdge node) {
    Map<StatisticsProvider, StatisticsDataProvider> dataProvider = new HashMap<>(data.size());
    for (Entry<StatisticsProvider, StatisticsDataProvider> providerEntry : data.entrySet()) {
      StatisticsProvider key = providerEntry.getKey();
      StatisticsDataProvider value = providerEntry.getValue();
      value = value.calculateNext(node);
      dataProvider.put(key, value);
    }
    return new StatisticsData(dataProvider);
  }

  @Override
  public Iterator<Entry<StatisticsProvider, StatisticsDataProvider>> iterator() {
    return new ReadonlyIterator<>(data.entrySet().iterator());
  }

  public static class ReadonlyIterator<T> implements Iterator<T> {
    private Iterator<T> wrapped;
    public ReadonlyIterator(Iterator<T> wrapped) {
      this.wrapped = wrapped;
    }
    @Override
    public boolean hasNext() {
      return this.wrapped.hasNext();
    }

    @Override
    public T next() {
      return this.wrapped.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove not supported!");
    }

  }

}
