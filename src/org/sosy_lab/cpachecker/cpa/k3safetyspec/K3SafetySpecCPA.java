// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.k3safetyspec;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class K3SafetySpecCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM {

  private final CFA cfa;
  private final LogManager logger;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(K3SafetySpecCPA.class);
  }

  private K3SafetySpecCPA(CFA pCfa, LogManager pLogger) {
    super("sep", "sep", null);
    cfa = pCfa;
    logger = pLogger;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new K3SafetySpecTransferRelation(cfa, logger);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new K3SafetySpecState(ImmutableSet.of(), false);
  }
}
