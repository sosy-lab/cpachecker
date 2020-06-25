// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpv.partition;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;

/**
 * This partitioning operator takes 2 steps:<br>
 * 1. put all the given properties in one partition with the given resource limitation (similar to
 * {@link NoPartitioningOperator});<br>
 * 2. put each unchecked on the first step property in the separate partition (similar to {@link
 * SeparatePartitioningOperator}).<br>
 */
@Options(prefix = "mpv")
public final class JointPartitioningOperator extends AbstractPartitioningOperator {

  @Option(
      secure = true,
      name = "limits.joint.firstPhaseRatio",
      description =
          "The ratio of CPU time limit in the first phase of Joint partitioning operator "
              + "to CPU time limit per each property.")
  private double firstPhaseRatio = 1.3;

  public JointPartitioningOperator(
      Configuration pConfiguration, MultipleProperties pProperties, TimeSpan pTimeLimitPerProperty)
      throws InvalidConfigurationException {
    super(pProperties, pTimeLimitPerProperty);
    pConfiguration.inject(this);
  }

  @Override
  public ImmutableList<Partition> createPartitions() {
    if (currentPhase() == 1) {
      nextPhase();
      return createJointPartition(getProperties(), scaleTimeLimit(firstPhaseRatio), false);
    } else {
      return createSeparatedPartition(getProperties());
    }
  }
}
