package org.sosy_lab.cpachecker.efshell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.fshell.interfaces.FQLCoverageAnalyser;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

import com.google.common.base.Joiner;

/*
 * TODO AutomatonBuilder <- integrate State-Pool there to ensure correct time
 * measurements when invoking FlleSh several times in a unit test.
 *
 * TODO Incremental test goal automaton creation: extending automata (can we reuse
 * parts of the reached set?) This requires a change in the coverage check.
 * -> Handle enormous amounts of test goals.
 *
 */

public class FShell3 implements FQLTestGenerator, FQLCoverageAnalyser {

  private final IncrementalFQLTestGenerator mIncrementalTestGenerator;
  private final IncrementalAndAlternatingFQLTestGenerator mIncrementalAndAlternatingTestGenerator;
  private final StandardFQLCoverageAnalyser mCoverageAnalyser;

  private final IncrementalARTReusingFQLTestGenerator mIncrementalARTReusingTestGenerator;

  public FShell3(String pSourceFileName, String pEntryFunction) {
    mIncrementalTestGenerator = new IncrementalFQLTestGenerator(pSourceFileName, pEntryFunction);
    mIncrementalAndAlternatingTestGenerator = new IncrementalAndAlternatingFQLTestGenerator(pSourceFileName, pEntryFunction);

    mCoverageAnalyser = new StandardFQLCoverageAnalyser(pSourceFileName, pEntryFunction);

    mIncrementalARTReusingTestGenerator = new IncrementalARTReusingFQLTestGenerator(pSourceFileName, pEntryFunction);
  }

  public FShell3Result run(String pFQLSpecification,TestCase pTestCase, PrintWriter out) {
    return run(pFQLSpecification, true, false, false, false, true, false,pTestCase,out);
  }

  @Override
  public FShell3Result run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating,TestCase pTestCase,PrintWriter out) {
    if (pGenerateTestGoalAutomataInAdvance) {
      //return mNonincrementalTestGenerator.run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pGenerateTestGoalAutomataInAdvance, pCheckCorrectnessOfCoverageCheck, pPedantic, pAlternating);

    }
    else {
      if (pAlternating) {
       // return mIncrementalAndAlternatingTestGenerator.run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pGenerateTestGoalAutomataInAdvance, pCheckCorrectnessOfCoverageCheck, pPedantic, pAlternating);
      }
      else {
        // TODO make configurable
        if (!pAlternating) {
          return mIncrementalARTReusingTestGenerator.run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pGenerateTestGoalAutomataInAdvance, pCheckCorrectnessOfCoverageCheck, pPedantic, pAlternating,pTestCase,out);
        }
        else {
          return mIncrementalTestGenerator.run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pGenerateTestGoalAutomataInAdvance, pCheckCorrectnessOfCoverageCheck, pPedantic, pAlternating,pTestCase,out);
        }
      }
    }
    return null;
  }

  @Override
  public void checkCoverage(String pFQLSpecification, Collection<TestCase> pTestSuite, boolean pPedantic) {
    mCoverageAnalyser.checkCoverage(pFQLSpecification, pTestSuite, pPedantic);
  }

  public static Map<String, CFAFunctionDefinitionNode> getCFAMap(String pSourceFileName, Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    CFACreator lCFACreator = new CFACreator( pConfiguration, pLogManager);

    // parse code file
    try {
      lCFACreator.parseFileAndCreateCFA(pSourceFileName);
    } catch (Exception e) {
      e.printStackTrace();

      throw new RuntimeException(e);
    }

    return lCFACreator.getFunctions();
  }

  public static ThreeValuedAnswer accepts(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, CFAEdge[] pCFAPath) {
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<NondeterministicFiniteAutomaton.State>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<NondeterministicFiniteAutomaton.State>();

    lCurrentStates.add(pAutomaton.getInitialState());

    boolean lHasPredicates = false;

    for (CFAEdge lCFAEdge : pCFAPath) {
      for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (pAutomaton.getFinalStates().contains(lCurrentState)) {
          return ThreeValuedAnswer.ACCEPT;
        }

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lCurrentState)) {
          GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

          if (lLabel.hasGuards()) {
            lHasPredicates = true;
          }
          else {
            if (lLabel.contains(lCFAEdge)) {
              lNextStates.add(lOutgoingEdge.getTarget());
            }
          }
        }
      }

      lCurrentStates.clear();

      Set<NondeterministicFiniteAutomaton.State> lTmp = lCurrentStates;
      lCurrentStates = lNextStates;
      lNextStates = lTmp;
    }

    for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
      // Automaton accepts as soon as it sees a final state (implicit self-loop)
      if (pAutomaton.getFinalStates().contains(lCurrentState)) {
        return ThreeValuedAnswer.ACCEPT;
      }
    }

    if (lHasPredicates) {
      return ThreeValuedAnswer.UNKNOWN;
    }
    else {
      return ThreeValuedAnswer.REJECT;
    }
  }

  public static Configuration createConfiguration(String pSourceFile, String pEntryFunction) throws InvalidConfigurationException {
    File lPropertiesFile = FShell3.createPropertiesFile(pEntryFunction);
    return createConfiguration(Collections.singletonList(pSourceFile), lPropertiesFile.getAbsolutePath());
  }

  private static Configuration createConfiguration(List<String> pSourceFiles, String pPropertiesFile) throws InvalidConfigurationException {
    Map<String, String> lCommandLineOptions = new HashMap<String, String>();

    lCommandLineOptions.put("analysis.programNames", Joiner.on(", ").join(pSourceFiles));
    //lCommandLineOptions.put("output.path", "test/output");

    Configuration lConfiguration = null;
    try {
      lConfiguration = Configuration.builder()
                                    .loadFromFile(pPropertiesFile)
                                    .setOptions(lCommandLineOptions)
                                    .build();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return lConfiguration;
  }

  private static File createPropertiesFile(String pEntryFunction) {
    if (pEntryFunction == null) {
      throw new IllegalArgumentException("Parameter pEntryFunction is null!");
    }

    File lPropertiesFile = null;

    try {

      lPropertiesFile = File.createTempFile("fshell.", ".properties");
      lPropertiesFile.deleteOnExit();

      PrintWriter lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile));
      // we do not use a fixed error location (error label) therefore
      // we do not want to remove parts of the CFA
      lWriter.println("cfa.removeIrrelevantForErrorLocations = false");

      //lWriter.println("log.consoleLevel = ALL");

      lWriter.println("analysis.traversal.order = topsort");
      lWriter.println("analysis.entryFunction = " + pEntryFunction);

      // we want to use CEGAR algorithm
      lWriter.println("analysis.useRefinement = true");
      lWriter.println("cegar.refiner = " + org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner.class.getCanonicalName());

      lWriter.println("cpa.predicate.addBranchingInformation = false");
      lWriter.println("cpa.predicate.useNondetFlags = true");
      lWriter.println("cpa.predicate.initAllVars = false");
      //lWriter.println("cpa.predicate.noAutoInitPrefix = __BLAST_NONDET");
      lWriter.println("cpa.predicate.blk.useCache = false");
      //lWriter.println("cpa.predicate.mathsat.lvalsAsUIFs = true");
      // we need theory combination for example for using uninterpreted functions used in conjunction with linear arithmetic (correctly)
      // TODO caution: using dtc changes the results ... WRONG RESULTS !!!
      //lWriter.println("cpa.predicate.mathsat.useDtc = true");

      lWriter.println("cpa.interval.merge = JOIN");

      lWriter.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return lPropertiesFile;
  }
}

