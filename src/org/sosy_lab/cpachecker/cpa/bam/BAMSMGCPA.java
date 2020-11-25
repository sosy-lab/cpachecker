// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;

public class BAMSMGCPA extends SMGCPA implements ConfigurableProgramAnalysisWithBAM {
  protected BAMSMGCPA(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      CFA pCfa) throws InvalidConfigurationException{
    super(pConfig, pLogger, pShutdownNotifier, pCfa);
    reducer = new BAMReducer();
  }
  private final BAMReducer reducer;

  @Override
  public Reducer getReducer() throws InvalidConfigurationException {
    return reducer;
  }
}
