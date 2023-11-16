// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.logging.Level.WARNING;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

@Options(prefix = "terminationtoreach")
public class TerminationToReachStatistics implements Statistics {
  @Option(
      secure = true,
      name = "violation.witness",
      description = "Export termination counterexample to file as GraphML automaton ")
  @FileOption(Type.OUTPUT_FILE)
  private Path violationWitness = Path.of("nontermination_witness.graphml");

  @Option(
      secure = true,
      name = "violation.witness.dot",
      description = "Export termination counterexample to file as dot/graphviz automaton ")
  @FileOption(Type.OUTPUT_FILE)
  private Path violationWitnessDot = Path.of("nontermination_witness.dot");

  @Option(
      secure = true,
      name = "compressWitness",
      description = "compress the produced violation-witness automata using GZIP compression.")
  private boolean compressWitness = true;

  private final LogManager logger;

  private final WitnessExporter witnessExporter;
  private final LocationStateFactory locFac;
  private ImmutableSet<Loop> nonterminatingLoop = null;

  public TerminationToReachStatistics(
      Configuration pConfig, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = checkNotNull(pLogger);

    witnessExporter =
        new WitnessExporter(
            pConfig,
            pLogger,
            Specification.alwaysSatisfied()
                .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION)),
            pCFA);
    locFac = new LocationStateFactory(pCFA, AnalysisDirection.FORWARD, pConfig);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    if (pResult == Result.FALSE && (violationWitness != null || violationWitnessDot != null)) {
      Iterator<ARGState> violations =
          pReached.stream()
              .filter(AbstractStates::isTargetState)
              .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
              .filter(s -> s.getCounterexampleInformation().isPresent())
              .iterator();
      Preconditions.checkState(violations.hasNext());
      exportTrivialWitness((ARGState) pReached.getFirstState(), violations.next());
      Preconditions.checkState(!violations.hasNext());
    }
  }

  @Override
  public String getName() {
    return null;
  }

  void setNonterminatingLoop(ImmutableSet<Loop> pLoop) {
    checkState(nonterminatingLoop == null);
    checkState(pLoop != null);
    nonterminatingLoop = pLoop;
  }

  private void exportTrivialWitness(final ARGState root, final ARGState loopStart) {
    CounterexampleInfo cexInfo = loopStart.getCounterexampleInformation().orElseThrow();

    ARGState loopStartInCEX =
        new ARGState(AbstractStates.extractStateByType(loopStart, LocationState.class), null);

    ARGState newRoot = new ARGState(root.getWrappedState(), null);
    Collection<ARGState> cexStates =
        copyStem(cexInfo.getTargetPath(), newRoot, loopStart, loopStartInCEX);

    ExpressionTree<Object> quasiInvariant = ExpressionTrees.getTrue();

    Function<? super ARGState, ExpressionTree<Object>> provideQuasiInvariant =
        (ARGState argState) -> {
          if (Objects.equals(argState, loopStartInCEX)) {
            return quasiInvariant;
          }
          return ExpressionTrees.getTrue();
        };

    for (Loop loop : nonterminatingLoop) {
      cexStates.addAll(addCEXLoopingPartToARG(loopStartInCEX, loop));
    }

    Predicate<? super ARGState> relevantStates = Predicates.in(cexStates);

    try {
      final Witness witness =
          witnessExporter.generateTerminationErrorWitness(
              newRoot,
              relevantStates,
              BiPredicates.bothSatisfy(relevantStates),
              state -> Objects.equals(state, loopStartInCEX),
              provideQuasiInvariant);

      if (violationWitness != null) {
        WitnessToOutputFormatsUtils.writeWitness(
            violationWitness,
            compressWitness,
            pAppendable -> WitnessToOutputFormatsUtils.writeToGraphMl(witness, pAppendable),
            logger);
      }

      if (violationWitnessDot != null) {
        WitnessToOutputFormatsUtils.writeWitness(
            violationWitnessDot,
            compressWitness,
            pAppendable -> WitnessToOutputFormatsUtils.writeToDot(witness, pAppendable),
            logger);
      }
    } catch (InterruptedException e) {
      logger.logUserException(
          WARNING, e, "Could not export termination witness due to interruption");
    }
  }

  private Collection<ARGState> copyStem(
      final ARGPath pStem,
      final ARGState newRoot,
      final ARGState loopStart,
      final ARGState newLoopStart) {
    Collection<ARGState> newStates = new HashSet<>();
    boolean first = true;
    ARGState parent = newRoot;
    newStates.add(newRoot);

    for (ARGState state : pStem.asStatesList()) {
      if (first) {
        first = false;
        continue;
      }

      if (Objects.equals(state, loopStart)) {
        newLoopStart.addParent(parent);
        newStates.add(newLoopStart);
        break;
      }

      ARGState child = new ARGState(state.getWrappedState(), parent);
      parent = child;
      newStates.add(parent);
    }

    return newStates;
  }

  private Collection<ARGState> addCEXLoopingPartToARG(final ARGState pLoopEntry, Loop pLoop) {
    CFANode loc = AbstractStates.extractLocation(pLoopEntry);
    Preconditions.checkState(pLoop.getLoopHeads().contains(loc));

    Collection<ARGState> relevantARGStates = new HashSet<>();

    Map<CFANode, ARGState> nodeToARGState =
        Maps.newHashMapWithExpectedSize(pLoop.getLoopNodes().size());
    nodeToARGState.put(loc, pLoopEntry);
    Deque<CFANode> waitlist = new ArrayDeque<>();
    waitlist.push(loc);

    while (!waitlist.isEmpty()) {
      loc = waitlist.pop();
      ARGState pred = nodeToARGState.get(loc);
      assert pred != null;

      for (CFAEdge leave : CFAUtils.leavingEdges(loc)) {
        if (pLoop.getLoopNodes().contains(leave.getSuccessor())) {
          ARGState succ = nodeToARGState.get(leave.getSuccessor());
          if (succ == null) {
            succ = new ARGState(locFac.getState(leave.getSuccessor()), null);
            nodeToARGState.put(leave.getSuccessor(), succ);
            waitlist.push(leave.getSuccessor());
          }

          succ.addParent(pred);

        } else if (leave instanceof FunctionCallEdge && pred.getChildren().isEmpty()) {
          // function calls are not considered to be part of the loop
          CFANode locContinueLoop = ((FunctionCallEdge) leave).getReturnNode();
          Map<Pair<CFANode, CallstackState>, ARGState> contextToARGState = new HashMap<>();
          Pair<CFANode, CallstackState> context =
              Pair.of(
                  leave.getSuccessor(),
                  new CallstackState(
                      null, leave.getSuccessor().getFunctionName(), leave.getPredecessor()));
          Deque<Pair<CFANode, CallstackState>> waitlistFun = new ArrayDeque<>();
          waitlistFun.push(context);

          ARGState succFun = new ARGState(locFac.getState(leave.getSuccessor()), null);
          contextToARGState.put(context, succFun);

          succFun.addParent(pred);

          while (!waitlistFun.isEmpty()) {
            context = waitlistFun.pop();
            ARGState predFun = contextToARGState.get(context);
            assert predFun != null;

            for (CFAEdge leaveFun : CFAUtils.leavingEdges(context.getFirst())) {
              Pair<CFANode, CallstackState> newContext =
                  Pair.of(leaveFun.getSuccessor(), context.getSecond());

              if (leaveFun instanceof FunctionReturnEdge) {
                if (!context
                    .getSecond()
                    .getCallNode()
                    .equals(((FunctionReturnEdge) leaveFun).getCallNode())) {
                  continue; // false context
                }
                newContext =
                    Pair.of(leaveFun.getSuccessor(), context.getSecond().getPreviousState());
              }

              if (leaveFun instanceof FunctionCallEdge) {
                newContext =
                    Pair.of(
                        leaveFun.getSuccessor(),
                        new CallstackState(
                            context.getSecond(),
                            leaveFun.getSuccessor().getFunctionName(),
                            leaveFun.getPredecessor()));
              }

              if (!Objects.equals(leaveFun.getSuccessor(), locContinueLoop)) {
                succFun = contextToARGState.get(newContext);
              } else {
                succFun = nodeToARGState.get(locContinueLoop);
                assert (newContext.getSecond() == null);
              }
              if (succFun == null) {
                succFun = new ARGState(locFac.getState(leaveFun.getSuccessor()), null);
                if (!Objects.equals(leaveFun.getSuccessor(), locContinueLoop)) {
                  contextToARGState.put(newContext, succFun);
                  waitlistFun.push(newContext);
                } else {
                  nodeToARGState.put(leaveFun.getSuccessor(), succFun);
                  waitlist.push(leaveFun.getSuccessor());
                }
              }

              succFun.addParent(predFun);
            }
          }

          assert nodeToARGState.containsKey(locContinueLoop);
          relevantARGStates.addAll(contextToARGState.values());
        }
      }
    }
    relevantARGStates.addAll(nodeToARGState.values());
    return relevantARGStates;
  }
}
