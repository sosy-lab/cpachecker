// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertWithMessage;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysisFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysisFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.test.DeserializationTestUtils;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DeserializePredicateStateOperatorTest {

  private static final String SPEC_PATH = "config/specification/default.spc";
  private static final String TEST_PROGRAM_PATH =
      "test/programs/block_analysis/simple_calculations_unsafe.c";
  private static final String CONFIG_PATH =
      "config/distributed-summary-synthesis/dss-block-analysis.properties";

  private CFA cfa;
  private BlockGraph blockGraph;
  private Map<MemoryLocation, CType> variableTypes;

  private Configuration config;
  private LogManager logger;
  private ShutdownManager shutdownManager;
  private ShutdownNotifier shutdownNotifier;
  private Solver solver;
  private Specification specification;
  private FormulaManagerView fmv;

  @Before
  public void setUp() throws Exception {
    config = DeserializationTestUtils.createTestConfiguration(CONFIG_PATH);
    logger = LogManager.createTestLogManager();
    shutdownManager = DeserializationTestUtils.createShutdownManager();
    shutdownNotifier = shutdownManager.getNotifier();

    cfa = DeserializationTestUtils.parseCFA(config, logger, shutdownNotifier, TEST_PROGRAM_PATH);
    variableTypes = DeserializationTestUtils.extractVariableTypes(cfa);
    blockGraph = DeserializationTestUtils.createBlockGraph(cfa, config);

    specification =
        DeserializationTestUtils.loadSpecification(
            SPEC_PATH, cfa, config, logger, shutdownNotifier);
  }

  @Test
  public void testDeserializeFromFormula_equivalenceHolds() throws Exception {
    for (BlockNode blockNode : blockGraph.getNodes()) {
      AnalysisComponents components =
          DssBlockAnalysisFactory.createAlgorithm(
              logger, specification, cfa, config, shutdownManager, blockNode);

      ConfigurableProgramAnalysis cpa = components.cpa();
      Algorithm algorithm = components.algorithm();
      algorithm.run(components.reached());

      PredicateCPA predicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
      solver = predicateCPA.getSolver();
      fmv = solver.getFormulaManager();

      DeserializePredicateStateOperator deserializer =
          new DeserializePredicateStateOperator(
              predicateCPA, cfa, blockNode, variableTypes, solver);
      SerializePredicateStateOperator serializer =
          new SerializePredicateStateOperator(predicateCPA, cfa, true, solver);
      BooleanFormula hardcodedFormula1 =
          fmv.parse(
              "(set-info :source |printed by MathSAT|)(declare-fun |main::y@1| () (_ BitVec"
                  + " 32))(assert (let ((.def_10 (= |main::y@1| (_ bv10 32))))(let ((.def_11 (not"
                  + " .def_10))).def_11)))");
      BooleanFormula hardcodedFormula2 =
          fmv.parse(
              "(set-info :source |printed by MathSAT|)(declare-fun __VERIFIER_nondet_int!2@ () (_"
                  + " BitVec 32))(declare-fun |main::x@3| () (_ BitVec 32))(declare-fun |main::x@4|"
                  + " () (_ BitVec 32))(declare-fun |main::x@5| () (_ BitVec 32))(declare-fun"
                  + " |main::y@3| () (_ BitVec 32))(declare-fun |main::y@4| () (_ BitVec"
                  + " 32))(assert (let ((.def_52 (bvneg |main::x@5|)))(let ((.def_53 (bvadd"
                  + " |main::y@3| .def_52)))(let ((.def_54 (= |main::y@4| .def_53)))(let ((.def_41"
                  + " (bvadd |main::x@5| |main::y@3|)))(let ((.def_42 (= .def_41 (_ bv10 32))))(let"
                  + " ((.def_39 (= |main::y@3| (_ bv10 32))))(let ((.def_34 (= |main::x@4|"
                  + " |main::x@5|)))(let ((.def_26 (bvslt (_ bv100 32) |main::x@4|)))(let ((.def_29"
                  + " (not .def_26)))(let ((.def_23 (= |main::x@3| |main::x@4|)))(let ((.def_13"
                  + " (bvslt (_ bv0 32) |main::x@3|)))(let ((.def_10 (= __VERIFIER_nondet_int!2@"
                  + " |main::x@3|)))(let ((.def_18 (and .def_10 .def_13)))(let ((.def_24 (and"
                  + " .def_18 .def_23)))(let ((.def_21 (= |main::x@4| (_ bv100 32))))(let ((.def_14"
                  + " (not .def_13)))(let ((.def_17 (and .def_10 .def_14)))(let ((.def_22 (and"
                  + " .def_17 .def_21)))(let ((.def_25 (or .def_22 .def_24)))(let ((.def_30 (and"
                  + " .def_25 .def_29)))(let ((.def_35 (and .def_30 .def_34)))(let ((.def_32 (="
                  + " |main::x@5| (_ bv100 32))))(let ((.def_28 (and .def_25 .def_26)))(let"
                  + " ((.def_33 (and .def_28 .def_32)))(let ((.def_36 (or .def_33 .def_35)))(let"
                  + " ((.def_40 (and .def_36 .def_39)))(let ((.def_44 (and .def_40 .def_42)))(let"
                  + " ((.def_55 (and .def_44 .def_54)))(let ((.def_49 (= .def_41 |main::y@4|)))(let"
                  + " ((.def_45 (not .def_42)))(let ((.def_46 (and .def_40 .def_45)))(let ((.def_50"
                  + " (and .def_46 .def_49)))(let ((.def_56 (or .def_50"
                  + " .def_55))).def_56))))))))))))))))))))))))))))))))))");

      PredicateAbstractState hardcodedState1 =
          deserializer.deserializeFromFormula(hardcodedFormula1);
      PredicateAbstractState hardcodedState2 =
          deserializer.deserializeFromFormula(hardcodedFormula2);

      BooleanFormula roundTripFormula1 = serializer.serializeToFormula(hardcodedState1);
      BooleanFormula roundTripFormula2 = serializer.serializeToFormula(hardcodedState2);

      boolean implies1 = solver.implies(hardcodedFormula1, roundTripFormula1);
      boolean implies2 = solver.implies(hardcodedFormula2, roundTripFormula2);

      assertWithMessage("Formula 1 must imply original formula").that(implies1).isTrue();
      assertWithMessage("Formula 2 must imply original formula").that(implies2).isTrue();
    }
  }
}
