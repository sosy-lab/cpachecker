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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

@Options
public class ProofGenerator {

  @Option(name = "pcc.proofgen.doPCC", description = "")
  private boolean doPCC = false;
  @Option(name = "pcc.proofFile", description = "file in which proof representation needed for proof checking is stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File file = new File("arg.obj");

  @Option(name="pcc.proofType", description = "defines proof representation, either abstract reachability graph or set of reachable abstract states", values={"ARG", "SET", "PSET"})
  private String pccType = "ARG";

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
    if (pResult.getResult() != Result.SAFE) {
      logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return;
    }
    // saves the proof in specified file
    logger.log(Level.INFO, "Proof Generation started.");
    Timer writingTimer = new Timer();
    writingTimer.start();

    if (pccType.equals("ARG")) {
      writeARG(reached);
    } else if (pccType.equals("SET") || pccType.equals("PSET")) {
      writeReachedSet(reached);
    } else {
      logger.log(Level.SEVERE, "Undefined proof format. No proof will be written.");
    }

    writingTimer.stop();
    logger.log(Level.INFO, "Writing proof took " + writingTimer.printMaxTime());
  }

  private void writeARG(UnmodifiableReachedSet pReached) {
    if (pReached.getFirstState() == null
        || !(pReached.getFirstState() instanceof ARGState)
        || (extractLocation(pReached.getFirstState()) == null)) {
      logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return;
    }
    // saves the ARG in specified file

    OutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      ZipOutputStream zos = new ZipOutputStream(fos);
      zos.setLevel(9);

      ZipEntry ze = new ZipEntry("Proof");
      zos.putNextEntry(ze);
      ObjectOutputStream o = new ObjectOutputStream(zos);
      //TODO might also want to write used configuration to the file so that proof checker does not need to get it as an argument
      //write ARG
      o.writeObject(pReached.getFirstState());
      zos.closeEntry();

      ze = new ZipEntry("Helper");
      zos.putNextEntry(ze);
      //write helper storages
      o = new ObjectOutputStream(zos);
      int numberOfStorages = GlobalInfo.getInstance().getNumberOfHelperStorages();
      o.writeInt(numberOfStorages);
      for (int i = 0; i < numberOfStorages; ++i) {
        o.writeObject(GlobalInfo.getInstance().getHelperStorage(i));
      }

      o.flush();
      zos.closeEntry();
      zos.close();
    } catch (NotSerializableException eS){
      logger.log(Level.SEVERE, "Proof cannot be written. Class " + eS.getMessage() + " does not implement Serializable interface");
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        fos.close();
      } catch (Exception e) {
      }
    }
  }

  /*
   * partial reached makes only sense for forward analysis
   */
  private void writeReachedSet(UnmodifiableReachedSet pReached) {
    // saves the abstract states in reached set in specified file
    OutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      ZipOutputStream zos = new ZipOutputStream(fos);
      zos.setLevel(9);

      ZipEntry ze = new ZipEntry("Proof");
      zos.putNextEntry(ze);
      ObjectOutputStream o = new ObjectOutputStream(zos);
      //TODO might also want to write used configuration to the file so that proof checker does not need to get it as an argument
      //write reached set
      AbstractState[] reachedSet;

      if (pccType.equals("SET")) {
        reachedSet = new AbstractState[pReached.size()];
        pReached.asCollection().toArray(reachedSet);
      } else if (pccType.equals("PSET")) {
        reachedSet = computePartialReachedSet(pReached);
      } else {
        return;
      }

      o.writeObject(reachedSet);
      zos.closeEntry();

      ze = new ZipEntry("Helper");
      zos.putNextEntry(ze);
      //write helper storages
      o = new ObjectOutputStream(zos);
      int numberOfStorages = GlobalInfo.getInstance().getNumberOfHelperStorages();
      o.writeInt(numberOfStorages);
      for (int i = 0; i < numberOfStorages; ++i) {
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
  }

  public static AbstractState[] computePartialReachedSet(UnmodifiableReachedSet pReached) {
    ArrayList<AbstractState> result = new ArrayList<>();
    CFANode node;
    for (AbstractState state : pReached.asCollection()) {
      node = AbstractStates.extractLocation(state);
      if (node == null || node.getNumEnteringEdges() > 1 || (node.getNumLeavingEdges()>0 && node.getLeavingEdge(0).getEdgeType()==CFAEdgeType.FunctionCallEdge)) {
        result.add(state);
      }
    }
    if(!result.contains(pReached.getFirstState())){
      result.add(pReached.getFirstState());
    }
    AbstractState[] arrayRep = new AbstractState[result.size()];
    result.toArray(arrayRep);
    return arrayRep;
  }
}
