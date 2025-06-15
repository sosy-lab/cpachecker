// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysisFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysisFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.java_smt.api.BooleanFormula;

@RunWith(Parameterized.class)
public class DeserializeValueAnalysisStateOperatorTest {

  private static final String SPEC_PATH = "config/specification/default.spc";
  private static final String TEST_PROGRAMS_DIR = "test/programs/block_analysis";
  private static final String CONFIG_PATH =
      "config/distributed-summary-synthesis/predicateAnalysis-value-block-forward.properties";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    File dir = new File(TEST_PROGRAMS_DIR);
    File[] files = dir.listFiles();
    Collection<Object[]> params = new ArrayList<>();
    if (files != null) {
      for (File f : files) {
        params.add(new Object[] {f.getPath()});
      }
    }
    return params;
  }

  private final String testProgramPath;

  public DeserializeValueAnalysisStateOperatorTest(String testProgramPath) {
    this.testProgramPath = testProgramPath;
  }

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
    config =
        TestDataTools.configurationForTest()
            .loadFromFile(CONFIG_PATH)
            .loadFromFile("config/value-dss-test.properties")
            .build();
    logger = LogManager.createTestLogManager();
    shutdownManager = ShutdownManager.create();
    shutdownNotifier = shutdownManager.getNotifier();
    CFACreator creator = new CFACreator(config, logger, shutdownNotifier);
    cfa = creator.parseFileAndCreateCFA(ImmutableList.of(testProgramPath));
    variableTypes = CFAUtils.extractVariableTypes(cfa);
    blockGraph = createBlockGraph();
    solver = Solver.create(config, logger, shutdownNotifier);
    specification =
        Specification.fromFiles(
            ImmutableList.of(Path.of(SPEC_PATH)), cfa, config, logger, shutdownNotifier);
  }

  @Test
  public void testDeserializeFromFormula_equivalentFormulas() throws Exception {
    for (BlockNode blockNode : blockGraph.getNodes()) {
      AnalysisComponents components =
          DssBlockAnalysisFactory.createAlgorithm(
              logger, specification, cfa, config, shutdownManager, blockNode);

      Algorithm algorithm = components.algorithm();
      algorithm.run(components.reached());

      AbstractState lastState = components.reached().getLastState();
      ValueAnalysisState originalState =
          AbstractStates.extractStateByType(lastState, ValueAnalysisState.class);

      DeserializeValueAnalysisStateOperator deserializeOperator =
          new DeserializeValueAnalysisStateOperator(cfa, variableTypes, solver);

      SerializeValueAnalysisStateOperator serializeOperator =
          new SerializeValueAnalysisStateOperator(solver);

      BooleanFormula originalFormula = serializeOperator.serializeToFormula(originalState);

      ValueAnalysisState deserializedState =
          deserializeOperator.deserializeFromFormula(originalFormula);
      BooleanFormula deserializedFormula = serializeOperator.serializeToFormula(deserializedState);

      boolean origImpliesDeserialized = solver.implies(originalFormula, deserializedFormula);
      boolean deserializedImpliesOrig = solver.implies(deserializedFormula, originalFormula);

      assertWithMessage("Deserialized state must imply original formula")
          .that(deserializedImpliesOrig)
          .isTrue();
      assertWithMessage("Original formula must imply deserialized state")
          .that(origImpliesDeserialized)
          .isTrue();
    }
  }

  private BlockGraph createBlockGraph()
      throws InvalidConfigurationException, CPAException, InterruptedException {
    BlockOperator blockOperator = new BlockOperator();
    blockOperator.setCFA(cfa);
    config.inject(blockOperator);

    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);
    LinearBlockNodeDecomposition decomposition = new LinearBlockNodeDecomposition(isBlockEnd);
    return decomposition.decompose(cfa);
  }
}
