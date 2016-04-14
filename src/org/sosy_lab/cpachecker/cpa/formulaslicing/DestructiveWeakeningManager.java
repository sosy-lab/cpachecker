package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static org.sosy_lab.solver.api.SolverContext.ProverOptions.GENERATE_UNSAT_CORE_OVER_ASSUMPTIONS;

import com.google.common.base.Optional;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.ProverEnvironment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Perform weakening by destructive iterations.
 */
@Options(prefix="cpa.slicing")
public class DestructiveWeakeningManager {
  @Option(secure=true, description="Pre-run syntactic weakening")
  private boolean preRunSyntacticWeakening = true;

  private final InductiveWeakeningStatistics statistics;
  private final Solver solver;
  private final BooleanFormulaManager bfmgr;
  private final SyntacticWeakeningManager swmgr;

  public DestructiveWeakeningManager(
      InductiveWeakeningStatistics pStatistics,
      Solver pSolver,
      FormulaManagerView pFmgr,
      Configuration pConfiguration) throws InvalidConfigurationException {
    pConfiguration.inject(this);

    statistics = pStatistics;
    solver = pSolver;
    bfmgr = pFmgr.getBooleanFormulaManager();
    swmgr = new SyntacticWeakeningManager(pFmgr);
  }

  /**
   * @return Set of selectors which should be abstracted.
   */
  public Set<BooleanFormula> performWeakening(
      Map<BooleanFormula, BooleanFormula> selectionsVarsInfo,
      BooleanFormula fromState,
      PathFormula transition,
      BooleanFormula toState,
      SSAMap fromSSA,
      Set<BooleanFormula> pFromStateLemmas
  ) throws SolverException, InterruptedException {
    Set<BooleanFormula> selectorsToAbstractOverApproximation;
    if (preRunSyntacticWeakening) {
      selectorsToAbstractOverApproximation = swmgr.performWeakening(
          fromSSA, selectionsVarsInfo, transition, pFromStateLemmas);
    } else {
      selectorsToAbstractOverApproximation = selectionsVarsInfo.keySet();
    }
    BooleanFormula query = bfmgr.and(
        fromState, transition.getFormula(), bfmgr.not(toState)
    );
    return destructiveWeakening(
        selectionsVarsInfo,
        selectorsToAbstractOverApproximation,
        query
    );
  }

  private Set<BooleanFormula> destructiveWeakening(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      Set<BooleanFormula> selectorsToAbstractOverApproximation,
      BooleanFormula query) throws SolverException, InterruptedException {

    try {
      statistics.destructiveWeakeningTime.start();
      return destructiveWeakening0(
          selectionVarsInfo, selectorsToAbstractOverApproximation, query);
    } finally {
      statistics.destructiveWeakeningTime.stop();
    }
  }

  private BooleanFormula generateNegations(
      Set<BooleanFormula> selectors,
      Set<BooleanFormula> toAbstract
  ) {
    List<BooleanFormula> out = new ArrayList<>();
    for (BooleanFormula sel : selectors) {
      if (!toAbstract.contains(sel)) {
        out.add(bfmgr.not(sel));
      }
    }
    return bfmgr.and(out);
  }

  /**
   * Implements the destructive algorithm for MUS extraction.
   * Starts with everything abstracted ("true" is inductive),
   * remove selectors which can be removed while keeping the overall query
   * inductive.
   *
   * <p>This is a standard algorithm, however it pays the cost of N SMT calls
   * upfront.
   * Note that since at every iteration the set of abstracted variables is
   * inductive, the algorithm can be terminated early.
   *
   * @param selectionInfo Mapping from selection variables
   *    to the atoms (possibly w/ negation) they represent.
   * @param selectionVars List of selection variables, already determined to
   *    be inductive.
   * @return Set of selectors which correspond to atoms which *should*
   *   be abstracted.
   */
  private Set<BooleanFormula> destructiveWeakening0(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      Set<BooleanFormula> selectionVars,
      BooleanFormula query) throws SolverException, InterruptedException {

    Set<BooleanFormula> walked = new HashSet<>();
    Set<BooleanFormula> toWalk;
    Set<BooleanFormula> toAbstract;

    try (ProverEnvironment pe = solver.newProverEnvironment(
        GENERATE_UNSAT_CORE_OVER_ASSUMPTIONS)) {
      pe.push();
      pe.addConstraint(query);

      Optional<List<BooleanFormula>> core =
          pe.unsatCoreOverAssumptions(selectionVars);

      if (core.isPresent()) {

        List<BooleanFormula> unsatCore = core.get();
        toWalk = new HashSet<>(unsatCore);
        toAbstract = new HashSet<>(unsatCore);
      } else {
        throw new IllegalStateException("Unexpected state");
      }

      while (!walked.containsAll(toWalk)) {
        BooleanFormula toTest = toWalk.iterator().next();
        toAbstract.remove(toTest);
        walked.add(toTest);

        pe.push();
        pe.addConstraint(generateNegations(selectionInfo.keySet(), toAbstract));

        core = pe.unsatCoreOverAssumptions(toAbstract);

        if (core.isPresent()) {

          List<BooleanFormula> unsatCore = core.get();
          toWalk = new HashSet<>(unsatCore);
          toAbstract = new HashSet<>(unsatCore);
        } else {
          toAbstract.add(toTest);
          toWalk.remove(toTest);
        }

        pe.pop();
      }
    }

    return toAbstract;
  }
}
