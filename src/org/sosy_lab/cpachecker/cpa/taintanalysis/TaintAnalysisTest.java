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

  String CATEGORY_ARRAYS = "arrays/";
  String CATEGORY_GENERAL_FUNCTIONALITY = "general_functionality/";
  String CATEGORY_CAST = "cast/";
  String CATEGORY_CONTROL_FLOW_STRUCTURES = "control_flow_structures/";
  String CATEGORY_FUNCTION_CALLS = "function_calls/";
  String CATEGORY_POINTERS = "pointers/";
  String CATEGORY_STRUCTS_AND_MEMBER_ACCESS = "structs_and_member_access/";

  String SOURCE_CORE_MODELING = "core_modeling/";
  String SOURCE_DCEARA = "dceara/";
  String SOURCE_IFC_BENCH = "ifc-bench/";

  private TestResults runCPAchecker(String pProgramName, String specificDir) throws Exception {
    String fileName = "config/predicateAnalysis--informationFlow.properties";
    Configuration config = TestDataTools.configurationForTest().loadFromFile(fileName).build();

    String testDir = "test/programs/taint_analysis/" + specificDir + "/";
    Path programPath = Path.of(testDir, pProgramName);

    return CPATestRunner.run(config, programPath.toString(), Level.FINEST);
  }

  @Test
  public void testTaintByExternFunctionSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByExternFunctionSafe.c", CATEGORY_FUNCTION_CALLS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByExternFunctionUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByExternFunctionUnsafe.c", CATEGORY_FUNCTION_CALLS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Ignore
  @Test
  public void testTaintByANDLogicalOperationUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByANDLogicalOperationUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByAssignmentOperationSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Addition() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByAssignmentOperationUnsafe_Addition.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Division() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByAssignmentOperationUnsafe_Division.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Modulo() throws Exception {
    TestResults results =
        runCPAchecker("taintByAssignmentOperationUnsafe_Modulo.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Multiplication() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByAssignmentOperationUnsafe_Multiplication.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByAssignmentOperationUnsafe_Subtraction() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByAssignmentOperationUnsafe_Subtraction.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByBasicArithmeticOperationSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Addition() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByBasicArithmeticOperationUnsafe_Addition.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Division() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByBasicArithmeticOperationUnsafe_Division.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_MixedOp() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByBasicArithmeticOperationUnsafe_MixedOp.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Modulo() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByBasicArithmeticOperationUnsafe_Modulo.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Multiplication() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByBasicArithmeticOperationUnsafe_Multiplication.c",
            CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBasicArithmeticOperationUnsafe_Subtraction() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByBasicArithmeticOperationUnsafe_Subtraction.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByBitwiseOperationSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_AND() throws Exception {
    TestResults results =
        runCPAchecker("taintByBitwiseOperationUnsafe_AND.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_LeftShift() throws Exception {
    TestResults results =
        runCPAchecker("taintByBitwiseOperationUnsafe_LeftShift.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_OR() throws Exception {
    TestResults results =
        runCPAchecker("taintByBitwiseOperationUnsafe_OR.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_RightShift() throws Exception {
    TestResults results =
        runCPAchecker("taintByBitwiseOperationUnsafe_RightShift.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByBitwiseOperationUnsafe_XOR() throws Exception {
    TestResults results =
        runCPAchecker("taintByBitwiseOperationUnsafe_XOR.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByCommaOperatorSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByCommaOperatorSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByCommaOperatorUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("taintByCommaOperatorUnsafe_1.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByCommaOperatorUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker("taintByCommaOperatorUnsafe_2.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByCommaOperatorUnsafe_3() throws Exception {
    TestResults results =
        runCPAchecker("taintByCommaOperatorUnsafe_3.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByCommaOperatorUnsafe_4() throws Exception {
    TestResults results =
        runCPAchecker("taintByCommaOperatorUnsafe_4.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByCommaOperatorUnsafe_5() throws Exception {
    TestResults results =
        runCPAchecker("taintByCommaOperatorUnsafe_5.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByComparisonOperationSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_Equal() throws Exception {
    TestResults results =
        runCPAchecker("taintByComparisonOperationUnsafe_Equal.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_GreaterThan() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByComparisonOperationUnsafe_GreaterThan.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_GreaterThanOrEqual() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByComparisonOperationUnsafe_GreaterThanOrEqual.c",
            CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_LessThan() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByComparisonOperationUnsafe_LessThan.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_LessThanOrEqual() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByComparisonOperationUnsafe_LessThanOrEqual.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByComparisonOperationUnsafe_NotEqual() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByComparisonOperationUnsafe_NotEqual.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorSafe_implicitTaint() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorSafe_implicitTaint.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_implicitTaint() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorUnsafe_implicitTaint.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testNestedTernaryOperatorSafe_implicitTaint_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedTernaryOperatorSafe_implicitTaint_1.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testNestedTernaryOperatorUnsafe_implicitTaint_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedTernaryOperatorUnsafe_implicitTaint_1.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testNestedTernaryOperatorSafe_implicitTaint_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedTernaryOperatorSafe_implicitTaint_2.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testNestedTernaryOperatorUnsafe_implicitTaint_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedTernaryOperatorUnsafe_implicitTaint_2.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorSafe_bothBranchesReachable() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorSafe_bothBranchesReachable.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTernaryOperatorSafe_unreachableBranch_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorSafe_unreachableBranch_1.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTernaryOperatorSafe_unreachableBranch_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorSafe_unreachableBranch_2.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_unreachableBranch_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorUnsafe_unreachableBranch_1.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_unreachableBranch_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorUnsafe_unreachableBranch_2.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_bothBranchesReachable_1_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorUnsafe_bothBranchesReachable_1_1.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_bothBranchesReachable_1_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorUnsafe_bothBranchesReachable_1_2.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_bothBranchesReachable_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorUnsafe_bothBranchesReachable_2.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTernaryOperatorUnsafe_bothBranchesReachable_3() throws Exception {
    TestResults results =
        runCPAchecker(
            "ternaryOperatorUnsafe_bothBranchesReachable_3.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Ignore
  @Test
  public void testTaintByLogicalOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByLogicalOperationSafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByORLogicalOperationUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByORLogicalOperationUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByMemberAccessOperationsSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByMemberAccessOperationsSafe.c",
            CATEGORY_STRUCTS_AND_MEMBER_ACCESS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByMemberAccessOperationsUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByMemberAccessOperationsUnsafe_1.c",
            CATEGORY_STRUCTS_AND_MEMBER_ACCESS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByMemberAccessOperationsUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByMemberAccessOperationsUnsafe_2.c",
            CATEGORY_STRUCTS_AND_MEMBER_ACCESS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByMemberAccessOperationsUnsafe_3() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByMemberAccessOperationsUnsafe_3.c",
            CATEGORY_STRUCTS_AND_MEMBER_ACCESS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPointerAndAddressOperatorsSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByPointerAndAddressOperatorsSafe.c", CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByPointerAndAddressOperatorsUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByPointerAndAddressOperatorsUnsafe_1.c",
            CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPointerAndAddressOperatorsUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByPointerAndAddressOperatorsUnsafe_2.c",
            CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPointerAndAddressOperatorsUnsafe_3() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByPointerAndAddressOperatorsUnsafe_3.c",
            CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPointerAndAddressOperatorsUnsafe_4() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByPointerAndAddressOperatorsUnsafe_4.c",
            CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPointerAndAddressOperatorsUnsafe_5() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByPointerAndAddressOperatorsUnsafe_5.c",
            CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPostfixOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByPostfixOperationSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByPostfixOperationUnsafe_Addition() throws Exception {
    TestResults results =
        runCPAchecker("taintByPostfixOperationUnsafe_Addition.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPostfixOperationUnsafe_Subtraction() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintByPostfixOperationUnsafe_Subtraction.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPrefixOperationSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintByPrefixOperationSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testTaintByPrefixOperationUnsafe_Addition() throws Exception {
    TestResults results =
        runCPAchecker("taintByPrefixOperationUnsafe_Addition.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintByPrefixOperationUnsafe_Subtraction() throws Exception {
    TestResults results =
        runCPAchecker("taintByPrefixOperationUnsafe_Subtraction.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationSafe() throws Exception {
    TestResults results =
        runCPAchecker("arraySanitizationSafe.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testArraySanitizationUnsafe_1_1() throws Exception {
    TestResults results =
        runCPAchecker("arraySanitizationUnsafe_1_1.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationUnsafe_1_2() throws Exception {
    TestResults results =
        runCPAchecker("arraySanitizationUnsafe_1_2.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationUnsafe_2_1() throws Exception {
    TestResults results =
        runCPAchecker("arraySanitizationUnsafe_2_1.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testArraySanitizationUnsafe_2_2() throws Exception {
    TestResults results =
        runCPAchecker("arraySanitizationUnsafe_2_2.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testArrayTaintedBySetNotPublicSafe() throws Exception {
    TestResults results =
        runCPAchecker("arrayTaintedBySetNotPublicSafe.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testArrayTaintedBySetNotPublicUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("arrayTaintedBySetNotPublicUnsafe.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testConditionalDataflowSafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "conditionalDataflowSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testConditionalDataflowSafe_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "conditionalDataflowSafe_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testConditionalDataflowUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "conditionalDataflowUnsafe_1.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testConditionalDataflowUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "conditionalDataflowUnsafe_2.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testMultipleSanitizeMethodsSafe() throws Exception {
    TestResults results =
        runCPAchecker("multipleSanitizeMethodsSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testMultipleSanitizeMethodsUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("multipleSanitizeMethodsUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  // TODO: change name
  @Test
  public void testOverApproximationLeadsToFalsePositiveSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "overApproximationLeadsToFalsePositiveSafe.c",
            CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testOverApproximationLeadsToFalsePositiveUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "overApproximationLeadsToFalsePositiveUnsafe.c",
            CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPublicArraySafe() throws Exception {
    TestResults results =
        runCPAchecker("publicArraySafe.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testPublicArrayUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("publicArrayUnsafe.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testPublicVariableSafe() throws Exception {
    TestResults results = runCPAchecker("publicVariableSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testPublicVariableUnsafe() throws Exception {
    TestResults results = runCPAchecker("publicVariableUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testSanitizeContainedVariableButNotArraySafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "sanitizeContainedVariableButNotArraySafe.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testSanitizeContainedVariableButNotArrayUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "sanitizeContainedVariableButNotArrayUnsafe.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testSanitizeSecretVariableByOverrideSafe() throws Exception {
    TestResults results =
        runCPAchecker("sanitizeSecretVariableByOverrideSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testSanitizeSecretVariableByOverrideUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("sanitizeSecretVariableByOverrideUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testSecretVariableSafe() throws Exception {
    TestResults results = runCPAchecker("secretVariableSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testSecretVariableUnsafe() throws Exception {
    TestResults results = runCPAchecker("secretVariableUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testSetPublicVariableSecretSafe() throws Exception {
    TestResults results =
        runCPAchecker("setPublicVariableSecretSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testSetPublicVariableSecretUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("setPublicVariableSecretUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testSetSecretVariablePublicSafe() throws Exception {
    TestResults results =
        runCPAchecker("setSecretVariablePublicSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testSetSecretVariablePublicUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("setSecretVariablePublicUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testCastedExpressionSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintCastedExpressionSafe.c", CATEGORY_CAST + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTaintedArraySafe() throws Exception {
    TestResults results =
        runCPAchecker("taintedArraySafe.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTaintedArrayUnsafe_1_1() throws Exception {
    TestResults results =
        runCPAchecker("taintedArrayUnsafe_1_1.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_1_2() throws Exception {
    TestResults results =
        runCPAchecker("taintedArrayUnsafe_1_2.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_2_1() throws Exception {
    TestResults results =
        runCPAchecker("taintedArrayUnsafe_2_1.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_2_2() throws Exception {
    TestResults results =
        runCPAchecker("taintedArrayUnsafe_2_2.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_3_1() throws Exception {
    TestResults results =
        runCPAchecker("taintedArrayUnsafe_3_1.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_3_2() throws Exception {
    TestResults results =
        runCPAchecker("taintedArrayUnsafe_3_2.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_4() throws Exception {
    TestResults results =
        runCPAchecker("taintedArrayUnsafe_4.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintedArrayUnsafe_5() throws Exception {
    TestResults results =
        runCPAchecker("taintedArrayUnsafe_5.c", CATEGORY_ARRAYS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintStructSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintStructSafe.c", CATEGORY_STRUCTS_AND_MEMBER_ACCESS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testUnreachableTaintSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "unreachableTaintSafe.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTaintFunctionCallSafe() throws Exception {
    TestResults results =
        runCPAchecker("taintFunctionCallSafe.c", CATEGORY_FUNCTION_CALLS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testAssignSafe() throws Exception {
    TestResults results =
        runCPAchecker("assignSafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSizeofAllignof() throws Exception {
    TestResults results =
        runCPAchecker("sizeof_allignof.c", CATEGORY_FUNCTION_CALLS + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfSafe_2() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfSafe_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfSafe_3() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfSafe_3.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfUnsafe_3() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfUnsafe_3.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfSafe_4() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfSafe_4.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleIfUnsafe_4() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfUnsafe_4.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testInnerIfSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("innerIfSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testInnerIfUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("innerIfUnsafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_1_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfUnsafe_1_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_1_2() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfUnsafe_1_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_1_3() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfUnsafe_1_3.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_2_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfUnsafe_2_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_2_2() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfUnsafe_2_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleIfUnsafe_2_3() throws Exception {
    TestResults results =
        runCPAchecker("simpleIfUnsafe_2_3.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("whileLoopSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopSafe_nested_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopSafe_nested_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopSafe_nested_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopSafe_nested_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopSafe_nested_3() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopSafe_nested_3.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopUnsafe_nested_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopUnsafe_nested_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopUnsafe_nested_2_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopUnsafe_nested_2_1.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopUnsafe_nested_2_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopUnsafe_nested_2_2.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopUnsafe_nested_2_3() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopUnsafe_nested_2_3.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopUnsafe_nested_3() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopUnsafe_nested_3.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testWhileLoopUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopUnsafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleWhileLoopSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleWhileLoopSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleWhileLoopUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "simpleWhileLoopUnsafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleWhileLoopSafe_2() throws Exception {
    TestResults results =
        runCPAchecker("simpleWhileLoopSafe_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleWhileLoopUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "simpleWhileLoopUnsafe_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleWhileLoopSafe_3() throws Exception {
    TestResults results =
        runCPAchecker("simpleWhileLoopSafe_3.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleWhileLoopUnsafe_3() throws Exception {
    TestResults results =
        runCPAchecker(
            "simpleWhileLoopUnsafe_3.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleWhileLoopSafe_4() throws Exception {
    TestResults results =
        runCPAchecker("simpleWhileLoopSafe_4.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleWhileLoopUnsafe_4() throws Exception {
    TestResults results =
        runCPAchecker(
            "simpleWhileLoopUnsafe_4.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testInnerWhileLoopSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("innerWhileLoopSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testInnerWhileLoopUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("innerWhileLoopUnsafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testInnerWhileLoopUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker("innerWhileLoopUnsafe_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testTrippleWhileLoopSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("trippleWhileLoopSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testTrippleWhileLoopUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "trippleWhileLoopUnsafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testCondTaintNotPropagatedSafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "condTaintNotPropagatedSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopPostTaintSafe() throws Exception {
    TestResults results =
        runCPAchecker("whileLoopPostTaintSafe.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testWhileLoopPostTaintUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "whileLoopPostTaintUnsafe.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testNestedIfWhileSafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedIfWhileSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testNestedIfWhileUnsafe_1_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedIfWhileUnsafe_1_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testNestedIfWhileUnsafe_1_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedIfWhileUnsafe_1_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testNestedIfWhileSafe_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedIfWhileSafe_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testNestedIfWhileUnsafe_2_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedIfWhileUnsafe_2_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testNestedIfWhileUnsafe_2_2() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedIfWhileUnsafe_2_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleForLoopSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleForLoopSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleForLoopUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleForLoopUnsafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testNestedForLoopSafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedForLoopSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testNestedForLoopUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "nestedForLoopUnsafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleCondForLoopSafe() throws Exception {
    TestResults results =
        runCPAchecker("simpleCondForLoopSafe.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleCondForLoopUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "simpleCondForLoopUnsafe.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testAllLoopsSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("allLoopsSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testAllLoopsUnsafe_1_1() throws Exception {
    TestResults results =
        runCPAchecker("allLoopsUnsafe_1_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testAllLoopsUnsafe_1_2() throws Exception {
    TestResults results =
        runCPAchecker("allLoopsUnsafe_1_2.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleInterproceduralSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleInterproceduralSafe_1.c", CATEGORY_FUNCTION_CALLS + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleInterproceduralUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleInterproceduralUnsafe_1.c", CATEGORY_FUNCTION_CALLS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleInterproceduralSafe_2() throws Exception {
    TestResults results =
        runCPAchecker("simpleInterproceduralSafe_2.c", CATEGORY_FUNCTION_CALLS + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleInterproceduralUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker("simpleInterproceduralUnsafe_2.c", CATEGORY_FUNCTION_CALLS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleInterproceduralSafe_multipleFuncs() throws Exception {
    TestResults results =
        runCPAchecker(
            "simpleInterproceduralSafe_multipleFuncs.c", CATEGORY_FUNCTION_CALLS + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleInterproceduralUnsafe_multipleFuncs() throws Exception {
    TestResults results =
        runCPAchecker(
            "simpleInterproceduralUnsafe_multipleFuncs.c", CATEGORY_FUNCTION_CALLS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointerAssignmentSafe() throws Exception {
    TestResults results =
        runCPAchecker("pointerAssignmentSafe.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testPointerAssignmentUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("pointerAssignmentUnsafe_1.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointerAssignmentUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker("pointerAssignmentUnsafe_2.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointerAssignmentUnsafe_3() throws Exception {
    TestResults results =
        runCPAchecker("pointerAssignmentUnsafe_3.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointerAssignmentUnsafe_4() throws Exception {
    TestResults results =
        runCPAchecker("pointerAssignmentUnsafe_4.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Ignore
  @Test
  public void testSimpleCastSafe_1() throws Exception {
    TestResults results = runCPAchecker("simpleCastSafe_1.c", CATEGORY_CAST + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleCastSafe_2() throws Exception {
    TestResults results = runCPAchecker("simpleCastSafe_2.c", CATEGORY_CAST + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testSimpleCastSafe_3() throws Exception {
    TestResults results = runCPAchecker("simpleCastSafe_3.c", CATEGORY_CAST + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleCastSafe_2_extension() throws Exception {
    TestResults results =
        runCPAchecker("simpleCastSafe_2_extension.c", CATEGORY_CAST + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testPointersAsParamSimpleSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("pointersAsParamSimpleSafe_1.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_1() throws Exception {
    TestResults results =
        runCPAchecker("pointersAsParamSimpleUnsafe_1_1.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_2() throws Exception {
    TestResults results =
        runCPAchecker("pointersAsParamSimpleUnsafe_1_2.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_3() throws Exception {
    TestResults results =
        runCPAchecker("pointersAsParamSimpleUnsafe_1_3.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_4() throws Exception {
    TestResults results =
        runCPAchecker("pointersAsParamSimpleUnsafe_1_4.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_1_5() throws Exception {
    TestResults results =
        runCPAchecker("pointersAsParamSimpleUnsafe_1_5.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testPointersAsParamSimpleSafe_2() throws Exception {
    TestResults results =
        runCPAchecker("pointersAsParamSimpleSafe_2.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testPointersAsParamSimpleUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker("pointersAsParamSimpleUnsafe_2.c", CATEGORY_POINTERS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testGlobalsSimpleSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("globalsSimpleSafe_1.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testGlobalsSimpleUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("globalsSimpleUnsafe_1.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimplePrintingSafe() throws Exception {
    TestResults results =
        runCPAchecker("simplePrintingSafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testSimplePrintingUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("simplePrintingUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintArraySafe() throws Exception {
    TestResults results = runCPAchecker("taintArraySafe.c", CATEGORY_ARRAYS + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testTaintArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("taintArrayUnsafe.c", CATEGORY_ARRAYS + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimplePrepareSliceSafe_2() throws Exception {
    TestResults results =
        runCPAchecker("simplePrepareSliceSafe_2.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testFunctionConstraintsSimple() throws Exception {
    TestResults results =
        runCPAchecker(
            "functionConstraintsSimple.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testReadMetricsSafe_1() throws Exception {
    TestResults results =
        runCPAchecker(
            "readMetricsSafe_1.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testPaperExampleSafe() throws Exception {
    TestResults results =
        runCPAchecker("paperExampleSafe.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Test
  public void testPaperExampleUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("paperExampleUnsafe.c", CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Ignore
  @Test
  public void testSimpleBufferExampleSafe() throws Exception {
    TestResults results =
        runCPAchecker("simpleBufferExampleSafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testSimpleBufferExampleUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "simpleBufferExampleUnsafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_DCEARA);
    results.assertIsUnsafe();
  }

  @Test
  public void testReturnTaintMainSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("returnTaintMainSafe_1.c", CATEGORY_FUNCTION_CALLS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testReturnTaintMainUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("returnTaintMainUnsafe_1.c", CATEGORY_FUNCTION_CALLS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleTaintByPointerSafe_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleTaintByPointerSafe_1.c", CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testSimpleTaintByPointerUnsafe_1() throws Exception {
    TestResults results =
        runCPAchecker("simpleTaintByPointerUnsafe_1.c", CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testSimpleTaintByPointerUnsafe_2() throws Exception {
    TestResults results =
        runCPAchecker("simpleTaintByPointerUnsafe_2.c", CATEGORY_POINTERS + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintMainArgsSafe() throws Exception {
    TestResults results = runCPAchecker("taintMainArgsSafe.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsSafe();
  }

  @Test
  public void testTaintMainArgsUnsafe_1() throws Exception {
    TestResults results = runCPAchecker("taintMainArgsUnsafe_1.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintMainArgsUnsafe_2() throws Exception {
    TestResults results = runCPAchecker("taintMainArgsUnsafe_2.c", CATEGORY_GENERAL_FUNCTIONALITY);
    results.assertIsUnsafe();
  }

  @Test
  public void testTaintInUnreachableStatementSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintInUnreachableStatementSafe.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testTaintInUnreachableStatementUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "taintInUnreachableStatementUnsafe.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Ignore
  @Test
  public void testEntwurfSafe() throws Exception {
    TestResults results = runCPAchecker("entwurfSafe.c", "");
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testEntwurfUnsafe() throws Exception {
    TestResults results = runCPAchecker("entwurfUnsafe.c", "");
    results.assertIsUnsafe();
  }

  @Test
  public void testImplicitDataFlowInIfStatementSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "implicitDataFlowInIfStatementSafe.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testImplicitDataFlowInIfStatementUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "implicitDataFlowInIfStatementUnsafe.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testSameAssignmentBothBranchesSafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "sameAssignmentBothBranchesSafe.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsSafe();
  }

  @Test
  public void testSameAssignmentBothBranchesUnsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "sameAssignmentBothBranchesUnsafe.c",
            CATEGORY_CONTROL_FLOW_STRUCTURES + SOURCE_CORE_MODELING);
    results.assertIsUnsafe();
  }

  @Test
  public void testFibSafe() throws Exception {
    TestResults results =
        runCPAchecker("fib_safe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsSafe();
  }

  @Test
  public void testFibUnsafe() throws Exception {
    TestResults results =
        runCPAchecker("fib_unsafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsUnsafe();
  }

  @Ignore
  @Test
  public void testModExp_safe() throws Exception {
    TestResults results =
        runCPAchecker(
            "modexp_safe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testModExp_unsafe() throws Exception {
    TestResults results =
        runCPAchecker("testModExp_unsafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsUnsafe();
  }

  @Test
  public void testModAdd_2048() throws Exception {
    TestResults results =
        runCPAchecker(
            "mod_add_2048.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsSafe();
  }

  @Test
  public void testModAdd_4096() throws Exception {
    TestResults results =
        runCPAchecker(
            "mod_add_4096.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testList_fakemalloc16() throws Exception {
    TestResults results =
        runCPAchecker(
            "list_fakemalloc16.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsSafe();
  }

  @Ignore
  @Test
  public void testPwdcheck2_safe() throws Exception {
    TestResults results =
        runCPAchecker(
            "pwdcheck2_safe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsSafe();
  }

  @Test
  public void testPwdcheck_safe() throws Exception {
    TestResults results =
        runCPAchecker(
            "pwdcheck_safe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsSafe();
  }

  @Test
  public void testPwdcheck_safe16() throws Exception {
    TestResults results =
        runCPAchecker(
            "pwdcheck_safe16.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsSafe();
  }

  @Test
  public void testPwdcheck_unsafe16() throws Exception {
    TestResults results =
        runCPAchecker(
            "pwdcheck_unsafe16.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsUnsafe();
  }

  @Test
  public void testPwdcheck_unsafe() throws Exception {
    TestResults results =
        runCPAchecker(
            "pwdcheck_unsafe.c", CATEGORY_GENERAL_FUNCTIONALITY + SOURCE_IFC_BENCH);
    results.assertIsUnsafe();
  }
}
