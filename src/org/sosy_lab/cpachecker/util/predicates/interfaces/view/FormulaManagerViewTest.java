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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import static com.google.common.truth.Truth.*;

import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

@RunWith(Parameterized.class)
public class FormulaManagerViewTest extends SolverBasedTest0 {

  @Parameters(name="{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  private FormulaManagerView mgrv;
  private BooleanFormulaManagerView bmgrv;

  @Before
  public void setUp() throws InvalidConfigurationException {
    Configuration viewConfig = Configuration.builder()
        .copyFrom(config)
        // use only theory supported by all solvers:
        .setOption("cpa.predicate.encodeBitvectorAs", "INTEGER")
        .setOption("cpa.predicate.encodeFloatAs", "INTEGER")
        .build();
    mgrv = new FormulaManagerView(factory, viewConfig, TestLogManager.getInstance());
    bmgrv = mgrv.getBooleanFormulaManager();
  }

  private BooleanFormula stripNot(BooleanFormula f) {
    return bmgrv.isNot(f) ? (BooleanFormula)mgr.getUnsafeFormulaManager().getArg(f, 0) : f;
  }

  @Test
  public void testExtractDisjuncts() {
    BooleanFormula atom1 = imgr.equal(imgr.makeVariable("a"), imgr.makeNumber(1));
    BooleanFormula atom2 = imgr.greaterThan(imgr.makeVariable("b"), imgr.makeNumber(2));
    BooleanFormula atom3 = imgr.greaterOrEquals(imgr.makeVariable("c"), imgr.makeNumber(3));
    BooleanFormula atom4 = imgr.lessThan(imgr.makeVariable("d"), imgr.makeNumber(4));
    BooleanFormula atom5 = imgr.lessOrEquals(imgr.makeVariable("e"), imgr.makeNumber(5));

    BooleanFormula f = bmgrv.and(ImmutableList.of(
        bmgrv.or(atom1, atom2), bmgrv.not(bmgrv.or(atom1, atom3)), atom4, atom5));

    assertThat(mgrv.extractDisjuncts(f))
        .containsExactly(bmgrv.or(atom1, atom2), bmgrv.or(atom1, atom3), stripNot(atom4), stripNot(atom5));
  }

  @Test
  public void testExtractLiterals() {
    BooleanFormula atom1 = imgr.equal(imgr.makeVariable("a"), imgr.makeNumber(1));
    BooleanFormula atom2 = imgr.greaterThan(imgr.makeVariable("b"), imgr.makeNumber(2));
    BooleanFormula atom3 = imgr.greaterOrEquals(imgr.makeVariable("c"), imgr.makeNumber(3));
    BooleanFormula atom4 = imgr.lessThan(imgr.makeVariable("d"), imgr.makeNumber(4));
    BooleanFormula atom5 = imgr.lessOrEquals(imgr.makeVariable("e"), imgr.makeNumber(5));

    BooleanFormula f = bmgrv.and(ImmutableList.of(
        bmgrv.or(atom1, atom2), bmgrv.not(bmgrv.or(atom1, atom3)), atom4, atom5));

    assertThat(mgrv.extractLiterals(f))
        .containsExactly(atom1, atom2, bmgrv.not(bmgrv.or(atom1, atom3)), atom4, atom5);

    // TODO: this should really be the following (c.f. FormulaManagerView.extractLiterals)
//    assertThat(mgrv.extractLiterals(f, false))
//        .containsExactly(atom1, atom2, atom3, atom4, atom5);
  }

  @Test
  public void testExtractAtoms() {
    BooleanFormula atom1 = imgr.equal(imgr.makeVariable("a"), imgr.makeNumber(1));
    BooleanFormula atom2 = imgr.greaterThan(imgr.makeVariable("b"), imgr.makeNumber(2));
    BooleanFormula atom3 = imgr.greaterOrEquals(imgr.makeVariable("c"), imgr.makeNumber(3));
    BooleanFormula atom4 = imgr.lessThan(imgr.makeVariable("d"), imgr.makeNumber(4));
    BooleanFormula atom5 = imgr.lessOrEquals(imgr.makeVariable("e"), imgr.makeNumber(5));

    BooleanFormula f = bmgrv.or(ImmutableList.of(
        bmgrv.and(atom1, atom2), bmgrv.and(atom1, atom3), atom4, atom5));

    assertThat(mgrv.extractAtoms(f, false))
        .containsExactly(stripNot(atom1), stripNot(atom2), stripNot(atom3), stripNot(atom4), stripNot(atom5));
  }

  private void testExtractAtoms_SplitEqualities(
      BooleanFormula atom1, BooleanFormula atom1ineq,
      BooleanFormula atom2, BooleanFormula atom3,
      BooleanFormula atom4, BooleanFormula atom5) throws SolverException, InterruptedException {

    BooleanFormula f = bmgrv.or(ImmutableList.of(
        bmgrv.and(atom1, atom2), bmgrv.and(atom1, atom3), atom4, atom5));

    Collection<BooleanFormula> atoms = mgrv.extractAtoms(f, true);
    Set<BooleanFormula> expected = ImmutableSet.of(stripNot(atom1), stripNot(atom2), stripNot(atom3), stripNot(atom4), stripNot(atom5));

    // Assert that atoms contains all of atom1-5
    // and another atom that is equivalent to atom1ineq
    assertThat(atoms).hasSize(6);
    assertThat(atoms).containsAllIn(expected);

    atoms.removeAll(expected);
    BooleanFormula remainingAtom = Iterables.getOnlyElement(atoms);
    assert_().about(BooleanFormula()).that(remainingAtom).isEquivalentTo(stripNot(atom1ineq));
  }

  private <T extends NumeralFormula> void testExtractAtoms_SplitEqualities_numeral(
      NumeralFormulaManager<T, ? extends T> nmgr) throws SolverException, InterruptedException {

    BooleanFormula atom1 = nmgr.equal(nmgr.makeVariable("a"), nmgr.makeNumber(1));
    BooleanFormula atom1ineq = nmgr.lessOrEquals(nmgr.makeVariable("a"), nmgr.makeNumber(1));
    BooleanFormula atom2 = nmgr.greaterThan(nmgr.makeVariable("b"), nmgr.makeNumber(2));
    BooleanFormula atom3 = nmgr.greaterOrEquals(nmgr.makeVariable("c"), nmgr.makeNumber(3));
    BooleanFormula atom4 = nmgr.lessThan(nmgr.makeVariable("d"), nmgr.makeNumber(4));
    BooleanFormula atom5 = nmgr.lessOrEquals(nmgr.makeVariable("e"), nmgr.makeNumber(5));

    testExtractAtoms_SplitEqualities(atom1, atom1ineq, atom2, atom3, atom4, atom5);
  }

  @Test
  public void testExtractAtoms_SplitEqualities_int() throws SolverException, InterruptedException {
    testExtractAtoms_SplitEqualities_numeral(imgr);
  }

  @Test
  public void testExtractAtoms_SplitEqualities_rat() throws SolverException, InterruptedException {
    requireRationals();
    testExtractAtoms_SplitEqualities_numeral(rmgr);
  }

  private void testExtractAtoms_SplitEqualities_bitvectors(BitvectorFormulaManager bvmgr) throws SolverException, InterruptedException {
    BooleanFormula atom1 = bvmgr.equal(bvmgr.makeVariable(32, "a"), bvmgr.makeBitvector(32, 1));
    BooleanFormula atom1ineq = bvmgr.lessOrEquals(bvmgr.makeVariable(32, "a"), bvmgr.makeBitvector(32, 1), false);
    BooleanFormula atom2 = bvmgr.greaterThan(bvmgr.makeVariable(32, "b"), bvmgr.makeBitvector(32, 2), false);
    BooleanFormula atom3 = bvmgr.greaterOrEquals(bvmgr.makeVariable(32, "c"), bvmgr.makeBitvector(32, 3), false);
    BooleanFormula atom4 = bvmgr.lessThan(bvmgr.makeVariable(32, "d"), bvmgr.makeBitvector(32, 4), false);
    BooleanFormula atom5 = bvmgr.lessOrEquals(bvmgr.makeVariable(32, "e"), bvmgr.makeBitvector(32, 5), false);

    testExtractAtoms_SplitEqualities(atom1, atom1ineq, atom2, atom3, atom4, atom5);
  }

  @Test
  public void testExtractAtoms_SplitEqualities_bv() throws SolverException, InterruptedException {
    requireBitvectors();
    testExtractAtoms_SplitEqualities_bitvectors(bvmgr);
  }

  @Test
  public void testExtractAtoms_SplitEqualities_bvReplaceByInt() throws SolverException, InterruptedException {
    // BitvectorFormulaManagerView here!
    testExtractAtoms_SplitEqualities_bitvectors(mgrv.getBitvectorFormulaManager());
  }
}
