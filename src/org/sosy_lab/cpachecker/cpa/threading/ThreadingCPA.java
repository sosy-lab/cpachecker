// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import com.google.common.base.Preconditions;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class ThreadingCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ThreadingCPA.class);
  }

  public ThreadingCPA(Configuration config, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException {
    super("sep", "sep", new ThreadingTransferRelation(config, pCfa, pLogger));
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    Preconditions.checkNotNull(pNode);
    // We create an empty ThreadingState and enter the main function with the first thread.
    // We use the main function's name as thread identifier.
    String mainThread = pNode.getFunctionName();
    return ((ThreadingTransferRelation) getTransferRelation())
        .addNewThread(new ThreadingState(), mainThread, ThreadingState.MIN_THREAD_NUM, mainThread);
  }
}
