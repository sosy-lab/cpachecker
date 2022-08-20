// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.UseDefBasedInterpolator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * PrefixProvider that extracts all infeasible prefixes for a path, starting with an initial empty
 * or given state. Uses a {@link StrongestPostOperator} for interpreting the semantics of
 * operations.
 */
public class GenericPrefixProvider<S extends ForgetfulState<?>> implements PrefixProvider {

  private final LogManager logger;
  private final StrongestPostOperator<S> strongestPost;
  private final VariableTrackingPrecision precision;
  private final CFA cfa;
  private final S initialState;
  private final ShutdownNotifier shutdownNotifier;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   */
  public GenericPrefixProvider(
      final StrongestPostOperator<S> pStrongestPost,
      final S pEmptyState,
      final LogManager pLogger,
      final CFA pCfa,
      final Configuration config,
      final Class<? extends ConfigurableProgramAnalysis> pCpaToRefine,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    logger = pLogger;
    cfa = pCfa;

    strongestPost = pStrongestPost;
    initialState = pEmptyState;
    precision =
        VariableTrackingPrecision.createStaticPrecision(
            config, cfa.getVarClassification(), pCpaToRefine);
    shutdownNotifier = pShutdownNotifier;
  }

  /**
   * This method obtains a list of prefixes of the path, that are infeasible by themselves. If the
   * path is feasible, the whole path is returned as the only element of the list.
   *
   * @param path the path to check
   * @return the list of prefix of the path that are feasible by themselves
   */
  @Override
  public List<InfeasiblePrefix> extractInfeasiblePrefixes(final ARGPath path)
      throws CPAException, InterruptedException {
    return extractInfeasiblePrefixes(path, initialState);
  }

  /**
   * This method obtains a list of prefixes of the path, that are infeasible by themselves. If the
   * path is feasible, the whole path is returned as the only element of the list.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @return the list of prefix of the path that are feasible by themselves
   */
  public List<InfeasiblePrefix> extractInfeasiblePrefixes(final ARGPath path, final S pInitial)
      throws CPAException, InterruptedException {

    List<InfeasiblePrefix> prefixes = new ArrayList<>();
    Deque<S> callstack = new ArrayDeque<>();

    try {
      ARGPathBuilder feasiblePrefixBuilder = ARGPath.builder();
      S next = pInitial;

      PathIterator iterator = path.pathIterator();
      while (iterator.hasNext()) {
        shutdownNotifier.shutdownIfNecessary();

        final CFAEdge outgoingEdge = iterator.getOutgoingEdge();
        final ARGState currentState = iterator.getAbstractState();

        Optional<S> successor;

        if (outgoingEdge != null) {
          successor = getSuccessor(next, outgoingEdge, callstack);
        } else {
          PathIterator holeIt = iterator.getPosition().fullPathIterator();

          S intermediateState = next;
          do {
            successor = getSuccessor(intermediateState, holeIt.getOutgoingEdge(), callstack);

            holeIt.advance();
            if (successor.isPresent()) {
              intermediateState = successor.orElseThrow();
            }

          } while (!holeIt.isPositionWithState() && successor.isPresent());
        }

        feasiblePrefixBuilder.add(currentState, outgoingEdge);

        // no successors => path is infeasible
        if (!successor.isPresent()) {
          logger.log(
              Level.FINE, "found infeasible prefix: ", outgoingEdge, " did not yield a successor");

          // last state is the one which is infeasible
          ARGPath infeasiblePrefix = feasiblePrefixBuilder.build(iterator.getNextAbstractState());

          // add infeasible prefix
          prefixes.add(buildInfeasiblePrefix(infeasiblePrefix));

          feasiblePrefixBuilder.removeLast();

          // continue with feasible prefix
          if (iterator.hasNext()) {
            checkState(outgoingEdge != null);
            feasiblePrefixBuilder.add(
                currentState,
                new BlankEdge(
                    outgoingEdge.getRawStatement(),
                    outgoingEdge.getFileLocation(),
                    outgoingEdge.getPredecessor(),
                    outgoingEdge.getSuccessor(),
                    "sliced infeasible condition"));
          }

          successor = Optional.of(next);
        }

        // extract singleton successor state
        next = successor.orElseThrow();

        // some variables might be blacklisted or tracked by BDDs
        // so perform abstraction computation here
        next =
            strongestPost.performAbstraction(
                next, extractLocation(iterator.getNextAbstractState()), path, precision);
        iterator.advance();
      }

      return prefixes;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of infeasible prefixes failed: " + e.getMessage(), e);
    }
  }

  private Optional<S> getSuccessor(final S pNext, final CFAEdge pEdge, final Deque<S> pCallstack)
      throws CPAException, InterruptedException {

    S next = pNext;

    if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      next = strongestPost.handleFunctionCall(next, pEdge, pCallstack);
    }

    if (!pCallstack.isEmpty() && pEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      next = strongestPost.handleFunctionReturn(next, pEdge, pCallstack);
    }

    return strongestPost.getStrongestPost(next, precision, pEdge);
  }

  private InfeasiblePrefix buildInfeasiblePrefix(final ARGPath infeasiblePrefix) {
    UseDefRelation useDefRelation =
        new UseDefRelation(
            infeasiblePrefix,
            cfa.getVarClassification().isPresent()
                ? cfa.getVarClassification().orElseThrow().getIntBoolVars()
                : ImmutableSet.of(),
            false);

    List<Pair<ARGState, ValueAnalysisInterpolant>> interpolants =
        new UseDefBasedInterpolator(infeasiblePrefix, useDefRelation, cfa.getMachineModel())
            .obtainInterpolants();

    return InfeasiblePrefix.buildForValueDomain(
        infeasiblePrefix, transformedImmutableListCopy(interpolants, Pair::getSecond));
  }
}
