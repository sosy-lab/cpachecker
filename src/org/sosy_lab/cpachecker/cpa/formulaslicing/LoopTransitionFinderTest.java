// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class LoopTransitionFinderTest {
  private CFACreator creator;
  private LogManager logger;
  private PathFormulaManager pfmgr;
  private FormulaManagerView fmgr;
  private Configuration config;
  private ShutdownNotifier notifier;
  private Solver solver;
  private BooleanFormulaManager bfmgr;

  @Before
  public void setUp() throws Exception {
    config =
        TestDataTools.configurationForTest()
            .setOptions(ImmutableMap.of("solver.solver", "z3"))
            .build();
    notifier = ShutdownNotifier.createDummy();
    logger = LogManager.createTestLogManager();
    try {
      solver = Solver.create(config, logger, notifier);
    } catch (InvalidConfigurationException e) {
      Throwable cause = Throwables.getRootCause(e);
      if (cause instanceof UnsatisfiedLinkError) {
        assume()
            .withMessage("Z3 requires newer libc than Ubuntu 18.04 provides")
            .that(cause)
            .hasMessageThat()
            .doesNotContain("version `GLIBCXX_3.4.26' not found");
      }
      throw e;
    }
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr =
        new PathFormulaManagerImpl(
            fmgr,
            config,
            logger,
            notifier,
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.FORWARD);
    creator = new CFACreator(config, logger, notifier);
  }

  @Test
  public void testGetEdgesInLoop() throws Exception {
    CFA cfa =
        TestDataTools.toSingleFunctionCFA(
            creator, "int x = 0; int y = 0;", "while (1) {", "x += 1; y += 1;", "}");
    CFANode loopHead = cfa.getAllLoopHeads().orElseThrow().iterator().next();
    LoopTransitionFinder loopTransitionFinder =
        new LoopTransitionFinder(
            config, cfa.getLoopStructure().orElseThrow(), pfmgr, logger, notifier);

    PathFormula loopTransition =
        loopTransitionFinder.generateLoopTransition(
            SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), loopHead);

    PathFormula expected = fromLine("int x, y; x += 1; y += 1;");

    assertEquivalent(loopTransition.getFormula(), expected.getFormula());
  }

  @Test
  public void testWithConditional() throws Exception {
    CFA cfa =
        TestDataTools.toSingleFunctionCFA(
            creator,
            "int x = 0; int y = 0; int p = 1;",
            "while (1) {",
            "if (p) { x += 1; } else { y += 1; }",
            "}");
    CFANode loopHead = cfa.getAllLoopHeads().orElseThrow().iterator().next();
    LoopTransitionFinder loopTransitionFinder =
        new LoopTransitionFinder(
            config, cfa.getLoopStructure().orElseThrow(), pfmgr, logger, notifier);
    PathFormula summary =
        loopTransitionFinder.generateLoopTransition(
            SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), loopHead);

    PathFormula expected = fromLine("int x, y, p; if (p) { x += 1; } else { y += 1; }; ");

    assertEquivalent(summary.getFormula(), expected.getFormula());
  }

  @Test
  public void testInterproceduralSummary() throws Exception {
    CFA cfa =
        TestDataTools.toMultiFunctionCFA(
            creator,
            "void log() {}",
            "int main() {",
            "int x;",
            "while (__VERIFIER_nondet_int()) {",
            "log();",
            "x += 1;",
            "log();",
            "}",
            "while (__VERIFIER_nondet_int()) {",
            "log();",
            "x += 2;",
            "log();",
            "}",
            "}");

    CFANode loopHead = cfa.getAllLoopHeads().orElseThrow().iterator().next();
    LoopTransitionFinder loopTransitionFinder =
        new LoopTransitionFinder(
            config, cfa.getLoopStructure().orElseThrow(), pfmgr, logger, notifier);
    PathFormula summary =
        loopTransitionFinder.generateLoopTransition(
            SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), loopHead);

    PathFormula expected = fromLine("int x; x += 1;");

    assertEquivalent(summary.getFormula(), expected.getFormula());
  }

  private PathFormula fromLine(String line) throws Exception {
    return TestDataTools.toPathFormula(
        TestDataTools.toSingleFunctionCFA(creator, line), SSAMap.emptySSAMap(), pfmgr, true);
  }

  private void assertEquivalent(BooleanFormula output, BooleanFormula expected)
      throws SolverException, InterruptedException {
    assertThat(solver.isUnsat(bfmgr.and(output, bfmgr.not(expected)))).isTrue();
    assertThat(solver.isUnsat(bfmgr.and(bfmgr.not(output), expected))).isTrue();
  }
}
