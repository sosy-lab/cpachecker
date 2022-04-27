// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.statistics;

/**
 * A StatisticsProvider is providing some kind of Statistics for the StatisticsCPA. For example the
 * function-call-count or some other metric. The data is tracked in an StatisticsDataProvider
 * instance. All instances of this interface should be immutable (there should be no need for
 * mutable state). Instances of this class are used in a HashMap and should therefore prefer to not
 * implement hashCode if you want to be able to use multiple instances of this Provider.
 */
public interface StatisticsProvider {
  /** The name of the metric this provider provides. */
  String getPropertyName();
  /** The type of merging this provider is configured for */
  String getMergeType();

  /** Creates an initial State with some initial data for the metric. */
  StatisticsDataProvider createDataProvider();
}
