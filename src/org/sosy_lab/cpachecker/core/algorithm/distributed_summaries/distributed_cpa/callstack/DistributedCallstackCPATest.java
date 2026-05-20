// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestBase;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DistributedCallstackCPATest {

  @Test
  public void test() throws Exception {
    Configuration config =
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    ConfigurableProgramAnalysis cpa =
        CallstackCPA.factory()
            .setConfiguration(config)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .createInstance();

    // TODO find program which tests something interesting!!
    DistributedConfigurableProgramAnalysisTestBase.testSerialization(
        "test/programs/cfa-ast-relation/full-expression.c", cpa);
  }
}
