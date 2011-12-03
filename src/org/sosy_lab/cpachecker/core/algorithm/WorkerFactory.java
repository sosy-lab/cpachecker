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
package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.worker.ConcurrentSuccessor;
import org.sosy_lab.cpachecker.core.algorithm.worker.ConcurrentSuccessorSingleThread;
import org.sosy_lab.cpachecker.core.algorithm.worker.ConcurrentWaitlist;
import org.sosy_lab.cpachecker.core.algorithm.worker.ConcurrentWaitlistSingleThread;
import org.sosy_lab.cpachecker.core.algorithm.worker.Sequential;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

@Options(prefix = "cpa.worker")
public class WorkerFactory {

  @Option(
      name = "strategy",
      values = { "sequential",
                  "concurrentwaitlist",
                  "concurrentsuccessor",
                  "concurrentwaitlistsingle",
                  "concurrentsuccessorsingle" },
      description = "decides which strategy shall be used to process CPA algorithm. also offers concurrent processingmodels.")
  private static String strategy = "sequential";

  public static Worker createNewInstance(ReachedSet reachedSet, ConfigurableProgramAnalysis cpa, LogManager logger,
      CPAStatistics stats) {
    if (strategy.equals("concurrentwaitlistsingle")) {
      return new ConcurrentWaitlistSingleThread(reachedSet, cpa, logger, stats);
    }
    else if (strategy.equals("concurrentsuccessorsingle")) {
      return new ConcurrentSuccessorSingleThread(reachedSet, cpa, logger, stats);
    }
    else if (strategy.equals("concurrentsuccessor")) {
      return new ConcurrentSuccessor(reachedSet, cpa, logger, stats);
    }
    else if (strategy.equals("concurrentwaitlist")) {
      return new ConcurrentWaitlist(reachedSet, cpa, logger, stats);
    }
    // strategy.equals("sequential")
    return new Sequential(reachedSet, cpa, logger, stats);
  }

}
