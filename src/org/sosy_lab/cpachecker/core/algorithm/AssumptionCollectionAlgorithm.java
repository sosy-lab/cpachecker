/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.util.AbstractWrappedElementVisitor;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Formula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.util.assumptions.ReportingUtils;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.assumptions.collector.AssumptionCollectorCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.collector.AssumptionCollectorElement;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;

import com.google.common.collect.ImmutableSet;

/**
 * Outer algorithm to collect all invariants generated during
 * the analysis, and report them to the user
 *
 * @author g.theoduloz
 */
@Options
public class AssumptionCollectionAlgorithm implements Algorithm, StatisticsProvider {

  private class AssumptionCollectionStatistics implements Statistics {
    @Override
    public String getName() {
      return "Assumption Collection algorithm";
    }
    
    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {

      out.println("Number of locations with assumptions: " + resultAssumption.getNumberOfLocations());
      
      if (exportAssumptions && assumptionsFile != null) {
        try {
          Files.writeFile(assumptionsFile, resultAssumption);
        } catch (IOException e) {
          logger.log(Level.WARNING,
              "Could not write assumptions to file ", assumptionsFile.getAbsolutePath(),
              ", (", e.getMessage(), ")");
        }
      }
    }
  }
  
  @Option(name="assumptions.export")
  private boolean exportAssumptions = true;

  @Option(name="assumptions.file", type=Option.Type.OUTPUT_FILE)
  private File assumptionsFile = new File("assumptions.txt");

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final SymbolicFormulaManager symbolicManager;
  private final AssumptionWithLocation resultAssumption;

  public AssumptionCollectionAlgorithm(Algorithm algo, Configuration config, LogManager logger) throws InvalidConfigurationException
  {
    config.inject(this);

    this.logger = logger;
    innerAlgorithm = algo;
    AssumptionCollectorCPA cpa = ((WrapperCPA)getCPA()).retrieveWrappedCpa(AssumptionCollectorCPA.class);
    if (cpa == null) {
      throw new InvalidConfigurationException("AssumptionCollectorCPA needed for AssumptionCollectionAlgorithm");
    }
    symbolicManager = cpa.getSymbolicFormulaManager();
    resultAssumption = new AssumptionWithLocation(symbolicManager);
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return innerAlgorithm.getCPA();
  }

  @Override
  public void run(ReachedSet reached) throws CPAException {

    boolean restartCPA = false;

    // loop if restartCPA is set to false
    do {
      restartCPA = false;
      try {
        // run the inner algorithm to fill the reached set
        innerAlgorithm.run(reached);
      } catch (RefinementFailedException failedRefinement) {
        logger.log(Level.FINER, "Dumping assumptions due to:", failedRefinement);
        addAssumptionsForFailedRefinement(resultAssumption, failedRefinement);

        // remove element and it's parent from reached set
        // parent needs to be removed, because CPAAlgorithm re-added it
        // to the waitlist just before refinement
        ARTElement ae = (ARTElement)reached.getLastElement();
        for (ARTElement p : ImmutableSet.copyOf(ae.getParents())) {
          reached.remove(p);
          p.removeFromART();
        }
        reached.remove(ae);
        ae.removeFromART();

        restartCPA = true;
      } catch (CPAException e) {
        logger.log(Level.FINER, "Dumping assumptions due to: " + e.toString());
      }
    } while (restartCPA);

    // collect and dump all assumptions stored in abstract states
    logger.log(Level.FINER, "Dumping assumptions resulting from tool assumptions");
    for (AbstractElement element : reached) {
      if (element instanceof AbstractWrapperElement) {
        CFANode location = ((AbstractWrapperElement)element).retrieveLocationElement().getLocationNode();

        AssumptionExtractor extractor = new AssumptionExtractor();
        extractor.visit(element);
        resultAssumption.add(location, extractor.result);
      } else {
        assert false;
      }
    }

    // dump invariants to prevent going further with nodes in
    // the waitlist
    if (reached.hasWaitingElement()) {
      logger.log(Level.FINER, "Dumping assumptions resulting from unprocessed elements");
      addAssumptionsForWaitlist(resultAssumption, reached.getWaitlist());
    }
  }

  
  /**
   * Extracts the invariant(s) stored in abstract elements.
   */
  private class AssumptionExtractor extends AbstractWrappedElementVisitor {
    
    private Formula result = symbolicManager.makeTrue();
    
    @Override
    public void process(AbstractElement pElement) {
      if (pElement instanceof AssumptionCollectorElement) {
        Formula dumpedInvariant = ((AssumptionCollectorElement)pElement).getCollectedAssumption();
        result = symbolicManager.makeAnd(result, dumpedInvariant);
      }
    }
  }
  

  /**
   * Add to the given map the invariant required to
   * avoid the given refinement failure
   */
  private void addAssumptionsForFailedRefinement(
      AssumptionWithLocation invariant,
      RefinementFailedException failedRefinement) {
    Path path = failedRefinement.getErrorPath();

    int pos = failedRefinement.getFailurePoint();

    if (pos == -1)
      pos = path.size() - 2; // the node before the error node

    Pair<ARTElement, CFAEdge> pair = path.get(pos);
    Formula dataRegion = ReportingUtils.extractReportedFormulas(symbolicManager, pair.getFirst());
    invariant.add(pair.getFirst().retrieveLocationElement().getLocationNode(), symbolicManager.makeNot(dataRegion));
  }

  /**
   * Add to the given map the invariant required to
   * avoid nodes in the given set of states
   */
  private void addAssumptionsForWaitlist(
      AssumptionWithLocation invariant,
      Iterable<AbstractElement> waitlist) {
    for (AbstractElement element : waitlist) {
      Formula dataRegion = ReportingUtils.extractReportedFormulas(symbolicManager, element);
      invariant.add(((AbstractWrapperElement)element).retrieveLocationElement().getLocationNode(), symbolicManager.makeNot(dataRegion));
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (innerAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)innerAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(new AssumptionCollectionStatistics());
  }
}
