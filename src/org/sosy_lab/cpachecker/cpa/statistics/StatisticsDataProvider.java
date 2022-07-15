// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.statistics;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Represents Data of an StatisticsProvider. The data provider takes care of tracking the current
 * value, calculating the next value and merging paths. All instances of this interface should be
 * immutable. (Create new StatisticsDataProvider instances for new data)
 */
public interface StatisticsDataProvider {
  Object getPropertyValue();

  StatisticsDataProvider calculateNext(CFAEdge node);

  StatisticsDataProvider mergePath(StatisticsDataProvider other);
}
