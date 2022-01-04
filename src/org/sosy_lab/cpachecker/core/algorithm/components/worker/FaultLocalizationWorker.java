// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.SelectorTraceWithKnownConditions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.OriginalMaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class FaultLocalizationWorker extends AnalysisWorker {

  private final FormulaContext context;
  private final TraceFormulaOptions options;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bmgr;

  private final List<CFAEdge> errorPath;

  FaultLocalizationWorker(
      String pId,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      SSAMap pTypeMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(pId, pBlock, pLogger, pCFA, pSpecification, pConfiguration, pShutdownManager, pTypeMap);
    context = new FormulaContext(
        backwardAnalysis.getSolver(),
        backwardAnalysis.getPathFormulaManager(),
        pCFA,
        pLogger,
        pConfiguration,
        pShutdownManager.getNotifier()
    );
    options = new TraceFormulaOptions(pConfiguration);
    fmgr = backwardAnalysis.getFmgr();
    bmgr = fmgr.getBooleanFormulaManager();
    errorPath = new ArrayList<>();
    earlyFalseResult = false;

    CFANode currNode = pBlock.getStartNode();
    do {
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currNode)) {
        if (block.getNodesInBlock().contains(leavingEdge.getSuccessor())) {
          errorPath.add(leavingEdge);
          currNode = leavingEdge.getSuccessor();
          break;
        }
      }
    } while (!currNode.equals(pBlock.getLastNode()));
  }

  @Override
  protected Collection<Message> backwardAnalysis(
      CFANode pStartNode, PathFormula pFormula)
      throws CPAException, InterruptedException, SolverException {
    Collection<Message> messages = super.backwardAnalysis(pStartNode, pFormula);
    if (messages.stream().anyMatch(m -> m.getType() == MessageType.ERROR_CONDITION)) {
      Optional<BooleanFormula> postCond = Optional.empty();
      if (pStartNode.equals(block.getLastNode())) {
        postCond = Optional.of(bmgr.not(pFormula.getFormula()));
      }
      SelectorTraceWithKnownConditions trace = new SelectorTraceWithKnownConditions(context, options, errorPath, getBooleanFormula(
          fmgr, backwardAnalysis.getPathFormulaManager(), receivedPreConditions).getFormula(), postCond);
      OriginalMaxSatAlgorithm algorithm = new OriginalMaxSatAlgorithm();
      Set<Fault> faults = algorithm.run(context, trace);
      if (!faults.isEmpty()) {
        Fault smallest = faults.stream().min(Comparator.comparingInt(f -> f.size())).orElseThrow();
        for (FaultContribution faultContribution : smallest) {
        }
      }
    }
    return messages;
  }
}
