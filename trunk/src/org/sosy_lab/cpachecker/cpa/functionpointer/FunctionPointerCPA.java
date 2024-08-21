// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.functionpointer;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;

public class FunctionPointerCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofCheckerCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(FunctionPointerCPA.class);
  }

  private FunctionPointerCPA(LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(
        "sep",
        "sep",
        DelegateAbstractDomain.<FunctionPointerState>getInstance(),
        new FunctionPointerTransferRelation(pLogger, pConfig));
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return FunctionPointerState.createEmptyState();
  }

  @Override
  public Reducer getReducer() {
    return new FunctionPointerReducer();
  }
}
