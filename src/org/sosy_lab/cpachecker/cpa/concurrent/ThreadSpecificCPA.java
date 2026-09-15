// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ThreadSpecificCPA extends AbstractSingleWrapperCPA {

  public ThreadSpecificCPA(Configuration pConfig, CFA pCfa, LogManager pLogger)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        CompositeCPA.factory()
            .setConfiguration(Configuration.defaultConfiguration())
            .setChildren(
                List.of(LocationCPA.create(pCfa, pConfig), new CallstackCPA(pConfig, pLogger)))
            .set(pCfa, CFA.class)
            .createInstance());
  }
}
