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

import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning.PartitioningStatus;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class AllThenNotExhaustedThenSepOperator extends AbstractPartitioningOperator {

  @Override
  public Partitioning partition(
      Partitioning pLastCheckedPartitioning,
      Set<Property> pToCheck, Set<Property> pExpensiveProperties,
      Comparator<Property> pPropertyExpenseComparator)
          throws PartitioningException {

    final Set<Property> cheapProperties = Sets.difference(pToCheck, pExpensiveProperties);

    switch(pLastCheckedPartitioning.getStatus()) {
    case ALL_IN_ONE:
      return create(PartitioningStatus.NOT_EXHAUSTED_ONLY, ImmutableList.of(ImmutableSet.copyOf(cheapProperties)));
    case NOT_EXHAUSTED_ONLY:
      return create(PartitioningStatus.ONE_FOR_EACH, singletonPartitions(pToCheck, pPropertyExpenseComparator));
    default:
      return create(PartitioningStatus.ALL_IN_ONE, ImmutableList.of(ImmutableSet.copyOf(pToCheck)));
    }

  }

}
