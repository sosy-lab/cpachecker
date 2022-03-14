// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

@RunWith(Parameterized.class)
@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class FormulaManagerViewTest extends SolverViewBasedTest0 {

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

  /** strip the most outer NOT, if there is one, else return the formula unchanged. */
  private BooleanFormula stripNot(final BooleanFormula f) {
    return mgrv.stripNegation(f).orElse(f);
  }

  @Test
  public void testExtractAtoms() {
    BooleanFormula atom1 = imgr.equal(imgr.makeVariable("a"), imgr.makeNumber(1));
    BooleanFormula atom2 = imgr.greaterThan(imgr.makeVariable("b"), imgr.makeNumber(2));
    BooleanFormula atom3 = imgr.greaterOrEquals(imgr.makeVariable("c"), imgr.makeNumber(3));
    BooleanFormula atom4 = imgr.lessThan(imgr.makeVariable("d"), imgr.makeNumber(4));
    BooleanFormula atom5 = imgr.lessOrEquals(imgr.makeVariable("e"), imgr.makeNumber(5));

    BooleanFormula f = bmgrv.or(bmgrv.and(atom1, atom2), bmgrv.and(atom1, atom3), atom4, atom5);

    assertThat(mgrv.extractAtoms(f, false))
        .containsExactly(
            stripNot(atom1), stripNot(atom2), stripNot(atom3), stripNot(atom4), stripNot(atom5));
  }

  private void testExtractAtoms_SplitEqualities(
      BooleanFormula atom1,
      BooleanFormula atom1ineq,
      BooleanFormula atom2,
      BooleanFormula atom3,
      BooleanFormula atom4,
      BooleanFormula atom5)
      throws SolverException, InterruptedException {

    BooleanFormula f = bmgrv.or(bmgrv.and(atom1, atom2), bmgrv.and(atom1, atom3), atom4, atom5);

    Set<BooleanFormula> atoms = mgrv.extractAtoms(f, true);
    Set<BooleanFormula> expected =
        ImmutableSet.of(
            stripNot(atom1), stripNot(atom2), stripNot(atom3), stripNot(atom4), stripNot(atom5));

    // Assert that atoms contains all of atom1-5
    // and another atom that is equivalent to atom1ineq
    assertThat(atoms).hasSize(6);
    assertThat(atoms).containsAtLeastElementsIn(expected);

    atoms = Sets.difference(atoms, expected);
    BooleanFormula remainingAtom = Iterables.getOnlyElement(atoms);
    assertThatFormula(remainingAtom).isEquivalentTo(stripNot(atom1ineq));
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

  @Test
  public void testExtractAtoms_SplitEqualities_bitvectors()
      throws SolverException, InterruptedException {
    bvmgr = mgrv.getBitvectorFormulaManager();
    BooleanFormula atom1 = bvmgr.equal(bvmgr.makeVariable(32, "a"), bvmgr.makeBitvector(32, 1));
    BooleanFormula atom1ineq =
        bvmgr.lessOrEquals(bvmgr.makeVariable(32, "a"), bvmgr.makeBitvector(32, 1), true);
    BooleanFormula atom2 =
        bvmgr.greaterThan(bvmgr.makeVariable(32, "b"), bvmgr.makeBitvector(32, 2), true);
    BooleanFormula atom3 =
        bvmgr.greaterOrEquals(bvmgr.makeVariable(32, "c"), bvmgr.makeBitvector(32, 3), true);
    BooleanFormula atom4 =
        bvmgr.lessThan(bvmgr.makeVariable(32, "d"), bvmgr.makeBitvector(32, 4), true);
    BooleanFormula atom5 =
        bvmgr.lessOrEquals(bvmgr.makeVariable(32, "e"), bvmgr.makeBitvector(32, 5), true);

    testExtractAtoms_SplitEqualities(atom1, atom1ineq, atom2, atom3, atom4, atom5);
  }

  private void assertIsConjunctive(BooleanFormula f) {
    assertWithMessage("formula <%s> detected as purely conjunctive: false", f)
        .that(mgrv.isPurelyConjunctive(f))
        .isTrue();
  }

  private void assertIsNotConjunctive(BooleanFormula f) {
    assertWithMessage("formula <%s> detected as purely conjunctive: true", f)
        .that(mgrv.isPurelyConjunctive(f))
        .isFalse();
  }

  @Test
  public void testIsPurelyConjunctive_Simple() {
    assertIsConjunctive(bmgrv.makeTrue());
    assertIsConjunctive(bmgrv.makeFalse());
    assertIsConjunctive(bmgrv.makeVariable("a"));
    assertIsConjunctive(bmgrv.not(bmgrv.makeVariable("a")));
    assertIsConjunctive(bmgrv.and(bmgrv.makeVariable("a"), bmgrv.makeVariable("b")));
    assertIsConjunctive(bmgrv.and(bmgrv.makeVariable("a"), bmgrv.not(bmgrv.makeVariable("b"))));
  }

  @Test
  public void testIsPurelyConjunctive_Atom() {
    BooleanFormula atom = imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(1));
    assertIsConjunctive(atom);
    assertIsConjunctive(bmgrv.not(atom));
    assertIsConjunctive(bmgrv.and(bmgrv.makeVariable("a"), atom));
    assertIsConjunctive(bmgrv.and(bmgrv.makeVariable("a"), bmgrv.not(atom)));
  }

  @Test
  public void testIsPurelyConjunctive_Negation() {
    assertIsNotConjunctive(bmgrv.not(bmgrv.and(bmgrv.makeVariable("a"), bmgrv.makeVariable("b"))));
  }

  @Test
  public void testIsPurelyConjunctive_Disjunction() {
    assertIsNotConjunctive(bmgrv.or(bmgrv.makeVariable("a"), bmgrv.makeVariable("b")));
  }

  @Test
  public void testIsPurelyConjunctive_Equivalence() {
    assertIsNotConjunctive(bmgrv.equivalence(bmgrv.makeVariable("a"), bmgrv.makeVariable("b")));
    assertIsNotConjunctive(
        bmgr.not(bmgrv.equivalence(bmgrv.makeVariable("a"), bmgrv.makeVariable("b"))));
  }

  @Test
  public void testIsPurelyConjunctive_Implication() {
    assertIsNotConjunctive(bmgrv.implication(bmgrv.makeVariable("a"), bmgrv.makeVariable("b")));
    assertIsNotConjunctive(
        bmgr.not(bmgrv.implication(bmgrv.makeVariable("a"), bmgrv.makeVariable("b"))));
  }

  @Test
  public void testIsPurelyConjunctive_Xor() {
    assertIsNotConjunctive(bmgrv.xor(bmgrv.makeVariable("a"), bmgrv.makeVariable("b")));
    assertIsNotConjunctive(bmgr.not(bmgrv.xor(bmgrv.makeVariable("a"), bmgrv.makeVariable("b"))));
  }

  @Test
  public void testIsPurelyConjunctive_BooleanIfThenElse() {
    assertIsNotConjunctive(
        bmgrv.ifThenElse(
            bmgrv.makeVariable("a"), bmgrv.makeVariable("b"), bmgrv.makeVariable("c")));
  }

  @Test
  public void testIsPurelyConjunctive_IfThenElse() {
    IntegerFormula ifThenElse =
        bmgrv.ifThenElse(bmgrv.makeVariable("a"), imgr.makeNumber(0), imgr.makeNumber(1));
    BooleanFormula atom = imgr.equal(imgr.makeVariable("x"), ifThenElse);
    assertIsNotConjunctive(atom);
    assertIsNotConjunctive(bmgrv.not(atom));
    assertIsNotConjunctive(bmgrv.and(bmgrv.makeVariable("a"), atom));
    assertIsNotConjunctive(bmgrv.and(bmgrv.makeVariable("a"), bmgrv.not(atom)));
  }

  @Test
  public void testUnInstantiateQuantifiersAndArrays() throws SolverException, InterruptedException {
    requireQuantifiers();
    requireArrays();

    IntegerFormula _0 = imgrv.makeNumber(0);
    IntegerFormula _i = imgrv.makeVariable("i");
    IntegerFormula _i1 = imgrv.makeVariable("i", 1);
    IntegerFormula _j = imgrv.makeVariable("j");
    IntegerFormula _j1 = imgrv.makeVariable("j", 1);
    IntegerFormula _x = imgrv.makeVariable("x");

    ArrayFormulaManagerView amgrv = mgrv.getArrayFormulaManager();
    ArrayFormula<IntegerFormula, IntegerFormula> _b =
        amgrv.makeArray("b", FormulaType.IntegerType, FormulaType.IntegerType);

    BooleanFormula _b_at_x_NOTEQ_0 = bmgrv.not(imgrv.equal(amgrv.select(_b, _x), _0));

    QuantifiedFormulaManagerView qmv = mgrv.getQuantifiedFormulaManager();
    BooleanFormula instantiated =
        qmv.forall(
            _x,
            bmgrv.and(
                _b_at_x_NOTEQ_0, imgrv.greaterOrEquals(_x, _j1), imgrv.lessOrEquals(_x, _i1)));

    BooleanFormula uninstantiated =
        qmv.forall(
            _x,
            bmgrv.and(_b_at_x_NOTEQ_0, imgrv.greaterOrEquals(_x, _j), imgrv.lessOrEquals(_x, _i)));

    SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
    ssaBuilder.setIndex("i", CNumericTypes.INT, 1);
    ssaBuilder.setIndex("j", CNumericTypes.INT, 1);

    testUnInstantiate(instantiated, uninstantiated, ssaBuilder);
  }

  @Test
  public void testUnInstantiate() throws SolverException, InterruptedException {
    IntegerFormula _0 = imgrv.makeNumber(0);
    IntegerFormula _1 = imgrv.makeNumber(1);
    IntegerFormula _i = imgrv.makeVariable("i");
    IntegerFormula _i1 = imgrv.makeVariable("i", 1);
    IntegerFormula _j = imgrv.makeVariable("j");
    IntegerFormula _j1 = imgrv.makeVariable("j", 1);
    IntegerFormula _x = imgrv.makeVariable("x");
    IntegerFormula _x1 = imgrv.makeVariable("x", 1);

    BooleanFormula _inst1 = imgrv.equal(imgrv.add(_1, _j1), imgrv.add(_0, _i1));
    BooleanFormula _inst2 =
        imgrv.equal(imgrv.add(_1, imgrv.subtract(_0, _i1)), imgrv.add(imgrv.add(_0, _x1), _i1));
    BooleanFormula _inst3 = bmgrv.and(_inst1, _inst2, bmgrv.not(_inst1));

    BooleanFormula _uinst1 = imgrv.equal(imgrv.add(_1, _j), imgrv.add(_0, _i));
    BooleanFormula _uinst2 =
        imgrv.equal(imgrv.add(_1, imgrv.subtract(_0, _i)), imgrv.add(imgrv.add(_0, _x), _i));
    BooleanFormula _uinst3 = bmgrv.and(_uinst1, _uinst2, bmgrv.not(_uinst1));

    SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
    ssaBuilder.setIndex("i", CNumericTypes.INT, 1);
    ssaBuilder.setIndex("j", CNumericTypes.INT, 1);
    ssaBuilder.setIndex("x", CNumericTypes.INT, 1);

    testUnInstantiate(_inst3, _uinst3, ssaBuilder);
  }

  private void testUnInstantiate(
      BooleanFormula pInstantiated, BooleanFormula pUninstantiated, SSAMapBuilder pSsaBuilder)
      throws SolverException, InterruptedException {
    BooleanFormula r1 = mgrv.instantiate(pUninstantiated, pSsaBuilder.build());
    assertThatFormula(r1).isEquivalentTo(pInstantiated);
    assertThat(r1.toString()).isEqualTo(pInstantiated.toString());

    BooleanFormula r2 = mgrv.uninstantiate(pInstantiated);
    assertThatFormula(r2).isEquivalentTo(pUninstantiated);
    assertThat(r2.toString()).isEqualTo(pUninstantiated.toString());
  }
}
