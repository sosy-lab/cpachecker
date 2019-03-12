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
