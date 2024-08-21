// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class DataRaceCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DataRaceCPA.class);
  }

  private DataRaceCPA() {
    // TODO: Stop always is sound if DataRaceCPA is used together with ThreadingCPA (as it should)
    //       and prevents exploding reached set due to not stopping.
    //       However, we might be able to switch to stop sep if the tracked memory accesses in each
    //       DataRaceState are further reduced.
    super("sep", "always", null);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new DataRaceTransferRelation();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new DataRaceState(ImmutableMap.of("main", new ThreadInfo("main", 0, true)), false);
  }
}
