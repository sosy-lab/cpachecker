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

import javax.management.JMException;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;

public final class Partition {
  private final MultipleProperties properties;
  private TimeSpan partitionTimeLimit;
  private final boolean isAssignUnknown;
  private long cpuTime = 0;
  private TimeSpan spentCpuTime = TimeSpan.ofNanos(-1);

  public Partition(
      MultipleProperties pProperties, TimeSpan pPartitionTimeLimit, boolean pIsAssignUnknown) {
    properties = pProperties;
    partitionTimeLimit = pPartitionTimeLimit;
    isAssignUnknown = pIsAssignUnknown;
  }

  public boolean isIntermediateStep() {
    return isAssignUnknown;
  }

  public void startAnalysis() {
    try {
      cpuTime = ProcessCpuTime.read();
    } catch (JMException | NoClassDefFoundError e) {
      // user was already warned in MainCPAStatistics
      cpuTime = -1;
    }
  }

  private void stopAnalysisOnSuccess(ReachedSet reached) {
    properties.stopAnalysisOnSuccess();
    stopAnalysis(reached);
  }

  public void stopAnalysisOnFailure(ReachedSet reached, String reason) {
    if (isAssignUnknown) {
      properties.stopAnalysisOnFailure(reason);
    }
    stopAnalysis(reached);
  }

  private void stopAnalysis(ReachedSet reached) {
    spentCpuTime = getSpentCPUTime();
    properties.divideSpentResources(spentCpuTime, reached);
  }

  public boolean isChecked(ReachedSet reached) {
    // Check for property violations
    if (reached.hasViolatedProperties()) {
      properties.processPropertyViolation(reached);
    }
    if (!reached.hasWaitingState()
        || (properties.isChecked() && !properties.isFindAllViolations())) {
      stopAnalysisOnSuccess(reached);
      return true;
    }
    return false;
  }

  public MultipleProperties getProperties() {
    return properties;
  }

  public int getNumberOfProperties() {
    return properties.getNumberOfProperties();
  }

  public TimeSpan getTimeLimit() {
    return partitionTimeLimit;
  }

  public void updateTimeLimit(TimeSpan timeLimit) {
    partitionTimeLimit = timeLimit;
  }

  public TimeSpan getSpentCPUTime() {
    if (spentCpuTime.asMillis() > 0) {
      return spentCpuTime;
    }
    try {
      long stopCpuTime = ProcessCpuTime.read();
      if (cpuTime >= 0) {
        cpuTime = stopCpuTime - cpuTime;
      }
    } catch (JMException | NoClassDefFoundError e) {
      // user was already warned in MainCPAStatistics
    }
    return TimeSpan.ofNanos(cpuTime);
  }

  @Override
  public String toString() {
    return properties.toString();
  }
}
