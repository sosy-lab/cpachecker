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
package org.sosy_lab.pcc.proof_check;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Separators;

public abstract class ARTProofCheckAlgorithm implements ProofCheckAlgorithm {

  protected LogManager logger;
  protected Configuration config;
  protected CFA cfaForProof = null;
  protected ARTNode root = null;

  public ARTProofCheckAlgorithm(Configuration pConfig, LogManager pLogger)
  {
    config = pConfig;
    logger = pLogger;
  }

  @Override
  public PCCCheckResult checkProvidedProof(CFA pCFA, File pProof){
    PCCCheckResult success;
    cfaForProof = pCFA;
    // read proof from file
    try {
      success = readFromFile(pProof);
      if(success!=PCCCheckResult.Success){
        return success;
      }
    } catch (FileNotFoundException e) {
      return PCCCheckResult.InvalidProofFile;
    }
    // check proof
    return checkProof();
  }

  private PCCCheckResult readFromFile(File pFile) throws FileNotFoundException{
    Scanner scan = new Scanner(pFile);
    scan.useDelimiter("(\\s)*"+Separators.commonSeparator+"(\\s)*");
    PCCCheckResult success = readNodes(scan);
    if(success != PCCCheckResult.Success){
      return success;
    }
    success = readEdges(scan);
    return success;
  }

  /**
   * set root
   * @param pScan
   * @return
   */
  protected abstract PCCCheckResult readNodes(Scanner pScan);

  protected abstract PCCCheckResult readEdges(Scanner pScan);

  protected abstract PCCCheckResult checkProof();

}
