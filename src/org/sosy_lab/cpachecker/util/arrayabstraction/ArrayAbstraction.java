// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;

public class ArrayAbstraction {

  private ArrayAbstraction() {}

  public static CFA transformCfa(Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    checkNotNull(pConfiguration);
    checkNotNull(pLogger);
    checkNotNull(pCfa);

    CFA simplifiedCfa =
        CfaSimplifications.simplifyArrayAccesses(
            pConfiguration, pLogger, pCfa, new VariableGenerator("__array_access_variable_"));
    simplifiedCfa =
        CfaSimplifications.simplifyPointerArrayAccesses(pConfiguration, pLogger, simplifiedCfa);
    simplifiedCfa =
        CfaSimplifications.simplifyIncDecLoopEdges(pConfiguration, pLogger, simplifiedCfa);

    return simplifiedCfa;
  }
}
