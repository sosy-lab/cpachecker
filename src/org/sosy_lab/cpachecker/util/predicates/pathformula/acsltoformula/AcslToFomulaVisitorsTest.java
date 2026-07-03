// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CFormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerBase;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class AcslToFomulaVisitorsTest {

  final LogManager logger = LogManager.createTestLogManager();
  private Solver smtSolver;
  private FormulaManagerView fmgr;

  @Before
  public void setUp() throws InvalidConfigurationException {
    // We need Z3 because some tests require quantifier support
    Configuration config =
        TestUtils.configurationForTest().setOption("solver.solver", "Z3").build();
    smtSolver = Solver.create(config, logger, ShutdownNotifier.createDummy());
    fmgr = smtSolver.getFormulaManager();
  }

  private BooleanFormula translate(AcslPredicate predicate) throws InvalidConfigurationException {
    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
    MachineModel machineModel = MachineModel.LINUX64;

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

    PointerTargetSetBuilder ptsb = createPointerTargetSetBuilder(converter);

    AcslPredicateToFormulaVisitor visitorP =
        new AcslPredicateToFormulaVisitor(fmgr, ssaMapBuilder, converter, machineModel, ptsb);

    return predicate.accept(visitorP);
  }

  private CSimpleType basicInt() {
    return new CSimpleType(
        CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);
  }

  private CType basicIntArray() {
    return new CArrayType(CTypeQualifiers.NONE, basicInt(), null);
  }

  private CProgramScope getCProgramScope() {
    String currentFunctionName = "f";

    CProgramScope scope =
        CProgramScope.mutableCoy(CProgramScope.empty().withFunctionScope(currentFunctionName));
    for (String var : ImmutableList.of("x", "y", "z")) {
      scope.registerDeclaration(
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.AUTO,
              basicInt(),
              var,
              var,
              var,
              null /* No initializer, we only want it for testing */));
    }

    scope.registerDeclaration(
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            basicIntArray(),
            "a",
            "a",
            "a",
            null /* No initializer, we only want it for testing */));

    scope.registerDeclaration(
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            new CFunctionType(basicInt(), ImmutableList.of(), false),
            currentFunctionName,
            ImmutableList.of(),
            ImmutableSet.of()));

    return scope;
  }

  // add variables that will be used as pointers below to pointer target set builder
  private PointerTargetSetBuilder createPointerTargetSetBuilder(CtoFormulaConverter converter) {
    PointerTargetSetBuilder ptsb =
        converter.createPointerTargetSetBuilder(PointerTargetSet.emptyPointerTargetSet());

    PointerBase baseZ = new PointerBase("z");
    ptsb.addNextBaseAddressConstraints(
        baseZ, basicInt(), null, false, new Constraints(fmgr.getBooleanFormulaManager()));

    ptsb.addBase(baseZ, basicInt());

    PointerBase baseA = new PointerBase("a");
    ptsb.addNextBaseAddressConstraints(
        baseA, basicIntArray(), null, false, new Constraints(fmgr.getBooleanFormulaManager()));

    ptsb.addBase(baseA, basicIntArray());

    return ptsb;
  }

  private AcslIdTerm getAcslIdTermFromVarName(CProgramScope cProgramScope, String name) {
    return new AcslIdTerm(
        FileLocation.DUMMY,
        new AcslCVariableDeclaration(
            (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable(name))));
  }

  @Test
  public void testPlusAndMinus()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // x + y - x != y should be unsatisfiable
    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslTerm y = getAcslIdTermFromVarName(cProgramScope, "y");

    AcslPredicate pred =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTerm(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.INTEGER,
                new AcslBinaryTerm(
                    FileLocation.DUMMY,
                    AcslBuiltinLogicType.INTEGER,
                    x,
                    y,
                    AcslBinaryTermOperator.PLUS),
                x,
                AcslBinaryTermOperator.MINUS),
            y,
            AcslBinaryTermExpressionOperator.NOT_EQUALS);

    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testLessEqualAntisymmetry()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(x <= y AND y <= x -> x == y) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslTerm y = getAcslIdTermFromVarName(cProgramScope, "y");

    AcslPredicate firstP =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, x, y, AcslBinaryTermExpressionOperator.LESS_EQUAL);

    AcslPredicate secondP =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, y, x, AcslBinaryTermExpressionOperator.LESS_EQUAL);

    AcslPredicate pred =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryPredicate(
                FileLocation.DUMMY, firstP, secondP, AcslBinaryPredicateOperator.AND),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY, x, y, AcslBinaryTermExpressionOperator.EQUALS),
            AcslBinaryPredicateOperator.IMPLICATION);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(FileLocation.DUMMY, pred, AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testGreaterEqualAndLess()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // (x >= y AND x < y) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");
    AcslTerm y = getAcslIdTermFromVarName(cProgramScope, "y");

    AcslPredicate pred =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY, x, y, AcslBinaryTermExpressionOperator.GREATER_EQUAL),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY, x, y, AcslBinaryTermExpressionOperator.LESS_THAN),
            AcslBinaryPredicateOperator.AND);

    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testUnaryPlus()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(x = +x) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslPredicate pred =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            x,
            new AcslUnaryTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, x, AcslUnaryTermOperator.PLUS),
            AcslBinaryTermExpressionOperator.EQUALS);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(FileLocation.DUMMY, pred, AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testUnaryMinus()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(x = -(-x)) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslTerm minusx =
        new AcslUnaryTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, x, AcslUnaryTermOperator.MINUS);

    AcslPredicate pred =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            x,
            new AcslUnaryTerm(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.INTEGER,
                minusx,
                AcslUnaryTermOperator.MINUS),
            AcslBinaryTermExpressionOperator.EQUALS);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(FileLocation.DUMMY, pred, AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testRealNumbers()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(5.0 * 0.2 == 1.0) should be unsatisfiable

    AcslPredicate pred =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTerm(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.REAL,
                new AcslRealLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.REAL, new BigDecimal("5.0")),
                new AcslRealLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.REAL, new BigDecimal("0.2")),
                AcslBinaryTermOperator.MULTIPLY),
            new AcslRealLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.REAL, new BigDecimal("1.0")),
            AcslBinaryTermExpressionOperator.EQUALS);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(FileLocation.DUMMY, pred, AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testNegationAndBooleanLiteralTerm()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(true = NEGATION(False)) should be unsatisfiable

    AcslPredicate pred =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslBooleanLiteralTerm(FileLocation.DUMMY, true),
            new AcslUnaryTerm(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslBooleanLiteralTerm(FileLocation.DUMMY, false),
                AcslUnaryTermOperator.NEGATION),
            AcslBinaryTermExpressionOperator.EQUALS);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(FileLocation.DUMMY, pred, AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testEquivalenceAndBooleanLiteralPredicate()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(true <=> (x = x)) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslPredicate pred =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBooleanLiteralPredicate(FileLocation.DUMMY, true),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY, x, x, AcslBinaryTermExpressionOperator.EQUALS),
            AcslBinaryPredicateOperator.EQUIVALENT);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(FileLocation.DUMMY, pred, AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testExists()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // \exists x: x != x should be unsatisfiable

    AcslParameterDeclaration x =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "x", "x");

    AcslPredicate pred =
        new AcslExistsPredicate(
            FileLocation.DUMMY,
            ImmutableList.of(x),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(FileLocation.DUMMY, x),
                new AcslIdTerm(FileLocation.DUMMY, x),
                AcslBinaryTermExpressionOperator.NOT_EQUALS));

    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testNeutralElementOfMultiplication()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // x * 1 != x should be unsatisfiable for all x
    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslPredicate pred =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTerm(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.INTEGER,
                x,
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
                AcslBinaryTermOperator.MULTIPLY),
            x,
            AcslBinaryTermExpressionOperator.NOT_EQUALS);

    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testForAll()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // \forall x: (x=5) and (x=6) should be unsatisfiable

    AcslParameterDeclaration x =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "x", "x");

    AcslPredicate body =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(FileLocation.DUMMY, x),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(5)),
                AcslBinaryTermExpressionOperator.EQUALS),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(FileLocation.DUMMY, x),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(6)),
                AcslBinaryTermExpressionOperator.EQUALS),
            AcslBinaryPredicateOperator.AND);

    AcslPredicate forall = new AcslForallPredicate(FileLocation.DUMMY, ImmutableList.of(x), body);

    BooleanFormula f = translate(forall);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testAcslCExpression()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // *(&z) != z should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    CSimpleDeclaration cVarZ = Objects.requireNonNull(cProgramScope.lookupVariable("z"));
    CIdExpression z = new CIdExpression(FileLocation.DUMMY, cVarZ);

    CExpression cExpression =
        new CPointerExpression(
            FileLocation.DUMMY,
            basicInt(),
            new CUnaryExpression(
                FileLocation.DUMMY,
                new CPointerType(CTypeQualifiers.NONE, basicInt()),
                z,
                CUnaryExpression.UnaryOperator.AMPER));

    AcslTerm termLeft = new AcslCExpression(FileLocation.DUMMY, cExpression);
    AcslTerm termRight = new AcslCExpression(FileLocation.DUMMY, z);

    AcslPredicate pred =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, termLeft, termRight, AcslBinaryTermExpressionOperator.NOT_EQUALS);

    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  // TODO Issue: I do not understand why this still creates constraints even though a is in the pts
  @Ignore
  @Test
  public void testAcslCExpressionWithArray()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // !((\forall integer i; 0 <= i && i < 3 ==> a[i] == 5) ==> a[1] == 5) should be unsat

    AcslParameterDeclaration i =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "i");

    CVariableDeclaration ci =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            basicInt(),
            "i",
            "i",
            "i",
            null /* No initializer, we only want it for testing */);

    AcslPredicate body =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryPredicate(
                FileLocation.DUMMY,
                new AcslBinaryTermPredicate(
                    FileLocation.DUMMY,
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                    new AcslIdTerm(FileLocation.DUMMY, i),
                    AcslBinaryTermExpressionOperator.LESS_EQUAL),
                new AcslBinaryTermPredicate(
                    FileLocation.DUMMY,
                    new AcslIdTerm(FileLocation.DUMMY, i),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(3)),
                    AcslBinaryTermExpressionOperator.LESS_THAN),
                AcslBinaryPredicateOperator.AND),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslCExpression(
                    FileLocation.DUMMY,
                    new CArraySubscriptExpression(
                        FileLocation.DUMMY,
                        basicInt(),
                        new CIdExpression(
                            FileLocation.DUMMY,
                            Objects.requireNonNull(getCProgramScope().lookupVariable("a"))),
                        new CIdExpression(FileLocation.DUMMY, ci))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(5)),
                AcslBinaryTermExpressionOperator.EQUALS),
            AcslBinaryPredicateOperator.IMPLICATION);

    AcslPredicate forall = new AcslForallPredicate(FileLocation.DUMMY, ImmutableList.of(i), body);
    AcslPredicate rhs =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslCExpression(
                FileLocation.DUMMY,
                new CArraySubscriptExpression(
                    FileLocation.DUMMY,
                    basicInt(),
                    new CIdExpression(
                        FileLocation.DUMMY,
                        Objects.requireNonNull(getCProgramScope().lookupVariable("a"))),
                    new CIntegerLiteralExpression(FileLocation.DUMMY, basicInt(), BigInteger.ONE))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(5)),
            AcslBinaryTermExpressionOperator.EQUALS);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryPredicate(
                FileLocation.DUMMY, forall, rhs, AcslBinaryPredicateOperator.IMPLICATION),
            AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Ignore
  @Test
  public void testAcslPredicateOverArray() {
    // predicate P(int *a, integer i) = (i == 0) ? (a[0] == 0) : (P(a, i-1) && a[i] == 0);
    // !(P(a, 2) => a[0] == 0 && a[1] == 0 && a[2] == 0) should be unsat

    AcslParameterDeclaration a =
        new AcslParameterDeclaration(
            FileLocation.DUMMY,
            new AcslCType(new CPointerType(CTypeQualifiers.NONE, basicInt())),
            "a",
            "a");

    AcslParameterDeclaration index =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "P::i");

    @SuppressWarnings("unused")
    AcslPredicateDeclaration declP =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            // Function type
            new AcslPredicateType(ImmutableList.of(a.getType(), index.getType()), false),
            "P",
            "P",
            // Polymorphic types
            ImmutableList.of(),
            // Parameters
            ImmutableList.of(a, index));

    // TODO before I continue coding this: how can a test like this even work if my visitor for
    // AcslPredicateApplicationPredicate just creates a UF?
  }
}
