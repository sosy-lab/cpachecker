/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import com.google.common.base.Strings;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * Collects statistics and forwards all calls of {@link #printStatistics(PrintStream, Result,
 * UnmodifiableReachedSet)} to them. This class can be useful if subcomponents of an algorithm
 * provide statistics, but can only be created at a time when all statistics objects have already
 * been collected. In this case, the subcomponents can register themselves if this
 * StatisticsDelegator is passed to their constructor as additional parameter.
 */
public class StatisticsDelegator implements Statistics {

  private final List<Statistics> delegates;
  private final @Nullable String name;

  /**
   * Creates a new StatisticsDelegator with the provided name and no registered sub-statistics.
   *
   * @param pName The name of this StatisticsDelegator that will be printed. May be null if nothing
   *     should be printed instead.
   */
  public StatisticsDelegator(String pName) {
    this.delegates = new LinkedList<>();
    this.name = pName;
  }

  /**
   * Adds the provided statistics object to the list of clients whose content will be printed.
   *
   * @param pDelegate The new client statistics to be registered.
   */
  public void register(Statistics pDelegate) {
    if (!delegates.contains(Objects.requireNonNull(pDelegate))) {
      delegates.add(pDelegate);
    }
  }

  /**
   * Removes the provided statistics object from the list of clients whose content will be printed.
   *
   * @param pDelegate The client statistics to be removed.
   */
  public void unregister(Statistics pDelegate) {
    delegates.remove(Objects.requireNonNull(pDelegate));
  }

  /** Removes all registered statistics. */
  public void unregisterAll() {
    delegates.clear();
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    for (Statistics delegate : delegates) {
      String name = delegate.getName();

      if (!Strings.isNullOrEmpty(name)) {
        name = name + " statistics";
        pOut.println(name);
        pOut.println(Strings.repeat("-", name.length()));
      }

      delegate.printStatistics(pOut, pResult, pReached);

      if (!Strings.isNullOrEmpty(name)) {
        pOut.println();
      }
    }
  }

  @Override
  public @Nullable String getName() {
    return name;
  }
}
