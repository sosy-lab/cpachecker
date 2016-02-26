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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpa.PropertyStats;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.InfinitePropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning.PartitioningStatus;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@Options
public class RelevanceThenIrrelevantThenRelevantOperator extends PartitioningBudgetOperator {

  static private int irrelevantIteration = 0; // iterations of step 2
  static private int lastRelevantProperties = 0;

  public RelevanceThenIrrelevantThenRelevantOperator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
  }

  @Override
  public Partitioning partition(
      Partitioning pLastCheckedPartitioning,
      Set<Property> pToCheck, Set<Property> pExpensiveProperties,
      Comparator<Property> pPropertyExpenseComparator)
          throws PartitioningException {

    PartitioningStatus lastStatus = pLastCheckedPartitioning.getStatus();


    if (pLastCheckedPartitioning.isEmpty()) {
      // Step 1 - determine relevant properties
      logger.logf(Level.INFO, "Step 1 of Relevance algorithm: determine all relevant properties");
      return create(PartitioningStatus.ALL_IN_ONE_SHORT, getPropertyBudgetingOperator(), getPartitionBudgetingOperator(),
          ImmutableList.of(ImmutableSet.copyOf(pToCheck)));
    }

    if (lastStatus.equals(PartitioningStatus.ALL_IN_ONE_SHORT)) {
      // Step 2 - check all supposed irrelevant properties; iteration 1
      irrelevantIteration = 1;
      logger.logf(Level.INFO, "Step 2 of Relevance algorithm: verify all supposed irrelevant properties");
      logger.log(Level.INFO, String.format("Iteration %d. ", irrelevantIteration));
      Set<Property> relevantProperties = PropertyStats.INSTANCE.getRelevantProperties();
      Set<Property> irrelevantProperties = new HashSet<>();
      for (Property propertyToCheck : pToCheck) {
        if (!relevantProperties.contains(propertyToCheck)) {
          irrelevantProperties.add(propertyToCheck);
        }
      }
      lastRelevantProperties = relevantProperties.size();
      return create(PartitioningStatus.ALL_IN_ONE, getPropertyBudgetingOperator(), getPartitionBudgetingOperator(),
          ImmutableList.of(ImmutableSet.copyOf(irrelevantProperties)));
    }

    if (lastStatus.equals(PartitioningStatus.ALL_IN_ONE)) {
      // Step 2; iterations 2+
      Set<Property> relevantProperties = PropertyStats.INSTANCE.getRelevantProperties();
      Set<Property> irrelevantProperties = new HashSet<>();
      for (Property propertyToCheck : pToCheck) {
        if (!relevantProperties.contains(propertyToCheck)) {
          irrelevantProperties.add(propertyToCheck);
        }
      }
      int newRelevantProperties = relevantProperties.size();
      int newIrrelevantProperties = irrelevantProperties.size();
      boolean isMoreIrrelevantProperties = (newIrrelevantProperties > 0);
      boolean isNewRelevant = (lastRelevantProperties < newRelevantProperties);
      if (isMoreIrrelevantProperties && isNewRelevant) {
        lastRelevantProperties = newRelevantProperties;
        logger.logf(Level.INFO, "Step 2 was not completed");
        irrelevantIteration++;
        logger.log(Level.INFO, String.format("Starting iteration %d. ", irrelevantIteration));
        return create(PartitioningStatus.ALL_IN_ONE, getPropertyBudgetingOperator(), getPartitionBudgetingOperator(),
            ImmutableList.of(ImmutableSet.copyOf(irrelevantProperties)));
      } else {
        logger.log(Level.INFO, "Step 2 has been completed");
        // proceed on the step 3
      }

    }


    // Step 3 - check all relevant properties
    logger.logf(Level.INFO, "Step 3 of Relevance algorithm: verify all remaining relevant properties");
    return create(PartitioningStatus.ONE_FOR_EACH, InfinitePropertyBudgeting.INSTANCE, getPartitionBudgetingOperator(),
        singletonPartitions(pToCheck, pPropertyExpenseComparator));

  }

}
