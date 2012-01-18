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
package org.sosy_lab.cpachecker.cmdline;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cmdline.CmdLineArguments.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.pcc.common.PCCAlgorithmType;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.proof_check.ABE_ARTProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.AdjustableLBE_ARTProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.AdjustableLBE_InvariantProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.LBE_ARTProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.LBE_InvariantProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.ProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.SBEWithIndices_ARTProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.SBEWithIndices_InvariantProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.SBEWithoutIndices2_InvariantProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.SBEWithoutIndices2a_InvariantProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.SBEWithoutIndices_ARTProofCheckAlgorithm;
import org.sosy_lab.pcc.proof_check.SBEWithoutIndices_InvariantProofCheckAlgorithm;

import com.google.common.base.Throwables;

public class PCCProofCheckMain {

  @Options
  private static class PCCProofChecker {

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
        name = "pcc.profgen.file",
        description = "export ART representation needed for proof checking in PCC, if the error location is not reached, the representation depends on the algorithm used for proof checking")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private File file = new File("pccProof.txt");
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
    @Option(name = "cpa.predicate.abstraction.solver", toUppercase = true, values = { "MATHSAT", "YICES" },
        description = "which solver to use?")
    private String whichProver = "MATHSAT";
    @Option(name = "cpa.predicate.satCheck",
        description = "maximum blocksize before a satisfiability check is done\n"
            + "(non-negative number, 0 means never, if positive should be smaller than blocksize)")
    private int satCheckBlockSize = 0;

    private PCCProofChecker(Configuration pConfig)
        throws InvalidConfigurationException {
      pConfig.inject(this, PCCProofChecker.class);
      if (!doPCC) { throw new InvalidConfigurationException(
          "Proof Checking is not enabled in the configuration."); }
    }

    private ProofCheckAlgorithm getCheckAlgorithm(Configuration pConfig,
        LogManager pLogger) throws InvalidConfigurationException {

      ProofCheckAlgorithm algorithm = null;
      switch (algorithmType) {
      case SBENOINDART: {
        if (alwaysAfterThreshold && threshold == 1 && switchToLBEAfter == 0
            && cartesianAbstraction) {
          algorithm =
              new SBEWithoutIndices_ARTProofCheckAlgorithm(pConfig, pLogger, whichProver, alwaysAtLoops,
                  alwaysAtFunctions);
        }
        break;
      }
      case SBEINDART: {
        if (alwaysAfterThreshold && threshold == 1 && switchToLBEAfter == 0
            && cartesianAbstraction) {
          algorithm =
              new SBEWithIndices_ARTProofCheckAlgorithm(pConfig, pLogger, whichProver, alwaysAtLoops,
                  alwaysAtFunctions);
        }
        break;
      }
      case SBENOINDINVARIANT: {
        if (alwaysAfterThreshold && threshold == 1 && switchToLBEAfter == 0
            && cartesianAbstraction) {
          algorithm =
              new SBEWithoutIndices_InvariantProofCheckAlgorithm(pConfig, pLogger, whichProver,
                  alwaysAtLoops, alwaysAtFunctions);
        }
        break;
      }
      case SBEINDINVARIANT: {
        if (alwaysAfterThreshold && threshold == 1 && switchToLBEAfter == 0
            && cartesianAbstraction) {
          algorithm =
              new SBEWithIndices_InvariantProofCheckAlgorithm(pConfig, pLogger, whichProver,
                  alwaysAtLoops, alwaysAtFunctions);
        }
        break;
      }
      case SBENOEDGENOINDINVARIANT: {
        if (alwaysAfterThreshold && threshold == 1 && switchToLBEAfter == 0
            && cartesianAbstraction) {
          algorithm =
              new SBEWithoutIndices2_InvariantProofCheckAlgorithm(pConfig, pLogger, whichProver,
                  alwaysAtLoops, alwaysAtFunctions);
        }
        break;
      }
      case SBENOEDGENOINDINVARIANT2: {
        if (alwaysAfterThreshold && threshold == 1 && switchToLBEAfter == 0
            && cartesianAbstraction) {
          algorithm =
              new SBEWithoutIndices2a_InvariantProofCheckAlgorithm(pConfig, pLogger, whichProver,
                  alwaysAtLoops, alwaysAtFunctions);
        }
        break;
      }
      case LBENOINDART: {
        if (alwaysAfterThreshold && alwaysAtFunctions && alwaysAtLoops
            && !cartesianAbstraction && threshold == 0) {
          algorithm = new LBE_ARTProofCheckAlgorithm(pConfig, pLogger, whichProver);
        }
        break;
      }
      case LBENOINDINVARIANT: {
        if (alwaysAfterThreshold && alwaysAtFunctions && alwaysAtLoops
            && !cartesianAbstraction && threshold == 0) {
          algorithm = new LBE_InvariantProofCheckAlgorithm(pConfig, pLogger, whichProver);
        }
        break;
      }
      case ALBENOINDART: {
        if (alwaysAfterThreshold && alwaysAtFunctions && alwaysAtLoops
            && ((threshold == 1 && cartesianAbstraction) || (threshold != 1 && !cartesianAbstraction))) {
          algorithm = new AdjustableLBE_ARTProofCheckAlgorithm(pConfig, pLogger, whichProver, threshold);
        }
        break;
      }
      case ALBENOINDINVARIANT: {
        if (alwaysAfterThreshold && alwaysAtFunctions && alwaysAtLoops
            && ((threshold == 1 && cartesianAbstraction) || (threshold != 1 && !cartesianAbstraction))
            && satCheckBlockSize == 0) {
          algorithm = new AdjustableLBE_InvariantProofCheckAlgorithm(pConfig, pLogger, whichProver, threshold);
        }
        break;
      }
      case ABENOINDART: {
        if (alwaysAfterThreshold && alwaysAtLoops
            && ((threshold == 1 && cartesianAbstraction) || (threshold != 1 && !cartesianAbstraction))) {
          algorithm = new ABE_ARTProofCheckAlgorithm(pConfig, pLogger, whichProver, alwaysAtFunctions, threshold);
        }
        break;
      }
      default: {
      }
      }
      return algorithm;
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    Configuration config = null;
    LogManager logger = null;
    // set up environment
    try {
      try {
        config = CPAMain.createConfiguration(args);
      } catch (InvalidCmdlineArgumentException e) {
        System.err.println("Could not parse command line arguments: "
            + e.getMessage());
        System.exit(1);
      } catch (IOException e) {
        System.err.println("Could not read config file " + e.getMessage());
        System.exit(1);
      }

      logger = new LogManager(config);

    } catch (InvalidConfigurationException e) {
      System.err.println("Invalid configuration: " + e.getMessage());
      System.exit(1);
    }

    CFA cfa = null;
    try {
      logger.log(Level.INFO, "Start PCC proof checking.");
      Timer time = new Timer();
      time.start();
      PCCProofChecker prover = new PCCProofChecker(config);
      time.stop();
      System.out.println("Configuration set up needed " + time.printMaxTime());
      // create CFA
      logger.log(Level.INFO, "Started CFA construction.");
      time.start();
      CFACreator cfaCreator = new CFACreator(config, logger);
      cfa =
          cfaCreator.parseFileAndCreateCFA(CPAMain.getCodeFile(config).getPath());
      System.out.println("Reading CFA lasted " + Timer.formatTime(time.stop()));
      logger.log(Level.INFO, "CFA construction finished.");
      // get algorithm for checking
      time.start();
      ProofCheckAlgorithm algorithm = prover.getCheckAlgorithm(config, logger);
      System.out.println("Getting correct algorithm lasted " + Timer.formatTime(time.stop()));
      //start check
      logger.log(Level.INFO, "Started proof validation.");
      time.start();
      PCCCheckResult result = algorithm.checkProvidedProof(cfa, prover.file);
      System.out.println("Reading and checking proof lasted " + Timer.formatTime(time.stop()));
      logger.log(Level.INFO, "Finished proof validation.");

      if (result == PCCCheckResult.Success) {
        System.out.println("Proof has been checked successfully.\n");

      } else {
        System.out.println("Proof failed with failure: " + result);
      }

    } catch (IOException e) {
      logger.logUserException(Level.SEVERE, e, "Could not read file");
      System.exit(1);
    } catch (ParserException e) {
      logger.logUserException(Level.SEVERE, Throwables.getRootCause(e),
          "Parsing failed");
      logger
          .log(
              Level.INFO,
              "Make sure that the code was preprocessed using Cil (HowTo.txt).\n"
                  + "If the error still occurs, please send this error message together with the input file to cpachecker-users@sosy-lab.org.");
    } catch (InterruptedException e) {
      // nothing to do
    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");
    }
  }
}
