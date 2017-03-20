/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import java.io.PrintStream;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.BAMTransferRelation;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix="cpa.usage")
public class UsageCPAStatistics implements Statistics {

  public static enum OutputFileType {
    ETV,
    KLEVER
  }

  @Option(name="outputType", description="all variables should be printed to the one file or to the different")
  private OutputFileType outputFileType = OutputFileType.KLEVER;

  /* Previous container is used when internal time limit occurs
   * and we need to store statistics. In current one the information can be not
   * relevant (for example not all ARG was built).
   * It is better to store unsafes from previous iteration of refinement.
   */

  private final LogManager logger;
  //What is true now?
  //private int trueUsagesInTrueUnsafe = 0;
  //private int trueUsagesInAllUnsafes = 0;
  //private int maxTrueUsages = 0;
 // private final ShutdownNotifier shutdownNotifier;

  private BAMTransferRelation transfer;
  private final Configuration config;
  private final LockTransferRelation lockTransfer;
  private ErrorTracePrinter errPrinter;

  public final StatTimer transferRelationTimer = new StatTimer("Time for transfer relation");
  public final StatTimer printStatisticsTimer = new StatTimer("Time for printing statistics");

  public UsageCPAStatistics(Configuration pConfig, LogManager pLogger,
      LockTransferRelation lTransfer) throws InvalidConfigurationException{
    pConfig.inject(this);
    logger = pLogger;
    lockTransfer = lTransfer;
    config = pConfig;
  }

  @Override
  public void printStatistics(final PrintStream out, final Result result, final UnmodifiableReachedSet reached) {
    printStatisticsTimer.start();
    assert errPrinter != null;
    errPrinter.printErrorTraces(reached);
    errPrinter.printStatistics(out);
    printStatisticsTimer.stop();
    //out.
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put(transferRelationTimer);
    writer.put(printStatisticsTimer);
    out.println("Time for expanding:                 " + UsageState.tmpTimer1);
    out.println("Time for joining:                   " + UsageState.tmpTimer2);
    out.println("Time for joining2:                  " + UsageState.tmpTimer3);
    out.println("Time for effect:                    " + TemporaryUsageStorage.effectTimer);
    out.println("Time for copy:                      " + TemporaryUsageStorage.copyTimer);
    out.println("Number of empty joins:              " + TemporaryUsageStorage.emptyJoin);
    out.println("Number of effect joins:             " + TemporaryUsageStorage.effectJoin);
    out.println("Number of hit joins:                " + TemporaryUsageStorage.hitTimes);
    out.println("Number of miss joins:               " + TemporaryUsageStorage.missTimes);
    out.println("Number of expanding querries:       " + TemporaryUsageStorage.totalUsages);
    out.println("Number of executed querries:        " + TemporaryUsageStorage.expandedUsages);

  }

  public void setBAMTransfer(BAMTransferRelation t) {
    transfer = t;
    if (outputFileType == OutputFileType.KLEVER) {
      errPrinter = new KleverErrorTracePrinter(config, transfer, logger);
    } else if (outputFileType == OutputFileType.ETV) {
      errPrinter = new ETVErrorTracePrinter(config, transfer, logger, lockTransfer);
    }
  }

  @Override
  public @Nullable String getName() {
    return "UsageCPAStatistics";
  }

}
