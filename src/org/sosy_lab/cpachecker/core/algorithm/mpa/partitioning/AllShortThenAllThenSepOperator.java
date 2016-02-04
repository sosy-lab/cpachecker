/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.DefaultPropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.InfinitePropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PartitionBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning.PartitioningStatus;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@Options
public class AllShortThenAllThenSepOperator extends PartitioningBudgetOperator {

  @Options(prefix="analysis.mpa.partition.short")
  public static class ShortPartitionBudgeting implements PartitionBudgeting {

    @Option(secure=true, name="time.wall",
        description="Limit for wall time used by CPAchecker (use seconds or specify a unit; -1 for infinite)")
    @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
        defaultUserUnit=TimeUnit.SECONDS,
        min=-1)
    protected TimeSpan wallTime = TimeSpan.ofNanos(-1);

    @Option(secure=true, name="time.cpu",
        description="Limit for cpu time used by CPAchecker (use seconds or specify a unit; -1 for infinite)")
    @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
        defaultUserUnit=TimeUnit.SECONDS,
        min=-1)
    protected TimeSpan cpuTime = TimeSpan.ofNanos(-1);

    protected final int budgetFactor;

    protected final LogManager logger;

    public ShortPartitionBudgeting(Configuration pConfig, LogManager pLogger)
        throws InvalidConfigurationException {
      pConfig.inject(this);
      logger = pLogger;
      budgetFactor = 1;
    }

    protected ShortPartitionBudgeting(LogManager pLogger, TimeSpan pCpuTime, TimeSpan pWallTime, int pBudgetFactor) {
      logger = pLogger;
      cpuTime = pCpuTime;
      wallTime = pWallTime;
      budgetFactor = pBudgetFactor;
    }

    @Override
    public PartitionBudgeting getBudgetTimesTwo() {
      return new ShortPartitionBudgeting(logger, cpuTime, wallTime, budgetFactor * 2);
    }

    @Override
    public Optional<TimeSpan> getPartitionWallTimeLimit() {
      if (wallTime.compareTo(TimeSpan.empty()) >= 0) {
        return Optional.of(wallTime.multiply(budgetFactor));
      }
      return Optional.absent();
    }

    @Override
    public Optional<TimeSpan> getPartitionCpuTimeLimit() {
      if (cpuTime.compareTo(TimeSpan.empty()) >= 0) {
        return Optional.of(cpuTime.multiply(budgetFactor));
      }
      return Optional.absent();
    }

  }

  @Options(prefix="analysis.mpa.budget.short")
  public static class ShortPropertyBudgeting extends DefaultPropertyBudgeting {

    public ShortPropertyBudgeting(Configuration pConfig, LogManager pLogger)
        throws InvalidConfigurationException {
      super(pConfig, pLogger);
    }

  }

  private final PropertyBudgeting shortPropertyBudgeting;
  private final PartitionBudgeting shortPartitionBudgeting;

  public AllShortThenAllThenSepOperator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {

    super(pConfig, pLogger);

    shortPropertyBudgeting = createPropertyBudgetingOperator(pConfig, pLogger, ShortPropertyBudgeting.class);
    shortPartitionBudgeting = createPartitionBudgetingOperator(pConfig, pLogger, ShortPartitionBudgeting.class);
  }

  @Override
  public Partitioning partition(
      Partitioning pLastCheckedPartitioning,
      Set<Property> pToCheck, Set<Property> pExpensiveProperties,
      Comparator<Property> pPropertyExpenseComparator)
          throws PartitioningException {

    switch(pLastCheckedPartitioning.getStatus()) {
    case ALL_IN_ONE_SHORT:
      return create(PartitioningStatus.ALL_IN_ONE, getPropertyBudgetingOperator(), getPartitionBudgetingOperator(),
          ImmutableList.of(ImmutableSet.copyOf(pToCheck)));
    case ALL_IN_ONE:
      return create(PartitioningStatus.ONE_FOR_EACH, InfinitePropertyBudgeting.INSTANCE, getPartitionBudgetingOperator(),
          singletonPartitions(pToCheck, pPropertyExpenseComparator));
    default:
      return create(PartitioningStatus.ALL_IN_ONE_SHORT, shortPropertyBudgeting, shortPartitionBudgeting,
          ImmutableList.of(ImmutableSet.copyOf(pToCheck)));
    }

  }

}
