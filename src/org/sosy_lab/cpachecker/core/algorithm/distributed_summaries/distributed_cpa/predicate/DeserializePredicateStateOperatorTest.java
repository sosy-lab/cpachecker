package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.test.DeserializationTestUtils;
import org.sosy_lab.java_smt.api.SolverException;

public class DeserializePredicateStateOperatorTest {

  private static final String SPEC_PATH = "config/specification/default.spc";
  private static final String TEST_PROGRAM_PATH = "test/programs/block_analysis/intro_example.c";
  private static final String CONFIG_PATH =
      "config/distributed-block-summaries/predicateAnalysis-block-forward.properties";

  private CFA cfa;
  private BlockGraph blockGraph;
  private Map<MemoryLocation, CType> variableTypes;

  private Configuration config;
  private LogManager logger;
  private ShutdownManager shutdownManager;
  private ShutdownNotifier shutdownNotifier;
  private Solver solver;
  private Specification specification;

  @Before
  public void setUp() throws Exception {
    config = DeserializationTestUtils.createTestConfiguration(CONFIG_PATH);
    logger = LogManager.createTestLogManager();
    shutdownManager = DeserializationTestUtils.createShutdownManager();
    shutdownNotifier = shutdownManager.getNotifier();

    cfa = DeserializationTestUtils.parseCFA(config, logger, shutdownNotifier, TEST_PROGRAM_PATH);
    variableTypes = DeserializationTestUtils.extractVariableTypes(cfa);
    blockGraph = DeserializationTestUtils.createBlockGraph(cfa, config);

    solver = DeserializationTestUtils.createSolver(config, logger, shutdownNotifier);
    specification =
        DeserializationTestUtils.loadSpecification(
            SPEC_PATH, cfa, config, logger, shutdownNotifier);
  }

  @Test
  public void testDeserializeFromFormula_equivalenceHolds() throws Exception {
    for (BlockNode blockNode : blockGraph.getNodes()) {
      runAnalysisAndCheckDeserialization(blockNode);
    }
  }

  private void runAnalysisAndCheckDeserialization(BlockNode blockNode)
      throws CPAException, InterruptedException, InvalidConfigurationException, SolverException {

    AnalysisComponents components =
        DCPAAlgorithmFactory.createAlgorithm(
            logger, specification, cfa, config, shutdownManager, blockNode);

    ConfigurableProgramAnalysis cpa = components.cpa();
    Algorithm algorithm = components.algorithm();
    algorithm.run(components.reached());

    AbstractState lastState = components.reached().getLastState();
    PredicateAbstractState originalState =
        AbstractStates.extractStateByType(lastState, PredicateAbstractState.class);

    PredicateCPA predicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);

    DeserializePredicateStateOperator deserializeOperator =
        new DeserializePredicateStateOperator(predicateCPA, cfa, blockNode, variableTypes);

    SerializePredicateStateOperator serializeOperator =
        new SerializePredicateStateOperator(predicateCPA, cfa, true);
    org.sosy_lab.java_smt.api.BooleanFormula originalFormula =
        serializeOperator.serializeToFormula(originalState);
    PredicateAbstractState deserializedState =
        deserializeOperator.deserializeFromFormula(originalFormula);
    org.sosy_lab.java_smt.api.BooleanFormula deserializedFormula = null;
    if (deserializedState.isAbstractionState()) {
      deserializedFormula = deserializedState.getAbstractionFormula().asFormula();
    } else {
      deserializedFormula = deserializedState.getPathFormula().getFormula();
    }

    boolean implication = solver.implies(originalFormula, deserializedFormula);
    if (implication) {
      System.out.println("Original Formula:\n" + originalFormula);
      System.out.println("Deserialized Formula:\n" + deserializedFormula);
    }
    assertTrue("Deserialized predicate state must imply original formula", implication);
  }
}
