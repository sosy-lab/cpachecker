// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.weakening;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

@RunWith(Parameterized.class)
public class InductiveWeakeningManagerTest extends SolverViewBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solverToUse;

  @Override
  protected Solvers solverToUse() {
    return solverToUse;
  }

  private InductiveWeakeningManager inductiveWeakeningManager;
  private IntegerFormulaManagerView ifmgr;
  private BooleanFormulaManagerView bfmgr;

  @Before
  public void setUp() throws Exception {
    ShutdownNotifier notifier = ShutdownNotifier.createDummy();
    inductiveWeakeningManager =
        new InductiveWeakeningManager(new WeakeningOptions(config), solver, logger, notifier);
    ifmgr = mgrv.getIntegerFormulaManager();
    bfmgr = mgrv.getBooleanFormulaManager();
  }

  @Test
  public void testSlicingVerySimple() throws Exception {
    SSAMap startingSsa = SSAMap.emptySSAMap().withDefault(0);
    @SuppressWarnings("deprecation") // just for test
    PathFormula transition =
        PathFormula.createManually(
            ifmgr.equal(
                ifmgr.makeVariable("x", 1),
                ifmgr.add(ifmgr.makeVariable("x", 0), ifmgr.makeNumber(1))),
            startingSsa.builder().setIndex("x", CNumericTypes.INT, 1).build(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    Set<BooleanFormula> lemmas =
        ImmutableSet.of(
            ifmgr.equal(ifmgr.makeVariable("x"), ifmgr.makeNumber(1)),
            ifmgr.equal(ifmgr.makeVariable("y"), ifmgr.makeNumber(0)));
    Set<BooleanFormula> weakening =
        inductiveWeakeningManager.findInductiveWeakeningForRCNF(startingSsa, transition, lemmas);
    assertThat(weakening)
        .containsExactly(ifmgr.equal(ifmgr.makeVariable("y"), ifmgr.makeNumber(0)));
  }

  @Test
  public void testRemovingRedundancies() throws Exception {
    IntegerFormula x, y;
    x = ifmgr.makeVariable("x");
    y = ifmgr.makeVariable("y");
    IntegerFormula zero = ifmgr.makeNumber(0);

    BooleanFormula input =
        bfmgr.and(
            ifmgr.greaterThan(x, zero),
            ifmgr.greaterThan(y, zero),
            ifmgr.greaterThan(ifmgr.add(x, y), zero));

    BooleanFormula simplified = inductiveWeakeningManager.removeRedundancies(input);
    assertThat(simplified)
        .isEqualTo(bfmgr.and(ifmgr.greaterThan(x, zero), ifmgr.greaterThan(y, zero)));
  }
}
