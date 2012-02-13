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
package org.sosy_lab.pcc.proof_gen;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;

@Options
public class GenericProofGenerator {

  @Option(name = "pcc.proofgen.doPCC", description = "")
  private boolean doPCC = true;
  @Option(description = "file in which ART representation needed for proof checking is stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File file = new File("art.obj");

  private final LogManager logger;

  public GenericProofGenerator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
  }

  public void generateProof(CPAcheckerResult pResult) {
    if (!doPCC) { return; }
    UnmodifiableReachedSet reached = pResult.getReached();
    // check result
    if (pResult.getResult() != Result.SAFE
        || reached.getFirstElement() == null
        || !(reached.getFirstElement() instanceof ARTElement)
        || (extractLocation(reached.getFirstElement()) == null)) {
      logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return;
    }
    // saves the proof in specified file
    logger.log(Level.INFO, "PCC Proof Generation started.");
    Timer writingTimer = new Timer();
    writingTimer.start();

    OutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      ObjectOutputStream o = new ObjectOutputStream(fos);
      //TODO might also want to write used configuration to the file so that proof checker does not need to get it as an argument
      //write ART
      o.writeObject(reached.getFirstElement());
    } catch (IOException e) {
      System.err.println(e);
    } finally {
      try {
        fos.close();
      } catch (Exception e) {
      }
    }

    /*try {
      Files.writeFile(new File("ComputedART.dot"), ARTUtils.convertARTToDot((ARTElement)reached.getFirstElement(), Collections.<Pair<ARTElement, ARTElement>>emptySet()));
    } catch (IOException e) { System.err.println(e); }*/

    writingTimer.stop();
    logger.log(Level.INFO, "Writing proof took " + writingTimer.printMaxTime());
  }

}
