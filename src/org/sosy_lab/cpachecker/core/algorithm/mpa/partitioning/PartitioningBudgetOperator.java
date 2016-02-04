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

import javax.annotation.Nonnull;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.BasicPartitionBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.DefaultPropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PartitionBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PropertyBudgeting;

@Options
abstract class PartitioningBudgetOperator extends AbstractPartitioningOperator {

  @Option(secure=true, name="budgeting.property.operator",
      description = "Operator that defines the (remaining) budget for a property.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting")
  @Nonnull private Class<? extends PropertyBudgeting> propertyBudgetingOperatorClass = DefaultPropertyBudgeting.class;
  private final PropertyBudgeting propertyBudgetingOperator;

  @Option(secure=true, name="budgeting.partition.operator",
      description = "Operator that defines the (remaining) budget for a property.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting")
  @Nonnull private Class<? extends PartitionBudgeting> partitionBudgetingOperatorClass = BasicPartitionBudgeting.class;
  private final PartitionBudgeting partitionBudgetingOperator;

  public PartitioningBudgetOperator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);

    propertyBudgetingOperator = createPropertyBudgetingOperator(pConfig, pLogger, propertyBudgetingOperatorClass);
    partitionBudgetingOperator = createPartitionBudgetingOperator(pConfig, pLogger, partitionBudgetingOperatorClass);
  }

  protected PartitionBudgeting createPartitionBudgetingOperator(Configuration pConfig, LogManager pLogger,
      Class<? extends PartitionBudgeting> pClass)
      throws InvalidConfigurationException {
    return Classes.createInstance(PartitionBudgeting.class, pClass,
        new Class[] { Configuration.class, LogManager.class },
        new Object[] { pConfig, pLogger },
        InvalidConfigurationException.class);
  }

  protected PropertyBudgeting createPropertyBudgetingOperator(Configuration pConfig, LogManager pLogger,
      Class<? extends PropertyBudgeting> pClass)
      throws InvalidConfigurationException {
    return Classes.createInstance(PropertyBudgeting.class, pClass,
        new Class[] { Configuration.class, LogManager.class },
        new Object[] { pConfig, pLogger },
        InvalidConfigurationException.class);
  }

  protected PropertyBudgeting getPropertyBudgetingOperator() {
    return propertyBudgetingOperator;
  }

  public PartitionBudgeting getPartitionBudgetingOperator() {
    return partitionBudgetingOperator;
  }

}
