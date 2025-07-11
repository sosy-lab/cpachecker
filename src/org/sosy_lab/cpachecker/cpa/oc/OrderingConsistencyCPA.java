// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class OrderingConsistencyCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(OrderingConsistencyCPA.class);
  }

  public OrderingConsistencyCPA(Configuration config, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException {
    super("sep", "sep", new OrderingConsistencyTransferRelation(config, pCfa, pLogger));
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return ((OrderingConsistencyTransferRelation) getTransferRelation()).initial();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return (state1,state2,precision) -> {
      if(state1 instanceof OrderingConsistencyState hbState1 &&
      state2 instanceof OrderingConsistencyState hbState2) {
        if(hbState1.canMerge(hbState2)) {
          return hbState1;
        }
      } else {
        throw new UnsupportedOperationException("OrderingConsistencyCPA does not support merge operators over non-OrderingConsistencyState");
      }
      return state2;
    };
  }
}
