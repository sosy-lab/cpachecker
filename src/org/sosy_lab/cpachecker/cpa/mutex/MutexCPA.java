// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

/**
 * CPA that tracks the state of mutexes in concurrent C programs, supporting both POSIX pthread
 * mutexes and C11 threading mutexes.
 *
 * <p>This CPA maintains a {@link MutexState} that records which mutexes have been initialized and
 * which are currently locked (and by which thread). It communicates with the POR CPA via the {@code
 * strengthen} operator to learn the executing thread's PID.
 */
public class MutexCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(MutexCPA.class);
  }

  @SuppressWarnings("unused")
  public MutexCPA() throws InvalidConfigurationException {
    super("sep", "sep", new MutexTransferRelation());
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return MutexState.EMPTY;
  }
}
