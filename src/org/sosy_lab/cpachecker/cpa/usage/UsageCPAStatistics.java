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
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.usage")
public class UsageCPAStatistics extends AbstractStatistics {

  public static enum OutputFileType {
    ETV,
    KLEVER,
    KLEVER_OLD
  }

  @Option(
    name = "outputType",
    description = "all variables should be printed to the one file or to the different",
    secure = true
  )
  private OutputFileType outputFileType = OutputFileType.KLEVER;

  /* Previous container is used when internal time limit occurs
   * and we need to store statistics. In current one the information can be not
   * relevant (for example not all ARG was built).
   * It is better to store unsafes from previous iteration of refinement.
   */

  private final LogManager logger;

  private final Configuration config;
  private final LockTransferRelation lockTransfer;
  private ErrorTracePrinter errPrinter;
  private final CFA cfa;

  private BAMCPA bamCpa;

  public final StatTimer transferRelationTimer = new StatTimer("Time for transfer relation");
  public final StatTimer printStatisticsTimer = new StatTimer("Time for printing statistics");
  public final StatTimer printUnsafesTimer = new StatTimer("Time for unsafes printing");

  public UsageCPAStatistics(
      Configuration pConfig, LogManager pLogger, CFA pCfa, LockTransferRelation lTransfer)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    lockTransfer = lTransfer;
    config = pConfig;
    cfa = pCfa;
  }

  @Override
  public void printStatistics(
      final PrintStream out, final Result result, final UnmodifiableReachedSet reached) {
    try {
      printUnsafesTimer.start();
      if (errPrinter == null) {
        BAMMultipleCEXSubgraphComputer computer =
            bamCpa.createBAMMultipleSubgraphComputer(ARGState::getStateId);
        if (outputFileType == OutputFileType.KLEVER) {
          errPrinter = new KleverErrorTracePrinter(config, computer, cfa, logger, lockTransfer);
        } else if (outputFileType == OutputFileType.KLEVER_OLD) {
          errPrinter = new KleverErrorTracePrinterOld(config, computer, cfa, logger, lockTransfer);
        } else if (outputFileType == OutputFileType.ETV) {
          errPrinter = new ETVErrorTracePrinter(config, computer, cfa, logger, lockTransfer);
        }
      }
      errPrinter.printErrorTraces(reached);
      printUnsafesTimer.stop();

      printStatisticsTimer.start();
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
      writer.put(transferRelationTimer);
      writer.put(printStatisticsTimer);
      errPrinter.printStatistics(writer);
      UsageState.get(reached.getFirstState()).getStatistics().printStatistics(writer);
      // out.
      printStatisticsTimer.stop();
    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, "Cannot create error trace printer: " + e.getMessage());
    }
  }

  public void setBAMCPA(BAMCPA pBamCpa) {
    bamCpa = pBamCpa;
  }

  @Override
  public @Nullable String getName() {
    return "UsageCPAStatistics";
  }
}
