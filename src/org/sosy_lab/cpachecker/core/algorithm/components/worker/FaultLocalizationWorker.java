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
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaEntryList;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.Selector.Factory;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.SelectorTraceWithKnownConditions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.OriginalMaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
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

  public Set<Fault> performFaultLocalization(Optional<BooleanFormula> pPostCondition)
      throws CPATransferException, InterruptedException, SolverException {
    return new OriginalMaxSatAlgorithm().run(context, createTraceFormula(pPostCondition));
  }

  public TraceFormula createTraceFormula(Optional<BooleanFormula> pPostCondition) throws CPATransferException, InterruptedException {
    PathFormulaManagerImpl pathFormulaManager = context.getManager();
    PathFormula precondition = getBooleanFormula(fmgr, pathFormulaManager, receivedPostConditions);
    FormulaEntryList entries = new FormulaEntryList();
    PathFormula current = precondition;
    BooleanFormula oldFormula = current.getFormula();
    Factory selectorFactory = new Factory();
    int id = 0;
    for (CFAEdge cfaEdge : errorPath) {
      current = pathFormulaManager.makeAnd(current, cfaEdge);
      if (current.getFormula().equals(oldFormula)) {
        continue;
      }
      id++;
      BooleanFormula newFormula = current.getFormula();
      List<BooleanFormula> parts = new ArrayList<>(bmgr.toConjunctionArgs(newFormula, false));
      if (parts.size() != 2) {
        throw new AssertionError("Splitting a BooleanFormula has to result in exactly two formulas: " + parts);
      }
      BooleanFormula correctPart;
      if (parts.get(0).equals(oldFormula)) {
        correctPart = parts.get(1);
      } else {
        correctPart = parts.get(0);
      }
      entries.addEntry(id, current.getSsa(), selectorFactory.makeSelector(context, correctPart, cfaEdge), correctPart);
      oldFormula = current.getFormula();
    }
    Optional<BooleanFormula> postCondition = Optional.empty();
    if (pPostCondition.isPresent()) {
      postCondition = Optional.of(fmgr.instantiate(fmgr.uninstantiate(pPostCondition.orElseThrow()), current.getSsa()));
    }
    return new SelectorTraceWithKnownConditions(context, options, entries, precondition.getFormula(), postCondition, errorPath);
  }
}
