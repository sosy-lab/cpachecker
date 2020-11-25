// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.bam;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;

@Options(prefix = "cpa.smg.bam")
public class BAMSMGCPA extends SMGCPA implements ConfigurableProgramAnalysisWithBAM {

  private final BAMSMGReducer reducer;

  protected BAMSMGCPA(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      CFA pCfa) throws InvalidConfigurationException{
    super(pConfig, pLogger, pShutdownNotifier, pCfa);
    reducer = new BAMSMGReducer();
  }

  @Override
  public Reducer getReducer() throws InvalidConfigurationException {
    return reducer;
  }
}
