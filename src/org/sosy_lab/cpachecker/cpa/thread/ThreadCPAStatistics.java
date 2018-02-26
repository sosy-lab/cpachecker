/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.thread;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;


public class ThreadCPAStatistics extends AbstractStatistics {

  public final StatTimer transfer = new StatTimer("Time for transfer relation");
  public final StatTimer tSetTimer = new StatTimer("Time for thread sets update");
  public final StatTimer internalCPAtimer = new StatTimer("Time for internal CPAs");
  public final StatTimer internalLocationTimer = new StatTimer("Time for Location CPA");
  public final StatTimer internalCallstackTimer = new StatTimer("Time for Callstack CPA");
  public final StatCounter threadCreates = new StatCounter("Number of thread creates");
  public final StatCounter threadJoins = new StatCounter("Number of thread joins");
  public final StatInt maxNumberOfThreads = new StatInt(StatKind.COUNT, "Max number of threads");
  public final Set<String> createdThreads = new HashSet<>();

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
    writer.put(transfer)
          .put(tSetTimer)
          .put(internalCPAtimer)
          .put(internalLocationTimer)
          .put(internalCallstackTimer)
          .put(threadCreates)
          .put("Names of created threads:", createdThreads)
          .put(threadJoins)
          .put(maxNumberOfThreads);
  }

  @Override
  public String getName() {
    return "ThreadCPA";
  }

}
