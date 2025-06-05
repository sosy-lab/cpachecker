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
  public void testTaintByAssignmentOperationUnsafe_Subtraction() throws Exception {
    TestResults results =
        runCPAchecker("taintByAssignmentOperationUnsafe_Subtraction.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Addition() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationUnsafe_Addition.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Division() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationUnsafe_Division.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_MixedOp() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationUnsafe_MixedOp.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Modulo() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationUnsafe_Modulo.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Multiplication() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByBasicArithmeticOperationUnsafe_Multiplication.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Subtraction() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationUnsafe_Subtraction.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationSafe() throws Exception {
    TestResults results = runCPAchecker("taintByBitwiseOperationSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_AND() throws Exception {
    TestResults results = runCPAchecker("taintByBitwiseOperationUnsafe_AND.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_LeftShift() throws Exception {
    TestResults results =
        runCPAchecker("taintByBitwiseOperationUnsafe_LeftShift.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_OR() throws Exception {
    TestResults results = runCPAchecker("taintByBitwiseOperationUnsafe_OR.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_RightShift() throws Exception {
    TestResults results =
        runCPAchecker("taintByBitwiseOperationUnsafe_RightShift.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_XOR() throws Exception {
    TestResults results = runCPAchecker("taintByBitwiseOperationUnsafe_XOR.c", "c_infix_operators");
    results.assertIsUnsafe();
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
  public void testTernaryOperatorSafe_bothBranchesReachable() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorSafe_bothBranchesReachable.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTernaryOperatorSafe_unreachableBranch_1() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorSafe_unreachableBranch_1.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTernaryOperatorSafe_unreachableBranch_2() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorSafe_unreachableBranch_2.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_unreachableBranch_1() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorUnsafe_unreachableBranch_1.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_unreachableBranch_2() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorUnsafe_unreachableBranch_2.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_bothBranchesReachable_1_1() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorUnsafe_bothBranchesReachable_1_1.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_bothBranchesReachable_1_2() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorUnsafe_bothBranchesReachable_1_2.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_bothBranchesReachable_2() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorUnsafe_bothBranchesReachable_2.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_bothBranchesReachable_3() throws Exception {
    TestResults results =
        runCPAchecker("ternaryOperatorUnsafe_bothBranchesReachable_3.c", "c_infix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByLogicalOperationSafe() throws Exception {
    TestResults results = runCPAchecker("taintByLogicalOperationSafe.c", "c_infix_operators");
    results.assertIsSafe();
  }

  @Test
  public void testTaintByORLogicalOperationUnsafe() throws Exception {
    TestResults results = runCPAchecker("taintByORLogicalOperationUnsafe.c", "c_infix_operators");
    results.assertIsUnsafe();
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
  public void testTaintByPostfixOperationUnsafe_Subtraction() throws Exception {
    TestResults results =
        runCPAchecker("taintByPostfixOperationUnsafe_Subtraction.c", "c_pre_and_postfix_operators");
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
  public void testTaintByPrefixOperationUnsafe_Subtraction() throws Exception {
    TestResults results =
        runCPAchecker("taintByPrefixOperationUnsafe_Subtraction.c", "c_pre_and_postfix_operators");
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationSafe() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testArraySanitizationUnsafe_1_1() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationUnsafe_1_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationUnsafe_1_2() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationUnsafe_1_2.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationUnsafe_2_1() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationUnsafe_2_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationUnsafe_2_2() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationUnsafe_2_2.c", "");
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
    TestResults results =
        runCPAchecker("overApproximationLeadsToFalsePositiveSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testOverApproximationLeadsToFalsePositiveUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("overApproximationLeadsToFalsePositiveUnsafe.c", "extern_benchmarks");
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

  @Test
  public void testCastedExpressionSafe() throws Exception {
    TestResults results = runCPAchecker("taintCastedExpressionSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testTaintedArraySafe() throws Exception {
    TestResults results = runCPAchecker("taintedArraySafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testTaintedArrayUnsafe_1_1() throws Exception {
    TestResults results = runCPAchecker("taintedArrayUnsafe_1_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_1_2() throws Exception {
    TestResults results = runCPAchecker("taintedArrayUnsafe_1_2.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_2_1() throws Exception {
    TestResults results = runCPAchecker("taintedArrayUnsafe_2_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_2_2() throws Exception {
    TestResults results = runCPAchecker("taintedArrayUnsafe_2_2.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_3_1() throws Exception {
    TestResults results = runCPAchecker("taintedArrayUnsafe_3_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_3_2() throws Exception {
    TestResults results = runCPAchecker("taintedArrayUnsafe_3_2.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_4() throws Exception {
    TestResults results = runCPAchecker("taintedArrayUnsafe_4.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_5() throws Exception {
    TestResults results = runCPAchecker("taintedArrayUnsafe_5.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintStructSafe() throws Exception {
    TestResults results = runCPAchecker("taintStructSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testUnreachableTaintSafe() throws Exception {
    TestResults results = runCPAchecker("unreachableTaintSafe.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testTaintFunctionCallSafe() throws Exception {
    TestResults results = runCPAchecker("taintFunctionCallSafe.c", "");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testAssignSafe() throws Exception {
    TestResults results = runCPAchecker("assignSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfSafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleIfSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfSafe_2() throws Exception {
    TestResults results = runCPAchecker("simpleIfSafe_2.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfSafe_3() throws Exception {
    TestResults results = runCPAchecker("simpleIfSafe_3.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfUnsafe_3() throws Exception {
    TestResults results = runCPAchecker("simpleIfUnsafe_3.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfSafe_4() throws Exception {
    TestResults results = runCPAchecker("simpleIfSafe_4.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfUnsafe_4() throws Exception {
    TestResults results = runCPAchecker("simpleIfUnsafe_4.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testInnerIfSafe_1() throws Exception {
    TestResults results = runCPAchecker("innerIfSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testInnerIfUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("innerIfUnsafe_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_1_1() throws Exception {
    TestResults results = runCPAchecker("simpleIfUnsafe_1_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_1_2() throws Exception {
    TestResults results = runCPAchecker("simpleIfUnsafe_1_2.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_1_3() throws Exception {
    TestResults results = runCPAchecker("simpleIfUnsafe_1_3.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_2_1() throws Exception {
    TestResults results = runCPAchecker("simpleIfUnsafe_2_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_2_2() throws Exception {
    TestResults results = runCPAchecker("simpleIfUnsafe_2_2.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_2_3() throws Exception {
    TestResults results = runCPAchecker("simpleIfUnsafe_2_3.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopSafe_1() throws Exception {
    TestResults results = runCPAchecker("whileLoopSafe_1.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopSafe_nested_1() throws Exception {
    TestResults results = runCPAchecker("whileLoopSafe_nested_1.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopSafe_nested_2() throws Exception {
    TestResults results = runCPAchecker("whileLoopSafe_nested_2.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopSafe_nested_3() throws Exception {
    TestResults results = runCPAchecker("whileLoopSafe_nested_3.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopUnsafe_nested_1() throws Exception {
    TestResults results = runCPAchecker("whileLoopUnsafe_nested_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopUnsafe_nested_2() throws Exception {
    TestResults results = runCPAchecker("whileLoopUnsafe_nested_2.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopUnsafe_nested_3() throws Exception {
    TestResults results = runCPAchecker("whileLoopUnsafe_nested_3.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("whileLoopUnsafe_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleWhileLoopSafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleWhileLoopSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleWhileLoopUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleWhileLoopUnsafe_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleWhileLoopSafe_2() throws Exception {
    TestResults results = runCPAchecker("simpleWhileLoopSafe_2.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleWhileLoopUnsafe_2() throws Exception {
    TestResults results = runCPAchecker("simpleWhileLoopUnsafe_2.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleWhileLoopSafe_3() throws Exception {
    TestResults results = runCPAchecker("simpleWhileLoopSafe_3.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleWhileLoopUnsafe_3() throws Exception {
    TestResults results = runCPAchecker("simpleWhileLoopUnsafe_3.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleWhileLoopSafe_4() throws Exception {
    TestResults results = runCPAchecker("simpleWhileLoopSafe_4.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleWhileLoopUnsafe_4() throws Exception {
    TestResults results = runCPAchecker("simpleWhileLoopUnsafe_4.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testInnerWhileLoopSafe_1() throws Exception {
    TestResults results = runCPAchecker("innerWhileLoopSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testInnerWhileLoopUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("innerWhileLoopUnsafe_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testTrippleWhileLoopSafe_1() throws Exception {
    TestResults results = runCPAchecker("trippleWhileLoopSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testTrippleWhileLoopUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("trippleWhileLoopUnsafe_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testCondTaintNotPropagatedSafe_1() throws Exception {
    TestResults results = runCPAchecker("condTaintNotPropagatedSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopPostTaintSafe() throws Exception {
    TestResults results = runCPAchecker("whileLoopPostTaintSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopPostTaintUnsafe() throws Exception {
    TestResults results = runCPAchecker("whileLoopPostTaintUnsafe.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleForLoopSafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleForLoopSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleForLoopUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleForLoopUnsafe_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testNestedForLoopSafe_1() throws Exception {
    TestResults results = runCPAchecker("nestedForLoopSafe_1.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testNestedForLoopUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("nestedForLoopUnsafe_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleCondForLoopSafe() throws Exception {
    TestResults results = runCPAchecker("simpleCondForLoopSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleCondForLoopUnsafe() throws Exception {
    TestResults results = runCPAchecker("simpleCondForLoopUnsafe.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testAllLoopsSafe_1() throws Exception {
    TestResults results = runCPAchecker("allLoopsSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testAllLoopsUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("allLoopsUnsafe_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleInterproceduralSafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleInterproceduralSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleInterproceduralSafe_2() throws Exception {
    TestResults results = runCPAchecker("simpleInterproceduralSafe_2.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleInterproceduralSafe_multipleFuncs() throws Exception {
    TestResults results =
        runCPAchecker("simpleInterproceduralSafe_multipleFuncs.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testPointerAssignmentSafe() throws Exception {
    TestResults results = runCPAchecker("pointerAssignmentSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testPointerAssignmentUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("pointerAssignmentUnsafe_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testPointerAssignmentUnsafe_2() throws Exception {
    TestResults results = runCPAchecker("pointerAssignmentUnsafe_2.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testPointerAssignmentUnsafe_3() throws Exception {
    TestResults results = runCPAchecker("pointerAssignmentUnsafe_3.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testPointerAssignmentUnsafe_4() throws Exception {
    TestResults results = runCPAchecker("pointerAssignmentUnsafe_4.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleCastSafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleCastSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleCastSafe_2() throws Exception {
    TestResults results = runCPAchecker("simpleCastSafe_2.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testSimpleCastSafe_3() throws Exception {
    TestResults results = runCPAchecker("simpleCastSafe_3.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testSimpleRecursionSafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleRecursionSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testPointersAsParamSimpleSafe_1() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamSimpleSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_1() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamSimpleUnsafe_1_1.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_2() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamSimpleUnsafe_1_2.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_3() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamSimpleUnsafe_1_3.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_4() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamSimpleUnsafe_1_4.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_5() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamSimpleUnsafe_1_5.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleSafe_2() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamSimpleSafe_2.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_2() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamSimpleUnsafe_2.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testGlobalsSimpleSafe_1() throws Exception {
    TestResults results = runCPAchecker("globalsSimpleSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testGlobalsSimpleSafe_recursion() throws Exception {
    TestResults results = runCPAchecker("globalsSimpleSafe_recursion.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testPointersAsParamsRecursiveSafe_1() throws Exception {
    TestResults results = runCPAchecker("pointersAsParamsRecursiveSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimplePrintingSafe() throws Exception {
    TestResults results = runCPAchecker("simplePrintingSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimplePrintingUnsafe() throws Exception {
    TestResults results = runCPAchecker("simplePrintingUnsafe.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintArraySafe() throws Exception {
    TestResults results = runCPAchecker("taintArraySafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testFreePtrSafe() throws Exception {
    TestResults results = runCPAchecker("freePtrSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testFreePtrUnsafe() throws Exception {
    TestResults results = runCPAchecker("freePtrUnsafe.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Ignore
  @Test
  public void testSimpleInterProcPrintResultsSafe() throws Exception {
    TestResults results = runCPAchecker("simpleInterProcPrintResultsSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimplePrepareSliceSafe_2() throws Exception {
    TestResults results = runCPAchecker("simplePrepareSliceSafe_2.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testFunctionConstraintsSimple() throws Exception {
    TestResults results = runCPAchecker("functionConstraintsSimple.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testReadMetricsBasicRecursionSafe_1() throws Exception {
    TestResults results = runCPAchecker("readMetricsBasicRecursionSafe_1.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testReadMetricsBasicRecursionSafe_2() throws Exception {
    TestResults results = runCPAchecker("readMetricsBasicRecursionSafe_2.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testPaperExampleSafe() throws Exception {
    TestResults results = runCPAchecker("paperExampleSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testPaperExampleUnsafe() throws Exception {
    TestResults results = runCPAchecker("paperExampleUnsafe.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleBufferExampleSafe() throws Exception {
    TestResults results = runCPAchecker("simpleBufferExampleSafe.c", "extern_benchmarks");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleBufferExampleUnsafe() throws Exception {
    TestResults results = runCPAchecker("simpleBufferExampleUnsafe.c", "extern_benchmarks");
    results.assertIsUnsafe();
  }

  @Test
  public void testReturnTaintMainSafe_1() throws Exception {
    TestResults results = runCPAchecker("returnTaintMainSafe_1.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testReturnTaintMainUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("returnTaintMainUnsafe_1.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleTaintByPointerSafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleTaintByPointerSafe_1.c", "");
    results.assertIsSafe();
  }

  @Test
  public void testSimpleTaintByPointerUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleTaintByPointerUnsafe_1.c", "");
    results.assertIsUnsafe();
  }
}
