// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import java.io.IOException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/** Base class for SMG2 integration tests that assert verification results. */
public abstract class SMGCPAResultIntegrationTest0 extends SMGCPAIntegrationTest0 {

  @Override
  protected Configuration buildConfiguration(String pProgramPath, MachineModel pMachineModel)
      throws IOException, InvalidConfigurationException {
    return TestUtils.configurationForTest()
        .loadFromFile(CPA_CONFIG_COMMON_PREFIX + configToUse)
        .setOption("analysis.machineModel", pMachineModel.toString())
        .setOption("language", Language.C.name())
        .setOption("specification", SPECIFICATION_COMMON_PREFIX + specToUse)
        .build();
  }
}
