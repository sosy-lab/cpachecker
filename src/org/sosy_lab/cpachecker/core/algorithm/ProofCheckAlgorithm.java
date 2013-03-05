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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

@Options
public class ProofCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer = new Timer();
    private Timer transferTimer = new Timer();
    private Timer stopTimer = new Timer();
    private Timer readTimer = new Timer();

    private int countIterations = 0;

    @Override
    public String getName() {
      return "Proof Check algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {
      out.println("Number of iterations:                     " + countIterations);
      out.println();
      out.println("Total time for proof check algorithm: " + totalTimer);
      out.println("  Time for reading in proof:          " + readTimer);
      out.println("  Time for abstract successor checks: " + transferTimer + " (Calls: "
          + transferTimer.getNumberOfIntervals() + ")");
      out.println("  Time for covering checks:           " + stopTimer + " (Calls: " + stopTimer.getNumberOfIntervals()
          + ")");
    }
  }

  private final CPAStatistics stats = new CPAStatistics();
  private final ProofChecker cpa;
  private final LogManager logger;

  @Option(name = "pcc.proofFile", description = "file in which ARG representation needed for proof checking is stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File file = new File("arg.obj");
  private final ARGState rootState;


  public ProofCheckAlgorithm(ConfigurableProgramAnalysis cpa, Configuration pConfig, LogManager logger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    if (!(cpa instanceof ProofChecker)) { throw new InvalidConfigurationException(
        "ProofCheckAlgorithm needs a CPA that implements the ProofChecker interface."); }
    this.cpa = (ProofChecker) cpa;
    this.logger = logger;

    ARGState rootState = null;
    try {
      rootState = readART();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException("Failed reading ARG.", e);
    }
    this.rootState = rootState;
    System.gc();
  }

  private ARGState readART() throws IOException, ClassNotFoundException {
    stats.totalTimer.start();
    stats.readTimer.start();
    InputStream fis = null;
    try {
      fis = new FileInputStream(file);
      ZipInputStream zis = new ZipInputStream(fis);

      ZipEntry entry = zis.getNextEntry();
      assert entry.getName().equals("Proof");
      zis.closeEntry();

      entry = zis.getNextEntry();
      assert entry.getName().equals("Helper");
      ObjectInputStream o = new ObjectInputStream(zis);
      //read helper storages
      int numberOfStorages = o.readInt();
      for (int i = 0; i < numberOfStorages; ++i) {
        Serializable storage = (Serializable) o.readObject();
        GlobalInfo.getInstance().addHelperStorage(storage);
      }
      zis.closeEntry();

      o.close();
      zis.close();
      fis.close();

      fis = new FileInputStream(file);
      zis = new ZipInputStream(fis);
      entry = zis.getNextEntry();
      assert entry.getName().equals("Proof");
      o = new ObjectInputStream(zis);
      //read ARG
      return (ARGState) o.readObject();
    } finally {
      fis.close();
      stats.readTimer.stop();
      stats.totalTimer.stop();
    }
  }

  @Override
  public boolean run(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    //TODO does not account for strengthen yet (proof check will fail if strengthen is needed to explain successor states)

    stats.totalTimer.start();

    logger.log(Level.INFO, "Proof check algorithm started");

    AbstractState initialState = reachedSet.popFromWaitlist();
    Precision initialPrecision = reachedSet.getPrecision(initialState);

    logger.log(Level.FINE, "Checking root state");

    if (!(cpa.isCoveredBy(initialState, rootState) && cpa.isCoveredBy(rootState, initialState))) {
      stats.totalTimer.stop();
      logger.log(Level.WARNING, "Root state of proof is invalid.");
      return false;
    }

    reachedSet.add(rootState, initialPrecision);

    Set<ARGState> postponedStates = new HashSet<>();

    Set<ARGState> waitingForUnexploredParents = new HashSet<>();
    Set<ARGState> inWaitlist = new HashSet<>();
    inWaitlist.add(rootState);

    boolean unexploredParent;

    do {
      for (ARGState e : postponedStates) {
        if (!reachedSet.contains(e.getCoveringState())) {
          stats.totalTimer.stop();
          logger.log(Level.WARNING, "Covering state", e.getCoveringState(), "was not found in reached set");
          return false;
        }
        reachedSet.reAddToWaitlist(e);
      }
      postponedStates.clear();

      while (reachedSet.hasWaitingState()) {
        CPAchecker.stopIfNecessary();

        stats.countIterations++;
        ARGState state = (ARGState) reachedSet.popFromWaitlist();
        inWaitlist.remove(state);

        logger.log(Level.FINE, "Looking at state", state);

        if (state.isTarget()) { return false; }

        if (state.isCovered()) {

          logger.log(Level.FINER, "State is covered by another abstract state; checking coverage");
          ARGState coveringState = state.getCoveringState();

          if (!reachedSet.contains(coveringState)) {
            postponedStates.add(state);
            continue;
          }

          stats.stopTimer.start();
          if (!isCoveringCycleFree(state)) {
            stats.stopTimer.stop();
            stats.totalTimer.stop();
            logger.log(Level.WARNING, "Found cycle in covering relation for state", state);
            return false;
          }
          if (!cpa.isCoveredBy(state, coveringState)) {
            stats.stopTimer.stop();
            stats.totalTimer.stop();
            logger.log(Level.WARNING, "State", state, "is not covered by", coveringState);
            return false;
          }
          stats.stopTimer.stop();
        } else {
          stats.transferTimer.start();
          Collection<ARGState> successors = state.getChildren();
          logger.log(Level.FINER, "Checking abstract successors", successors);
          if (!cpa.areAbstractSuccessors(state, null, successors)) {
            stats.transferTimer.stop();
            stats.totalTimer.stop();
            logger.log(Level.WARNING, "State", state, "has other successors than", successors);
            return false;
          }
          stats.transferTimer.stop();
          for (ARGState e : successors) {
            unexploredParent = false;
            for (ARGState p:e.getParents()) {
              if (!reachedSet.contains(p) || inWaitlist.contains(p)) {
                waitingForUnexploredParents.add(e);
                unexploredParent = true;
                break;
              }
            }
            if (unexploredParent) {
              continue;
            }
            if (reachedSet.contains(e)) {
              // state unknown parent of e
              stats.totalTimer.stop();
              logger.log(Level.WARNING, "State", e, "has other parents than", e.getParents());
              return false;
            } else {
              waitingForUnexploredParents.remove(e);
              reachedSet.add(e, initialPrecision);
              inWaitlist.add(e);
            }
          }
        }
      }
    } while (!postponedStates.isEmpty());
    stats.totalTimer.stop();

    return waitingForUnexploredParents.isEmpty();
  }

  private boolean isCoveringCycleFree(ARGState pState) {
    HashSet<ARGState> seen = new HashSet<>();
    seen.add(pState);
    while (pState.isCovered()) {
      pState = pState.getCoveringState();
      boolean isNew = seen.add(pState);
      if (!isNew) { return false; }
    }
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
