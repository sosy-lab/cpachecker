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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpa.PropertyStats;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.InfinitePropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PartitionBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning.PartitioningStatus;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Options(prefix="analysis.mpa.partition")
public class AllThenIrrelevantThenRelevantSepOperator extends PartitioningBudgetOperator {

  @Option(secure=true, name="first.time.cpu",
      description="1st partition limit for cpu time used by CPAchecker (use seconds or specify a unit; -1 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS, defaultUserUnit=TimeUnit.SECONDS, min=-1)
  protected TimeSpan firstCpuTime = TimeSpan.ofNanos(-1);

  @Option(secure=true, name="second.time.cpu",
      description="1st partition limit for cpu time used by CPAchecker (use seconds or specify a unit; -1 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS, defaultUserUnit=TimeUnit.SECONDS, min=-1)
  protected TimeSpan secondCpuTime = TimeSpan.ofNanos(-1);

  private final PartitionBudgeting firstPartitionBudgeting;
  private final PartitionBudgeting secondPartitionBudgeting;

  public AllThenIrrelevantThenRelevantSepOperator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);

    if (firstCpuTime.isEmpty()) {
      firstPartitionBudgeting = getPartitionBudgetingOperator();
    } else {
      firstPartitionBudgeting = createPartitionBudgetingOperator(firstCpuTime, TimeSpan.empty(), pLogger);
    }

    if (secondCpuTime.isEmpty()) {
      secondPartitionBudgeting = getPartitionBudgetingOperator();
    } else {
      secondPartitionBudgeting = createPartitionBudgetingOperator(secondCpuTime, TimeSpan.empty(), pLogger);
    }
  }

  @Override
  public Partitioning partition(
      Partitioning pLastCheckedPartitioning,
      Set<Property> pToCheck, Set<Property> pExpensiveProperties,
      Comparator<Property> pPropertyExpenseComparator)
          throws PartitioningException {

    final PartitioningStatus lastType = pLastCheckedPartitioning.getStatus();

    switch (lastType) {
      case NONE:
        return create(PartitioningStatus.ALL_IN_ONE,
            getPropertyBudgetingOperator(),
            firstPartitionBudgeting,
            ImmutableList.of(ImmutableSet.copyOf(pToCheck)));

      case ALL_IN_ONE:
        final Set<Property> irrelevantProperties = Sets.difference(pToCheck, PropertyStats.INSTANCE.getRelevantProperties());

        return create(PartitioningStatus.IRRELEVANT,
            getPropertyBudgetingOperator(),
            secondPartitionBudgeting,
            ImmutableList.of(ImmutableSet.copyOf(irrelevantProperties)));

      case IRRELEVANT:
        final Set<Property> relevantToCheck = Sets.intersection(pToCheck, PropertyStats.INSTANCE.getRelevantProperties());

        return create(PartitioningStatus.ONE_FOR_EACH,
            InfinitePropertyBudgeting.INSTANCE,
            getPartitionBudgetingOperator(),
            singletonPartitions(relevantToCheck, pPropertyExpenseComparator));

      default:
        throw new IllegalStateException("This case should not be entered. Bug!");
    }

  }

}
