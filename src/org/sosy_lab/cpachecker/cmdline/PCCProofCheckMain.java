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
import java.nio.charset.Charset;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cmdline.CPAMain.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.pcc.common.PCCAlgorithmType;
import org.sosy_lab.pcc.proof_check.ProofCheckAlgorithm;

import com.google.common.base.Throwables;
import com.google.common.io.Files;

public class PCCProofCheckMain {

  @Options
  private class PCCProofChecker {

    //TODO possibly add values
    @Option(name = "pcc.proofgen.algorithm", description = "Type of the algorithm which should be used for PCC")
    private PCCAlgorithmType algorithmType = PCCAlgorithmType.SBEasART;
    @Option(name = "pcc.proofgen.doPCC", description = "")
    private boolean          doPCC         = false;
    //TODO further values if other algorithms available
    @Option(name = "cpa.predicate.blk.alwaysAfterThreshold", description = "force abstractions immediately after threshold is reached (no effect if threshold = 0)")
    private boolean          alwaysAfterThreshold;
    @Option(name = "cpa.predicate.blk.threshold", description = "maximum blocksize before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
    private int              threshold;
    @Option(name = "cpa.predicate.blk.switchToLBEAfter", description = "Switch to a LBE configuration after so many milliseconds (0 to disable)")
    private int              switchToLBEAfter;
    @Option(name = "pcc.proofgen.ART.file", type = Option.Type.OUTPUT_FILE, description = "export ART needed for proof checking in PCC, if the error location is not reached")
    private File             file          = new File("pccART.txt");

    private PCCProofChecker(Configuration pConfig)
        throws InvalidConfigurationException {
      pConfig.inject(this, PCCProofChecker.class);
      if (!doPCC) { throw new InvalidConfigurationException(
          "Proof Checking is not enabled in the configuration."); }
    }

    private ProofCheckAlgorithm getCheckAlgorithm() {
      ProofCheckAlgorithm algorithm = null;
      switch (algorithmType) {
      case SBEasART: {


        break;
      }
      default: {
      }
      }
      return algorithm;
    }
// really needed or provide only File TODO
    private StringBuilder readFromFile(File pFile) throws IOException{
      StringBuilder builder = new StringBuilder();
      Files.copy(pFile, Charset.defaultCharset(), builder);
      return null;
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
// TODO do logging INFO level
      PCCProofChecker prover =
          (new PCCProofCheckMain()).new PCCProofChecker(config);
      // create CFA
      CFACreator cfaCreator = new CFACreator(config, logger);
      cfa =
          cfaCreator.parseFileAndCreateCFA(CPAMain.getCodeFilePath(config,
              logger));

      // get algorithm for checking
      ProofCheckAlgorithm algorithm = prover.getCheckAlgorithm();

      //start check
      boolean success = algorithm.checkProvidedProof(cfa);

      if(success){}else{}//TODO

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

  private static ProofCheckAlgorithm too() {
    return null;
  }

}
