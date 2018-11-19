/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.mpv.partition;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;

/**
 * This operator distributes the given multiple properties into several partitions (groups) and
 * divides the given resources between each partition for multi-property verification algorithm.
 * Properties, which are in the same partition, will be checked in a single algorithm run. The main
 * idea of this operator is to separate properties into different partitions, which may cause state
 * space explosion in case of joint checking, and to unite all other properties for more efficient
 * verification.
 */
public interface PartitioningOperator {

  /** Creates list of partitions for multi-property verification algorithm. */
  public ImmutableList<Partition> createPartitions();

  interface Factory {
    PartitioningOperator create(
        Configuration pConfiguration,
        LogManager pLogger,
        ShutdownNotifier pShutdownManager,
        MultipleProperties pProperties,
        TimeSpan pTimeLimitPerProperty)
        throws InvalidConfigurationException;
  }
}
