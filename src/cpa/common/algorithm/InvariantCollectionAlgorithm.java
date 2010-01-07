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

import java.util.List;
import java.util.logging.Level;

import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import common.Pair;

import cpa.art.ARTElement;
import cpa.common.Path;
import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.invariant.dump.DumpInvariantElement;
import cpa.invariant.util.FormulaReportingElement;
import cpa.invariant.util.FormulaReportingUtils;
import cpa.invariant.util.InvariantWithLocation;
import cpa.invariant.util.MathsatInvariantSymbolicFormulaManager;
import exceptions.CPAException;
import exceptions.RefinementFailedException;

/**
 * Outer algorithm to collect all invariants generated during
 * the analysis, and report them to the user
 * 
 * @author g.theoduloz
 */
public class InvariantCollectionAlgorithm implements Algorithm {

  private final Algorithm innerAlgorithm;
  private final MathsatInvariantSymbolicFormulaManager symbolicManager;
  
  public InvariantCollectionAlgorithm(Algorithm algo)
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
    
    InvariantWithLocation invariantMap = new InvariantWithLocation();
    
    try {
      // run the inner algorithm to fill the reached set
      innerAlgorithm.run(reached, stopAfterError);
      
    } catch (RefinementFailedException failedRefinement) {
      CPAMain.logManager.log(Level.ALL, "Dumping invariants due to: " + failedRefinement.toString());
      addInvariantsForFailedRefinement(invariantMap, failedRefinement);
    } catch (CPAException e) {
      CPAMain.logManager.log(Level.ALL, "Dumping invariants due to: " + e.toString());
    }
      
    // collect and dump all assumptions stored in abstract states
    CPAMain.logManager.log(Level.FINEST, "Dumping invariants resulting from assumptions");
    for (Pair<AbstractElementWithLocation, Precision> pair : reached.getReached())
    {
      AbstractElementWithLocation element = pair.getFirst();
      
      CFANode loc = element.getLocationNode();
      SymbolicFormula invariant = extractInvariant(element);
      
      invariantMap.addInvariant(loc, invariant);
    }
    
    // dump invariants to prevent going further with nodes in
    // the waitlist
    if (reached.hasWaitingElement()) {
      CPAMain.logManager.log(Level.FINEST, "Dumping invariants resulting from unprocessed elements");
      addInvariantsForWaitlist(invariantMap, reached.getWaitlist());
    }
    
    CPAMain.logManager.log(Level.ALL, "THE SYSTEM IS SAFE UNDER THE FOLLOWING INVARIANT:");
    invariantMap.dump(System.out);
  }

  /**
   * Returns the invariant(s) stored in the given abstract
   * element
   */
  private SymbolicFormula extractInvariant(AbstractElement element)
  {
    SymbolicFormula result = symbolicManager.makeTrue();
    
    // If it is a wrapper, add its sub-element's assertions
    if (element instanceof AbstractWrapperElement)
    {
      for (AbstractElement subel : ((AbstractWrapperElement) element).getWrappedElements())
        result = symbolicManager.makeAnd(result, extractInvariant(subel));
    }
    
    if (element instanceof DumpInvariantElement)
    {
      SymbolicFormula dumpedInvariant = ((DumpInvariantElement) element).getInvariant();
      if (dumpedInvariant != null)
        result = symbolicManager.makeAnd(result, dumpedInvariant);
    }
     
    return result;
  }

  /**
   * Add to the given map the invariant required to
   * avoid the given refinement failure 
   */
  private void addInvariantsForFailedRefinement(
      InvariantWithLocation invariant,
      RefinementFailedException failedRefinement) {
    Path path = failedRefinement.getErrorPath();
    
    int pos = failedRefinement.getFailurePoint();
    
    if (pos == -1)
      pos = path.size() - 2; // the node before the error node
    
    Pair<ARTElement, CFAEdge> pair = path.get(pos);
    SymbolicFormula dataRegion = FormulaReportingUtils.extractReportedFormulas(symbolicManager, pair.getFirst());
    invariant.addInvariant(pair.getFirst().getLocationNode(), dataRegion);
  }
  
  /**
   * Add to the given map the invariant required to
   * avoid nodes in the given set of states
   */
  private void addInvariantsForWaitlist(
      InvariantWithLocation invariant,
      List<Pair<AbstractElementWithLocation, Precision>> waitlist) {
    for (Pair<AbstractElementWithLocation, Precision> pair : waitlist) {
      AbstractElementWithLocation element = pair.getFirst();
      SymbolicFormula dataRegion = FormulaReportingUtils.extractReportedFormulas(symbolicManager, element);
      invariant.addInvariant(element.getLocationNode(), symbolicManager.makeNot(dataRegion));
    }
  }
}
