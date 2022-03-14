// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpv.partition;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;

/**
 * This partitioning operator puts all the given properties in one partition and uses all the given
 * resources for it.
 */
public final class NoPartitioningOperator extends AbstractPartitioningOperator {

  public NoPartitioningOperator(MultipleProperties pProperties, TimeSpan pTimeLimitPerProperty) {
    super(pProperties, pTimeLimitPerProperty);
  }

  @Override
  public ImmutableList<Partition> createPartitions() {
    return createJointPartition(
        getProperties(), scaleTimeLimit(getProperties().getNumberOfProperties()), true);
  }
}
