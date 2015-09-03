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

/**
 * A StatisticsProvider is providing some kind of Statistics for the StatisticsCPA.
 * For example the function-call-count or some other metric.
 * The data is tracked in an StatisticsDataProvider instance.
 * All instances of this interface should be immutable (there should be no need for mutable state).
 * Instances of this class are used in a HashMap and
 * should therefore prefer to not implement hashCode if you
 * want to be able to use multiple instances of this Provider.
 */
public interface StatisticsProvider {
  /**
   * The name of the metric this provider provides.
   */
  String getPropertyName();
  /**
   * The type of merging this provider is configured for
   */
  String getMergeType();

  /**
   * Creates an initial State with some initial data for the metric.
   */
  StatisticsDataProvider createDataProvider();
}
