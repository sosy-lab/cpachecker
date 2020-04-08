/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

public class LocationCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofCheckerCPA {

  private final LocationStateFactory stateFactory;

  private LocationCPA(LocationStateFactory pStateFactory) {
    super("sep", "sep", new LocationTransferRelation(pStateFactory));
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
    return stateFactory.getState(pNode);
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
}