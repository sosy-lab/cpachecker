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
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.assumptions.Assumption;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithMultipleLocations;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionSymbolicFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.assumptions.ReportingUtils;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.assumptions.collector.AssumptionCollectorElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;

/**
 * Outer algorithm to collect all invariants generated during
 * the analysis, and report them to the user
 *
 * @author g.theoduloz
 */
@Options
public class AssumptionCollectionAlgorithm implements Algorithm, StatisticsProvider {

  @Option(name="assumptions.export")
  private boolean exportAssumptions = false;

  @Option(name="assumptions.file", type=Option.Type.OUTPUT_FILE)
  private File assumptionsFile = new File("assumptions.txt");

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final SymbolicFormulaManager symbolicManager;

  public AssumptionCollectionAlgorithm(Algorithm algo, Configuration config, LogManager logger) throws InvalidConfigurationException
  {
    config.inject(this);

    this.logger = logger;
    innerAlgorithm = algo;
    symbolicManager = AssumptionSymbolicFormulaManagerImpl.createSymbolicFormulaManager(config, logger);
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return innerAlgorithm.getCPA();
  }

  @Override
  public void run(ReachedSet reached) throws CPAException {

    AssumptionWithMultipleLocations resultAssumption = new AssumptionWithMultipleLocations();
    boolean restartCPA = false;

    // loop if restartCPA is set to false
    do {
      restartCPA = false;
      try {
        // run the inner algorithm to fill the reached set
        innerAlgorithm.run(reached);
      } catch (RefinementFailedException failedRefinement) {
        logger.log(Level.FINER, "Dumping assumptions due to: " + failedRefinement.toString());
        addAssumptionsForFailedRefinement(resultAssumption, failedRefinement);
      } catch (CPAException e) {
        logger.log(Level.FINER, "Dumping assumptions due to: " + e.toString());
      }
    } while (restartCPA);

    // collect and dump all assumptions stored in abstract states
    logger.log(Level.FINER, "Dumping assumptions resulting from tool assumptions");
    for (AbstractElement element : reached) {
      AssumptionWithLocation assumption = extractAssumption(element);

      resultAssumption.addAssumption(assumption);
    }

    // dump invariants to prevent going further with nodes in
    // the waitlist
    if (reached.hasWaitingElement()) {
      logger.log(Level.FINER, "Dumping assumptions resulting from unprocessed elements");
      addAssumptionsForWaitlist(resultAssumption, reached.getWaitlist());
    }

    Appendable output;
    if (exportAssumptions && assumptionsFile != null) {
      //if no filename is given, use default value

      try {
        output = new PrintWriter(assumptionsFile);
      } catch (Exception e) {
        logger.log(Level.WARNING,
            "Could not write assumptions to file ", assumptionsFile.getAbsolutePath(),
            ", (", e.getMessage(), ")");
        output = null;
      }
    } else {
      output = System.out;
    }
    if (output != null) {
      resultAssumption.dump(output);
      if (output != System.out)
        ((PrintWriter)output).close();
    }
  }

  /**
   * Returns the invariant(s) stored in the given abstract
   * element
   */
  private AssumptionWithLocation extractAssumption(AbstractElement element)
  {
    AssumptionWithLocation result = AssumptionWithLocation.TRUE;

    // If it is a wrapper, add its sub-element's assertions
    if (element instanceof AbstractWrapperElement)
    {
      for (AbstractElement subel : ((AbstractWrapperElement) element).getWrappedElements())
        result = result.and(extractAssumption(subel));
    }

    if (element instanceof AssumptionCollectorElement)
    {
      AssumptionWithLocation dumpedInvariant = ((AssumptionCollectorElement) element).getCollectedAssumptions();
      if (dumpedInvariant != null)
        result = result.and(dumpedInvariant);
    }

    return result;
  }

  /**
   * Add to the given map the invariant required to
   * avoid the given refinement failure
   */
  private void addAssumptionsForFailedRefinement(
      AssumptionWithMultipleLocations invariant,
      RefinementFailedException failedRefinement) {
    Path path = failedRefinement.getErrorPath();

    int pos = failedRefinement.getFailurePoint();

    if (pos == -1)
      pos = path.size() - 2; // the node before the error node

    Pair<ARTElement, CFAEdge> pair = path.get(pos);
    SymbolicFormula dataRegion = ReportingUtils.extractReportedFormulas(symbolicManager, pair.getFirst());
    invariant.addAssumption(pair.getFirst().retrieveLocationElement().getLocationNode(), new Assumption(dataRegion, false));
  }

  /**
   * Add to the given map the invariant required to
   * avoid nodes in the given set of states
   */
  private void addAssumptionsForWaitlist(
      AssumptionWithMultipleLocations invariant,
      List<AbstractElement> waitlist) {
    for (AbstractElement element : waitlist) {
      SymbolicFormula dataRegion = ReportingUtils.extractReportedFormulas(symbolicManager, element);
      invariant.addAssumption(((AbstractWrapperElement)element).retrieveLocationElement().getLocationNode(), new Assumption(symbolicManager.makeNot(dataRegion), false));
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (innerAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)innerAlgorithm).collectStatistics(pStatsCollection);
    }
  }
}
