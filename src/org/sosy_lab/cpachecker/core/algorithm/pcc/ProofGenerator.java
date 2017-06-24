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
package org.sosy_lab.cpachecker.core.algorithm.pcc;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.pcc.strategy.PCCStrategyBuilder;

@Options
public class ProofGenerator {

  @Option(secure=true,
      name = "pcc.sliceProof",
      description = "Make proof more abstract, remove some of the information not needed to prove the property.")
  private boolean slicingEnabled = false;

  @Option(secure=true,
      name = "pcc.proofFile",
      description = "file in which proof representation will be stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  protected Path file = Paths.get("arg.obj");

  private PCCStrategy checkingStrategy;

  private final LogManager logger;
  private final Timer writingTimer = new Timer();

  private final Statistics proofGeneratorStats = new Statistics() {

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println();
      pOut.println(getName() + " statistics");
      pOut.println("------------------------------------");
      pOut.println("Time for proof writing: " + writingTimer);

      if (checkingStrategy != null) {
        for (Statistics stats : checkingStrategy.getAdditionalProofGenerationStatistics()) {
          stats.printStatistics(pOut, pResult, pReached);
        }
      }
    }

    @Override
    public String getName() {
      return "Proof Generation";
    }
  };

  public ProofGenerator(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;

    checkingStrategy =
        PCCStrategyBuilder.buildStrategy(pConfig, pLogger, pShutdownNotifier, file, null, null, null);
  }

  public void generateProof(CPAcheckerResult pResult) {
    // check result
    if (pResult.getResult() != Result.TRUE) {
      logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return;
    }

    if(pResult.getReached() == null) {
      logger.log(Level.SEVERE, "Proof cannot be generated because reached set not available");
    }

    constructAndWriteProof(pResult.getReached());

    pResult.addProofGeneratorStatistics(proofGeneratorStats);

  }

  private void constructAndWriteProof(UnmodifiableReachedSet pReached) {
    if(slicingEnabled){
      logger.log(Level.INFO, "Start slicing of proof");
      pReached = new ProofSlicer().sliceProof(pReached);
    }

    // saves the proof
    logger.log(Level.INFO, "Proof Generation started.");

    writingTimer.start();

    checkingStrategy.writeProof(pReached);

    writingTimer.stop();
    logger.log(Level.INFO, "Writing proof took " + writingTimer.getMaxTime().formatAs(TimeUnit.SECONDS));

  }

  protected Statistics generateProofUnchecked(final UnmodifiableReachedSet pReached) {
    constructAndWriteProof(pReached);

    return proofGeneratorStats;
  }

}
