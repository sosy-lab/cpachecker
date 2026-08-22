// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

@Options(prefix = "cpa.datarace")
public class DataRaceCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DataRaceCPA.class);
  }

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "ALWAYS"},
      description =
          "which stop operator to use for DataRaceCPA. A DataRaceState carries history that no"
              + " other CPA of the composite tracks (the memory accesses of the running threads,"
              + " the synchronizes-with edges and the last lock releases), so with ALWAYS coverage"
              + " is decided by the other components alone: of two states that agree there but"
              + " have different access histories, the one reached later is dropped, and the"
              + " analysis can miss a data race that the dropped history would have exposed."
              + " Which of the two is reached first depends on the traversal order, so the result"
              + " is not stable either. SEP only covers a state by an equal one and is therefore"
              + " sound, but it enlarges the reached set considerably, which costs much more than"
              + " it gains when the goal is to prove a program race free. Use SEP where a missed"
              + " race is not acceptable, most notably when validating a violation witness that"
              + " claims a data race.")
  private String stopType = "ALWAYS";

  private DataRaceCPA(Configuration pConfig) throws InvalidConfigurationException {
    super("sep", "always", null);
    pConfig.inject(this);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
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
