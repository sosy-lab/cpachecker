// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalSerializationInformation;

public class LocationCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofCheckerCPA {

  private final LocationStateFactory stateFactory;

  private LocationCPA(LocationStateFactory pStateFactory) {
    super("sep", "sep", new LocationTransferRelation(pStateFactory));
    stateFactory = pStateFactory;

    GlobalSerializationInformation.getWrappedInstance()
        .flatMap(GlobalSerializationInformation::getCFAInfo)
        .ifPresent(cfaInfo -> cfaInfo.storeLocationStateFactory(stateFactory));
  }

  public static CPAFactory factory() {
    return new LocationCPAFactory(AnalysisDirection.FORWARD);
  }

  public static LocationCPA create(CFA pCFA, Configuration pConfig)
      throws InvalidConfigurationException {
    return new LocationCPA(new LocationStateFactory(pCFA, AnalysisDirection.FORWARD, pConfig));
  }

  @Override
  public LocationState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return stateFactory.getState(pNode);
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    ImmutableSet<? extends AbstractState> successors = ImmutableSet.copyOf(pSuccessors);
    ImmutableSet<? extends AbstractState> actualSuccessors =
        ImmutableSet.copyOf(
            getTransferRelation()
                .getAbstractSuccessorsForEdge(
                    pElement, SingletonPrecision.getInstance(), pCfaEdge));
    return successors.equals(actualSuccessors);
  }
}
