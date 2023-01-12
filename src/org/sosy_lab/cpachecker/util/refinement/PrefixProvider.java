// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import java.util.List;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface PrefixProvider {

  List<InfeasiblePrefix> extractInfeasiblePrefixes(ARGPath path)
      throws CPAException, InterruptedException;

  interface Factory {
    PrefixProvider create(ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException;
  }
}
