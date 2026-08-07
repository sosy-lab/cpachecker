// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

/**
 * This is a CPA whose goal is to find and inject ACSL invariants. For now this class is used for
 * testing and proof of concept.
 */
@SuppressWarnings("unused")
public class AcslCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  private final CFA cfa;
  private final LogManager logger;

  private AcslCPA(CFA pCFA, LogManager pLogManager, Configuration pConfig) {
    super("sep", "sep", null);
    this.cfa = pCFA;
    this.logger = pLogManager;
    System.out.println("AcslCPA created.");
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AcslCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return null;
  }
}
