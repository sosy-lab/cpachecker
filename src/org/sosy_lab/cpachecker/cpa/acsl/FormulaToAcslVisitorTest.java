// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import java.util.Optional;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAstNode;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula.AcslPredicateToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula.AcslTestBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CFormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class FormulaToAcslVisitorTest {

  final LogManager logger = LogManager.createTestLogManager();
  private FormulaManagerView fmgr;
  private Solver solver;
  private MachineModel machineModel;
  private FormulaToAcslVisitor visitor;
  private AcslTestBuilder b;

  @Before
  public void setUp() throws InvalidConfigurationException {
    Configuration config =
        TestUtils.configurationForTest()
            .setOption("solver.solver", "Z3")
            .setOption("cpa.predicate.useConstraintOptimization", "false")
            .build();
    solver = Solver.create(config, logger, ShutdownNotifier.createDummy());
    fmgr = solver.getFormulaManager();
    machineModel = MachineModel.LINUX64;
    visitor = new FormulaToAcslVisitor(fmgr, machineModel);
    b = new AcslTestBuilder();
  }

  private AcslAstNode translate(Formula formula) {
    fmgr.transformRecursively(formula, visitor);
    return visitor.getAcslExpressionForFormula(formula);
  }

  private BooleanFormula translate(AcslPredicate predicate) throws InvalidConfigurationException {
    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();

    Configuration config = TestUtils.configurationForTest().build();
    CFormulaEncodingWithPointerAliasingOptions formulaOptions =
        new CFormulaEncodingWithPointerAliasingOptions(config);
    TypeHandlerWithPointerAliasing typeHandler =
        new TypeHandlerWithPointerAliasing(logger, machineModel, formulaOptions);

    CToFormulaConverterWithPointerAliasing converter =
        new CToFormulaConverterWithPointerAliasing(
            formulaOptions,
            fmgr,
            machineModel,
            Optional.empty(),
            logger,
            ShutdownNotifier.createDummy(),
            typeHandler,
            AnalysisDirection.FORWARD);

    PointerTargetSetBuilder ptsb =
        converter.createPointerTargetSetBuilder(PointerTargetSet.emptyPointerTargetSet());

    AcslPredicateToFormulaVisitor visitorP =
        new AcslPredicateToFormulaVisitor(
            fmgr,
            ssaMapBuilder,
            converter,
            machineModel,
            ptsb,
            new Constraints(fmgr.getBooleanFormulaManager()));

    return predicate.accept(visitorP);
  }

  @Test
  public void testIntegerConstant() {
    // 42
    IntegerFormula formula = fmgr.getIntegerFormulaManager().makeNumber(42);
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslIntegerLiteralTerm.class);
    assertThat(BigInteger.valueOf(42)).isEqualTo(((AcslIntegerLiteralTerm) result).getValue());
  }

  @Test
  public void testBooleanConstant() {
    // true
    BooleanFormula formula = fmgr.getBooleanFormulaManager().makeTrue();
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslBooleanLiteralPredicate.class);
    assertThat(((AcslBooleanLiteralPredicate) result).getValue()).isTrue();
  }

  @Test
  public void testIntegerVariable() {
    // x
    IntegerFormula formula = fmgr.getIntegerFormulaManager().makeVariable("x");
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslIdTerm.class);
    assertThat(((AcslIdTerm) result).getName()).isEqualTo("x");
  }

  @Test
  public void testIntegerVariableWithSSA() {
    // f::x@2
    IntegerFormula formula = fmgr.getIntegerFormulaManager().makeVariable("f::x@2");
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslIdTerm.class);
    assertThat(((AcslIdTerm) result).getName()).isEqualTo("x@2");
  }

  @Test
  public void testBooleanVariable() {
    // p
    BooleanFormula formula = fmgr.getBooleanFormulaManager().makeVariable("p");
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslIdPredicate.class);
    assertThat(((AcslIdPredicate) result).getName()).isEqualTo("p");
  }

  @Test
  public void testAddition() {
    // 2 + 3
    IntegerFormula formula =
        fmgr.getIntegerFormulaManager()
            .add(
                fmgr.getIntegerFormulaManager().makeNumber(2),
                fmgr.getIntegerFormulaManager().makeNumber(3));
    AcslAstNode result = translate(formula);

    AcslTerm expected = b.plus(b.integer(2), b.integer(3));

    assertThat(result).isInstanceOf(AcslBinaryTerm.class);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testSubtraction() {
    // 5 - 2
    IntegerFormula formula =
        fmgr.getIntegerFormulaManager()
            .subtract(
                fmgr.getIntegerFormulaManager().makeNumber(5),
                fmgr.getIntegerFormulaManager().makeNumber(2));
    AcslAstNode result = translate(formula);

    AcslTerm expected = b.minus(b.integer(5), b.integer(2));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testMultiplication() {
    // 2 * 3
    IntegerFormula formula =
        fmgr.getIntegerFormulaManager()
            .multiply(
                fmgr.getIntegerFormulaManager().makeNumber(2),
                fmgr.getIntegerFormulaManager().makeNumber(3));
    AcslAstNode result = translate(formula);

    AcslAstNode expected = b.multiply(b.integer(2), b.integer(3));
    assertThat(result).isEqualTo(expected);
  }

  // TODO why does SMT do division as an unterpreted function?!
  @Ignore
  @Test
  public void testDivision() {
    // 6 / 2
    IntegerFormula formula =
        fmgr.getIntegerFormulaManager()
            .divide(
                fmgr.getIntegerFormulaManager().makeNumber(6),
                fmgr.getIntegerFormulaManager().makeNumber(2));
    AcslAstNode result = translate(formula);

    AcslAstNode expected = b.divide(b.integer(6), b.integer(2));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testEquality() {
    // 2 == 2
    BooleanFormula formula =
        fmgr.getIntegerFormulaManager()
            .equal(
                fmgr.getIntegerFormulaManager().makeNumber(2),
                fmgr.getIntegerFormulaManager().makeNumber(2));
    AcslAstNode result = translate(formula);

    AcslAstNode expected = b.eq(b.integer(2), b.integer(2));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testLessThan() {
    // 2 <= 3
    BooleanFormula formula =
        fmgr.getIntegerFormulaManager()
            .lessThan(
                fmgr.getIntegerFormulaManager().makeNumber(2),
                fmgr.getIntegerFormulaManager().makeNumber(3));
    AcslAstNode result = translate(formula);

    AcslAstNode expected = b.lt(b.integer(2), b.integer(3));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testLogicalNot() {
    // not(p)
    BooleanFormula formula =
        fmgr.getBooleanFormulaManager().not(fmgr.getBooleanFormulaManager().makeVariable("p"));
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslUnaryPredicate.class);
    assertThat(((AcslUnaryPredicate) result).getOperator())
        .isEqualTo(AcslUnaryExpressionOperator.NEGATION);
  }

  @Test
  public void testLogicalAnd() {
    // p and q
    BooleanFormula formula =
        fmgr.getBooleanFormulaManager()
            .and(
                fmgr.getBooleanFormulaManager().makeVariable("p"),
                fmgr.getBooleanFormulaManager().makeVariable("q"));
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslBinaryPredicate.class);
    assertThat(((AcslBinaryPredicate) result).getOperator())
        .isEqualTo(AcslBinaryPredicateOperator.AND);
  }

  @Test
  public void testLogicalOr() {
    // p or q
    BooleanFormula formula =
        fmgr.getBooleanFormulaManager()
            .or(
                fmgr.getBooleanFormulaManager().makeVariable("p"),
                fmgr.getBooleanFormulaManager().makeVariable("q"));
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslBinaryPredicate.class);
    assertThat(((AcslBinaryPredicate) result).getOperator())
        .isEqualTo(AcslBinaryPredicateOperator.OR);
  }

  @Test
  public void testImplication() {
    // p ==> q
    BooleanFormula formula =
        fmgr.getBooleanFormulaManager()
            .implication(
                fmgr.getBooleanFormulaManager().makeVariable("p"),
                fmgr.getBooleanFormulaManager().makeVariable("q"));
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslBinaryPredicate.class);
    assertThat(((AcslBinaryPredicate) result).getOperator())
        .isEqualTo(AcslBinaryPredicateOperator.IMPLICATION);
  }

  @Test
  public void testArrayAccess() {
    // a[2]
    ArrayFormula<IntegerFormula, IntegerFormula> array =
        fmgr.getArrayFormulaManager()
            .makeArray("a", FormulaType.IntegerType, FormulaType.IntegerType);
    IntegerFormula index = fmgr.getIntegerFormulaManager().makeNumber(2);
    Formula formula = fmgr.getArrayFormulaManager().select(array, index);
    AcslAstNode result = translate(formula);

    assertThat(result).isInstanceOf(AcslArraySubscriptTerm.class);
    assertThat(((AcslArraySubscriptTerm) result).getSubscriptExpression()).isEqualTo(b.integer(2));
    assertThat(((AcslArraySubscriptTerm) result).getArrayExpression().toASTString()).isEqualTo("a");
  }

  @Test
  public void testBothTranslationDirections() throws InvalidConfigurationException {
    // 2 + 3 = 5
    AcslPredicate original = b.eq(b.plus(b.integer(2), b.integer(3)), b.integer(5));

    // Translation ACSL to SMT formula (with AcslPredicateToFormulaVisitor)
    BooleanFormula formula = translate(original);

    // Translate SMT formula back to ACSL (with FormulaToAcslVisitor)
    AcslAstNode translated = translate(formula);

    assertThat(translated).isEqualTo(original);
  }
}
