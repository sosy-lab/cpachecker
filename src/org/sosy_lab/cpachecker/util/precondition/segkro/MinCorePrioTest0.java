/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.RuleEngine;
import org.sosy_lab.solver.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaType.NumeralType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.SolverBasedTest0;

import com.google.common.collect.Lists;


public class MinCorePrioTest0 extends SolverBasedTest0 {

  private MinCorePrio mcp;
  private ExtractNewPreds enp;

  private FormulaManagerView mgrv;
  private ArrayFormulaManagerView afm;
  private BooleanFormulaManagerView bfm;
  private NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm;
  private Solver solver;

  private IntegerFormula _0;
  private IntegerFormula _1;
  private IntegerFormula _i;
  private IntegerFormula _al;
  private IntegerFormula _bl;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    mgrv = new FormulaManagerView(factory, config, TestLogManager.getInstance());
    solver = new Solver(mgrv, factory, config, TestLogManager.getInstance());

    afm = mgrv.getArrayFormulaManager();
    bfm = mgrv.getBooleanFormulaManager();
    ifm = mgrv.getIntegerFormulaManager();

    RuleEngine rulesEngine = new RuleEngine(logger, solver);
    mcp = new MinCorePrio(logger, Mockito.mock(CFA.class), solver);
    enp = new ExtractNewPreds(solver, rulesEngine);

    _0 = ifm.makeNumber(0);
    _1 = ifm.makeNumber(1);
    _i = mgrv.makeVariable(NumeralType.IntegerType, "i");
    _al = mgrv.makeVariable(NumeralType.IntegerType, "al");
    _bl = mgrv.makeVariable(NumeralType.IntegerType, "bl");
    _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
  }

  @Test
  public void testWpPair1() throws SolverException, InterruptedException {
    BooleanFormula wpUnsafe = bfm.and(Lists.newArrayList(
        ifm.greaterOrEquals(ifm.add(_i, _1), _al),                // i+1 >= al
        bfm.not(ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0)),  // b[i+1] != 0
        ifm.lessThan(_i, _al),                                    // i < al
        bfm.not(ifm.equal(afm.select(_b, _i), _0))                // b[i] != 0
        ));

    BooleanFormula wpSafe = bfm.and(Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _i), _0)),     // b[i] != 0
        ifm.lessOrEquals(ifm.add(_i, _1), _al),         // i+1 <= al
        ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0)  // b[i+1] = 0
        ));

    assertThat(solver.isUnsat(bfm.and(wpUnsafe, wpSafe))).isTrue();

    List<BooleanFormula> interpolantCandidates = enp.extractNewPreds(wpUnsafe); // TODO: Construct static formulas instead.

    BooleanFormula interpolant = mcp.getInterpolant(wpSafe, wpUnsafe, interpolantCandidates, null);

    assertThat(solver.isUnsat(bfm.and(interpolant, wpSafe))).isTrue();
  }

  @Test
  public void testWpPair2() throws SolverException, InterruptedException {
    BooleanFormula wpUnsafe = bfm.and(Lists.newArrayList(
        ifm.greaterOrEquals(_1, _al),               // 1 >= al
        bfm.not(ifm.equal(afm.select(_b, _1), _0)), // b[1] != 0
        ifm.lessThan(_0, _al),                      // 0 < al
        bfm.not(ifm.equal(afm.select(_b, _0), _0))  // b[0] != 0
        ));

    BooleanFormula wpSafe = bfm.and(Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _0), _0)), // b[0] != 0
        ifm.lessOrEquals(_1, _al),                  // 1 <= al
        ifm.equal(afm.select(_b, _1), _0)           // b[1] = 0
        ));

    assertThat(solver.isUnsat(bfm.and(wpUnsafe, wpSafe))).isTrue();

    List<BooleanFormula> interpolantCandidates = enp.extractNewPreds(wpUnsafe); // TODO: Construct static formulas instead.

    BooleanFormula interpolant = mcp.getInterpolant(wpSafe, wpUnsafe, interpolantCandidates, null);

    assertThat(solver.isUnsat(bfm.and(interpolant, wpSafe))).isTrue();
  }

  @Test
  public void testLinCombinRemoval() throws SolverException, InterruptedException {
    BooleanFormula linCombi = ifm.lessThan(_al, _bl);

    List<BooleanFormula> candidates = Lists.newArrayList(
        bfm.not(ifm.lessOrEquals(_bl, _0)),
        ifm.greaterOrEquals(_0, _al),
        linCombi
        );

    BooleanFormula phiMinus = bfm.and(Lists.newArrayList(
        ifm.greaterOrEquals(_0, _al),
        bfm.not(ifm.lessOrEquals(_bl, _0)),
        bfm.not(ifm.lessOrEquals(_bl, _0)),
        ifm.greaterOrEquals(_0, _al),
        ifm.lessThan(_al, _bl)
        ));

    BooleanFormula phiPlus = bfm.and(Lists.newArrayList(
        bfm.not(ifm.greaterOrEquals(_0, _al)),
        bfm.not(ifm.lessOrEquals(_bl, _0))
        ));

    Collection<BooleanFormula> resultingInterpolantPreds = mcp.getInterpolantAsPredicateCollection(phiMinus, phiPlus, candidates, null);
    BooleanFormula resultingInterpolant = bfm.and(Lists.newArrayList(resultingInterpolantPreds));

    BooleanFormula betterInterpolant = bfm.and(linCombi, ifm.greaterOrEquals(_0, _al));

    assertThat(solver.isUnsat(bfm.and(resultingInterpolant, phiPlus))).isTrue();
    assertThat(solver.isUnsat(bfm.and(betterInterpolant, phiPlus))).isTrue();
    assertThat(solver.isUnsat(bfm.and(linCombi, phiPlus))).isTrue();
    assertThat(resultingInterpolantPreds).contains(linCombi);
  }

  @Test
  public void testWpExample2() throws SolverException, InterruptedException {
    final BooleanFormula phiMinus = bfm.and(Lists.newArrayList(
          bfm.not(ifm.equal(ifm.makeVariable("p"), ifm.makeNumber(1))),
          ifm.lessOrEquals(ifm.makeVariable("a"), ifm.makeNumber(1000)))
        );
    final BooleanFormula phiPlus = bfm.and(Lists.newArrayList(
        ifm.equal(ifm.makeVariable("p"), ifm.makeNumber(1)),
        ifm.greaterThan(ifm.makeVariable("a"), ifm.makeNumber(50)))
      );

    List<BooleanFormula> candidates = Lists.newArrayList(
        bfm.not(ifm.equal(ifm.makeVariable("p"), ifm.makeNumber(1))),
        ifm.lessOrEquals(ifm.makeVariable("a"), ifm.makeNumber(1000)),
        ifm.equal(ifm.makeVariable("p"), ifm.makeNumber(1)),
        ifm.greaterThan(ifm.makeVariable("a"), ifm.makeNumber(50))
        );

    assertThat(solver.isUnsat(bfm.and(phiMinus, phiPlus))).isTrue();

    Collection<BooleanFormula> result = mcp.getInterpolantAsPredicateCollection(phiMinus, phiPlus, candidates, null);

    assertThat(result).contains(bfm.not(ifm.equal(ifm.makeVariable("p"), ifm.makeNumber(1))));
  }

}
