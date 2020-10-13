// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisTM;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

public class LocationCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofCheckerCPA, StatisticsProvider,
    ConfigurableProgramAnalysisTM {

  private final LocationStateFactory stateFactory;

  private LocationCPA(LocationStateFactory pStateFactory) {
    super("sep", "sep", new LocationTransferRelation(pStateFactory, new LocationStatistics()));
    stateFactory = pStateFactory;

    Optional<CFAInfo> cfaInfo = GlobalInfo.getInstance().getCFAInfo();
    if (cfaInfo.isPresent()) {
      cfaInfo.orElseThrow().storeLocationStateFactory(stateFactory);
    }
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
    return Iterables.getOnlyElement(stateFactory.getState(pNode));
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    ImmutableSet<? extends AbstractState> successors = ImmutableSet.copyOf(pSuccessors);
    ImmutableSet<? extends AbstractState> actualSuccessors =
        ImmutableSet.copyOf(
            getTransferRelation()
                .getAbstractSuccessorsForEdge(
                    pElement, SingletonPrecision.getInstance(), pCfaEdge));
    return successors.equals(actualSuccessors);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(((LocationTransferRelation) getTransferRelation()).getStatistics());
  }

  @Override
  public ApplyOperator getApplyOperator() {
    return new LocationApplyOperator();
  }
}