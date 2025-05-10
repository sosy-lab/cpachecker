// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import java.nio.file.Path;
import java.util.logging.Level;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class TaintAnalysisTest {

  private TestResults runCPAchecker(String pProgramName, String specificDir) throws Exception {
    String fileName = "config/predicateAnalysis--informationFlow.properties";
    Configuration config = TestDataTools.configurationForTest().loadFromFile(fileName).build();

    String testDir = "test/programs/taint_analysis/" + specificDir + "/";
    Path programPath = Path.of(testDir, pProgramName);

    return CPATestRunner.run(config, programPath.toString(), Level.FINEST);
  }

  @Test
  public void testTaintBySqrtSafe() throws Exception {
    TestResults results = runCPAchecker("taintBySqrtSafe.c", "c_function_notation_calls");
    results.assertIsSafe();
  }

  @Test
  public void testTaintBySqrtUnsafe() throws Exception {
    TestResults results = runCPAchecker("taintBySqrtUnsafe.c", "c_function_notation_calls");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByANDLogicalOperationUnsafe() throws Exception {
    TestResults results = runCPAchecker("taintByANDLogicalOperationUnsafe.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByORLogicalOperationUnsafe() throws Exception {
    TestResults results = runCPAchecker("taintByORLogicalOperationUnsafe.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationSafe() throws Exception {
    TestResults results = runCPAchecker("taintByAssignmentOperationSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Addition() throws Exception {
    TestResults results =
        runCPAchecker("taintByAssignmentOperationUnsafe_Addition.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Division() throws Exception {
    TestResults results =
        runCPAchecker("taintByAssignmentOperationUnsafe_Division.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Modulo() throws Exception {
    TestResults results =
        runCPAchecker("taintByAssignmentOperationUnsafe_Modulo.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Multiplication() throws Exception {
    TestResults results =
        runCPAchecker("taintByAssignmentOperationUnsafe_Multiplication.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Substraction() throws Exception {
    TestResults results =
        runCPAchecker("taintByAssignmentOperationUnsafe_Substraction.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationUnsafe.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationSafe() throws Exception {
    TestResults results = runCPAchecker("taintByBitwiseOperationSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByCommaOperatorSafe() throws Exception {
    TestResults results = runCPAchecker("taintByCommaOperatorSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByCommaOperatorUnsafe() throws Exception {
    TestResults results = runCPAchecker("taintByCommaOperatorUnsafe.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationSafe() throws Exception {
    TestResults results = runCPAchecker("taintByComparisonOperationSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_Equal() throws Exception {
    TestResults results =
        runCPAchecker("taintByComparisonOperationUnsafe_Equal.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_GreaterThan() throws Exception {
    TestResults results =
        runCPAchecker("taintByComparisonOperationUnsafe_GreaterThan.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_GreaterThanOrEqual() throws Exception {
    TestResults results =
        runCPAchecker("taintByComparisonOperationUnsafe_GreaterThanOrEqual.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_LessThan() throws Exception {
    TestResults results =
        runCPAchecker("taintByComparisonOperationUnsafe_LessThan.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_LessThanOrEqual() throws Exception {
    TestResults results =
        runCPAchecker("taintByComparisonOperationUnsafe_LessThanOrEqual.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_NotEqual() throws Exception {
    TestResults results =
        runCPAchecker("taintByComparisonOperationUnsafe_NotEqual.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByConditionalTernaryOperatorSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByConditionalTernaryOperatorSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByConditionalTernaryOperatorUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByConditionalTernaryOperatorUnsafe.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByLogicalOperationSafe() throws Exception {
    TestResults results = runCPAchecker("taintByLogicalOperationSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testTaintByMemberAccessOperationsSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByMemberAccessOperationsSafe.c", "c_member_access_operators");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testTaintByPointerAndAddressOperatorsSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByPointerAndAddressOperatorsSafe.c", "c_pointer_and_access_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByPostfixOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByPostfixOperationSafe.c", "c_pre_and_postfix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByPostfixOperationUnsafe_Addition() throws Exception {
    TestResults results =
        runCPAchecker("taintByPostfixOperationUnsafe_Addition.c", "c_pre_and_postfix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPostfixOperationUnsafe_Substraction() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByPostfixOperationUnsafe_Substraction.c", "c_pre_and_postfix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPrefixOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByPrefixOperationSafe.c", "c_pre_and_postfix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByPrefixOperationUnsafe_Addition() throws Exception {
    TestResults results =
        runCPAchecker("taintByPrefixOperationUnsafe_Addition.c", "c_pre_and_postfix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPrefixOperationUnsafe_Substraction() throws Exception {
    TestResults results =
        runCPAchecker("taintByPrefixOperationUnsafe_Substraction.c", "c_pre_and_postfix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationBySettingPublicSafe() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationBySettingPublicSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testArraySanitizationBySettingPublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationBySettingPublicUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testArrayTaintedBySetNotPublicSafe() throws Exception {
    TestResults results = runCPAchecker("arrayTaintedBySetNotPublicSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testArrayTaintedBySetNotPublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("arrayTaintedBySetNotPublicUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testConditionalDataflowSafe_1() throws Exception {
    TestResults results = runCPAchecker("conditionalDataflowSafe_1.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testConditionalDataflowSafe_2() throws Exception {
    TestResults results = runCPAchecker("conditionalDataflowSafe_2.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testConditionalDataflowUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("conditionalDataflowUnsafe_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testConditionalDataflowUnsafe_2() throws Exception {
    TestResults results = runCPAchecker("conditionalDataflowUnsafe_2.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testMultipleSanitizeMethodsSafe() throws Exception {
    TestResults results = runCPAchecker("multipleSanitizeMethodsSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testMultipleSanitizeMethodsUnsafe() throws Exception {
    TestResults results = runCPAchecker("multipleSanitizeMethodsUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testOverApproximationLeadsToFalsePositiveSafe() throws Exception {
    TestResults results = runCPAchecker("overApproximationLeadsToFalsePositiveSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testOverApproximationLeadsToFalsePositiveUnsafe() throws Exception {
    TestResults results = runCPAchecker("overApproximationLeadsToFalsePositiveUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testPublicArraySafe() throws Exception {
    TestResults results = runCPAchecker("publicArraySafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testPublicArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("publicArrayUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testPublicVariableSafe() throws Exception {
    TestResults results = runCPAchecker("publicVariableSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testPublicVariableUnsafe() throws Exception {
    TestResults results = runCPAchecker("publicVariableUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSanitizeContainedVariableButNotArraySafe() throws Exception {
    TestResults results = runCPAchecker("sanitizeContainedVariableButNotArraySafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testSanitizeContainedVariableButNotArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("sanitizeContainedVariableButNotArrayUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSanitizeSecretVariableByOverrideSafe() throws Exception {
    TestResults results = runCPAchecker("sanitizeSecretVariableByOverrideSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testSanitizeSecretVariableByOverrideUnsafe() throws Exception {
    TestResults results = runCPAchecker("sanitizeSecretVariableByOverrideUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSecretArraySafe() throws Exception {
    TestResults results = runCPAchecker("secretArraySafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testSecretArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("secretArrayUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSecretVariableSafe() throws Exception {
    TestResults results = runCPAchecker("secretVariableSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testSecretVariableUnsafe() throws Exception {
    TestResults results = runCPAchecker("secretVariableUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSetPublicVariableSecretSafe() throws Exception {
    TestResults results = runCPAchecker("setPublicVariableSecretSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testSetPublicVariableSecretUnsafe() throws Exception {
    TestResults results = runCPAchecker("setPublicVariableSecretUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSetSecretVariablePublicSafe() throws Exception {
    TestResults results = runCPAchecker("setSecretVariablePublicSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testSetSecretVariablePublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("setSecretVariablePublicUnsafe.c", "");
    results.assertIsUnsafe();
  }
}
