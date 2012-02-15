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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options
public class ProofCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer         = new Timer();
    private Timer transferTimer      = new Timer();
    private Timer stopTimer          = new Timer();
    private Timer readTimer          = new Timer();

    private int   countIterations   = 0;

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
      out.println("  Time for reading in proof           " + readTimer);
      out.println("  Time for abstract successor check:  " + transferTimer + " (Calls: " + transferTimer.getNumberOfIntervals() + ")");
      out.println("  Time for covering check:            " + stopTimer  + " (Calls: " + stopTimer.getNumberOfIntervals() + ")");
    }
  }

  private final CPAStatistics stats = new CPAStatistics();
  private final ProofChecker cpa;
  private final LogManager logger;

  @Option(name = "pcc.proofFile", description = "file in which ART representation needed for proof checking is stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File file = new File("art.obj");
  private final ARTElement rootElement;


  public ProofCheckAlgorithm(ConfigurableProgramAnalysis cpa, Configuration pConfig, LogManager logger) throws InvalidConfigurationException {
    pConfig.inject(this);

    if(!(cpa instanceof ProofChecker)) {
      throw new InvalidConfigurationException("ProofCheckAlgorithm needs a CPA that implements the ProofChecker interface.");
    }
    this.cpa = (ProofChecker)cpa;
    this.logger = logger;

    ARTElement rootElement = null;
    try {
      rootElement = readART();
    } catch(Throwable e) {
      e.printStackTrace();
      throw new RuntimeException("Failed reading ART.", e);
    }
    this.rootElement = rootElement;
    System.gc();
  }

  private ARTElement readART() throws IOException, ClassNotFoundException {
    stats.totalTimer.start();
    stats.readTimer.start();
    InputStream fis = null;
    try {
      fis = new FileInputStream(file);
      ZipInputStream zis = new ZipInputStream(fis);
      ZipEntry entry = zis.getNextEntry();
      assert entry.getName().equals("Proof");
      ObjectInputStream o = new ObjectInputStream(zis);
      return (ARTElement)o.readObject();
    }
    finally {
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

    AbstractElement initialElement = reachedSet.popFromWaitlist();
    Precision initialPrecision = reachedSet.getPrecision(initialElement);

    logger.log(Level.FINE, "Checking root element");

    if(!(cpa.isCoveredBy(initialElement, rootElement) && cpa.isCoveredBy(rootElement, initialElement))) {
      stats.totalTimer.stop();
      logger.log(Level.WARNING, "Root element of proof is invalid.");
      return false;
    }

    reachedSet.add(rootElement, initialPrecision);

    while (reachedSet.hasWaitingElement()) {
      CPAchecker.stopIfNecessary();

      stats.countIterations++;
      ARTElement element = (ARTElement)reachedSet.popFromWaitlist();

      logger.log(Level.FINE, "Looking at element", element);

      if(element.isCovered()) {
        logger.log(Level.FINER, "Element is covered by another abstract element; checking coverage");
        stats.stopTimer.start();
        ARTElement coveringElement = element.getCoveringElement();
        if(!isCoveringCycleFree(element)) {
          stats.stopTimer.stop();
          stats.totalTimer.stop();
          logger.log(Level.WARNING, "Found cycle in covering relation for element", element);
          return false;
        }
        if(!cpa.isCoveredBy(element, coveringElement)) {
          stats.stopTimer.stop();
          stats.totalTimer.stop();
          logger.log(Level.WARNING, "Element", element, "is not covered by", coveringElement);
          return false;
        }
        stats.stopTimer.stop();
        if(!reachedSet.contains(coveringElement)) {
          stats.totalTimer.stop();
          logger.log(Level.WARNING, "Covering element", coveringElement, "was not found in reached set");
          return false; //this is a bit more strict than need, but allows the covering check to "trust" the elements
        }
      }
      else {
        stats.transferTimer.start();
        Collection<ARTElement> successors = element.getChildren();
        logger.log(Level.FINER, "Checking abstract successors", successors);
        if(!cpa.areAbstractSuccessors(element, null, successors)) {
          stats.transferTimer.stop();
          stats.totalTimer.stop();
          logger.log(Level.WARNING, "Element", element, "has other successors than", successors);
          return false;
        }
        stats.transferTimer.stop();
        for(ARTElement e : successors) {
          reachedSet.add(e, initialPrecision);
        }
      }
    }
    stats.totalTimer.stop();
    return true;
  }

  private boolean isCoveringCycleFree(ARTElement pElement) {
    HashSet<ARTElement> seen = new HashSet<ARTElement>();
    seen.add(pElement);
    while(pElement.isCovered()) {
      pElement = pElement.getCoveringElement();
      boolean isNew = seen.add(pElement);
      if(!isNew) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
