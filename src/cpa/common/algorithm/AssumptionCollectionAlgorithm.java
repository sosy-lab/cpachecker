/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.common.algorithm;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import symbpredabstraction.interfaces.SymbolicFormula;
import assumptions.Assumption;
import assumptions.AssumptionWithLocation;
import assumptions.AssumptionWithMultipleLocations;
import assumptions.MathsatInvariantSymbolicFormulaManager;
import assumptions.ReportingUtils;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

import common.Pair;

import cpa.art.ARTElement;
import cpa.art.Path;
import cpa.assumptions.collector.AssumptionCollectorElement;
import cpa.common.CPAchecker;
import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractWrapperElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Statistics;
import cpa.common.interfaces.StatisticsProvider;
import exceptions.CPAException;
import exceptions.RefinementFailedException;
import exceptions.TransferTimeOutException;

/**
 * Outer algorithm to collect all invariants generated during
 * the analysis, and report them to the user
 * 
 * @author g.theoduloz
 */
public class AssumptionCollectionAlgorithm implements Algorithm, StatisticsProvider {

  private final Algorithm innerAlgorithm;
  private final MathsatInvariantSymbolicFormulaManager symbolicManager;
  
  public AssumptionCollectionAlgorithm(Algorithm algo)
  {
    innerAlgorithm = algo;
    symbolicManager = MathsatInvariantSymbolicFormulaManager.getInstance();
  }
  
  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return innerAlgorithm.getCPA();
  }
  
  @Override
  public void run(ReachedElements reached, boolean stopAfterError)
      throws CPAException {
    
    AssumptionWithMultipleLocations resultAssumption = new AssumptionWithMultipleLocations();
    boolean restartCPA = false;
    
    // loop if restartCPA is set to false
    do {
      restartCPA = false;
      try {
        // run the inner algorithm to fill the reached set
        innerAlgorithm.run(reached, stopAfterError);
      } catch (RefinementFailedException failedRefinement) {
        CPAchecker.logger.log(Level.ALL, "Dumping assumptions due to: " + failedRefinement.toString());
        addAssumptionsForFailedRefinement(resultAssumption, failedRefinement);
      } catch (TransferTimeOutException failedTransfer) {
        CPAchecker.logger.log(Level.ALL, "Dumping assumptions due to: " + failedTransfer.toString());
        addAssumptionsForFailedTransfer(resultAssumption, failedTransfer);
        restartCPA = true;
      } catch (CPAException e) {
        CPAchecker.logger.log(Level.ALL, "Dumping assumptions due to: " + e.toString());
      }
    } while (restartCPA);
      
    // collect and dump all assumptions stored in abstract states
    CPAchecker.logger.log(Level.FINEST, "Dumping assumptions resulting from tool assumptions");
    for (AbstractElement element : reached) {      
      AssumptionWithLocation assumption = extractAssumption(element);
      
      resultAssumption.addAssumption(assumption);
    }
    
    // dump invariants to prevent going further with nodes in
    // the waitlist
    if (reached.hasWaitingElement()) {
      CPAchecker.logger.log(Level.FINEST, "Dumping assumptions resulting from unprocessed elements");
      addAssumptionsForWaitlist(resultAssumption, reached.getWaitlist());
    }
    
    CPAchecker.logger.log(Level.ALL, "THE SYSTEM IS SAFE UNDER THE FOLLOWING ASSUMPTION:");
    resultAssumption.dump(System.out);
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
  
  /**
   * Returns the invariant(s) necessary to avoid the failed transfer
   */
  private void addAssumptionsForFailedTransfer(
      AssumptionWithMultipleLocations invariant,
      TransferTimeOutException failedTransfer) {
    CFANode sourceLocation = failedTransfer.getCfaEdge().getPredecessor();
    SymbolicFormula dataRegion = ReportingUtils.extractReportedFormulas(symbolicManager, failedTransfer.getAbstractElement());
    invariant.addAssumption(sourceLocation, new Assumption (symbolicManager.makeNot(dataRegion), false));
  }
  
  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (innerAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)innerAlgorithm).collectStatistics(pStatsCollection);
    }
  }
}
