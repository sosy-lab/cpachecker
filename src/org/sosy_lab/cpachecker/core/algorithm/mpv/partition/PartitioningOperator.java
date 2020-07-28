// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
