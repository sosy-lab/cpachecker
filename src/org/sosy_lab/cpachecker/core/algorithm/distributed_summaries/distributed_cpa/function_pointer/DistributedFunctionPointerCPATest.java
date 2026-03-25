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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestUtil;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DistributedFunctionPointerCPATest {

  @Test
  public void test() throws Exception {

    Configuration config =
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    ConfigurableProgramAnalysis cpa =
        FunctionPointerCPA.factory()
            .setConfiguration(config)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .createInstance();

    Precision prec = SingletonPrecision.getInstance(); // No relevant precision
    // TODO find program which tests something interesting!!
    DistributedConfigurableProgramAnalysisTestUtil.testSerialization(
        "test/programs/dss/simple-function-pointer.c", cpa, prec);
  }
}
