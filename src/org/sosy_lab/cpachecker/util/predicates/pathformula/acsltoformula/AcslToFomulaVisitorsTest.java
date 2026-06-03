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
import org.junit.Before;
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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CFormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
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

  private BooleanFormula translate(AcslPredicate predicate, Boolean doRenaming)
      throws InvalidConfigurationException {
    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
    MachineModel machineModel = MachineModel.LINUX64;

    Configuration config = TestUtils.configurationForTest().build();
    CFormulaEncodingOptions options = new CFormulaEncodingOptions(config);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(logger, machineModel);

    CtoFormulaConverter converter =
        new CtoFormulaConverter(
            options,
            fmgr,
            machineModel,
            Optional.empty(),
            logger,
            ShutdownNotifier.createDummy(),
            typeHandler,
            AnalysisDirection.FORWARD);

    AcslPredicateToFormulaVisitor visitorP =
        new AcslPredicateToFormulaVisitor(fmgr, ssaMapBuilder, converter, machineModel);

    if (doRenaming) {
      AcslRenamingVisitor vistorR = new AcslRenamingVisitor();
      predicate = predicate.accept(vistorR);
    }

    return predicate.accept(visitorP);
  }

  private CSimpleType basicInt() {
    return new CSimpleType(
        CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);
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
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            new CFunctionType(basicInt(), ImmutableList.of(), false),
            currentFunctionName,
            ImmutableList.of(),
            ImmutableSet.of()));

    return scope;
  }

  @Test
  public void testPlusAndMinus()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // x + y - x != y should be unsatisfiable
    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x"))));

    AcslTerm y =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("y"))));

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

    BooleanFormula f = translate(pred, false);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testLessEqualAntisymmetry()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(x <= y AND y <= x -> x == y) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x"))));

    AcslTerm y =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("y"))));

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

    BooleanFormula f = translate(unsatPred, false);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testGreaterEqualAndLess()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // (x >= y AND x < y) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x"))));
    AcslTerm y =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("y"))));

    AcslPredicate pred =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY, x, y, AcslBinaryTermExpressionOperator.GREATER_EQUAL),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY, x, y, AcslBinaryTermExpressionOperator.LESS_THAN),
            AcslBinaryPredicateOperator.AND);

    BooleanFormula f = translate(pred, false);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testUnaryPlus()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(x = +x) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x"))));

    AcslPredicate pred =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            x,
            new AcslUnaryTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, x, AcslUnaryTermOperator.PLUS),
            AcslBinaryTermExpressionOperator.EQUALS);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(FileLocation.DUMMY, pred, AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred, false);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testUnaryMinus()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(x = -(-x)) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x"))));

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

    BooleanFormula f = translate(unsatPred, false);
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

    BooleanFormula f = translate(unsatPred, false);
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

    BooleanFormula f = translate(unsatPred, false);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testEquivalenceAndBooleanLiteralPredicate()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(true <=> (x = x)) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x"))));

    AcslPredicate pred =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBooleanLiteralPredicate(FileLocation.DUMMY, true),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY, x, x, AcslBinaryTermExpressionOperator.EQUALS),
            AcslBinaryPredicateOperator.EQUIVALENT);

    AcslPredicate unsatPred =
        new AcslUnaryPredicate(FileLocation.DUMMY, pred, AcslUnaryExpressionOperator.NEGATION);

    BooleanFormula f = translate(unsatPred, false);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testExists()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // \exists x: x != x should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();

    AcslCVariableDeclaration decl =
        new AcslCVariableDeclaration(
            (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x")));

    AcslTerm x = new AcslIdTerm(FileLocation.DUMMY, decl);

    AcslPredicate body =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, x, x, AcslBinaryTermExpressionOperator.NOT_EQUALS);

    AcslPredicate exists =
        new AcslExistsPredicate(
            FileLocation.DUMMY,
            ImmutableList.of(
                new AcslParameterDeclaration(FileLocation.DUMMY, decl.getType(), decl.getName())),
            body);

    BooleanFormula f = translate(exists, true);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testNeutralElementOfMultiplication()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // x * 1 != x should be unsatisfiable for all x
    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x =
        new AcslIdTerm(
            FileLocation.DUMMY,
            new AcslCVariableDeclaration(
                (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x"))));

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

    BooleanFormula f = translate(pred, false);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testForAll()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // \forall x: (x=5) and (x=6) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();

    AcslCVariableDeclaration decl =
        new AcslCVariableDeclaration(
            (CVariableDeclaration) Objects.requireNonNull(cProgramScope.lookupVariable("x")));

    AcslTerm x = new AcslIdTerm(FileLocation.DUMMY, decl);

    AcslPredicate body =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                x,
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(5)),
                AcslBinaryTermExpressionOperator.EQUALS),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                x,
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(6)),
                AcslBinaryTermExpressionOperator.EQUALS),
            AcslBinaryPredicateOperator.AND);

    AcslPredicate forall =
        new AcslForallPredicate(
            FileLocation.DUMMY,
            ImmutableList.of(
                new AcslParameterDeclaration(FileLocation.DUMMY, decl.getType(), decl.getName())),
            body);

    BooleanFormula f = translate(forall, true);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }
}
