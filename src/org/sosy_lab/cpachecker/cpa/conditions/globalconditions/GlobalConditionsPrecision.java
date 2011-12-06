/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.conditions.globalconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

@Options(prefix="cpa.conditions.global")
class GlobalConditionsPrecision implements Precision {

  @Option(name="reached.size",
      description="Limit for size of reached set (-1 for infinite)")
  @IntegerOption(min=-1)
  private int reachedSetSize = -1;


  @Option(name="time.wall",
      description="Limit for wall time used by CPAchecker (in milliseconds; -1 for infinite)")
  @IntegerOption(min=-1)
  private long wallTime = -1;

  private long endTime; // when to end analysis (according to wall time limit)

  @Option(name="time.cpu",
      description="Limit for cpu time used by CPAchecker (in milliseconds; -1 for infinite)")
  @IntegerOption(min=-1)
  private long cpuTime = -1;


  @Option(name="memory.heap",
      description="Limit for Java heap memory used by CPAchecker (in MiB; -1 for infinite)")
  @IntegerOption(min=-1)
  private long heapMemory = -1;

  @Option(name="memory.process",
      description="Limit for process memory used by CPAchecker (in MiB; -1 for infinite)")
  @IntegerOption(min=-1)
  private long processMemory = -1;


  private final String humanReadableString;

  GlobalConditionsPrecision(Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    if (wallTime >= 0) {
      endTime = System.currentTimeMillis() + wallTime;
    } else {
      endTime = Long.MAX_VALUE;
    }
    humanReadableString = asHumanReadableString();
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
      sb.append(" s; ");
    }
    if (cpuTime >= 0) {
      sb.append("timeout (cputime): ");
      sb.append(cpuTime/1000);
      sb.append(" s; ");
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
    return endTime;
  }

  long getCpuTimeThreshold() {
    return cpuTime;
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