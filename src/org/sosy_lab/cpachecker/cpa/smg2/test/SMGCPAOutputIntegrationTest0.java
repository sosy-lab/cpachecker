// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import java.io.IOException;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.cmdline.InvalidCmdlineArgumentException;

/**
 * Base class for SMG2 integration tests that inspect generated YAML output files.
 *
 * <p>Tests execute the required SMG2 witness configurations on both supported machine models.
 * Program paths may be relative to {@code test/programs/} or already include that prefix.
 */
public abstract class SMGCPAOutputIntegrationTest0 extends SMGCPAIntegrationTest0 {

  @Parameters(name = "CPA: {0} with specification: {1}")
  public static String[][] getAllConfigurationsAndSpecifications() {
    return new String[][] {
      {SMG_SYMBOLIC_EXECUTION, VALID_MEMSAFETY_PROPERTY},
      {SMG_SYMBOLIC_EXECUTION, SPECIFICATION_COMMON_PREFIX + MEMSAFETY_SPECIFICATION},
      {SVCOMP27, VALID_MEMSAFETY_PROPERTY}
    };
  }

  @Override
  protected Configuration buildConfiguration(String pProgramPath, MachineModel pMachineModel)
      throws IOException,
          InvalidConfigurationException,
          InvalidCmdlineArgumentException,
          InterruptedException {
    return CPAMain.createConfiguration(
            new String[] {
              "--config",
              CPA_CONFIG_COMMON_PREFIX + configToUse,
              "--spec",
              specToUse,
              pMachineModel == MachineModel.LINUX32 ? "--32" : "--64",
              "--option",
              "limits.time.cpu=30s",
              "--option",
              "counterexample.export.yaml=" + WitnessType.YML_VIOLATION,
              "--output-path",
              tempFolder.getRoot().getAbsolutePath(),
              pProgramPath,
            })
        .configuration();
  }
}
