/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathStandardElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterElement;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.cfa.Wrapper;
import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.interfaces.FQLCoverageAnalyser;
import org.sosy_lab.cpachecker.fshell.testcases.CRESTToFShell3;
import org.sosy_lab.cpachecker.fshell.testcases.FShell2ToFShell3;
import org.sosy_lab.cpachecker.fshell.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.fshell.testcases.KLEEToFShell3;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;
import org.sosy_lab.cpachecker.fshell.testcases.TestSuite;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.util.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.ToGuardedAutomatonTranslator;

/*
 * TODO AutomatonBuilder <- integrate State-Pool there to ensure correct time
 * measurements when invoking FlleSh several times in a unit test.
 *
 * TODO Incremental test goal automaton creation: extending automata (can we reuse
 * parts of the reached set?) This requires a change in the coverage check.
 * -> Handle enormous amounts of test goals.
 */

public class StandardFQLCoverageAnalyser implements FQLCoverageAnalyser {

  private final Configuration mConfiguration;
  private final LogManager mLogManager;
  private final Wrapper mWrapper;
  private final CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  private final LocationCPA mLocationCPA;
  private final CallstackCPA mCallStackCPA;
  private final AssumeCPA mAssumeCPA;
  private final CFAPathCPA mCFAPathCPA;

  private final GuardedEdgeLabel mAlphaLabel;
  private final GuardedEdgeLabel mOmegaLabel;
  private final GuardedEdgeLabel mInverseAlphaLabel;

  private static void printUsage() {
    System.err.println("Usage: java " + StandardFQLCoverageAnalyser.class.getCanonicalName() + " [--fshell2|--klee|--crest] <source file> <entry function> <fql specification> <test suite file>");
  }

  public static void main(String args[]) throws IOException {
    if (args.length != 4 && args.length != 5) {
      printUsage();
      return;
    }

    String lSourceFileName = args[0];
    String lEntryFunction = args[1];
    String lFQLSpecification = args[2];
    String lTestSuiteFileName = args[3];

    if (args.length == 5) {
      if (args[0].equals("--fshell2")) {
        File lTmpTestSuiteFile = File.createTempFile("testsuite.", ".tst");
        lTmpTestSuiteFile.deleteOnExit();

        System.out.print("Translate FShell2 test suite to FShell3 test suite ... ");

        FShell2ToFShell3.translateTestsuite(args[4], lTmpTestSuiteFile.getAbsolutePath());

        System.out.println("done.");

        lTestSuiteFileName = lTmpTestSuiteFile.getAbsolutePath();
      }
      else if (args[0].equals("--klee")) {
        File lTmpTestsuiteFile = File.createTempFile("testsuite.", ".tst");
        lTmpTestsuiteFile.deleteOnExit();

        System.out.print("Translate KLEE test suite to FShell3 test suite ... ");

        TestSuite lTestSuite = KLEEToFShell3.translateTestSuite(args[2]);
        lTestSuite.write(lTmpTestsuiteFile);

        System.out.println("done.");

        lTestSuiteFileName = lTmpTestsuiteFile.getAbsolutePath();
      }
      else if (args[0].equals("--crest")) {
        File lTmpTestsuiteFile = File.createTempFile("testsuite.", ".tst");
        lTmpTestsuiteFile.deleteOnExit();

        System.out.print("Translate CREST test suite to FShell3 test suite ... ");

        TestSuite lTestSuite = CRESTToFShell3.translateTestSuite(args[2]);
        lTestSuite.write(lTmpTestsuiteFile);

        System.out.println("done.");

        lTestSuiteFileName = lTmpTestsuiteFile.getAbsolutePath();
      }
      else {
        printUsage();
        return;
      }

      lSourceFileName = args[1];
      lEntryFunction = args[2];
      lFQLSpecification = args[3];
    }
    else {
      lSourceFileName = args[0];
      lEntryFunction = args[1];
      lFQLSpecification = args[2];
      lTestSuiteFileName = args[3];
    }

    StandardFQLCoverageAnalyser lCoverageAnalyser = new StandardFQLCoverageAnalyser(lSourceFileName, lEntryFunction);
    Collection<TestCase> lTestSuite = TestCase.fromFile(lTestSuiteFileName);
    lCoverageAnalyser.checkCoverage(lFQLSpecification, lTestSuite, true);
  }

  public StandardFQLCoverageAnalyser(String pSourceFileName, String pEntryFunction) {
    Map<String, CFAFunctionDefinitionNode> lCFAMap;
    CFAFunctionDefinitionNode lMainFunction;

    try {
      mConfiguration = FShell3.createConfiguration(pSourceFileName, pEntryFunction);
      mLogManager = new LogManager(mConfiguration);

      lCFAMap = FShell3.getCFAMap(pSourceFileName, mConfiguration, mLogManager);
      lMainFunction = lCFAMap.get(pEntryFunction);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    /*
     * We have to instantiate mCoverageSpecificationTranslator before the wrapper
     * changes the underlying CFA. FQL specifications are evaluated against the
     * target graph generated during initialization of mCoverageSpecificationTranslator.
     */
    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(lMainFunction);

    mWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, mLogManager);

    try {
      mWrapper.toDot("test/output/wrapper.dot");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(mWrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(mWrapper.getOmegaEdge()));


    /*
     * Initialize shared CPAs.
     */
    // location CPA
    try {
      mLocationCPA = (LocationCPA)LocationCPA.factory().createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // callstack CPA
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    try {
      mCallStackCPA = (CallstackCPA)lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // assume CPA
    mAssumeCPA = AssumeCPA.getCBMCAssume();

    // cfa path CPA
    mCFAPathCPA = CFAPathCPA.getInstance();
  }

  @Override
  public void checkCoverage(String pFQLSpecification, Collection<TestCase> pTestSuite, boolean pPedantic) {
    getCoverage(pFQLSpecification, pTestSuite, pPedantic);
  }

  public boolean[] getCoverage(String pFQLSpecification, Collection<TestCase> pTestSuite, boolean pPedantic) {
    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return checkCoverage(lFQLSpecification, pTestSuite, pPedantic);
  }

  private boolean[] checkCoverage(FQLSpecification pFQLSpecification, Collection<TestCase> pTestSuite, boolean pPedantic) {

    GuardedEdgeAutomatonCPA lPassingCPA = null;

    if (pFQLSpecification.hasPassingClause()) {
      ElementaryCoveragePattern lPassingClause = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(pFQLSpecification.getPathPattern());
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lPassingClause, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
      lPassingCPA = new GuardedEdgeAutomatonCPA(lAutomaton);
    }

    IncrementalCoverageSpecificationTranslator lTranslator = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);

    int lNumberOfTestGoals = lTranslator.getNumberOfTestGoals(pFQLSpecification.getCoverageSpecification());

    System.out.println("Number of test goals: " + lNumberOfTestGoals);

    boolean[] lCoverage = new boolean[lNumberOfTestGoals];

    Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator.translate(pFQLSpecification.getCoverageSpecification());

    int lIndex = 0;

    int lNumberOfCoveredTestGoals = 0;

    Map<TestCase, CFAEdge[]> mPathCache = new HashMap<TestCase, CFAEdge[]>();

    FQLSpecification lIdStarFQLSpecification;
    try {
      lIdStarFQLSpecification = FQLSpecification.parse("COVER \"EDGES(ID)*\" PASSING EDGES(ID)*");
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      throw new RuntimeException(e1);
    }
    ElementaryCoveragePattern lIdStarPattern = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(lIdStarFQLSpecification.getPathPattern());
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lIdStarPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
    GuardedEdgeAutomatonCPA lIdStarCPA = new GuardedEdgeAutomatonCPA(lAutomaton);

    while (lGoalIterator.hasNext()) {
      lIndex++;

      ElementaryCoveragePattern lGoalPattern = lGoalIterator.next();

      System.out.println("Processing test goal #" + lIndex + " of " + lNumberOfTestGoals + " test goals.");

      Goal lGoal = new Goal(lGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton());

      boolean lIsCovered = false;

      TestCase lLastTestCase = null;

      for (TestCase lTestCase : pTestSuite) {
        // TODO combine with pedantic flag
        /*if (!lTestCase.isPrecise()) {
          throw new RuntimeException();
        }*/

        lLastTestCase = lTestCase;

        CFAEdge[] lCFAPath = mPathCache.get(lTestCase);

        boolean lIsPrecise = true;

        if (lCFAPath == null) {
          try {
            lCFAPath = reconstructPath(lTestCase, mWrapper.getEntry(), lIdStarCPA, null, mWrapper.getOmegaEdge().getSuccessor());
          } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
          } catch (CPAException e) {
            throw new RuntimeException(e);
          } catch (ImpreciseExecutionException e) {
            lIsPrecise = false;
            lTestCase = e.getTestCase();

            if (pPedantic) {
              throw new RuntimeException(e);
            }
          }

          if (lIsPrecise) {
            mPathCache.put(lTestCase, lCFAPath);
          }
        }

        ThreeValuedAnswer lCoverageAnswer = FShell3.accepts(lGoal.getAutomaton(), lCFAPath);

        if (lCoverageAnswer.equals(ThreeValuedAnswer.ACCEPT)) {
          lIsCovered = true;

          /*if (lIndex == 88) {
            int lLine = 1;
            for (CFAEdge lEdge : lCFAPath) {
              System.out.println(lLine + ": " + lEdge.toString());
              lLine++;
            }
            System.out.println(lGoal.getPattern());
            System.exit(-1);
          }*/

          break;
        }
        else if (lCoverageAnswer.equals(ThreeValuedAnswer.UNKNOWN)) {
          try {
            if (checkCoverage(lTestCase, mWrapper.getEntry(), lAutomatonCPA, lPassingCPA, mWrapper.getOmegaEdge().getSuccessor())) {
              lIsCovered = true;

              break;
            }
          } catch (InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
          } catch (CPAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
          } catch (ImpreciseExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }
      }


      if (lIsCovered) {
        System.out.println("Test goal #" + lIndex + " is covered");

        System.out.println(lLastTestCase);

        lNumberOfCoveredTestGoals++;
      }
      else {
        System.out.println("Test goal #" + lIndex + " is not covered");
      }

      lCoverage[lIndex - 1] = lIsCovered;
    }

    System.out.println("Coverage: " + ((double)lNumberOfCoveredTestGoals)/((double)lIndex));

    return lCoverage;
  }

  private boolean checkCoverage(TestCase pTestCase, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<ConfigurableProgramAnalysis>(2);

    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lAutomatonCPAs.add(pPassingAutomatonCPA);
    }

    lAutomatonCPAs.add(pCoverAutomatonCPA);

    int lProductAutomatonCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs());
    lComponentAnalyses.add(lInterpreterCPA);

    // assume CPA
    lComponentAnalyses.add(mAssumeCPA);


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(mConfiguration);
    lCPAFactory.setLogger(mLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

    // TODO sanity check by assertion
    CompositeElement lEndNode = (CompositeElement)lReachedSet.getLastElement();

    if (lEndNode == null) {
      return false;
    }
    else {
      if (((LocationElement)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
        // location of last element is at end node

        AbstractElement lProductAutomatonElement = lEndNode.get(lProductAutomatonCPAIndex);

        if (lProductAutomatonElement instanceof Targetable) {
          Targetable lTargetable = (Targetable)lProductAutomatonElement;

          return lTargetable.isTarget();
        }

        return false;
      }

      return false;
    }
  }

  private CFAEdge[] reconstructPath(TestCase pTestCase, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<ConfigurableProgramAnalysis>(2);

    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lAutomatonCPAs.add(pPassingAutomatonCPA);
    }

    lAutomatonCPAs.add(pCoverAutomatonCPA);

    int lProductAutomatonCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs());
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
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

    // TODO sanity check by assertion
    CompositeElement lEndNode = (CompositeElement)lReachedSet.getLastElement();

    if (lEndNode == null) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }

    if (!((LocationElement)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }

    AbstractElement lProductAutomatonElement = lEndNode.get(lProductAutomatonCPAIndex);

    if (!(lProductAutomatonElement instanceof Targetable)) {
      throw new RuntimeException();
    }

    Targetable lTargetable = (Targetable)lProductAutomatonElement;

    if (!lTargetable.isTarget()) {
      throw new RuntimeException();
    }

    CFAPathStandardElement lPathElement = (CFAPathStandardElement)lEndNode.get(lCFAPathCPAIndex);

    InterpreterElement lInterpreterElement = (InterpreterElement)lEndNode.get(lInterpreterCPAIndex);

    if (lInterpreterElement.getInputs().length != lInterpreterElement.getInputIndex()) {
      throw new RuntimeException("Unused Inputs!!!");
    }

    return lPathElement.toArray();
  }
}

