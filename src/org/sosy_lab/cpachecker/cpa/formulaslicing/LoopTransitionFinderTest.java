/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
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
    config = TestDataTools.configurationForTest().setOptions(
        ImmutableMap.of(
            "solver.solver", "z3",
            "cpa.predicate.handlePointerAliasing", "false"
        )
    ).build();
    notifier = ShutdownNotifier.createDummy();
    logger = LogManager.createTestLogManager();
    solver = Solver.create(config, logger, notifier);
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier, MachineModel.LINUX32, Optional.empty(),
        AnalysisDirection.FORWARD);
    creator = new CFACreator(config, logger, notifier);

  }

  @Test
  public void testGetEdgesInLoop() throws Exception {
    CFA cfa = TestDataTools.toSingleFunctionCFA(creator,
        "int x = 0; int y = 0;",
        "while (1) {",
          "x += 1; y += 1;",
        "}"
    );
    CFANode loopHead = cfa.getAllLoopHeads().get().iterator().next();
    LoopTransitionFinder loopTransitionFinder =
        new LoopTransitionFinder(
            config,
            cfa.getLoopStructure().get(),
            pfmgr, fmgr, logger, notifier);

    PathFormula loopTransition = loopTransitionFinder.generateLoopTransition(
        SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), loopHead);

    PathFormula expected = fromLine("int x, y; x += 1; y += 1;");

    assertEquivalent(loopTransition.getFormula(), expected.getFormula());
  }

  @Test public void testWithConditional() throws Exception {
    CFA cfa = TestDataTools.toSingleFunctionCFA(creator,
        "int x = 0; int y = 0; int p = 1;",
        "while (1) {",
          "if (p) { x += 1; } else { y += 1; }",
        "}"
    );
    CFANode loopHead = cfa.getAllLoopHeads().get().iterator().next();
    LoopTransitionFinder loopTransitionFinder =
        new LoopTransitionFinder(config, cfa.getLoopStructure().get(), pfmgr, fmgr, logger, notifier);
    PathFormula summary = loopTransitionFinder.generateLoopTransition(
        SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), loopHead);

    PathFormula expected = fromLine("int x, y, p; if (p) { x += 1; } else { y += 1; }; ");

    assertEquivalent(summary.getFormula(), expected.getFormula());
  }

  @Test public void testInterproceduralSummary() throws Exception {
    CFA cfa = TestDataTools.toMultiFunctionCFA(creator,
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
        "}"
    );

    CFANode loopHead = cfa.getAllLoopHeads().get().iterator().next();
    LoopTransitionFinder loopTransitionFinder =
        new LoopTransitionFinder(config, cfa.getLoopStructure().get(), pfmgr, fmgr, logger, notifier);
    PathFormula summary = loopTransitionFinder.generateLoopTransition(
        SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), loopHead);

    PathFormula expected = fromLine("int x; x += 1;");

    assertEquivalent(summary.getFormula(), expected.getFormula());
  }

  private PathFormula fromLine(String line) throws Exception {
    return TestDataTools.toPathFormula(
        TestDataTools.toSingleFunctionCFA(creator, line),
        SSAMap.emptySSAMap(), fmgr, pfmgr, true);
  }

  private void assertEquivalent(BooleanFormula output, BooleanFormula expected)
      throws SolverException, InterruptedException {
    assertThat(solver.isUnsat(bfmgr.and(output, bfmgr.not(expected)))).isTrue();
    assertThat(solver.isUnsat(bfmgr.and(bfmgr.not(output), expected))).isTrue();
  }
}
