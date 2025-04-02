// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.EdgeAnalyzer;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class DeserializationTestUtils {

  public static Configuration createTestConfiguration(String propertiesFilePath)
      throws IOException, InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromFile(propertiesFilePath)
        .setOption("cpa.predicate.blk.alwaysAtJoin", "true")
        .setOption("cpa.predicate.blk.alwaysAtBranch", "true")
        .setOption("cpa.predicate.blk.alwaysAtProgramExit", "true")
        .setOption("cpa.predicate.blk.alwaysAtLoops", "false")
        .setOption("cpa.predicate.blk.alwaysAtFunctions", "false")
        .setOption("cpa.predicate.blk.alwaysAfterThreshold", "false")
        .setOption("cpa.predicate.blk.alwaysAtFunctionHeads", "true")
        .setOption("cpa.predicate.blk.alwaysAtFunctionCallNodes", "false")
        .setOption("cpa.predicate.blk.alwaysAtFunctionExit", "true")
        .build();
  }

  public static CFA parseCFA(
      Configuration config, LogManager logger, ShutdownNotifier notifier, String filePath)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {
    CFACreator creator = new CFACreator(config, logger, notifier);
    return creator.parseFileAndCreateCFA(ImmutableList.of(filePath));
  }

  public static Map<MemoryLocation, CType> extractVariableTypes(CFA cfa) {
    Map<MemoryLocation, CType> types = new HashMap<>();
    EdgeAnalyzer edgeAnalyzer =
        new EdgeAnalyzer(
            CompoundBitVectorIntervalManagerFactory.forbidSignedWrapAround(),
            cfa.getMachineModel());

    for (CFAEdge edge : CFAUtils.allEdges(cfa)) {
      types.putAll(edgeAnalyzer.getInvolvedVariableTypes(edge));
    }
    return types;
  }

  public static BlockGraph createBlockGraph(CFA cfa, Configuration config)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    BlockOperator blockOperator = new BlockOperator();
    blockOperator.setCFA(cfa);
    config.inject(blockOperator);

    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);
    LinearBlockNodeDecomposition decomposition = new LinearBlockNodeDecomposition(isBlockEnd);
    return decomposition.decompose(cfa);
  }

  public static Specification loadSpecification(
      String specPath, CFA cfa, Configuration config, LogManager logger, ShutdownNotifier notifier)
      throws InvalidConfigurationException, InterruptedException {
    return Specification.fromFiles(
        ImmutableList.of(Path.of(specPath)), cfa, config, logger, notifier);
  }

  public static Solver createSolver(
      Configuration config, LogManager logger, ShutdownNotifier notifier)
      throws InvalidConfigurationException {
    return Solver.create(config, logger, notifier);
  }

  public static FormulaManagerView createFormulaManager(Solver solver) {
    return solver.getFormulaManager();
  }

  public static ShutdownManager createShutdownManager() {
    return ShutdownManager.create();
  }
}
