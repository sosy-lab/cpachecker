// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

/** Callstack CPA for the block analyses of distributed summary synthesis. */
public final class DssCallstackCPA extends CallstackCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DssCallstackCPA.class);
  }

  public DssCallstackCPA(Configuration pConfiguration, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfiguration, pLogger);
    if (getCallstackOptions().traverseBackwards()) {
      throw new InvalidConfigurationException("DssCallstackCPA only supports forward analyses");
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return createState(null, pNode.getFunctionName(), pNode, false);
  }

  /** Creates a DSS callstack state, optionally allowing every transfer. */
  public DssCallstackState createState(
      @Nullable CallstackState pPreviousState,
      String pFunction,
      CFANode pCallerNode,
      boolean pAllowAllTransfers) {
    CallstackState wrappedState =
        new CallstackState(DssCallstackState.unwrap(pPreviousState), pFunction, pCallerNode);
    return new DssCallstackState(wrappedState, pAllowAllTransfers);
  }

  @Override
  public DssCallstackTransferRelation getTransferRelation() {
    return new DssCallstackTransferRelation(getCallstackOptions(), getLogger());
  }
}
