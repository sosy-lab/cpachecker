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
package org.sosy_lab.cpachecker.core.algorithm;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import org.sosy_lab.cpachecker.cpa.arg.ARGElement;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

@Options
public class ProofGenerator {

  @Option(name = "pcc.proofgen.doPCC", description = "")
  private boolean doPCC = false;
  @Option(name = "pcc.proofFile", description = "file in which ART representation needed for proof checking is stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File file = new File("art.obj");

  private final LogManager logger;

  public ProofGenerator(Configuration pConfig, LogManager pLogger)
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
        || !(reached.getFirstElement() instanceof ARGElement)
        || (extractLocation(reached.getFirstElement()) == null)) {
      logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return;
    }
    // saves the proof in specified file
    logger.log(Level.INFO, "Proof Generation started.");
    Timer writingTimer = new Timer();
    writingTimer.start();

    OutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      ZipOutputStream zos = new ZipOutputStream(fos);
      zos.setLevel(9);

      ZipEntry ze = new ZipEntry("Proof");
      zos.putNextEntry(ze);
      ObjectOutputStream o = new ObjectOutputStream(zos);
      //TODO might also want to write used configuration to the file so that proof checker does not need to get it as an argument
      //write ART
      o.writeObject(reached.getFirstElement());
      zos.closeEntry();

      ze = new ZipEntry("Helper");
      zos.putNextEntry(ze);
      //write helper storages
      o = new ObjectOutputStream(zos);
      int numberOfStorages = GlobalInfo.getInstance().getNumberOfHelperStorages();
      o.writeInt(numberOfStorages);
      for(int i = 0; i < numberOfStorages; ++i) {
        o.writeObject(GlobalInfo.getInstance().getHelperStorage(i));
      }

      o.flush();
      zos.closeEntry();
      zos.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        fos.close();
      } catch (Exception e) {
      }
    }

    writingTimer.stop();
    logger.log(Level.INFO, "Writing proof took " + writingTimer.printMaxTime());
  }
}
