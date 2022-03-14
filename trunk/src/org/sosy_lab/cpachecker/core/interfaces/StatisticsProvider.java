// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Collection;

/** Interface for classes that provide statistics. This can be CPAs and algorithms. */
public interface StatisticsProvider {

  /**
   * Add a {@link Statistics} object from this provider to a collection.
   *
   * <p>The provider is free to add zero, one or more objects. However it SHOULD not make any other
   * modifications to the collection.
   *
   * @param statsCollection The collection where the statistics are added.
   */
  void collectStatistics(Collection<Statistics> statsCollection);
}
