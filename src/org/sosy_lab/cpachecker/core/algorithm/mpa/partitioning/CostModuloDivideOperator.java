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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.InfinitePropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning.PartitioningStatus;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Options
public class CostModuloDivideOperator extends PartitioningBudgetOperator {

  public CostModuloDivideOperator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
  }

  @Override
  public Partitioning partition(Partitioning pLastCheckedPartitioning, Set<Property> pToCheck,
      Set<Property> pDisabledProperties, Comparator<Property> pPropertyExpenseComparator) throws PartitioningException {

    Preconditions.checkNotNull(pLastCheckedPartitioning);
    Preconditions.checkNotNull(pToCheck);
    Preconditions.checkNotNull(pDisabledProperties);
    Preconditions.checkNotNull(pPropertyExpenseComparator);

    final int partitionsToCreate =
        Math.min(pToCheck.size(),
            pLastCheckedPartitioning.partitionCount() == 0
            ? 1 : pLastCheckedPartitioning.partitionCount() * 2);

    final List<Set<Property>> partitions = Lists.newArrayListWithExpectedSize(partitionsToCreate);
    for (int p=0; p<partitionsToCreate; p++) {
      partitions.add(Sets.<Property>newHashSet());
    }

    final List<Property> propertiesByCost = Lists.newLinkedList(pToCheck);
    Collections.sort(propertiesByCost, pPropertyExpenseComparator);

    for (int rank=0; rank<propertiesByCost.size(); rank++) {
      final int targetPartitionIndex = rank % partitionsToCreate;
      partitions.get(targetPartitionIndex).add(propertiesByCost.get(rank));
    }

    return Partitions.partitions(PartitioningStatus.MORE_PARTITIONS,
        InfinitePropertyBudgeting.INSTANCE, // The expense comparison and the partitioning is responsible for this.
        getPartitionBudgetingOperator(),
        immutable(partitions));
  }

}
