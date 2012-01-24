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
package org.sosy_lab.pcc.proof_check;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Separators;

public abstract class InvariantProofCheckAlgorithm implements
    ProofCheckAlgorithm {

  protected CFA cfaForProof = null;
  protected Configuration config;
  protected LogManager logger;

  protected final String stackName = "_STACK";
  protected final String goalDes = "_GOAL";
  protected final String stackLength = "_STACKLEN";

  public InvariantProofCheckAlgorithm(Configuration pConfig, LogManager pLogger) {
    config = pConfig;
    logger = pLogger;
  }

  @Override
  public PCCCheckResult checkProvidedProof(CFA pCFA, File pProof) {
    cfaForProof = pCFA;
    PCCCheckResult intermediateRes;
    try {
      logger.log(Level.INFO, "Start reading proof.");
      intermediateRes = readFromFile(pProof);
    } catch (FileNotFoundException e1) {
      logger.logUserException(Level.WARNING, e1, "");
      return PCCCheckResult.InvalidProofFile;
    }
    if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
    logger.log(Level.INFO, "Finished reading proof and start validating proof.");
    return checkProof();
  }

  private PCCCheckResult readFromFile(File pFile) throws FileNotFoundException {
    Scanner scan = new Scanner(pFile);
    scan.useDelimiter("(\\s)*" + Separators.commonSeparator + "(\\s)*");
    logger.log(Level.INFO, "Read regions.");
    PCCCheckResult success = readNodes(scan);
    if (success != PCCCheckResult.Success) { return success; }
    logger.log(Level.INFO, "Read edges connecting regions.");
    success = readEdges(scan);
    return success;
  }

  protected abstract PCCCheckResult readEdges(Scanner pScan);

  protected abstract PCCCheckResult readNodes(Scanner pScan);

  protected abstract PCCCheckResult checkProof();
}
