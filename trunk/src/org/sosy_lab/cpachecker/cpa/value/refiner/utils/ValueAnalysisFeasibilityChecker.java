// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.GenericFeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

public class ValueAnalysisFeasibilityChecker extends GenericFeasibilityChecker<ValueAnalysisState> {

  private final StrongestPostOperator<ValueAnalysisState> strongestPostOp;
  private final VariableTrackingPrecision precision;
  private final MachineModel machineModel;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   */
  public ValueAnalysisFeasibilityChecker(
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOp,
      final LogManager pLogger,
      final CFA pCfa,
      final Configuration config)
      throws InvalidConfigurationException {

    super(
        pStrongestPostOp,
        new ValueAnalysisState(pCfa.getMachineModel()),
        ValueAnalysisCPA.class,
        pLogger,
        config,
        pCfa);

    strongestPostOp = pStrongestPostOp;
    precision =
        VariableTrackingPrecision.createStaticPrecision(
            config, pCfa.getVarClassification(), ValueAnalysisCPA.class);
    machineModel = pCfa.getMachineModel();
  }

  public List<Pair<ValueAnalysisState, List<CFAEdge>>> evaluate(final ARGPath path)
      throws CPAException, InterruptedException {

    try {
      List<Pair<ValueAnalysisState, List<CFAEdge>>> reevaluatedPath = new ArrayList<>();
      ValueAnalysisState next = new ValueAnalysisState(machineModel);

      PathIterator iterator = path.fullPathIterator();
      while (iterator.hasNext()) {
        Optional<ValueAnalysisState> successor;
        CFAEdge outgoingEdge;
        List<CFAEdge> allOutgoingEdges = new ArrayList<>();
        do {
          outgoingEdge = iterator.getOutgoingEdge();
          allOutgoingEdges.add(outgoingEdge);
          successor = strongestPostOp.getStrongestPost(next, precision, outgoingEdge);
          iterator.advance();

          if (!successor.isPresent()) {
            return reevaluatedPath;
          }

          // extract singleton successor state
          next = successor.orElseThrow();
        } while (!iterator.isPositionWithState());

        reevaluatedPath.add(Pair.of(next, allOutgoingEdges));
      }

      return reevaluatedPath;
    } catch (CPATransferException e) {
      throw new CPAException(
          "Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }
}
