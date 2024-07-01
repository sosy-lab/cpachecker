// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.atexit;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;

/**
 * The atexit CPA
 *
 * <p>Keeps track of the functions that were registered with atexit() by storing them on a stack.
 * Once the program finishes, either by returning from the main function or by calling exit(), the
 * registered functions are called in reverse order of their registration.
 *
 * @see <a href="https://www.open-std.org/jtc1/sc22/wg14/www/docs/n1548.pdf">C11 standard (draft),
 *     §7.22.4.2 and §7.22.4.4</a>
 */
public class AtExitCPA extends AbstractCPA implements ProofCheckerCPA {
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AtExitCPA.class);
  }

  private AtExitCPA(LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(
        "sep",
        "never",
        DelegateAbstractDomain.<FunctionPointerState>getInstance(),
        new AtExitTransferRelation(pLogger, pConfig));
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return AtExitState.createEmptyState();
  }
}
