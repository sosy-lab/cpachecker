/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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

@Options(prefix="cpa.conditions.global")
class GlobalConditionsThresholds {

  @Option(secure=true, name="reached.size",
      description="Limit for size of reached set (-1 for infinite)")
  @IntegerOption(min=-1)
  private int reachedSetSize = -1;


  @Option(secure=true, name="time.wall",
      description="Limit for wall time used by CPAchecker (use milliseconds or specify a unit; -1 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit=TimeUnit.MILLISECONDS,
      min=-1)
  private long wallTime = -1;

  private long wallEndTime; // when to end analysis (according to wall time limit)

  @Option(secure=true, name="time.wall.hardlimit",
      description="Hard limit for wall time used by CPAchecker (use milliseconds or specify a unit; -1 for infinite)" +
                  "\nWhen using adjustable conditions, analysis will end after this threshold")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit=TimeUnit.MILLISECONDS,
      min=-1)
  private long wallTimeHardLimit = -1;

  private long wallEndTimeHardLimit;

  @Option(secure=true, name="time.cpu",
      description="Limit for cpu time used by CPAchecker (use milliseconds or specify a unit; -1 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit=TimeUnit.MILLISECONDS,
      min=-1)
  private long cpuTime = -1;

  private long cpuEndTime;  // when to end analysis (according to cpu time limit)

  @Option(secure=true, name="time.cpu.hardlimit",
      description="Hard limit for cpu time used by CPAchecker (use milliseconds or specify a unit; -1 for infinite)" +
          "\nWhen using adjustable conditions, analysis will end after this threshold")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit=TimeUnit.MILLISECONDS,
      min=-1)
  private long cpuTimeHardLimit = -1;


  @Option(secure=true, name="memory.heap",
      description="Limit for Java heap memory used by CPAchecker (in MB, not MiB!; -1 for infinite)")
  @IntegerOption(min=-1)
  private long heapMemory = -1;

  @Option(secure=true, name="memory.process",
      description="Limit for process memory used by CPAchecker (in MB, not MiB!; -1 for infinite)")
  @IntegerOption(min=-1)
  private long processMemory = -1;

  private final LogManager logger;
  private String humanReadableString;

  GlobalConditionsThresholds(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
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
      wallEndTime = Math.min(wallEndTime,  wallEndTimeHardLimit);
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
      sb.append(wallTime/1000);
      sb.append(" s");
      if (wallTimeHardLimit >= 0) {
        sb.append(" (up to ");
        sb.append(wallTimeHardLimit/1000);
        sb.append(" s)");
      }
      sb.append("; ");
    }
    if (cpuTime >= 0) {
      sb.append("timeout (cputime): ");
      sb.append(cpuTime/1000);
      sb.append(" s; ");
      if (cpuTimeHardLimit >= 0) {
        sb.append(" (up to ");
        sb.append(cpuTimeHardLimit/1000);
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

    sb.setLength(sb.length()-2); // remove trailing "; "

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