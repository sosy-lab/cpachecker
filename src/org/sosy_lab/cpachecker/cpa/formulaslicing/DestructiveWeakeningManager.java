package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.ProverEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Perform weakening by destructive iterations.
 */
@Options(prefix="cpa.slicing")
public class DestructiveWeakeningManager {
  @Option(secure=true, description="Pre-run syntactic weakening")
  private boolean preRunSyntacticWeakening = true;

  @Option(secure=true, description="Limits the number of iteration for the "
      + "destructive slicing strategy. Set to -1 for no limit.")
  private int destructiveIterationLimit = -1;

  @Option(secure=true, description="Sort selection variables based on syntactic "
      + "similarity to the transition relation")
  private boolean sortSelectionVariablesSyntactic = true;

  private final InductiveWeakeningStatistics statistics;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final LogManager logger;
  private final BooleanFormulaManager bfmgr;
  private final SyntacticWeakeningManager swmgr;

  public DestructiveWeakeningManager(
      InductiveWeakeningStatistics pStatistics,
      Solver pSolver,
      FormulaManagerView pFmgr,
      LogManager pLogger,
      Configuration pConfiguration) throws InvalidConfigurationException {
    pConfiguration.inject(this);

    statistics = pStatistics;
    solver = pSolver;
    fmgr = pFmgr;
    logger = pLogger;
    bfmgr = fmgr.getBooleanFormulaManager();
    swmgr = new SyntacticWeakeningManager(fmgr);
  }

  /**
   * @return Set of selectors which should be abstracted.
   */
  public Set<BooleanFormula> performWeakening(
      Map<BooleanFormula, BooleanFormula> selectionsVarsInfo,
      BooleanFormula fromState,
      PathFormula transition,
      BooleanFormula toState,
      Set<BooleanFormula> selectorsWithIntermediate,
      SSAMap fromSSA
  ) throws SolverException, InterruptedException {
    Set<BooleanFormula> selectorsToAbstractOverApproximation;
    if (preRunSyntacticWeakening) {
      selectorsToAbstractOverApproximation = swmgr.performWeakening(
          fromSSA, selectionsVarsInfo, transition);
    } else {
      selectorsToAbstractOverApproximation = selectionsVarsInfo.keySet();
    }
    BooleanFormula query = bfmgr.and(
        fromState, transition.getFormula(), bfmgr.not(toState)
    );
    return destructiveWeakening(
        selectionsVarsInfo,
        selectorsToAbstractOverApproximation,
        transition.getFormula(),
        query,
        selectorsWithIntermediate
    );
  }

  private Set<BooleanFormula> destructiveWeakening(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      Set<BooleanFormula> selectorsToAbstractOverApproximation,
      BooleanFormula transition,
      BooleanFormula query,
      Set<BooleanFormula> selectorsWithIntermediate) throws SolverException, InterruptedException {
    List<BooleanFormula> orderedList;
    if (sortSelectionVariablesSyntactic) {
      orderedList = sortBySyntacticSimilarity(
          selectionVarsInfo, selectorsToAbstractOverApproximation, transition);
    } else {
      orderedList = new ArrayList<>(selectionVarsInfo.keySet());
    }

    try {
      statistics.destructiveWeakeningTime.start();
      return destructiveWeakening0(selectionVarsInfo, orderedList, query, selectorsWithIntermediate);
    } finally {
      statistics.destructiveWeakeningTime.stop();
    }
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
   *    The order is very important and determines which MUS we will get out.
   * @return Set of selectors which correspond to atoms which *should*
   */
  private Set<BooleanFormula> destructiveWeakening0(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      List<BooleanFormula> selectionVars,
      BooleanFormula query,
      Set<BooleanFormula> pSelectorsWithIntermediate) throws SolverException, InterruptedException {

    query = fmgr.simplify(query);
    List<BooleanFormula> abstractedSelectors = selectionVars;

    try (ProverEnvironment env = solver.newProverEnvironment()) {

      //noinspection ResultOfMethodCallIgnored
      env.push(query);

      if (env.isUnsat()) {
        // Questionable, but very useful for testing.
        logger.log(Level.INFO, "Everything is inductive under the transition!",
            "That looks suspicious");
        return ImmutableSet.of();
      }

      // Make everything abstracted.
      BooleanFormula selectionFormula = bfmgr.and(abstractedSelectors);

      //noinspection ResultOfMethodCallIgnored
      env.push(selectionFormula);

      if (!env.isUnsat()) {

        // No non-trivial assignment exists: rely on the caller to return
        // the trivial environment "true".
        return new HashSet<>(selectionVars);
      }

      // Remove the selection constraint.
      env.pop();

      int noRemoved = 0;
      for (int i=0; i<selectionVars.size(); i++) {
        if (destructiveIterationLimit != -1 && i == destructiveIterationLimit) {
          // Terminate early.
          break;
        }

        // Remove this variable from the selection.
        List<BooleanFormula> newSelection = Lists.newArrayList(abstractedSelectors);

        BooleanFormula selVar = selectionVars.get(i);
        if (pSelectorsWithIntermediate.contains(selVar)) {
          // Can't un-abstract selectors containing the intermediate value.
          continue;
        }
        Verify.verify(selVar.equals(newSelection.get(i - noRemoved)));

        // Try removing the corresponding element from the selection.
        newSelection.remove(i - noRemoved);

        logger.log(Level.FINE, "Attempting to add an atom",
            selectionInfo.get(selVar));

        //noinspection ResultOfMethodCallIgnored
        env.push(bfmgr.and(newSelection));

        if (env.isUnsat()) {

          // Still unsat: keep that element non-abstracted.
          abstractedSelectors = newSelection;
          noRemoved++;
        } else {
          logger.log(Level.FINE, "Query became non-inductive: not adding the atom");
        }

        env.pop();
      }

      //noinspection ResultOfMethodCallIgnored
      env.push(bfmgr.and(abstractedSelectors));

      Verify.verify(env.isUnsat());
    }
    return new HashSet<>(abstractedSelectors);
  }

  /**
   * Sort selectors by syntacticWeakening similarity, variables most similar to the
   * transition relation come last.
   *
   * todo: might be a good idea to use the information about the variables
   * which get _changed_ inside the transition as well.
   */
  private List<BooleanFormula> sortBySyntacticSimilarity(
      final Map<BooleanFormula, BooleanFormula> selectors,
      Collection<BooleanFormula> inductiveSlice,
      BooleanFormula transitionRelation
  ) {
    List<BooleanFormula> selectorVars = new ArrayList<>(inductiveSlice);
    final Comparator<BooleanFormula> syntacticComparator =
        syntacticSimilarityComparator(transitionRelation);
    Collections.sort(selectorVars, new Comparator<BooleanFormula>() {
      @Override
      public int compare(BooleanFormula s1, BooleanFormula s2) {
        return syntacticComparator.compare(selectors.get(s1), selectors.get(s2));
      }
    });
    return selectorVars;
  }

  /**
   * Compares formulas based on the similarity with the transition relation.
   */
  private Comparator<BooleanFormula> syntacticSimilarityComparator(
      BooleanFormula transitionRelation
  ) {
    final Set<String> transitionVars = fmgr.extractFunctionNames(
        fmgr.uninstantiate(transitionRelation));
    return new Comparator<BooleanFormula>() {
      @Override
      public int compare(BooleanFormula a1, BooleanFormula a2) {

        // NB: uninstantiating is cached.
        Set<String> a1Vars = fmgr.extractFunctionNames(fmgr.uninstantiate(a1));
        Set<String> a2Vars = fmgr.extractFunctionNames(fmgr.uninstantiate(a2));

        Set<String> intersection1 = Sets.intersection(a1Vars, transitionVars);
        Set<String> intersection2 = Sets.intersection(a2Vars, transitionVars);

        return Integer.compare(intersection1.size(), intersection2.size());
      }
    };
  }
}
