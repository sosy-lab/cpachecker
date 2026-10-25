// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class DataRaceCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DataRaceCPA.class);
  }

  private final MachineModel machineModel;
  private final Set<String> addressedVariables;
  private final CBinaryExpressionBuilder expressionBuilder;

  private DataRaceCPA(CFA pCfa, LogManager pLogger) throws InvalidConfigurationException {
    // TODO: Stop ignores the tracked memory accesses (see getStopOperator), which is sound if
    //       DataRaceCPA is used together with ThreadingCPA (as it should)
    //       and prevents exploding reached set due to not stopping.
    //       However, we might be able to switch to stop sep if the tracked memory accesses in each
    //       DataRaceState are further reduced.
    super("sep", "always", null);
    machineModel = pCfa.getMachineModel();
    expressionBuilder = new CBinaryExpressionBuilder(machineModel, pLogger);
    addressedVariables =
        pCfa.getVarClassification()
            .orElseThrow(
                () ->
                    new InvalidConfigurationException(
                        "DataRaceCPA needs the variable classification of the CFA"))
            .getAddressedVariables();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new DataRaceTransferRelation(machineModel, addressedVariables, expressionBuilder);
  }

  @Override
  public StopOperator getStopOperator() {
    // A state with a data race must not be covered by (or merged into) one without, because the
    // specification only sees the data race in the next transfer.
    return (state, reached, precision) -> {
      boolean hasDataRace = ((DataRaceState) state).hasDataRace();
      return Iterables.any(
          reached, reachedState -> ((DataRaceState) reachedState).hasDataRace() == hasDataRace);
    };
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new DataRaceState(ImmutableMap.of("main", new ThreadInfo("main", 0, true)), false);
  }
}
