// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cmdline.CPAMain;

/**
 * Base class for SMG2 integration tests that inspect generated YAML output files.
 *
 * <p>Tests execute the required SMG2 witness configurations on both supported machine models.
 * Program paths may be relative to {@code test/programs/} or already include that prefix.
 */
public abstract class SMGCPAOutputIntegrationTest0 extends SMGCPAIntegrationTest0 {

  private static final String SMG_SYMBOLIC_EXECUTION = "smgSymbolicExecution.properties";
  private static final String SVCOMP27 = "svcomp27.properties";
  private static final String VALID_MEMSAFETY_PROPERTY = "config/properties/valid-memsafety.prp";
  private static final String MEMSAFETY_SPECIFICATION = "config/specification/memorysafety.spc";

  @Parameters(name = "CPA: {0} with specification: {1}")
  public static String[][] getAllConfigurationsAndSpecifications() {
    return new String[][] {
      {SMG_SYMBOLIC_EXECUTION, VALID_MEMSAFETY_PROPERTY},
      {SMG_SYMBOLIC_EXECUTION, MEMSAFETY_SPECIFICATION},
      {SVCOMP27, VALID_MEMSAFETY_PROPERTY}
    };
  }

  protected final ProgramSubject assertThatOutputILP32Program(String pPathToProgram)
      throws Exception {
    return assertThatOutputProgram(pPathToProgram, MachineModel.LINUX32);
  }

  protected final ProgramSubject assertThatOutputLP64Program(String pPathToProgram)
      throws Exception {
    return assertThatOutputProgram(pPathToProgram, MachineModel.LINUX64);
  }

  private ProgramSubject assertThatOutputProgram(String pPathToProgram, MachineModel pMachineModel)
      throws Exception {
    String programPath = addProgramPathPrefixIfNeeded(pPathToProgram);
    Configuration configuration =
        CPAMain.createConfiguration(
                new String[] {
                  "--config",
                  "config/" + configToUse,
                  "--spec",
                  specToUse,
                  pMachineModel == MachineModel.LINUX32 ? "--32" : "--64",
                  "--option",
                  "limits.time.cpu=30s",
                  "--option",
                  "counterexample.export.yaml=" + WitnessType.YML_VIOLATION,
                  "--output-path",
                  tempFolder.getRoot().getAbsolutePath(),
                  programPath,
                })
            .configuration();
    return ProgramSubject.assertUsing(configuration, tempFolder).that(programPath);
  }
}
