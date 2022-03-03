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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings.EdgeTypeScoring;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaEntryList;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaEntryList.FormulaEntry;
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
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
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
  private final List<CFAEdge> errorPath;
  private Fault minimalFault;
  private BooleanFormula actualPost;

  private final StatTimer maxSatTimer;

  FaultLocalizationWorker(
      String pId,
      AnalysisOptions pOptions,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      UpdatedTypeMap pTypeMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(pId, pOptions, pBlock, pLogger, pCFA, pSpecification, pShutdownManager,
        pTypeMap);
    PredicateCPA predicateCPA = backwardAnalysis.getDistributedCPA().getOriginalCPA(PredicateCPA.class);
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

    // block.getEdgesInBlock does currently not guarantee correct order
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

    Collections.reverse(errorPath);

    maxSatTimer = new StatTimer("Max Sat Timer");
    stats.faultLocalizationTime.register(maxSatTimer);
  }

  @Override
  protected Collection<Message> backwardAnalysis(MessageProcessing pMessageProcessing)
      throws CPAException, InterruptedException, SolverException {
    Collection<Message> responses = super.backwardAnalysis(pMessageProcessing);
    assert responses.size() == 1 : "Every backward analysis must result in exactly one message: " + responses;
    Message currentResult = responses.stream().findFirst().orElseThrow();
    // early return for non error condition messages;
    if (currentResult.getType() != MessageType.ERROR_CONDITION) {
      return responses;
    }
    // super backward analysis ensures exactly one entry in pMessageProcessing
    try {
      maxSatTimer.start();
      Message message = pMessageProcessing.stream().findFirst().orElseThrow();
      DistributedPredicateCPA dpcpa = (DistributedPredicateCPA) backwardAnalysis.getDistributedCPA()
          .getDistributedAnalysis(PredicateCPA.class);
      if (message.getPayload().containsKey(POSTCONDITION_KEY)) {
        actualPost = dpcpa.getPathFormula(message.getPayload().get(POSTCONDITION_KEY)).getFormula();
      }
      PredicateAbstractState state = (PredicateAbstractState) dpcpa.deserialize(message);
      TraceFormula tf = createTraceFormula(state.getPathFormula());
      if (!tf.isCalculationPossible() && Boolean.parseBoolean(message.getPayload().get(Payload.FIRST))) {
        for (int i = tf.getEntries().size() - 1; i >= 0; i--) {
          FormulaEntry entry = tf.getEntries().get(i);
          if (entry.getSelector().correspondingEdge() instanceof AssumeEdge) {
            Payload updated = Payload.builder().putAll(currentResult.getPayload())
                .addEntry(POSTCONDITION_KEY, fmgr.dumpFormula(entry.getAtom()).toString()).build();
            currentResult = Message.replacePayload(currentResult, updated);
            return ImmutableSet.of(currentResult);
          }
        }
      }
      Set<Fault> faults = performFaultLocalization(tf);
      actualPost =
          (actualPost == null || dpcpa.getSolver().getFormulaManager().getBooleanFormulaManager()
              .isTrue(actualPost)) ? tf.getPostConditionStatement() : actualPost;
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
          .addEntry(Payload.FAULT_LOCALIZATION, getBlockId() + ": " + minimalFault)
          .build();
      currentResult = Message.replacePayload(currentResult, updated);
    } catch (TraceFormulaUnsatisfiableException pE) {
      // current block does most likely not lead to an error
      return responses;
    } finally {
      maxSatTimer.stop();
    }
    return ImmutableSet.of(currentResult);
  }

  public Set<Fault> performFaultLocalization(TraceFormula tf)
      throws CPATransferException, InterruptedException, SolverException {
    if (!tf.isCalculationPossible()) {
      return ImmutableSet.of();
    }
    return new OriginalMaxSatAlgorithm().run(context, tf);
  }

  private TraceFormula createTraceFormula(PathFormula pPostCondition)
      throws CPATransferException, InterruptedException, SolverException,
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
    BooleanFormula precondition = bmgr.makeTrue();
    if (!analysisOptions.isFlPreconditionAlwaysTrue()) {
      Map<String, Integer> minimalIndices = new HashMap<>();
      Map<String, BooleanFormula> minimalFormulas = new HashMap<>();
      try (ProverEnvironment prover = context.getSolver()
          .newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        prover.push(current.getFormula());
        if (prover.isUnsat()) {
          throw new TraceFormulaUnsatisfiableException(
              "The trace formula is unsatisfiable, the path cannot be traversed. "
                  + current.getFormula());
        }
        for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
          Pair<String, OptionalInt> pair = FormulaManagerView.parseName(modelAssignment.getName());
          if (pair.getFirst().contains(".")) {
            continue;
          }
          int newVal =
              minimalIndices.merge(pair.getFirst(), pair.getSecond().orElse(-2), Integer::max);
          if (newVal == pair.getSecond().orElse(-2)) {
            minimalFormulas.put(pair.getFirst(), modelAssignment.getAssignmentAsFormula());
          }
        }
      }
      precondition = bmgr.and(minimalFormulas.values());
    }
    return new SelectorTraceWithKnownConditions(context, options, entries, precondition,
        transformPostCondition(pPostCondition.getFormula()), errorPath);
  }

  private BooleanFormula transformPostCondition(BooleanFormula pPostCondition) {
    if (bmgr.isTrue(pPostCondition)) {
      return pPostCondition;
    }
    Set<BooleanFormula> postConds = ImmutableSet.of();
    if (actualPost != null) {
      postConds = ImmutableSet.copyOf(bmgr.toConjunctionArgs(actualPost, true));
    }
    ImmutableSet.Builder<BooleanFormula> negate = ImmutableSet.builder();
    BooleanFormula formula = bmgr.makeTrue();
    for (BooleanFormula f : bmgr.toConjunctionArgs(pPostCondition, true)) {
      if (postConds.contains(f)) {
        negate.add(f);
      } else {
        formula = bmgr.and(formula, f);
      }
    }
    return bmgr.not(bmgr.and(formula, bmgr.not(bmgr.and(negate.build()))));
  }

  private static class TraceFormulaUnsatisfiableException extends Exception {

    private static final long serialVersionUID = 428932L;

    public TraceFormulaUnsatisfiableException(String pMessage) {
      super(pMessage);
    }
  }
}
