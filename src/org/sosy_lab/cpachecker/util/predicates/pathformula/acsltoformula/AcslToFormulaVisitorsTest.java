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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicPredicateDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class AcslToFormulaVisitorsTest {

  final LogManager logger = LogManager.createTestLogManager();
  private Solver smtSolver;
  private FormulaManagerView fmgr;
  private AcslTestBuilder b;
  private Constraints constraints;

  @Before
  public void setUp() throws InvalidConfigurationException {
    // We need Z3 because some tests require quantifier support
    Configuration config =
        TestUtils.configurationForTest()
            .setOption("solver.solver", "Z3")
            .setOption("cpa.predicate.useConstraintOptimization", "false")
            .build();
    smtSolver = Solver.create(config, logger, ShutdownNotifier.createDummy());
    fmgr = smtSolver.getFormulaManager();
    b = new AcslTestBuilder();
    constraints = new Constraints(fmgr.getBooleanFormulaManager());
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
        new AcslPredicateToFormulaVisitor(
            fmgr, ssaMapBuilder, converter, machineModel, ptsb, constraints);

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

  private CVariableDeclaration createCVariableDeclaration(String name) {
    return new CVariableDeclaration(
        FileLocation.DUMMY,
        true,
        CStorageClass.AUTO,
        basicInt(),
        name,
        name,
        name,
        null /* No initializer, we only want it for testing */);
  }

  @Test
  public void testPlusAndMinus()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // x + y - x != y should be unsatisfiable
    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");
    AcslTerm y = getAcslIdTermFromVarName(cProgramScope, "y");

    AcslPredicate pred = b.neq(b.minus(b.plus(x, y), x), y);

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

    AcslPredicate pred = b.implies(b.and(b.leq(x, y), b.leq(y, x)), b.eq(x, y));
    AcslPredicate unsatPred = b.not(pred);

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

    AcslPredicate pred = b.and(b.geq(x, y), b.lt(x, y));

    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testUnaryPlus()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(x = +x) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslPredicate pred = b.eq(x, b.unaryPlus(x));
    AcslPredicate unsatPred = b.not(pred);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testUnaryMinus()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(x = -(-x)) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslTerm minusx = b.unaryMinus(x);
    AcslPredicate pred = b.eq(x, b.unaryMinus(minusx));
    AcslPredicate unsatPred = b.not(pred);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testRealNumbers()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(5.0 * 0.2 == 1.0) should be unsatisfiable

    AcslPredicate pred =
        b.eq(b.multiply(b.real(5.0), b.real(0.2), AcslBuiltinLogicType.REAL), b.real(1.0));
    AcslPredicate unsatPred = b.not(pred);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testNegationAndBooleanLiteralTerm()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(true = NEGATION(False)) should be unsatisfiable

    AcslPredicate pred =
        b.eq(b.bool(true), b.unaryNegation(b.bool(false), AcslBuiltinLogicType.BOOLEAN));
    AcslPredicate unsatPred = b.not(pred);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testEquivalenceAndBooleanLiteralPredicate()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // NOT(true <=> (x = x)) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslPredicate pred = b.equivalent(b.boolPred(true), b.eq(x, x));
    AcslPredicate unsatPred = b.not(pred);

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testExists()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // \exists x: x != x should be unsatisfiable

    AcslParameterDeclaration x =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "x");

    AcslPredicate pred =
        new AcslExistsPredicate(
            FileLocation.DUMMY,
            ImmutableList.of(x),
            b.neq(new AcslIdTerm(FileLocation.DUMMY, x), new AcslIdTerm(FileLocation.DUMMY, x)));

    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testNeutralElementOfMultiplication()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // x * 1 != x should be unsatisfiable for all x
    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslPredicate pred = b.neq(b.multiply(x, b.integer(1)), x);

    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testForAll()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // \forall x: (x=5) and (x=6) should be unsatisfiable

    AcslParameterDeclaration x =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "x");

    AcslPredicate body =
        b.and(
            b.eq(new AcslIdTerm(FileLocation.DUMMY, x), b.integer(5)),
            b.eq(new AcslIdTerm(FileLocation.DUMMY, x), b.integer(6)));

    AcslPredicate forall = new AcslForallPredicate(FileLocation.DUMMY, ImmutableList.of(x), body);

    BooleanFormula f = translate(forall);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testPredicateApplication()
      throws SolverException, InterruptedException, InvalidConfigurationException {
    // P(x) and not P(x) should be unsatisfiable

    CProgramScope cProgramScope = getCProgramScope();
    AcslTerm x = getAcslIdTermFromVarName(cProgramScope, "x");

    AcslParameterDeclaration paramX =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "x", "P");

    AcslPredicateDeclaration predDecl =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            new AcslPredicateType(ImmutableList.of(paramX.getType()), false),
            "P",
            "P",
            ImmutableList.of(),
            ImmutableList.of(paramX));

    AcslPredicate predApp =
        new AcslPredicateApplicationPredicate(FileLocation.DUMMY, predDecl, ImmutableList.of(x));

    AcslPredicate finalPred = b.and(predApp, b.not(predApp));

    BooleanFormula f = translate(finalPred);
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

    AcslPredicate pred = b.neq(termLeft, termRight);
    BooleanFormula f = translate(pred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testAcslCExpressionWithArray()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // !((\forall integer i; 0 <= i && i < 3 ==> a[i] == 5) ==> a[1] == 5) should be unsat

    AcslParameterDeclaration i =
        new AcslParameterDeclaration(FileLocation.DUMMY, new AcslCType(basicInt()), "i");

    CVariableDeclaration ci = createCVariableDeclaration("i");

    AcslCExpression a1 =
        b.arrayAcslCExpression(
            basicInt(), Objects.requireNonNull(getCProgramScope().lookupVariable("a")), 1);

    AcslCExpression ai =
        b.arrayAcslCExpression(
            basicInt(),
            Objects.requireNonNull(getCProgramScope().lookupVariable("a")),
            new CIdExpression(FileLocation.DUMMY, ci));

    AcslPredicate body =
        b.implies(
            b.and(
                b.leq(b.integer(0), new AcslIdTerm(FileLocation.DUMMY, i)),
                b.lt(new AcslIdTerm(FileLocation.DUMMY, i), b.integer(3))),
            b.eq(ai, b.integer(5)));

    AcslPredicate forall = new AcslForallPredicate(FileLocation.DUMMY, ImmutableList.of(i), body);

    AcslPredicate rhs = b.eq(a1, b.integer(5));

    AcslPredicate unsatPred = b.not(b.implies(forall, rhs));

    BooleanFormula f = translate(unsatPred);
    assertThat(smtSolver.isUnsat(f)).isTrue();
  }

  @Test
  public void testRecursivePredicate()
      throws InvalidConfigurationException, InterruptedException, SolverException {
    // P(x,i) = (i = 0) ? (x == 0) : (P(x, i-1);
    // !(P(x,2) => (x=0)) should be unsat

    AcslParameterDeclaration x =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "x", "P");
    AcslParameterDeclaration index =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "P");

    AcslPredicateDeclaration declP =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            // Function type: use INTEGER, because AcslCType can be cast to it,
            // AcslPredicateApplicationPredicate expects the most general type here
            new AcslPredicateType(
                ImmutableList.of(AcslBuiltinLogicType.INTEGER, AcslBuiltinLogicType.INTEGER),
                false),
            "P",
            "P",
            // Polymorphic types
            ImmutableList.of(),
            // Parameters
            ImmutableList.of(x, index));

    AcslLogicPredicateDefinition defP =
        new AcslLogicPredicateDefinition(
            FileLocation.DUMMY,
            // Function declaration
            declP,
            // Function body
            b.ite(
                // if
                b.eq(new AcslIdTerm(FileLocation.DUMMY, index), b.integer(0)),
                // then
                b.eq(new AcslIdTerm(FileLocation.DUMMY, x), b.integer(0)),
                // else
                new AcslPredicateApplicationPredicate(
                    FileLocation.DUMMY,
                    declP,
                    ImmutableList.of(
                        new AcslIdTerm(FileLocation.DUMMY, x),
                        b.minus(new AcslIdTerm(FileLocation.DUMMY, index), b.integer(1))))));

    // P(x,i)
    AcslPredicateApplicationPredicate pai =
        new AcslPredicateApplicationPredicate(
            FileLocation.DUMMY,
            declP,
            ImmutableList.of(
                new AcslIdTerm(FileLocation.DUMMY, x), new AcslIdTerm(FileLocation.DUMMY, index)));

    AcslPredicateApplicationPredicate px2 =
        new AcslPredicateApplicationPredicate(
            FileLocation.DUMMY,
            declP,
            ImmutableList.of(new AcslIdTerm(FileLocation.DUMMY, x), b.integer(2)));

    AcslPredicate pTest = b.implies(px2, b.eq(new AcslIdTerm(FileLocation.DUMMY, x), b.integer(0)));

    // \forall i: P(x,i) <-> defP.body
    BooleanFormula fDefinition =
        translate(
            new AcslForallPredicate(
                FileLocation.DUMMY, ImmutableList.of(x, index), b.equivalent(pai, defP.getBody())));

    boolean unsat;

    try (ProverEnvironment prover = smtSolver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.addConstraint(fDefinition);
      prover.addConstraint(translate(b.not(pTest)));
      prover.addConstraint(constraints.get());
      unsat = prover.isUnsat();
    }
    assertThat(unsat).isTrue();
  }

  @Test
  public void testUnrolledPredicateOverArray()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // predicate P(int *a, integer i):
    //      P(a,0) <-> a[0] == 0;
    //      P(a,1) <-> P(a,0) && a[1] == 0;
    //      P(a,2) <-> P(a,1) && a[2] == 0;
    // !(P(a, 2) => a[0] == 0 && a[1] == 0 && a[2] == 0) should be unsat

    AcslParameterDeclaration a =
        new AcslParameterDeclaration(
            FileLocation.DUMMY,
            new AcslCType(new CPointerType(CTypeQualifiers.NONE, basicInt())),
            "a",
            "P");

    AcslParameterDeclaration index =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "P");

    AcslPredicateDeclaration declP =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            // Function type: use INTEGER, because AcslCType can be cast to it,
            // AcslPredicateApplicationPredicate expects the most general type here
            new AcslPredicateType(
                ImmutableList.of(a.getType(), AcslBuiltinLogicType.INTEGER), false),
            "P",
            "P",
            // Polymorphic types
            ImmutableList.of(),
            // Parameters
            ImmutableList.of(a, index));

    AcslCExpression a0 =
        b.arrayAcslCExpression(
            basicInt(), Objects.requireNonNull(getCProgramScope().lookupVariable("a")), 0);
    AcslCExpression a1 =
        b.arrayAcslCExpression(
            basicInt(), Objects.requireNonNull(getCProgramScope().lookupVariable("a")), 1);
    AcslCExpression a2 =
        b.arrayAcslCExpression(
            basicInt(), Objects.requireNonNull(getCProgramScope().lookupVariable("a")), 2);

    AcslPredicateApplicationPredicate pa0 =
        new AcslPredicateApplicationPredicate(
            FileLocation.DUMMY,
            declP,
            ImmutableList.of(new AcslIdTerm(FileLocation.DUMMY, a), b.integer(0)));
    AcslPredicateApplicationPredicate pa1 =
        new AcslPredicateApplicationPredicate(
            FileLocation.DUMMY,
            declP,
            ImmutableList.of(new AcslIdTerm(FileLocation.DUMMY, a), b.integer(1)));
    AcslPredicateApplicationPredicate pa2 =
        new AcslPredicateApplicationPredicate(
            FileLocation.DUMMY,
            declP,
            ImmutableList.of(new AcslIdTerm(FileLocation.DUMMY, a), b.integer(2)));

    // P(a,0) <-> a[0] == 0
    AcslPredicate def0 = b.equivalent(pa0, b.eq(a0, b.integer(0)));
    // P(a,1) <-> P(a,0) && a[1] == 0
    AcslPredicate def1 = b.equivalent(pa1, b.and(pa0, b.eq(a1, b.integer(0))));
    // P(a,2) <-> P(a,1) && a[2] == 0
    AcslPredicate def2 = b.equivalent(pa2, b.and(pa1, b.eq(a2, b.integer(0))));

    // P(a,2) => a[0] == 0 && a[1] == 0 && a[2] == 0
    AcslPredicate pred =
        b.implies(
            pa2,
            b.and(b.eq(a0, b.integer(0)), b.and(b.eq(a1, b.integer(0)), b.eq(a2, b.integer(0)))));

    // not(P(a,2) => a[0] == 0 && a[1] == 0 && a[2] == 0)
    AcslPredicate unsatPred = b.not(pred);
    BooleanFormula f = translate(unsatPred);

    boolean unsat;

    try (ProverEnvironment prover = smtSolver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.addConstraint(translate(def0));
      prover.addConstraint(translate(def1));
      prover.addConstraint(translate(def2));
      prover.addConstraint(f);
      prover.addConstraint(constraints.get());
      unsat = prover.isUnsat();
    }
    assertThat(unsat).isTrue();
  }

  @Test
  public void testPredicateOverArrayWithQuantifier()
      throws InvalidConfigurationException, SolverException, InterruptedException {
    // predicate P(i) = (i == 0) ? (a[0] == 0) : (P(i-1) && a[i] == 0);
    // !(P(2) => a[0] == 0 && a[1] == 0 && a[2] == 0) should be unsat

    AcslParameterDeclaration index =
        new AcslParameterDeclaration(FileLocation.DUMMY, new AcslCType(basicInt()), "i", "P");

    AcslPredicateDeclaration declP =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            // Function type: use INTEGER, because AcslCType can be cast to it,
            // AcslPredicateApplicationPredicate expects the most general type here
            new AcslPredicateType(ImmutableList.of(new AcslCType(basicInt())), false),
            "P",
            "P",
            // Polymorphic types
            ImmutableList.of(),
            // Parameters
            ImmutableList.of(index));

    CVariableDeclaration ci = createCVariableDeclaration("i");
    AcslCExpression a0 =
        b.arrayAcslCExpression(
            basicInt(), Objects.requireNonNull(getCProgramScope().lookupVariable("a")), 0);
    AcslCExpression a1 =
        b.arrayAcslCExpression(
            basicInt(), Objects.requireNonNull(getCProgramScope().lookupVariable("a")), 1);
    AcslCExpression a2 =
        b.arrayAcslCExpression(
            basicInt(), Objects.requireNonNull(getCProgramScope().lookupVariable("a")), 2);
    AcslCExpression ai =
        b.arrayAcslCExpression(
            basicInt(),
            Objects.requireNonNull(getCProgramScope().lookupVariable("a")),
            new CIdExpression(FileLocation.DUMMY, ci));

    AcslLogicPredicateDefinition defP =
        new AcslLogicPredicateDefinition(
            FileLocation.DUMMY,
            // Function declaration
            declP,
            // Function body
            b.ite(
                // if
                b.eq(new AcslIdTerm(FileLocation.DUMMY, index), b.integer(0)),
                // then
                b.eq(a0, b.integer(0)),
                // else
                b.and(
                    new AcslPredicateApplicationPredicate(
                        FileLocation.DUMMY,
                        declP,
                        ImmutableList.of(
                            b.minus(new AcslIdTerm(FileLocation.DUMMY, index), b.integer(1)))),
                    b.eq(ai, b.integer(0)))));

    // P(i)
    AcslPredicateApplicationPredicate pai =
        new AcslPredicateApplicationPredicate(
            FileLocation.DUMMY, declP, ImmutableList.of(new AcslIdTerm(FileLocation.DUMMY, index)));

    // \forall i: P(i) <-> defP.body
    BooleanFormula fDefinition =
        translate(
            new AcslForallPredicate(
                FileLocation.DUMMY, ImmutableList.of(index), b.equivalent(pai, defP.getBody())));

    AcslPredicateApplicationPredicate pa2 =
        new AcslPredicateApplicationPredicate(
            FileLocation.DUMMY, declP, ImmutableList.of(b.integer(2)));

    // P(2) => a[0] == 0 && a[1] == 0 && a[2] == 0
    AcslPredicate pred =
        b.implies(
            pa2,
            b.and(b.eq(a0, b.integer(0)), b.and(b.eq(a1, b.integer(0)), b.eq(a2, b.integer(0)))));

    // not(P(2) => a[0] == 0 && a[1] == 0 && a[2] == 0)
    AcslPredicate unsatPred = b.not(pred);
    BooleanFormula f = translate(unsatPred);

    boolean unsat;

    try (ProverEnvironment prover = smtSolver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.addConstraint(fDefinition);
      prover.addConstraint(f);
      prover.addConstraint(constraints.get());
      unsat = prover.isUnsat();
    }
    assertThat(unsat).isTrue();
  }
}
