/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.pcc.strategy.PCCStrategyBuilder;

@Options
public class ProofGenerator {

  @Option(name = "pcc.proofgen.doPCC", description = "")
  private boolean doPCC = false;
  @Option(
      name = "pcc.strategy",
      description = "Qualified name for class which implements certification strategy, hence proof writing, to be used.")
  private String pccStrategy = "org.sosy_lab.cpachecker.pcc.strategy.ARGProofCheckerStrategy";

  private PCCStrategy checkingStrategy;

  private final LogManager logger;

  public ProofGenerator(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;

    checkingStrategy = PCCStrategyBuilder.buildStrategy(pccStrategy, pConfig, pLogger, pShutdownNotifier, null);
  }

  public void generateProof(CPAcheckerResult pResult) {
    if (!doPCC) { return; }
    UnmodifiableReachedSet reached = pResult.getReached();

    // check result
    if (pResult.getResult() != Result.TRUE) {
      logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return;
    }
    // saves the proof
    logger.log(Level.INFO, "Proof Generation started.");
    Timer writingTimer = new Timer();
    writingTimer.start();

    checkingStrategy.writeProof(reached);

    writingTimer.stop();
    logger.log(Level.INFO, "Writing proof took " + writingTimer.printMaxTime());
  }

}
