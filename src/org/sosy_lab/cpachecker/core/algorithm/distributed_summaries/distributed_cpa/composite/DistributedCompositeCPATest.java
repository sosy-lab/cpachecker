// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestBase;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DistributedCompositeCPATest {

  @Test
  public void test() throws Exception {

    // TODO find program which tests something interesting!!
    CFA cfa = TestUtil.buildTestCFA("test/programs/cfa-ast-relation/full-expression.c");

    Configuration config =
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    ConfigurableProgramAnalysis cpa1 =
        LocationCPA.factory()
            .setConfiguration(config)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .set(cfa, CFA.class)
            .createInstance();

    ConfigurableProgramAnalysis cpa2 =
        CallstackCPA.factory()
            .setConfiguration(config)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .createInstance();

    ConfigurableProgramAnalysis cpa =
        CompositeCPA.factory()
            .setConfiguration(config)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .set(cfa, CFA.class)
            .setChildren(ImmutableList.of(cpa1, cpa2))
            .createInstance();

    Precision prec =
        new CompositePrecision(
            ImmutableList.of(SingletonPrecision.getInstance(), SingletonPrecision.getInstance()));
    DistributedConfigurableProgramAnalysisTestBase.testSerialization(cfa, cpa, prec);
  }
}
