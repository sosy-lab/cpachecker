// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestBase;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.InvalidTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NullTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DistributedFunctionPointerCPATest {

  @Test
  public void testFunctionPointerSerializationOnFile() throws Exception {

    Configuration config =
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    CFA cfa = TestUtil.buildTestCFA("test/programs/dss/simple-function-pointer.c");
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
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    CFA cfa = TestUtil.buildTestCFA("test/programs/dss/simple-function-pointer.c");
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
    builder.setTarget("tmp@1", new NamedFunctionTarget("f"));
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
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    CFA cfa = TestUtil.buildTestCFA("test/programs/dss/simple-function-pointer.c");
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
