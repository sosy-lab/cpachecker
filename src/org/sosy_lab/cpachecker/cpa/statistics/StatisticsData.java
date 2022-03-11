// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.statistics;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A simple wrapper around the management of a {@code Map<StatisticsProvider,
 * StatisticsDataProvider>} field. All instances of this class are immutable.
 */
public class StatisticsData implements Iterable<Entry<StatisticsProvider, StatisticsDataProvider>> {
  private final Map<StatisticsProvider, StatisticsDataProvider> data;

  public StatisticsData(Set<StatisticsProvider> propertyProviders) {
    Map<StatisticsProvider, StatisticsDataProvider> dataProvider =
        Maps.newHashMapWithExpectedSize(propertyProviders.size());
    for (StatisticsProvider providerEntry : propertyProviders) {
      dataProvider.put(providerEntry, providerEntry.createDataProvider());
    }
    data = dataProvider;
  }

  private StatisticsData(Map<StatisticsProvider, StatisticsDataProvider> data) {
    this.data = data;
  }

  public StatisticsData mergeState(StatisticsData state2) {
    assert data.size() == state2.data.size() : "sized and properties have to match";
    Map<StatisticsProvider, StatisticsDataProvider> merged =
        Maps.newHashMapWithExpectedSize(data.size());
    for (Entry<StatisticsProvider, StatisticsDataProvider> providerEntry : data.entrySet()) {
      merged.put(
          providerEntry.getKey(),
          providerEntry.getValue().mergePath(state2.data.get(providerEntry.getKey())));
    }
    return new StatisticsData(merged);
  }

  public StatisticsData getNextState(CFAEdge node) {
    Map<StatisticsProvider, StatisticsDataProvider> dataProvider =
        Maps.newHashMapWithExpectedSize(data.size());
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
    return Iterators.unmodifiableIterator(data.entrySet().iterator());
  }
}
