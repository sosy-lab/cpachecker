// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestBase;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.ARGStateCombineViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.CombineCompositeStateViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.InvalidTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NullTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DistributedFunctionPointerCPATest {

  @Test
  public void combineOnlyEqualStatesIncludingExitHandlers() throws Exception {
    FunctionPointerStateCombinePreconditionsOperator operator =
        new FunctionPointerStateCombinePreconditionsOperator();
    FunctionPointerState empty = FunctionPointerState.createEmptyState();
    FunctionPointerState.Builder builder = empty.createBuilder();
    builder.setTarget("fp", new NamedFunctionTarget("f"));
    FunctionPointerState target = builder.build();
    assertThat(operator.combineIfPossible(ImmutableList.of(empty, target))).isEmpty();
    assertThat(operator.combineIfPossible(ImmutableList.of(target, target.createBuilder().build())))
        .hasValue(target);
    builder = target.createBuilder();
    builder.pushTarget(new NamedFunctionTarget("exitHandler"));
    assertThat(operator.combineIfPossible(ImmutableList.of(target, builder.build()))).isEmpty();
  }

  @Test
  public void violationCombinationPreservesIncompatibleFunctionPointerStates() throws Exception {
    var empty = FunctionPointerState.createEmptyState();
    var builder = empty.createBuilder();
    builder.setTarget("fp", new NamedFunctionTarget("f"));
    var target = builder.build();
    var pointer = mock(DistributedConfigurableProgramAnalysis.class);
    when(pointer.getCombineViolationConditionsOperator())
        .thenReturn(new FunctionPointerStateCombinePreconditionsOperator());
    var composite = mock(DistributedConfigurableProgramAnalysis.class);
    when(composite.getCombineViolationConditionsOperator())
        .thenReturn(
            new CombineCompositeStateViolationConditionOperator(
                ImmutableList.of(pointer), CFANode.newDummyCFANode()));
    var operator = new ARGStateCombineViolationConditionOperator(composite);
    var first = new ARGState(new CompositeState(ImmutableList.of(empty)), null);
    var second = new ARGState(new CompositeState(ImmutableList.of(target)), null);
    assertThat(operator.combineIfPossible(ImmutableList.of(first, second))).isEmpty();
    assertThat(operator.combineIfPossible(ImmutableList.of(second, second))).isPresent();
  }

  @Test
  public void testFunctionPointerSerializationOnFile() throws Exception {

    Configuration config =
        TestUtils.configurationForTest().loadFromFile(DssTestUtils.DSS_CONFIGURATION_FILE).build();
    CFA cfa = TestCfaUtils.makeCfaFromFile("test/programs/dss/simple-function-pointer.c");
    ConfigurableProgramAnalysis cpa =
        FunctionPointerCPA.factory()
            .setConfiguration(config)
            .set(cfa, CFA.class)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .createInstance();

    DistributedConfigurableProgramAnalysisTestBase.testSerialization(cfa, cpa);
  }

  @Test
  public void testAllCombinations() throws Exception {

    Configuration config =
        TestUtils.configurationForTest().loadFromFile(DssTestUtils.DSS_CONFIGURATION_FILE).build();
    CFA cfa = TestCfaUtils.makeCfaFromFile("test/programs/dss/simple-function-pointer.c");
    ConfigurableProgramAnalysis cpa =
        FunctionPointerCPA.factory()
            .setConfiguration(config)
            .set(cfa, CFA.class)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .createInstance();

    FunctionPointerState.Builder builder = FunctionPointerState.createEmptyState().createBuilder();
    builder.setTarget("fp_invalid", InvalidTarget.getInstance());
    builder.setTarget("fp_null", NullTarget.getInstance());
    builder.setTarget("fp_named", new NamedFunctionTarget("myFunction"));
    builder.setTarget("fp_overwritten", new NamedFunctionTarget("first"));
    builder.setTarget("fp_overwritten", new NamedFunctionTarget("second"));
    builder.setTarget("fp_removed", new NamedFunctionTarget("f"));
    builder.setTarget("fp_removed", UnknownTarget.getInstance());
    builder.pushTarget(new NamedFunctionTarget("atExitHandler1"));
    builder.pushTarget(InvalidTarget.getInstance());
    builder.pushTarget(builder.popTarget());

    DistributedConfigurableProgramAnalysisTestBase.checkSingleStateSerialization(
        cpa, builder.build(), cfa);
  }

  @Test
  public void testEmpty() throws Exception {

    Configuration config =
        TestUtils.configurationForTest().loadFromFile(DssTestUtils.DSS_CONFIGURATION_FILE).build();
    CFA cfa = TestCfaUtils.makeCfaFromFile("test/programs/dss/simple-function-pointer.c");
    ConfigurableProgramAnalysis cpa =
        FunctionPointerCPA.factory()
            .setConfiguration(config)
            .set(cfa, CFA.class)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .createInstance();

    DistributedConfigurableProgramAnalysisTestBase.checkSingleStateSerialization(
        cpa, FunctionPointerState.createEmptyState(), cfa);
  }
}
