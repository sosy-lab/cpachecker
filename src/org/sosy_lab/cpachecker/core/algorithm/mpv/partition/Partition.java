// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpv.partition;

import javax.management.JMException;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;

/**
 * Partition represents a group of multiple properties, which will be checked together in a single
 * algorithm run.
 */
public final class Partition {
  private final MultipleProperties properties;
  private TimeSpan partitionTimeLimit;
  private final boolean
      isAssignUnknown; // whether treat analysis failure as Unknown for checked properties or not
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

  /** Prepare partition for algorithm run. */
  public void startAnalysis() {
    try {
      cpuTime = ProcessCpuTime.read();
    } catch (JMException | NoClassDefFoundError e) {
      // user was already warned in MainCPAStatistics
      cpuTime = -1;
    }
  }

  /** Stop checking of the partition on algorithm failure. */
  public void stopAnalysisOnFailure(ReachedSet reached, String reason) {
    if (isAssignUnknown) {
      properties.stopAnalysisOnFailure(reason);
    }
    stopAnalysis(reached);
  }

  /**
   * Check if all properties received final result on algorithm successful stop or new property
   * violation has been found. In the first case analysis of this partition will be stopped,
   * otherwise it will be continued.
   */
  public boolean isChecked(ReachedSet reached) {
    // Check for property violations
    if (reached.hasViolatedProperties()) {
      properties.processPropertyViolation(reached);
    }
    // Stop if all properties have been checked
    if (!reached.hasWaitingState()
        || (properties.isChecked() && !properties.isFindAllViolations())) {
      stopAnalysisOnSuccess(reached);
      return true;
    }
    // Analysis was not completed - continue it
    return false;
  }

  private void stopAnalysis(ReachedSet reached) {
    spentCpuTime = getSpentCPUTime();
    properties.divideSpentResources(spentCpuTime, reached);
  }

  private void stopAnalysisOnSuccess(ReachedSet reached) {
    properties.stopAnalysisOnSuccess();
    stopAnalysis(reached);
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
