// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner.filterAbstractionStates;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix.RawInfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.predicate.refinement")
public class PredicateBasedPrefixProvider implements PrefixProvider {
  @Option(secure = true, description = "Max. number of prefixes to extract")
  private int maxPrefixCount = 64;

  @Option(
      secure = true,
      description =
          "Max. length of feasible prefixes to extract from if at least one prefix was already"
              + " extracted")
  private int maxPrefixLength = 1024;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final Solver solver;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pSolver the solver to use
   */
  public PredicateBasedPrefixProvider(
      Configuration config,
      LogManager pLogger,
      Solver pSolver,
      ShutdownNotifier pShutdownNotifier) {
    try {
      config.inject(this);
    } catch (InvalidConfigurationException e) {
      pLogger.log(
          Level.INFO,
          "Invalid configuration given to "
              + getClass().getSimpleName()
              + ". Using defaults instead.");
    }

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    solver = pSolver;
  }

  public static PredicateBasedPrefixProvider create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, PredicateBasedPrefixProvider.class);
    return new PredicateBasedPrefixProvider(
        predicateCpa.getConfiguration(),
        predicateCpa.getLogger(),
        predicateCpa.getSolver(),
        predicateCpa.getShutdownNotifier());
  }

  @Override
  public List<InfeasiblePrefix> extractInfeasiblePrefixes(final ARGPath pPath)
      throws CPAException, InterruptedException {

    List<ARGState> abstractionStates = filterAbstractionStates(pPath);
    List<BooleanFormula> blockFormulas =
        from(abstractionStates)
            .transform(AbstractStates.toState(PredicateAbstractState.class))
            .transform(PredicateAbstractState::getBlockFormula)
            .toList();

    List<RawInfeasiblePrefix> rawPrefixes;

    try (InterpolatingProverEnvironment<?> prover =
        solver.newProverEnvironmentWithInterpolation()) {
      rawPrefixes = extractInfeasiblePrefixes(pPath, blockFormulas, prover);
    }

    // finally, create actual prefixes after solver stack is empty again,
    // doing it that way avoids problems with SMTInterpol (cf. commit 20405)
    List<InfeasiblePrefix> infeasiblePrefixes = new ArrayList<>(rawPrefixes.size());
    for (RawInfeasiblePrefix rawPrefix : rawPrefixes) {
      infeasiblePrefixes.add(
          InfeasiblePrefix.buildForPredicateDomain(rawPrefix, solver.getFormulaManager()));
    }

    return infeasiblePrefixes;
  }

  private <T> List<RawInfeasiblePrefix> extractInfeasiblePrefixes(
      final ARGPath pPath,
      List<BooleanFormula> blockFormulas,
      InterpolatingProverEnvironment<T> prover)
      throws CPAException, InterruptedException {
    List<RawInfeasiblePrefix> rawPrefixes = new ArrayList<>();
    List<T> terms = new ArrayList<>(blockFormulas.size());

    List<BooleanFormula> pathFormula = new ArrayList<>();

    int currentBlockIndex = 0;

    PathIterator iterator = pPath.pathIterator();
    while (iterator.hasNext()) {
      // if we should shutdown we do just break out of this while loop
      if (shutdownNotifier.shouldShutdown()) {
        break;
      }

      ARGState currentState = iterator.getAbstractState();

      if (iterator.getIndex() == 0) {
        assert isAbstractionState(currentState);
      }

      // only compute prefixes at abstraction states
      if (isAbstractionState(currentState)) {

        BooleanFormula currentBlockFormula = blockFormulas.get(currentBlockIndex);
        pathFormula.add(currentBlockFormula);

        try {
          T term = prover.push(currentBlockFormula);
          terms.add(term);

          if (checkUnsat(pPath, iterator.getOutgoingEdge()) && prover.isUnsat()) {

            logger.log(
                Level.FINE,
                "found infeasible prefix, ending with edge ",
                iterator.getOutgoingEdge(),
                " in block # ",
                currentBlockIndex,
                ", that resulted in an unsat-formula");

            List<BooleanFormula> interpolantSequence = extractInterpolantSequence(terms, prover);
            List<BooleanFormula> finalPathFormula = new ArrayList<>(pathFormula);

            // create and add infeasible prefix, mind that the ARGPath has not (!)
            // failing assume operations replaced with no-ops, as this is not needed here,
            // and it would be cumbersome for ABE, so lets skip it
            ARGPath currentPrefixPath = ARGUtils.getOnePathTo(currentState);

            // put prefix data into a simple container for now
            rawPrefixes.add(
                new RawInfeasiblePrefix(currentPrefixPath, interpolantSequence, finalPathFormula));

            // stop once threshold for max. length of prefix is reached, relevant
            // e.g., for ECA programs where error paths often exceed 10.000 transition
            if (currentPrefixPath.size() >= maxPrefixLength) {
              break;
            }

            // remove reason for UNSAT from solver stack
            prover.pop();

            // replace respective term by tautology
            terms.remove(terms.size() - 1);
            terms.add(prover.push(makeTrue()));

            // replace failing block formula by tautology, too
            pathFormula.remove(pathFormula.size() - 1);
            pathFormula.add(makeTrue());
          }
        } catch (SolverException e) {
          throw new CPAException("Error during computation of prefixes: " + e.getMessage(), e);
        }

        currentBlockIndex++;

        // put hard-limit on number of prefixes
        if (rawPrefixes.size() == maxPrefixCount) {
          break;
        }
      }

      iterator.advance();
    }

    return rawPrefixes;
  }

  private <T> List<BooleanFormula> extractInterpolantSequence(
      final List<T> pTerms, final InterpolatingProverEnvironment<T> pProver)
      throws SolverException, InterruptedException {

    List<BooleanFormula> interpolantSequence = new ArrayList<>();

    for (int i = 1; i < pTerms.size(); i++) {
      interpolantSequence.add(pProver.getInterpolant(pTerms.subList(0, i)));
    }

    return interpolantSequence;
  }

  /**
   * This method checks if a unsat call is necessary. It the path is not single-block encoded, then
   * unsatisfiability has to be checked always. In case it is single-block encoded, then it suffices
   * to check unsatisfiability at assume edges.
   *
   * @param pPath the path to check
   * @param pCfaEdge the current edge
   * @return true if a unsat call is neccessary, else false
   */
  private boolean checkUnsat(final ARGPath pPath, final CFAEdge pCfaEdge) {
    if (!isSingleBlockEncoded(pPath)) {
      return true;
    }

    // since replacing multi-edges with aggregateBasicBlocks,
    // (cf. option cpa.composite.aggregateBasicBlocks) there
    // may be holes in the path, represented by nulls,
    // therefore the special handling of null is required
    if (pCfaEdge == null) {
      return false;
    }

    return pCfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge;
  }

  /**
   * This method checks if the given path is single-block-encoded, which is the case, if all states
   * in the path are abstraction states.
   *
   * @param pPath the path to check
   * @return true, if all states in the path are abstraction states, else false
   */
  private boolean isSingleBlockEncoded(final ARGPath pPath) {
    return from(pPath.asStatesList()).allMatch(PredicateAbstractState::containsAbstractionState);
  }

  private boolean isAbstractionState(ARGState pCurrentState) {
    return PredicateAbstractState.getPredicateState(pCurrentState).isAbstractionState();
  }

  private BooleanFormula makeTrue() {
    return solver.getFormulaManager().getBooleanFormulaManager().makeTrue();
  }
}
