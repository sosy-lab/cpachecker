// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Queues;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.automaton.VerificationTaskMetaData;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.Simplifier;

public class WitnessExporter {

  private static final class ProofInvariantProvider implements InvariantProvider {

    private final ExpressionTreeFactory<Object> factory;
    private final CFA cfa;

    public ProofInvariantProvider(CFA pCfa, ExpressionTreeFactory<Object> pFactory) {
      cfa = pCfa;
      factory = pFactory;
    }

    @Override
    public ExpressionTree<Object> provideInvariantFor(
        CFAEdge pEdge, Optional<? extends Collection<? extends ARGState>> pStates)
        throws InterruptedException {
      if (!pStates.isPresent()) {
        return ExpressionTrees.getTrue();
      }
      Set<ExpressionTree<Object>> stateInvariants = new LinkedHashSet<>();
      String functionName = pEdge.getSuccessor().getFunctionName();
      for (ARGState state : pStates.get()) {
        Set<ExpressionTree<Object>> approximations = new LinkedHashSet<>();
        for (ExpressionTreeReportingState etrs :
            AbstractStates.asIterable(state).filter(ExpressionTreeReportingState.class)) {
          approximations.add(
              etrs.getFormulaApproximation(
                  cfa.getFunctionHead(functionName), pEdge.getSuccessor()));
        }
        stateInvariants.add(factory.and(approximations));
      }
      return factory.or(stateInvariants);
    }
  }

  protected final WitnessOptions options;

  protected final CFA cfa;
  protected final LogManager logger;

  protected final ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
  protected final Simplifier<Object> simplifier = ExpressionTrees.newSimplifier(factory);

  protected final VerificationTaskMetaData verificationTaskMetaData;

  public WitnessExporter(
      final Configuration pConfig,
      final LogManager pLogger,
      final Specification pSpecification,
      final CFA pCFA)
      throws InvalidConfigurationException {
    Preconditions.checkNotNull(pConfig);
    options = new WitnessOptions();
    pConfig.inject(options);
    cfa = pCFA;
    logger = pLogger;
    verificationTaskMetaData = new VerificationTaskMetaData(pConfig, pSpecification);
  }

  public ProofInvariantProvider getProofInvariantProvider() {
    return new ProofInvariantProvider(cfa, factory);
  }

  public Witness generateErrorWitness(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      CounterexampleInfo pCounterExample)
      throws InterruptedException {

    String defaultFileName = getInitialFileName(pRootState);
    WitnessFactory writer =
        new WitnessFactory(
            options,
            cfa,
            logger,
            verificationTaskMetaData,
            factory,
            simplifier,
            defaultFileName,
            WitnessType.VIOLATION_WITNESS,
            InvariantProvider.TrueInvariantProvider.INSTANCE);
    return writer.produceWitness(
        pRootState,
        pIsRelevantState,
        pIsRelevantEdge,
        Predicates.alwaysFalse(),
        Optional.empty(),
        Optional.ofNullable(pCounterExample),
        GraphBuilder.ARG_PATH);
  }

  public Witness generateTerminationErrorWitness(
      final ARGState pRoot,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      final Predicate<? super ARGState> pIsCycleHead,
      final Function<? super ARGState, ExpressionTree<Object>> toQuasiInvariant)
      throws InterruptedException {
    String defaultFileName = getInitialFileName(pRoot);
    WitnessFactory writer =
        new WitnessFactory(
            options,
            cfa,
            logger,
            verificationTaskMetaData,
            factory,
            simplifier,
            defaultFileName,
            WitnessType.VIOLATION_WITNESS,
            InvariantProvider.TrueInvariantProvider.INSTANCE);
    return writer.produceWitness(
        pRoot,
        pIsRelevantState,
        pIsRelevantEdge,
        pIsCycleHead,
        Optional.of(toQuasiInvariant),
        Optional.empty(),
        GraphBuilder.ARG_PATH);
  }

  public Witness generateProofWitness(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      InvariantProvider pInvariantProvider)
      throws InterruptedException {

    Preconditions.checkNotNull(pRootState);
    Preconditions.checkNotNull(pIsRelevantState);
    Preconditions.checkNotNull(pIsRelevantEdge);
    Preconditions.checkNotNull(pInvariantProvider);

    String defaultFileName = getInitialFileName(pRootState);
    WitnessFactory writer =
        new WitnessFactory(
            options,
            cfa,
            logger,
            verificationTaskMetaData,
            factory,
            simplifier,
            defaultFileName,
            WitnessType.CORRECTNESS_WITNESS,
            pInvariantProvider);
    return writer.produceWitness(
        pRootState,
        pIsRelevantState,
        pIsRelevantEdge,
        Predicates.alwaysFalse(),
        Optional.empty(),
        Optional.empty(),
        GraphBuilder.CFA_FULL);
  }

  protected String getInitialFileName(ARGState pRootState) {
    Deque<CFANode> worklist = Queues.newArrayDeque(AbstractStates.extractLocations(pRootState));
    Set<CFANode> visited = new HashSet<>();
    while (!worklist.isEmpty()) {
      CFANode l = worklist.pop();
      visited.add(l);
      for (CFAEdge e : CFAUtils.leavingEdges(l)) {
        Set<FileLocation> fileLocations = CFAUtils.getFileLocationsFromCfaEdge(e);
        if (!fileLocations.isEmpty()) {
          return fileLocations.iterator().next().getFileName().toString();
        }
        if (!visited.contains(e.getSuccessor())) {
          worklist.push(e.getSuccessor());
        }
      }
    }

    throw new RuntimeException("Could not determine file name based on abstract state!");
  }
}
