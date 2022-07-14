// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.EdgeAnalyzer;

public class DataRaceCPA extends AbstractCPA {

  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DataRaceCPA.class);
  }

  private DataRaceCPA(CFA pCfa) {
    // TODO: Stop always is sound if DataRaceCPA is used together with ThreadingCPA (as it should)
    //       and prevents exploding reached set due to not stopping.
    //       However, we might be able to switch to stop sep if the tracked memory accesses in each
    //       DataRaceState are further reduced.
    super("sep", "always", null);
    cfa = pCfa;
  }

  @Override
  public TransferRelation getTransferRelation() {
    CompoundIntervalManagerFactory compoundIntervalManagerFactory =
        CompoundBitVectorIntervalManagerFactory.FORBID_SIGNED_WRAP_AROUND;
    EdgeAnalyzer edgeAnalyzer =
        new EdgeAnalyzer(compoundIntervalManagerFactory, cfa.getMachineModel());
    return new DataRaceTransferRelation(edgeAnalyzer);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new DataRaceState(
        ImmutableSet.of(),
        ImmutableMap.of("main", 0),
        ImmutableSet.of(),
        false);
  }
}
