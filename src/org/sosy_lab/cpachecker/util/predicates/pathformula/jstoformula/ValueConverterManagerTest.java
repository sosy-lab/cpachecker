/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import java.util.function.Function;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.test.BooleanFormulaSubject;

@RunWith(Parameterized.class)
public class ValueConverterManagerTest extends SolverViewBasedTest0 {
  private enum Satisfiability {
    SATISFIABLE,
    UNSATISFIABLE,
    SATISFIABLE_NON_TAUTOLOGICAL
  }

  private static final Satisfiability satisfiable = Satisfiability.SATISFIABLE;

  @SuppressWarnings("unused")
  private static final Satisfiability satisfiable_non_tautological =
      Satisfiability.SATISFIABLE_NON_TAUTOLOGICAL;

  private static final Satisfiability unsatisfiable = Satisfiability.UNSATISFIABLE;

  private TypedValueManager tvmgr;
  private StringFormulaManager strMgr;
  private TypedVariableValues typedVarValues;
  private TypeTags typeTags;

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return new Object[] {Solvers.MATHSAT5, Solvers.Z3};
  }

  @Parameter() public Solvers usedSolver;

  @Override
  protected Solvers solverToUse() {
    return usedSolver;
  }

  private ValueConverterManager valConvMgr;

  @Before
  public void init() throws InvalidConfigurationException {
    initSolver();
    initCPAcheckerSolver();
    final ObjectIdFormulaManager objIdMgr = new ObjectIdFormulaManager(mgrv);
    typeTags = new TypeTags(imgrv);
    tvmgr = new TypedValueManager(mgrv, typeTags, objIdMgr.getNullObjectId());
    strMgr = new StringFormulaManager(mgrv, 20);
    typedVarValues = new TypedVariableValues(mgrv.getFunctionFormulaManager());
    valConvMgr = new ValueConverterManager(typedVarValues, typeTags, tvmgr, strMgr, mgrv);
  }

  private void assertToInt32(final double pFrom, final int pTo, final boolean pEqual)
      throws SolverException, InterruptedException {
    @SuppressWarnings("ConstantConditions")
    final BooleanFormula formula =
        bvmgr.equal(
            valConvMgr.toInt32(
                fpmgr.makeNumber(pFrom, FormulaType.getDoublePrecisionFloatingPointType())),
            bvmgr.makeBitvector(32, pTo));
    if (pEqual) {
      assertThatFormula(formula).isSatisfiable();
      assertThatFormula(bmgr.not(formula)).isUnsatisfiable();
    } else {
      assertThatFormula(formula).isUnsatisfiable();
      assertThatFormula(bmgr.not(formula)).isSatisfiable();
    }
  }

  @Test
  public void toInt32() throws SolverException, InterruptedException {
    // int32 to int32
    assertToInt32(-1, -1, true);
    assertToInt32(0, 0, true);
    assertToInt32(2147483647, 2147483647, true);
    assertToInt32(-2147483648, -2147483648, true);

    assertToInt32(-1, -2, false);
    assertToInt32(0, 1, false);
    assertToInt32(2147483647, -2147483648, false);
    assertToInt32(-2147483648, 2147483647, false);

    // double to int32
    assertToInt32(-1.9, -1, true);
    assertToInt32(0.1, 0, true);
    assertToInt32(2147483647.999, 2147483647, true);
    assertToInt32(-2147483648.999, -2147483648, true);

    assertToInt32(-1.9, -2, false);
    assertToInt32(0.1, 1, false);
    assertToInt32(2147483647.999, -2147483648, false);
    assertToInt32(-2147483648.999, 2147483647, false);

    // integer out of 32-bit range are converted with overflow
    assertToInt32(2147483648.0, -2147483648, true);
    assertToInt32(6442450944.0, -2147483648, true);
    assertToInt32(2147483649.0, -2147483647, true);
    assertToInt32(-2147483649.0, 2147483647, true);
    assertToInt32(-6442450945.0, 2147483647, true);
    assertToInt32(-2147483650.0, 2147483646, true);
    assertToInt32(Double.MAX_VALUE, 0, true);
    assertToInt32(-Double.MAX_VALUE, 0, true);

    assertToInt32(2147483648.0, 2147483647, false);
    assertToInt32(6442450944.0, 2147483647, false);
    assertToInt32(2147483649.0, 2147483647, false);
    assertToInt32(-2147483649.0, 0, false);
    assertToInt32(-6442450945.0, 0, false);
    assertToInt32(-2147483650.0, 0, false);
    assertToInt32(Double.MAX_VALUE, 2147483647, false);
    assertToInt32(-Double.MAX_VALUE, -2147483648, false);

    // special values
    assertToInt32(Double.POSITIVE_INFINITY, 0, true);
    assertToInt32(Double.NEGATIVE_INFINITY, 0, true);
    assertToInt32(Double.NaN, 0, true);
    assertToInt32(-0.0, 0, true);
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void toStringFormula() throws SolverException, InterruptedException {
    assertBooleanToStringFormula(true, "true", satisfiable);
    assertBooleanToStringFormula(true, "false", unsatisfiable);
    assertBooleanToStringFormula(false, "false", satisfiable);
    assertBooleanToStringFormula(false, "true", unsatisfiable);

    final IntegerFormula f1 = imgr.makeNumber(1);
    final IntegerFormula f2 = imgr.makeNumber(2);
    assertFunctionToStringFormula(f1, f1, satisfiable);
    // can not be distinguished yet
    // TODO the following asserts should actually be satisfiable_non_tautological
    assertFunctionToStringFormula(f1, "true", satisfiable);
    assertFunctionToStringFormula(f1, "false", satisfiable);
    assertFunctionToStringFormula(f1, f2, satisfiable);
    assertFunctionToStringFormula(f2, f1, satisfiable);

    assertNumberToStringFormula(fpmgr.makeNaN(Types.NUMBER_TYPE), "NaN", satisfiable);
    assertNumberToStringFormula(fpmgr.makePlusInfinity(Types.NUMBER_TYPE), "Infinity", satisfiable);
    assertNumberToStringFormula(
        fpmgr.makePlusInfinity(Types.NUMBER_TYPE), "-Infinity", unsatisfiable);
    assertNumberToStringFormula(
        fpmgr.makeMinusInfinity(Types.NUMBER_TYPE), "-Infinity", satisfiable);
    assertNumberToStringFormula(
        fpmgr.makeMinusInfinity(Types.NUMBER_TYPE), "Infinity", unsatisfiable);
    final FloatingPointFormula zero = fpmgr.makeNumber(0, Types.NUMBER_TYPE);
    assertNumberToStringFormula(zero, "0", satisfiable);
    assertNumberToStringFormula(zero, "0.0", unsatisfiable);

    final IntegerFormula o1 = imgr.makeNumber(1);
    final IntegerFormula o2 = imgr.makeNumber(2);
    assertObjectToStringFormula(o1, o1, satisfiable);
    // equality of two unknown strings is always satisfiable and non tautological
    // TODO the following asserts should actually be satisfiable_non_tautological
    assertObjectToStringFormula(o1, "[]", satisfiable);
    assertObjectToStringFormula(o1, "{}", satisfiable);
    assertObjectToStringFormula(o1, o2, satisfiable);
    assertObjectToStringFormula(o2, o1, satisfiable);

    assertStringToStringFormula("true", "true", satisfiable);
    assertStringToStringFormula("true", "false", unsatisfiable);
    assertStringToStringFormula("false", "false", satisfiable);
    assertStringToStringFormula("false", "true", unsatisfiable);

    assertUndefinedToStringFormula("undefined", satisfiable);
    assertUndefinedToStringFormula("false", unsatisfiable);
    assertUndefinedToStringFormula("", unsatisfiable);
  }

  @Nonnull
  private void assertBooleanToStringFormula(
      final boolean pInput, final String pOutput, final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final BooleanFormula inputFormula = bmgr.makeBoolean(pInput);
    final BooleanFormula valueFormula =
        mgrv.makeEqual(
            strMgr.getStringFormula(pOutput),
            valConvMgr.toStringFormula(tvmgr.createBooleanValue(inputFormula)));
    final BooleanFormula variableFormula =
        getVariableToStringFormula(
            typeTags.BOOLEAN, inputFormula, typedVarValues::booleanValue, pOutput);
    assertSatisfiability(valueFormula, pSatisfiability);
    assertSatisfiability(variableFormula, pSatisfiability);
  }

  @Nonnull
  private void assertFunctionToStringFormula(
      final IntegerFormula pFunctionObjectId,
      final String pOutput,
      @SuppressWarnings("SameParameterValue") final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final BooleanFormula valueFormula =
        mgrv.makeEqual(
            strMgr.getStringFormula(pOutput),
            valConvMgr.toStringFormula(tvmgr.createFunctionValue(pFunctionObjectId)));
    final BooleanFormula variableFormula =
        getVariableToStringFormula(
            typeTags.FUNCTION, pFunctionObjectId, typedVarValues::functionValue, pOutput);
    assertSatisfiability(valueFormula, pSatisfiability);
    assertSatisfiability(variableFormula, pSatisfiability);
  }

  @Nonnull
  private void assertFunctionToStringFormula(
      final IntegerFormula pFunctionObjectId1,
      final IntegerFormula pFunctionObjectId2,
      @SuppressWarnings("SameParameterValue") final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final IntegerFormula variable = imgrv.makeNumber(1);
    final IntegerFormula typeofVar = typedVarValues.typeof(variable);
    final Formula varValue = typedVarValues.functionValue(variable);
    final BooleanFormula booleanFormula =
        bmgr.and(
            mgrv.makeEqual(typeofVar, typeTags.FUNCTION),
            mgrv.makeEqual(varValue, pFunctionObjectId1),
            mgrv.makeEqual(
                valConvMgr.toStringFormula(tvmgr.createFunctionValue(pFunctionObjectId2)),
                valConvMgr.toStringFormula(new TypedValue(typeofVar, variable))));
    assertSatisfiability(booleanFormula, pSatisfiability);
  }

  @SuppressWarnings("unused")
  @Nonnull
  private void assertNumberToStringFormula(
      final FloatingPointFormula pInputFormula,
      final String pOutput,
      final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final BooleanFormula valueFormula =
        mgrv.makeEqual(
            strMgr.getStringFormula(pOutput),
            valConvMgr.toStringFormula(tvmgr.createNumberValue(pInputFormula)));
    final BooleanFormula variableFormula =
        getVariableToStringFormula(
            typeTags.NUMBER, pInputFormula, typedVarValues::numberValue, pOutput);
    assertSatisfiability(valueFormula, pSatisfiability);
    assertSatisfiability(variableFormula, pSatisfiability);
  }

  @Nonnull
  private void assertObjectToStringFormula(
      final IntegerFormula pObjectId,
      final String pOutput,
      @SuppressWarnings("SameParameterValue") final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final BooleanFormula valueFormula =
        mgrv.makeEqual(
            strMgr.getStringFormula(pOutput),
            valConvMgr.toStringFormula(tvmgr.createObjectValue(pObjectId)));
    final BooleanFormula variableFormula =
        getVariableToStringFormula(
            typeTags.FUNCTION, pObjectId, typedVarValues::objectValue, pOutput);
    assertSatisfiability(valueFormula, pSatisfiability);
    assertSatisfiability(variableFormula, pSatisfiability);
  }

  @Nonnull
  private void assertObjectToStringFormula(
      final IntegerFormula pObjectId1,
      final IntegerFormula pObjectId2,
      @SuppressWarnings("SameParameterValue") final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final IntegerFormula variable = imgrv.makeNumber(1);
    final IntegerFormula typeofVar = typedVarValues.typeof(variable);
    final Formula varValue = typedVarValues.objectValue(variable);
    final BooleanFormula booleanFormula =
        bmgr.and(
            mgrv.makeEqual(typeofVar, typeTags.FUNCTION),
            mgrv.makeEqual(varValue, pObjectId1),
            mgrv.makeEqual(
                valConvMgr.toStringFormula(tvmgr.createObjectValue(pObjectId2)),
                valConvMgr.toStringFormula(new TypedValue(typeofVar, variable))));
    assertSatisfiability(booleanFormula, pSatisfiability);
  }

  @Nonnull
  private void assertStringToStringFormula(
      final String pInput, final String pOutput, final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final FloatingPointFormula inputFormula = strMgr.getStringFormula(pInput);
    final BooleanFormula valueFormula =
        mgrv.makeEqual(
            strMgr.getStringFormula(pOutput),
            valConvMgr.toStringFormula(tvmgr.createStringValue(inputFormula)));
    final BooleanFormula variableFormula =
        getVariableToStringFormula(
            typeTags.STRING, inputFormula, typedVarValues::stringValue, pOutput);
    assertSatisfiability(valueFormula, pSatisfiability);
    assertSatisfiability(variableFormula, pSatisfiability);
  }

  @Nonnull
  private void assertUndefinedToStringFormula(
      final String pOutput, final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final BooleanFormula valueFormula =
        mgrv.makeEqual(
            strMgr.getStringFormula(pOutput),
            valConvMgr.toStringFormula(tvmgr.getUndefinedValue()));

    final IntegerFormula variable = imgrv.makeNumber(1);
    final IntegerFormula typeofVar = typedVarValues.typeof(variable);
    final BooleanFormula variableFormula =
        bmgr.and(
            mgrv.makeEqual(typeofVar, typeTags.UNDEFINED),
            mgrv.makeEqual(
                strMgr.getStringFormula(pOutput),
                valConvMgr.toStringFormula(new TypedValue(typeofVar, variable))));

    assertSatisfiability(valueFormula, pSatisfiability);
    assertSatisfiability(variableFormula, pSatisfiability);
  }

  @Nonnull
  private BooleanFormula getVariableToStringFormula(
      final IntegerFormula pTypeTag,
      final Formula pInput,
      final Function<IntegerFormula, Formula> getvalueOfVar,
      final String pOutput) {
    final IntegerFormula variable = imgrv.makeNumber(1);
    final IntegerFormula typeofVar = typedVarValues.typeof(variable);
    final Formula varValue = getvalueOfVar.apply(variable);
    return bmgr.and(
        mgrv.assignment(typeofVar, pTypeTag),
        mgrv.assignment(varValue, pInput),
        mgrv.makeEqual(
            strMgr.getStringFormula(pOutput),
            valConvMgr.toStringFormula(new TypedValue(typeofVar, variable))));
  }

  private void assertSatisfiability(
      final BooleanFormula pBooleanFormula, final Satisfiability pSatisfiability)
      throws SolverException, InterruptedException {
    final BooleanFormulaSubject booleanFormulaSubject = assertThatFormula(pBooleanFormula);
    switch (pSatisfiability) {
      case SATISFIABLE:
        booleanFormulaSubject.isSatisfiable();
        break;
      case SATISFIABLE_NON_TAUTOLOGICAL:
        booleanFormulaSubject.isSatisfiable();
        assertThatFormula(bmgr.not(pBooleanFormula)).isSatisfiable();
        break;
      case UNSATISFIABLE:
        booleanFormulaSubject.isUnsatisfiable();
        break;
    }
  }
}
