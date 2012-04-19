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
package org.sosy_lab.cpachecker.fshell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathStandardElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress.ProgressPrecision;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterElement;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.MissingInputException;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.testcases.PreciseInputsTestCase;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;

public class AlternatingRefiner implements Refiner {

  private final PredicateRefiner mPredicateRefiner;

  private final Configuration mConfiguration;
  private final LogManager mLogManager;

  private final GuardedEdgeAutomatonCPA mCoverAutomatonCPA;
  private final GuardedEdgeAutomatonCPA mPassingAutomatonCPA;
  private final ProductAutomatonCPA mProductAutomatonCPA;
  private final LocationCPA mLocationCPA;
  private final CallstackCPA mCallStackCPA;
  private final AssumeCPA mAssumeCPA;
  private final CFAPathCPA mCFAPathCPA;

  private final CFAFunctionDefinitionNode mEntryNode;
  private final CFANode mEndNode;

  private TestCase mCoveringTestCase = null;
  // set of non-covering test cases
  private Set<TestCase> mIntermediateTestSuite;
  private CFAEdge[] mExecutionPath = null;

  public AlternatingRefiner(GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, LocationCPA pLocationCPA, CallstackCPA pCallStackCPA, AssumeCPA pAssumeCPA, CFAPathCPA pCFAPathCPA, ARTCPA pARTCPA, CFAFunctionDefinitionNode pEntryNode, CFANode pEndNode, PredicateRefiner pPredicateRefiner, Configuration pConfiguration, LogManager pLogManager) {
    mCoverAutomatonCPA = pCoverAutomatonCPA;
    mPassingAutomatonCPA = pPassingAutomatonCPA;
    mLocationCPA = pLocationCPA;
    mCallStackCPA = pCallStackCPA;
    mAssumeCPA = pAssumeCPA;
    mCFAPathCPA = pCFAPathCPA;

    mPredicateRefiner = pPredicateRefiner;

    mConfiguration = pConfiguration;
    mLogManager = pLogManager;

    mEntryNode = pEntryNode;
    mEndNode = pEndNode;

    // create product automaton CPA
    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<ConfigurableProgramAnalysis>(2);

    // test goal automata CPAs
    if (mPassingAutomatonCPA != null) {
      lAutomatonCPAs.add(mPassingAutomatonCPA);
    }

    lAutomatonCPAs.add(mCoverAutomatonCPA);

    mProductAutomatonCPA = ProductAutomatonCPA.create(lAutomatonCPAs, false);


    mIntermediateTestSuite = new HashSet<TestCase>();
  }

  public boolean hasCoveringTestCase() {
    return (mCoveringTestCase != null);
  }

  public TestCase getCoveringTestCase() {
    return mCoveringTestCase;
  }

  public CFAEdge[] getExecutionPath() {
    return mExecutionPath;
  }

  public Set<TestCase> getIntermediateTestSuite() {
    return mIntermediateTestSuite;
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {

    CounterexampleInfo lTraceInfo = mPredicateRefiner.performRefinementWithInfo(pReached);

    if (lTraceInfo.isSpurious()) {
      // symbolic path is infeasible, we continue symbolic
      // exploration after the refinement

      return true;
    }

    // symbolic path is feasible

    // construct (partial) test case
    TestCase lTestCase = TestCase.fromCounterexample(lTraceInfo, mLogManager);

    if (!lTestCase.isPrecise()) {
      // test case is imprecise
      // TODO implement proper logging mechanism
      System.err.println("TEST CASE IS IMPRECISE!");
    }


    // reconstruct path

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);

    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);

    InterpreterCPA lInterpreterCPA = new InterpreterCPA(lTestCase.getInputs(), true);
    int lInterpreterCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(lInterpreterCPA);

    int lProductAutomatonCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(mProductAutomatonCPA);

    // CFA path CPA
    int lCFAPathCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(mCFAPathCPA);

    // assume CPA
    lComponentAnalyses.add(mAssumeCPA);


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(mConfiguration);
    lCPAFactory.setLogger(mLogManager);
    ConfigurableProgramAnalysis lCPA;
    try {
      lCPA = lCPAFactory.createInstance();
    } catch (InvalidConfigurationException e1) {
      throw new RuntimeException(e1);
    }

    CPAAlgorithm lAlgorithm;
    try {
      lAlgorithm = new CPAAlgorithm(lCPA, mLogManager, mConfiguration);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    AbstractElement lInitialElement = lCPA.getInitialElement(mEntryNode);
    Precision lInitialPrecision = lCPA.getInitialPrecision(mEntryNode);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    boolean lMissesInput = false;

    try {
      lAlgorithm.run(lReachedSet);
    } catch (MissingInputException e) {
      // apply refinement (see below) and continue symbolic state space exploration
      lMissesInput = true;
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    CompositeElement lEndNode = (CompositeElement)lReachedSet.getLastElement();

    // TODO generalize index position of Location CPA (new variable)
    if (!lMissesInput && lEndNode != null) {
      if (((LocationElement)lEndNode.get(0)).getLocationNode().equals(mEndNode)) {
        if (((ProductAutomatonElement)lEndNode.get(lProductAutomatonCPAIndex)).isFinalState()) {
          InterpreterElement lInterpreterElement = (InterpreterElement)lEndNode.get(lInterpreterCPAIndex);

          // get inputs

          int[] lInputs = lInterpreterElement.getInputs();

          if (lTestCase.getInputs() == lInputs) {
            mCoveringTestCase = lTestCase;
          }
          else {
            mCoveringTestCase = new PreciseInputsTestCase(lInputs);
          }

          CFAPathStandardElement lPathElement = (CFAPathStandardElement)lEndNode.get(lCFAPathCPAIndex);

          mExecutionPath = lPathElement.toArray();

          // no refinement necessary, since test case covers
          return false;
        }
        else {
          throw new RuntimeException();
        }
      }
      else {
      /*  // reconstruct CFA path without automata

        LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses_b = new LinkedList<ConfigurableProgramAnalysis>();
        lComponentAnalyses_b.add(mLocationCPA);

        // call stack CPA
        lComponentAnalyses_b.add(mCallStackCPA);

        // a) test goal is satisfied, i.e., product automaton accepts program execution

        InterpreterCPA lInterpreterCPA = new InterpreterCPA(lTestCase.getInputs(), true);
        int lInterpreterCPAIndex = lComponentAnalyses.size();
        lComponentAnalyses.add(lInterpreterCPA);

        // CFA path CPA
        int lCFAPathCPAIndex = lComponentAnalyses.size();
        lComponentAnalyses.add(mCFAPathCPA);

        // assume CPA
        lComponentAnalyses.add(mAssumeCPA);


        CPAFactory lCPAFactory = CompositeCPA.factory();
        lCPAFactory.setChildren(lComponentAnalyses);
        lCPAFactory.setConfiguration(mConfiguration);
        lCPAFactory.setLogger(mLogManager);
        ConfigurableProgramAnalysis lCPA;
        try {
          lCPA = lCPAFactory.createInstance();
        } catch (InvalidConfigurationException e1) {
          throw new RuntimeException(e1);
        }

        CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager);

        AbstractElement lInitialElement = lCPA.getInitialElement(mEntryNode);
        Precision lInitialPrecision = lCPA.getInitialPrecision(mEntryNode);

        ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
        lReachedSet.add(lInitialElement, lInitialPrecision);

        try {
          lAlgorithm.run(lReachedSet);
        } catch (CPAException e) {
          throw new RuntimeException(e);
        }

        CompositeElement lEndNode = (CompositeElement)lReachedSet.getLastElement();*/
      }
    }


    // test case does not cover

    mIntermediateTestSuite.add(lTestCase);

    /* refine precision in ART; CAUTION use ART indices!!! */

    AbstractElement lLastElement = pReached.getLastElement();

    CompositePrecision lPrecision = (CompositePrecision)pReached.getPrecision(lLastElement);

    ARTElement lLastARTElement = (ARTElement)lLastElement;

    CompositeElement lWrappedElement = (CompositeElement)lLastARTElement.getWrappedElement();

    // TODO generalize index
    CompositePrecision lProductAutomatonPrecision = (CompositePrecision)lPrecision.get(2);

    // TODO generalize to other indices
    ProgressPrecision lProgressPrecision = (ProgressPrecision)lProductAutomatonPrecision.get(0);

    Map<ARTElement, Precision> lRemoveElements = new HashMap<ARTElement, Precision>();

    for (ARTElement lParent : lLastARTElement.getParents()) {
      CompositePrecision lParentPrecision = (CompositePrecision)pReached.getPrecision(lParent);

      CompositePrecision lAutomatonPrecision = (CompositePrecision)lParentPrecision.get(2);

      List<Precision> lNewPrecisions = new ArrayList<Precision>(lProductAutomatonPrecision.getPrecisions().size());

      // TODO generalize to all indices
      lNewPrecisions.add(lProgressPrecision);

      for (int lIndex = 1; lIndex < lProductAutomatonPrecision.getPrecisions().size(); lIndex++) {
        lNewPrecisions.add(lAutomatonPrecision.get(lIndex));
      }

      CompositePrecision lNewAutomatonPrecision = new CompositePrecision(lNewPrecisions);

      List<Precision> lNewPrecisions2 = new ArrayList<Precision>(lParentPrecision.getPrecisions().size());

      for (int lIndex = 0; lIndex < 2; lIndex++) {
        lNewPrecisions2.add(lParentPrecision.get(lIndex));
      }

      lNewPrecisions2.add(lNewAutomatonPrecision);

      for (int lIndex = 2 + 1; lIndex < lWrappedElement.getNumberofElements(); lIndex++) {
        lNewPrecisions2.add(lParentPrecision.get(lIndex));
      }

      CompositePrecision lNewPrecision = new CompositePrecision(lNewPrecisions2);

      lRemoveElements.put(lParent, lNewPrecision);
    }

    ARTReachedSet lARTReached = new ARTReachedSet(pReached);

    for (Map.Entry<ARTElement, Precision> lEntry : lRemoveElements.entrySet()) {
      // TODO parents wieder zur Waitlist hinzufuegen?
      lARTReached.removeSubtree(lEntry.getKey(), lEntry.getValue());
    }

    // continue with symbolic exploration
    return true;
  }

}
