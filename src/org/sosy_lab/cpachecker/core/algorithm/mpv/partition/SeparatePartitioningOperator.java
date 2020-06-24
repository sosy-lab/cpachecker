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
 * This partitioning operator creates a separate partition for each property and distributes the
 * given resources between them equally.
 */
public final class SeparatePartitioningOperator extends AbstractPartitioningOperator {

  public SeparatePartitioningOperator(
      MultipleProperties pProperties, TimeSpan pTimeLimitPerProperty) {
    super(pProperties, pTimeLimitPerProperty);
  }

  @Override
  public ImmutableList<Partition> createPartitions() {
    return createSeparatedPartition(getProperties());
  }
}
