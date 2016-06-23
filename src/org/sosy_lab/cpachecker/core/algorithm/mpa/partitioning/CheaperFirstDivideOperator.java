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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

@Options
public class CheaperFirstDivideOperator extends PartitioningBudgetOperator {

  public CheaperFirstDivideOperator(Configuration pConfig, LogManager pLogger)
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

    final List<Property> propertiesByCost = Lists.newLinkedList(pToCheck);
    Collections.sort(propertiesByCost, pPropertyExpenseComparator);

    ImmutableList<ImmutableSet<Property>> newPartitions = ImmutableList.of(ImmutableSet.copyOf(pToCheck));

    while (newPartitions.size() < partitionsToCreate) {
      newPartitions = bisectPartitons(newPartitions, pPropertyExpenseComparator);
    }

    return Partitions.partitions(PartitioningStatus.MORE_PARTITIONS,
        InfinitePropertyBudgeting.INSTANCE, // The expense comparison and the partitioning is responsible for this.
        getPartitionBudgetingOperator(),
        newPartitions);
  }

}
