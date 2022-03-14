// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class LocationCPABackwards extends AbstractCPA {

  private final LocationStateFactory stateFactory;

  private LocationCPABackwards(LocationStateFactory pStateFactory) {
    super("sep", "sep", new LocationTransferRelationBackwards(pStateFactory));
    stateFactory = pStateFactory;
  }

  public static CPAFactory factory() {
    return new LocationCPAFactory(AnalysisDirection.BACKWARD);
  }

  public static LocationCPABackwards create(CFA pCFA, Configuration pConfig)
      throws InvalidConfigurationException {
    return new LocationCPABackwards(
        new LocationStateFactory(pCFA, AnalysisDirection.BACKWARD, pConfig));
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return stateFactory.getState(pNode);
  }
}
