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
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.AbstractSingleProperty;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;

abstract class AbstractPartitioningOperator implements PartitioningOperator {

  private final MultipleProperties properties; // multiple properties
  private final TimeSpan timeLimitPerProperty; // resource limitations per each property
  private int phase;

  protected AbstractPartitioningOperator(
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
