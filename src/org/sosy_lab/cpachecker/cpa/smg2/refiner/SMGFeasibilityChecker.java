// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.GenericFeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

public class SMGFeasibilityChecker extends GenericFeasibilityChecker<SMGState> {

  private final StrongestPostOperator<SMGState> strongestPostOp;
  private final VariableTrackingPrecision precision;
  private final MachineModel machineModel;
  private final LogManager logger;
  private final Configuration config;
  private final CFA cfa;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   */
  public SMGFeasibilityChecker(
      final StrongestPostOperator<SMGState> pStrongestPostOp,
      final LogManager pLogger,
      final CFA pCfa,
      final Configuration pConfig)
      throws InvalidConfigurationException {

    super(
        pStrongestPostOp,
        SMGState.of(pCfa.getMachineModel(), pLogger, new SMGOptions(pConfig))
            .copyAndAddStackFrame(
                ((CFunctionEntryNode) pCfa.getMainFunction()).getFunctionDefinition()),
        SMGCPA.class,
        pLogger,
        pConfig,
        pCfa);

    cfa = pCfa;
    strongestPostOp = pStrongestPostOp;
    config = pConfig;
    precision =
        VariableTrackingPrecision.createStaticPrecision(
            config, pCfa.getVarClassification(), ValueAnalysisCPA.class);
    machineModel = pCfa.getMachineModel();
    logger = pLogger;
  }

  public List<Pair<SMGState, List<CFAEdge>>> evaluate(final ARGPath path)
      throws CPAException, InterruptedException {

    try {
      List<Pair<SMGState, List<CFAEdge>>> reevaluatedPath = new ArrayList<>();
      SMGState next = SMGState.of(machineModel, logger, new SMGOptions(config));

      if (cfa.getMainFunction() instanceof CFunctionEntryNode) {
        // Init main
        CFunctionEntryNode functionNode = (CFunctionEntryNode) cfa.getMainFunction();
        next = next.copyAndAddStackFrame(functionNode.getFunctionDefinition());
      }

      PathIterator iterator = path.fullPathIterator();
      while (iterator.hasNext()) {
        Optional<SMGState> successor;
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
    } catch (CPATransferException | InvalidConfigurationException e) {
      throw new CPAException(
          "Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }
}
