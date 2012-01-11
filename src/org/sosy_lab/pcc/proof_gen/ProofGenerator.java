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
package org.sosy_lab.pcc.proof_gen;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.pcc.common.PCCAlgorithmType;

@Options
public class ProofGenerator {

  private ProofGenAlgorithm algorithm;
  private LogManager logger;


  @Option(name = "pcc.proofgen.algorithm", description = "Type of the algorithm which should be used for PCC")
  private PCCAlgorithmType algorithmType = PCCAlgorithmType.SBENOINDART;
  @Option(name = "pcc.proofgen.doPCC", description = "")
  private boolean doPCC = false;

  @Option(
      name = "cpa.predicate.blk.alwaysAfterThreshold",
      description = "force abstractions immediately after threshold is reached (no effect if threshold = 0)")
  private boolean alwaysAfterThreshold = true;
  @Option(name = "cpa.predicate.blk.threshold", description = "maximum blocksize before abstraction is forced\n"
      + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
  private int threshold = 0;
  @Option(
      name = "cpa.predicate.blk.switchToLBEAfter",
      description = "Switch to a LBE configuration after so many milliseconds (0 to disable)")
  private int switchToLBEAfter = 0;
  @Option(
      name = "cpa.predicate.blk.alwaysAtLoops",
      description = "force abstractions at loop heads, regardless of threshold")
  private boolean alwaysAtLoops = true;
  @Option(
      name = "cpa.predicate.blk.alwaysAtFunctions",
      description = "force abstractions at each function calls/returns, regardless of threshold")
  private boolean alwaysAtFunctions = true;
  @Option(
      name = "cpa.predicate.abstraction.cartesian",
      description = "whether to use Boolean (false) or Cartesian (true) abstraction")
  private boolean cartesianAbstraction = false;
  @Option(name="cpa.predicate.satCheck",
      description="maximum blocksize before a satisfiability check is done\n"
        + "(non-negative number, 0 means never, if positive should be smaller than blocksize)")
  private int satCheckBlockSize = 0;

  public ProofGenerator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    logger = pLogger;
    pConfig.inject(this);
    if (doPCC) {
      switch (algorithmType) {
      case SBENOINDART: {
        if (alwaysAfterThreshold && switchToLBEAfter == 0 && threshold == 1
            && cartesianAbstraction) {
          algorithm = new SBEWithoutIndices_ARTProofGenAlgorithm(pConfig, pLogger);
        } else {
          logger
              .log(Level.WARNING,
                  "Predicate Abstraction configuration does not fit to PCC algorithm.");
          algorithm = null;
        }
        break;
      }
      case SBEINDART: {
        if (alwaysAfterThreshold && switchToLBEAfter == 0 && threshold == 1
            && cartesianAbstraction) {
          algorithm = new SBEWithIndices_ARTProofGenAlgorithm(pConfig, pLogger);
        } else {
          logger
              .log(Level.WARNING,
                  "Predicate Abstraction configuration does not fit to PCC algorithm.");
          algorithm = null;
        }
        break;
      }
      case SBENOINDINVARIANT: {
        if (alwaysAfterThreshold && switchToLBEAfter == 0 && threshold == 1
            && cartesianAbstraction) {
          algorithm = new SBEWithoutIndices_InvariantProofGenAlgorithm(pConfig, pLogger);
        } else {
          logger
              .log(Level.WARNING,
                  "Predicate Abstraction configuration does not fit to PCC algorithm.");
          algorithm = null;
        }
        break;
      }
      case SBEINDINVARIANT: {
        if (alwaysAfterThreshold && switchToLBEAfter == 0 && threshold == 1
            && cartesianAbstraction) {
          algorithm = new SBEWithIndices_InvariantProofGenAlgorithm(pConfig, pLogger);
        } else {
          logger
              .log(Level.WARNING,
                  "Predicate Abstraction configuration does not fit to PCC algorithm.");
          algorithm = null;
        }
        break;
      }
      case SBENOEDGENOINDINVARIANT: {
        if (alwaysAfterThreshold && switchToLBEAfter == 0 && threshold == 1
            && cartesianAbstraction) {
          algorithm = new SBEWithoutIndices2_InvariantProofGenAlgorithm(pConfig, pLogger);
        } else {
          logger
              .log(Level.WARNING,
                  "Predicate Abstraction configuration does not fit to PCC algorithm.");
          algorithm = null;
        }
        break;
      }
      case SBENOEDGENOINDINVARIANT2: {
        if (alwaysAfterThreshold && switchToLBEAfter == 0 && threshold == 1
            && cartesianAbstraction) {
          algorithm = new SBEWithoutIndices2_InvariantProofGenAlgorithm(pConfig, pLogger);
        } else {
          logger
              .log(Level.WARNING,
                  "Predicate Abstraction configuration does not fit to PCC algorithm.");
          algorithm = null;
        }
        break;
      }
      case LBENOINDART: {
        if (alwaysAfterThreshold && alwaysAtFunctions && alwaysAtLoops
            && !cartesianAbstraction && threshold == 0) {
          algorithm = new LBE_ARTProofGenAlgorithm(pConfig, pLogger);
        }
        break;
      }
      case LBENOINDINVARIANT: {
        if (alwaysAfterThreshold && alwaysAtFunctions && alwaysAtLoops
            && !cartesianAbstraction && threshold == 0) {
          algorithm = new LBE_InvariantProofGenAlgorithm(pConfig, pLogger);
        }
        break;
      }
      case ALBENOINDART: {
        if (alwaysAfterThreshold && alwaysAtFunctions && alwaysAtLoops
            && ((threshold == 1 && cartesianAbstraction) || (threshold != 1 && !cartesianAbstraction))) {
          algorithm = new AdjustableLBE_ARTProofGenAlgorithm(pConfig, pLogger);
        }
        break;
      }
      case ALBENOINDINVARIANT: {
        if (alwaysAfterThreshold && alwaysAtFunctions && alwaysAtLoops
            && ((threshold == 1 && cartesianAbstraction) || (threshold != 1 && !cartesianAbstraction))
            && satCheckBlockSize == 0) {
          algorithm = new AdjustableLBE_InvariantProofGenAlgorithm(pConfig, pLogger);
        }
        break;
      }
      case ABENOINDART: {
        if (alwaysAfterThreshold && alwaysAtLoops
            && ((threshold == 1 && cartesianAbstraction) || (threshold != 1 && !cartesianAbstraction))) {
          algorithm = new ABE_ARTProofGenAlgorithm(pConfig, pLogger);
        }
        break;
      }
      default: {
        algorithm = null;
      }
      }
    } else {
      algorithm = null;
    }
  }

  public void generateProof(CPAcheckerResult pResult) {
    if (!doPCC) { return; }
    UnmodifiableReachedSet reached = pResult.getReached();
    // check result
    if (pResult.getResult() != Result.SAFE
        || reached.getFirstElement() == null
        || !(reached.getFirstElement() instanceof ARTElement)
        || (extractLocation(reached.getFirstElement()) == null)
        || AbstractElements.extractElementByType(reached.getFirstElement(),
            CallstackElement.class) == null) {
      logger
          .log(Level.SEVERE,
              "Proof cannot be generated because checked property not known to be true.");
      return;
    }
    // check algorithm
    if (algorithm == null) {
      logger.log(Level.SEVERE,
          "No algorithm available. Check for correct configuration.");
      return;
    }
    // saves the proof in specified file
    logger.log(Level.INFO, "PCC Proof Generation started.");
    Timer writingTimer = new Timer();
    writingTimer.start();
    boolean success =
        algorithm.writeProof((ARTElement) pResult.getReached()
            .getFirstElement());
    writingTimer.stop();
    System.out.println("Writing proof lasted " + writingTimer.printMaxTime());
    if (success) {
      logger.log(Level.INFO, "PCC Proof Generation finished.");
    } else {
      logger.log(Level.INFO,
          "PCC Proof Generation aborted because a failure occured.");
    }
  }

}
