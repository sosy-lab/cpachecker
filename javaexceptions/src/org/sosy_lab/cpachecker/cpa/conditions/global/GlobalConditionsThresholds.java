// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.conditions.global;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;

@Options(prefix = "cpa.conditions.global")
class GlobalConditionsThresholds {

  @Option(
      secure = true,
      name = "reached.size",
      description = "Limit for size of reached set (-1 for infinite)")
  @IntegerOption(min = -1)
  private int reachedSetSize = -1;

  @Option(
      secure = true,
      name = "time.wall",
      description =
          "Limit for wall time used by CPAchecker (use milliseconds or specify a unit; -1 for"
              + " infinite)")
  @TimeSpanOption(
      codeUnit = TimeUnit.MILLISECONDS,
      defaultUserUnit = TimeUnit.MILLISECONDS,
      min = -1)
  private long wallTime = -1;

  private long wallEndTime; // when to end analysis (according to wall time limit)

  @Option(
      secure = true,
      name = "time.wall.hardlimit",
      description =
          "Hard limit for wall time used by CPAchecker (use milliseconds or specify a unit; -1 for"
              + " infinite)\n"
              + "When using adjustable conditions, analysis will end after this threshold")
  @TimeSpanOption(
      codeUnit = TimeUnit.MILLISECONDS,
      defaultUserUnit = TimeUnit.MILLISECONDS,
      min = -1)
  private long wallTimeHardLimit = -1;

  private long wallEndTimeHardLimit;

  @Option(
      secure = true,
      name = "time.cpu",
      description =
          "Limit for cpu time used by CPAchecker (use milliseconds or specify a unit; -1 for"
              + " infinite)")
  @TimeSpanOption(
      codeUnit = TimeUnit.MILLISECONDS,
      defaultUserUnit = TimeUnit.MILLISECONDS,
      min = -1)
  private long cpuTime = -1;

  private long cpuEndTime; // when to end analysis (according to cpu time limit)

  @Option(
      secure = true,
      name = "time.cpu.hardlimit",
      description =
          "Hard limit for cpu time used by CPAchecker (use milliseconds or specify a unit; -1 for"
              + " infinite)\n"
              + "When using adjustable conditions, analysis will end after this threshold")
  @TimeSpanOption(
      codeUnit = TimeUnit.MILLISECONDS,
      defaultUserUnit = TimeUnit.MILLISECONDS,
      min = -1)
  private long cpuTimeHardLimit = -1;

  @Option(
      secure = true,
      name = "memory.heap",
      description =
          "Limit for Java heap memory used by CPAchecker (in MB, not MiB!; -1 for infinite)")
  @IntegerOption(min = -1)
  private long heapMemory = -1;

  @Option(
      secure = true,
      name = "memory.process",
      description =
          "Limit for process memory used by CPAchecker (in MB, not MiB!; -1 for infinite)")
  @IntegerOption(min = -1)
  private long processMemory = -1;

  private final LogManager logger;
  private String humanReadableString;

  GlobalConditionsThresholds(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;

    if (wallTime >= 0) {
      wallEndTime = System.currentTimeMillis() + wallTime;
    } else {
      wallEndTime = Long.MAX_VALUE;
    }
    if (wallTimeHardLimit >= 0) {
      wallEndTimeHardLimit = System.currentTimeMillis() + wallTimeHardLimit;
    } else {
      wallEndTimeHardLimit = Long.MAX_VALUE;
    }
    wallEndTime = Math.min(wallEndTime, wallEndTimeHardLimit);

    cpuEndTime = cpuTime;

    humanReadableString = asHumanReadableString();
  }

  boolean adjustPrecision() {
    if (System.currentTimeMillis() >= wallEndTimeHardLimit) {
      return false; // to not continue
    }

    if (cpuEndTime >= 0 && cpuEndTime >= cpuTimeHardLimit) {
      // TODO check current cpu time instead of cpuEndTime
      return false; // do not continue
    }

    boolean changed = false;

    if (wallTime >= 0) {
      wallEndTime = System.currentTimeMillis() + wallTime;
      wallEndTime = Math.min(wallEndTime, wallEndTimeHardLimit);
      changed = true;
    }

    if (cpuTime >= 0) {
      cpuEndTime += cpuTime;
      cpuEndTime = Math.min(cpuEndTime, cpuTimeHardLimit);
      changed = true;
    }

    if (changed) {
      humanReadableString = asHumanReadableString();
      logger.log(Level.INFO, "Increased global condition thresholds.");
    }

    return true;
  }

  public boolean isLimitEnabled() {
    return (reachedSetSize >= 0)
        || (wallTime >= 0)
        || (cpuTime >= 0)
        || (heapMemory >= 0)
        || (processMemory >= 0);
  }

  private String asHumanReadableString() {
    if (!isLimitEnabled()) {
      return "global conditions: none";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("global conditions: ");

    if (reachedSetSize >= 0) {
      sb.append("reached set size: ");
      sb.append(reachedSetSize);
      sb.append("; ");
    }
    if (wallTime >= 0) {
      sb.append("timeout (walltime): ");
      sb.append(wallTime / 1000);
      sb.append(" s");
      if (wallTimeHardLimit >= 0) {
        sb.append(" (up to ");
        sb.append(wallTimeHardLimit / 1000);
        sb.append(" s)");
      }
      sb.append("; ");
    }
    if (cpuTime >= 0) {
      sb.append("timeout (cputime): ");
      sb.append(cpuTime / 1000);
      sb.append(" s; ");
      if (cpuTimeHardLimit >= 0) {
        sb.append(" (up to ");
        sb.append(cpuTimeHardLimit / 1000);
        sb.append(" s)");
      }
    }
    if (heapMemory >= 0) {
      sb.append("heap usage limit: ");
      sb.append(heapMemory);
      sb.append(" MiB; ");
    }
    if (processMemory >= 0) {
      sb.append("process memory limit: ");
      sb.append(processMemory);
      sb.append(" MiB; ");
    }

    sb.setLength(sb.length() - 2); // remove trailing "; "

    return sb.toString();
  }

  int getReachedSetSizeThreshold() {
    return reachedSetSize;
  }

  long getWallTimeThreshold() {
    return wallEndTime;
  }

  long getCpuTimeThreshold() {
    return cpuEndTime;
  }

  public long getHeapMemoryThreshold() {
    return heapMemory;
  }

  public long getProcessMemoryThreshold() {
    return processMemory;
  }

  @Override
  public String toString() {
    return humanReadableString;
  }
}
