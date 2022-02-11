// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings.EdgeTypeScoring;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaEntryList;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.Selector.Factory;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.SelectorTraceWithKnownConditions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.OriginalMaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class FaultLocalizationWorker extends AnalysisWorker {

  private final static String POSTCONDITION_KEY = "fl-post";
  private final FormulaContext context;
  private final TraceFormulaOptions options;
  private final BooleanFormulaManagerView bmgr;
  private final FormulaManagerView fmgr;
  private final PredicateCPA predicateCPA;
  private final List<CFAEdge> errorPath;
  private Fault minimalFault;
  private BooleanFormula actualPost;

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
    predicateCPA = backwardAnalysis.getDistributedCPA().getOriginalCPA(PredicateCPA.class);
    Configuration config = Configuration.builder().copyFrom(pConfiguration)
        .setOption("cpa.predicate.handlePointerAliasing", "false").build();
    if (predicateCPA == null) {
      throw new InvalidConfigurationException(
          this.getClass().getCanonicalName() + " needs PredicateCPA to be activated.");
    }
    context = new FormulaContext(
        predicateCPA.getSolver(),
        new PathFormulaManagerImpl(
            predicateCPA.getSolver().getFormulaManager(),
            config,
            pLogger,
            pShutdownManager.getNotifier(),
            pCFA,
            AnalysisDirection.BACKWARD),
        pCFA,
        pLogger,
        config,
        pShutdownManager.getNotifier()
    );
    options = new TraceFormulaOptions(config);
    fmgr = predicateCPA.getSolver().getFormulaManager();
    bmgr = fmgr.getBooleanFormulaManager();
    errorPath = new ArrayList<>();
    minimalFault = new Fault();

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
  protected Collection<Message> backwardAnalysis(MessageProcessing pMessageProcessing)
      throws CPAException, InterruptedException, SolverException {
    Set<Message> responses = new HashSet<>(super.backwardAnalysis(pMessageProcessing));
    Message currentResult = responses.stream().findFirst().orElseThrow();
    // super backward analysis ensures exactly one entry in pMessageProcessing
    Message message = pMessageProcessing.stream().findFirst().orElseThrow();
    try {
      DistributedPredicateCPA dpcpa = (DistributedPredicateCPA) backwardAnalysis.getDistributedCPA()
          .getDistributedAnalysis(PredicateCPA.class);
      if (message.getPayload().containsKey(POSTCONDITION_KEY)) {
        actualPost = dpcpa.getPathFormula(message.getPayload().get(POSTCONDITION_KEY)).getFormula();
      }
      PredicateAbstractState state = (PredicateAbstractState) dpcpa.deserialize(message);
      TraceFormula tf = createTraceFormula(state.getPathFormula());
      Set<Fault> faults = performFaultLocalization(tf);
      actualPost = actualPost == null ? tf.getPostConditionStatement() : actualPost;
      actualPost = fmgr.substitute(actualPost, dpcpa.getSubstitutions());
      Payload updated = Payload.builder().putAll(currentResult.getPayload())
          .addEntry(POSTCONDITION_KEY, fmgr.dumpFormula(actualPost).toString()).build();
      currentResult = Message.replacePayload(currentResult, updated);
      if (faults.isEmpty()) {
        return ImmutableSet.of(currentResult);
      }
      List<Fault> ranked = FaultRankingUtils.rank(new EdgeTypeScoring(), faults);
      // get the smallest fault with the highest score
      minimalFault = ranked.stream()
          .min(Comparator.comparingInt(Fault::size).thenComparingDouble(f -> 1d / f.getScore()))
          .orElseThrow();
      updated = Payload.builder().putAll(currentResult.getPayload())
          .addEntry(Payload.FAULT_LOCALIZATION, getBlockId() + ": " + minimalFault.toString())
          .build();
      currentResult = Message.replacePayload(currentResult, updated);
    } catch (IOException pE) {
      throw new CPAException("IO Exception", pE);
    } catch (TraceFormulaUnsatisfiableException pE) {
      // current block does most likely not lead to an error
      return responses;
    }
    return ImmutableSet.of(currentResult);
  }

  public Set<Fault> performFaultLocalization(TraceFormula tf)
      throws CPATransferException, InterruptedException, SolverException, IOException,
             TraceFormulaUnsatisfiableException {
    if (!tf.isCalculationPossible()) {
      return ImmutableSet.of();
    }
    return new OriginalMaxSatAlgorithm().run(context, tf);
  }

  private TraceFormula createTraceFormula(PathFormula pPostCondition)
      throws CPATransferException, InterruptedException, IOException, SolverException,
             TraceFormulaUnsatisfiableException {
    PathFormulaManagerImpl pathFormulaManager = context.getManager();
    FormulaEntryList entries = new FormulaEntryList();
    PathFormula current = pPostCondition;
    BooleanFormula oldFormula = current.getFormula();
    Factory selectorFactory = new Factory();
    int intId = 0;
    for (CFAEdge cfaEdge : errorPath) {
      current = pathFormulaManager.makeAnd(current, cfaEdge);
      if (current.getFormula().equals(oldFormula)) {
        continue;
      }
      BooleanFormula newFormula = current.getFormula();
      if (bmgr.isFalse(newFormula)) {
        throw new TraceFormulaUnsatisfiableException("The post-condition is not reachable");
      }
      List<BooleanFormula> parts = new ArrayList<>(bmgr.toConjunctionArgs(newFormula, false));
      BooleanFormula correctPart = null;
      if (parts.size() == 1 && bmgr.isTrue(oldFormula)) {
        correctPart = parts.get(0);
      }
      if (parts.size() == 2) {
        if (parts.get(0).equals(oldFormula)) {
          correctPart = parts.get(1);
        } else {
          correctPart = parts.get(0);
        }
      }
      if (correctPart == null) {
        throw new AssertionError(
            "Splitting a BooleanFormula has to result in exactly two formulas: " + parts);
      }
      entries.addEntry(intId, current.getSsa(),
          selectorFactory.makeSelector(context, correctPart, cfaEdge), correctPart);
      oldFormula = current.getFormula();
      intId++;
    }
    Map<String, Integer> minimalIndices = new HashMap<>();
    Map<String, BooleanFormula> minimalFormulas = new HashMap<>();
    try (ProverEnvironment prover = predicateCPA.getSolver()
        .newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(current.getFormula());
      if (prover.isUnsat()) {
        throw new TraceFormulaUnsatisfiableException(
            "The trace formula is unsatisfiable, the path cannot be traversed. "
                + current.getFormula());
      }
      for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
        Pair<String, OptionalInt> pair = FormulaManagerView.parseName(modelAssignment.getName());
        int newVal =
            minimalIndices.merge(pair.getFirst(), pair.getSecond().orElse(-2), Integer::max);
        if (newVal == pair.getSecond().orElse(-2)) {
          minimalFormulas.put(pair.getFirst(), modelAssignment.getAssignmentAsFormula());
        }
      }
    }
    BooleanFormula precondition = bmgr.and(minimalFormulas.values());
    return new SelectorTraceWithKnownConditions(context, options, entries, precondition,
        transformPostCondition(pPostCondition.getFormula()), errorPath);
  }

  private BooleanFormula transformPostCondition(BooleanFormula pPostCondition) {
    if (bmgr.isTrue(pPostCondition)) {
      return pPostCondition;
    }
    Set<BooleanFormula> postConds = new HashSet<>();
    if (actualPost != null) {
      postConds.addAll(bmgr.toConjunctionArgs(actualPost, true));
    }
    Set<BooleanFormula> negate = new HashSet<>();
    BooleanFormula formula = bmgr.makeTrue();
    for (BooleanFormula f : bmgr.toConjunctionArgs(pPostCondition, true)) {
      if (postConds.contains(f)) {
        negate.add(f);
      } else {
        formula = bmgr.and(formula, f);
      }
    }
    return bmgr.not(bmgr.and(formula, bmgr.not(bmgr.and(negate))));
  }

  private static class TraceFormulaUnsatisfiableException extends Exception {

    private static final long serialVersionUID = 428932L;

    public TraceFormulaUnsatisfiableException(String pMessage) {
      super(pMessage);
    }
  }
}
