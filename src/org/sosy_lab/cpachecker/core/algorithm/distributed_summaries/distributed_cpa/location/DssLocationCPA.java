// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.location.CachedLocationStateProvider;
import org.sosy_lab.cpachecker.cpa.location.DssLocationStateFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelation;

/** Forward location CPA with a lazy state cache for DSS block analyses. */
public final class DssLocationCPA extends AbstractCPA {

  private final CachedLocationStateProvider stateProvider;
  private final TransferRelation transferRelation;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DssLocationCPA.class);
  }

  private DssLocationCPA(CFA pCfa, Configuration pConfig) throws InvalidConfigurationException {
    super("sep", "sep", null);
    stateProvider = new DssLocationStateFactory(pCfa, pConfig);
    transferRelation = new LocationTransferRelation(stateProvider);
  }

  @Override
  public LocationState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return stateProvider.getState(pNode);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  public CachedLocationStateProvider getStateProvider() {
    return stateProvider;
  }
}
