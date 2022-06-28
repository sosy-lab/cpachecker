// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class SplitValueAnalysisState extends ValueAnalysisState {

  private final List<Map<Integer, String>> valuesFromFiles;
  private final ValueAnalysisState oldState;
  private final ValueAnalysisTransferRelation transferRelation;
  private final VariableTrackingPrecision precision;
  private final CFAEdge edge;
  private final LogManagerWithoutDuplicates logger;
  private static final long serialVersionUID = -3152134551524554358L;

  public SplitValueAnalysisState(
      ValueAnalysisState pState,
      List<Map<Integer, String>> pValuesFromFile,
      ValueAnalysisTransferRelation pValueAnalysisTransferRelation,
      VariableTrackingPrecision pPrecision,
      CFAEdge pCfaEdgeFromInfo,
      LogManagerWithoutDuplicates pLogger) {
    super(pState);
    oldState = pState;
    this.valuesFromFiles = pValuesFromFile;
    transferRelation = pValueAnalysisTransferRelation;
    this.precision = pPrecision;
    this.edge = pCfaEdgeFromInfo;
    this.logger = pLogger;
  }

  public Collection<ValueAnalysisState> split() throws CPATransferException {

    if (this.valuesFromFiles.isEmpty()) {
      return Lists.newArrayList(ValueAnalysisState.copyOf(oldState));
    }

    List<ValueAnalysisState> splitStates = new ArrayList<>();
    for (Map<Integer, String> valuesMap : valuesFromFiles) {
      ValueAnalysisStateWithSavedValue newState =
          new ValueAnalysisStateWithSavedValue(ValueAnalysisState.copyOf(oldState), valuesMap);
      newState.copyCounter();
      try {
        Collection<ValueAnalysisState> newStateColl =
            transferRelation.getAbstractSuccessorsForEdge(newState, precision, edge);
        if (newStateColl.size() != 1) {
          throw new CPATransferException(
              "The transfer relation created more than one successor, although only one successor"
                  + " was expected!. Aborting.");
        }
        splitStates.add(new ArrayList<>(newStateColl).get(0));
      } catch (InterruptedException pE) {
        throw new CPATransferException("Execution is interrupted", pE);
      }
    }
    logger.log(Level.INFO, String.format("Split into %d states", splitStates.size()));
    return splitStates;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof SplitValueAnalysisState)) {
      return false;
    }
    if (!super.equals(pO)) {
      return false;
    }
    SplitValueAnalysisState that = (SplitValueAnalysisState) pO;
    return Objects.equals(valuesFromFiles, that.valuesFromFiles)
        && Objects.equals(oldState, that.oldState)
        && Objects.equals(transferRelation, that.transferRelation)
        && Objects.equals(precision, that.precision)
        && Objects.equals(edge, that.edge)
        && Objects.equals(logger, that.logger);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), valuesFromFiles, oldState, transferRelation, precision, edge, logger);
  }
}
