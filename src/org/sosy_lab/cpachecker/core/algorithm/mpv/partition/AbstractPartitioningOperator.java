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
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.AbstractSingleProperty;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;

abstract class AbstractPartitioningOperator implements PartitioningOperator {

  private final MultipleProperties properties; // multiple properties
  private final TimeSpan timeLimitPerProperty; // resource limitations per each property
  private int phase;

  public AbstractPartitioningOperator(
      MultipleProperties pProperties, TimeSpan pTimeLimitPerProperty) {
    properties = pProperties;
    timeLimitPerProperty = pTimeLimitPerProperty;
    phase = 1;
  }

  /*
   * Create a separate partition for each unchecked property.
   */
  protected ImmutableList<Partition> createSeparatedPartition(MultipleProperties targetProperties) {
    ImmutableList.Builder<Partition> builder = ImmutableList.builder();
    for (AbstractSingleProperty property : targetProperties.getProperties()) {
      if (property.isNotChecked()) {
        MultipleProperties singleProperty =
            new MultipleProperties(
                ImmutableList.of(property), targetProperties.isFindAllViolations());
        builder.add(new Partition(singleProperty, timeLimitPerProperty, true));
      }
    }
    return builder.build();
  }

  /*
   * Create a joint partition with currently unchecked properties.
   */
  protected ImmutableList<Partition> createJointPartition(
      MultipleProperties targetProperties, TimeSpan timeLimit, boolean isFinal) {
    ImmutableList.Builder<AbstractSingleProperty> propertyBuilder = ImmutableList.builder();
    for (AbstractSingleProperty property : targetProperties.getProperties()) {
      if (property.isNotChecked()) {
        propertyBuilder.add(property);
      }
    }
    MultipleProperties irrelevantProperties =
        new MultipleProperties(propertyBuilder.build(), targetProperties.isFindAllViolations());
    return ImmutableList.of(new Partition(irrelevantProperties, timeLimit, isFinal));
  }

  protected TimeSpan scaleTimeLimit(double ratio) {
    return TimeSpan.ofMillis(Math.round(ratio * timeLimitPerProperty.asMillis()));
  }

  protected MultipleProperties getProperties() {
    return properties;
  }

  protected int currentPhase() {
    return phase;
  }

  protected void nextPhase() {
    phase++;
  }
}
