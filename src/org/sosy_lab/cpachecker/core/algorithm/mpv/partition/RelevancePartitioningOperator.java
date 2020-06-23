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
 * This partitioning operator takes 3 steps:<br>
 * 1. put all the given properties in one partition with the given resource limitation (similar to
 * {@link NoPartitioningOperator});<br>
 * 2. put all potentially irrelevant properties, which were not successfully checked on the first
 * step, in one partition with the given resource limitation (similar to {@link
 * NoPartitioningOperator});<br>
 * 3. put each relevant property, which was not checked on the previous steps, in the separate
 * partition (similar to {@link SeparatePartitioningOperator}).<br>
 */
@Options(prefix = "mpv")
public final class RelevancePartitioningOperator extends AbstractPartitioningOperator {

  @Option(
      secure = true,
      name = "limits.relevance.firstPhaseRatio",
      description =
          "The ratio of CPU time limit in the first phase of Relevance partitioning operator "
              + "to CPU time limit per each property.")
  private double firstPhaseRatio = 0.2;

  @Option(
      secure = true,
      name = "limits.relevance.secondPhaseRatio",
      description =
          "The ratio of CPU time limit in the second phase of Relevance partitioning operator "
              + "to CPU time limit per each property.")
  private double secondPhaseRatio = 1.3;

  private int lastNumberOfIrrelevantProperties;

  public RelevancePartitioningOperator(
      Configuration pConfiguration, MultipleProperties pProperties, TimeSpan pTimeLimitPerProperty)
      throws InvalidConfigurationException {
    super(pProperties, pTimeLimitPerProperty);
    pConfiguration.inject(this);
    lastNumberOfIrrelevantProperties = 0;
  }

  @Override
  public ImmutableList<Partition> createPartitions() {
    if (currentPhase() == 1) {
      nextPhase();
      return createJointPartition(getProperties(), scaleTimeLimit(firstPhaseRatio), false);
    } else {
      MultipleProperties irrelevant = getProperties().createIrrelevantProperties();
      int curNumberOfIrrelevantProperties = irrelevant.getNumberOfProperties();
      if (lastNumberOfIrrelevantProperties != curNumberOfIrrelevantProperties) {
        lastNumberOfIrrelevantProperties = curNumberOfIrrelevantProperties;
        return createJointPartition(irrelevant, scaleTimeLimit(secondPhaseRatio), false);
      } else {
        if (curNumberOfIrrelevantProperties > 0) {
          irrelevant.stopAnalysisOnFailure("Relavance algorithm, second phase");
        }
        return createSeparatedPartition(getProperties());
      }
    }
  }
}
