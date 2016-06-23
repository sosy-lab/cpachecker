package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.SolverContext.ProverOptions;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Finds inductive weakening of formulas (originally: formula slicing).
 * This class operates on formulas, and should be orthogonal to
 * CFA- and CPA-specific concepts.
 */
@Options(prefix="cpa.slicing")
public class InductiveWeakeningManager {

  @Option(secure=true, description="Use syntactic formula slicing, which"
      + " uses only the syntactic structure of the formula and does not involve"
      + " any calls to the SMT solver.")
  private boolean runSyntacticSlicing = false;

  @Option(secure=true, description="Run destructive formula slicing, which starts with an "
  + "unsatisfiable set and tries to add elements to it, making sure it stays unsatisfiable.")
  private boolean runDestructiveSlicing = true;

  @Option(secure=true, description="Use formula slicing based on counterexamples.")
  private boolean runCounterexampleBasedSlicing = false;

  @Option(secure=true, description="Sort selection variables based on syntactic "
      + "similarity to the transition relation")
  private boolean sortSelectionVariablesSyntactic = true;

  @Option(secure=true, description="Limits the number of iteration for the "
      + "destructive slicing strategy. Set to -1 for no limit.")
  private int destructiveIterationLimit = -1;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final LogManager logger;

  public InductiveWeakeningManager(
      Configuration config, FormulaManagerView pFmgr, Solver pSolver, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
    solver = pSolver;
    logger = pLogger;
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  /**
   * Find the inductive weakening of {@code input} subject to the loop
   * transition over-approximation shown in {@code transition}.
   *
   * @param strengthening Strengthening which is guaranteed to be universally
   * true (under the given path) at the given point.
   */
  public BooleanFormula slice(
      PathFormula input, PathFormula transition,
      BooleanFormula strengthening
  ) throws SolverException, InterruptedException {
    if (input.getFormula().equals(bfmgr.makeBoolean(true))) {
      return bfmgr.makeBoolean(true);
    }

    // Step 0: todo (optional): add quantifiers next to intermediate variables,
    // perform quantification, run QE_LIGHT to remove the ones we can.

    // Step 1: get rid of intermediate variables in "input".

    // ...remove atoms containing intermediate variables.
    BooleanFormula noIntermediate = fmgr.simplify(bfmgr.visit(
        SlicingPreprocessor.of(fmgr, input.getSsa()), input.getFormula()));

    BooleanFormula noIntermediateNNF = fmgr.applyTactic(noIntermediate,
        Tactic.NNF);
    if (noIntermediateNNF.equals(bfmgr.makeBoolean(false))) {

      // Shortcut, no atoms with only non-intermediate variables existed in the
      // original formula.
      return bfmgr.makeBoolean(true);
    }

    // Step 2: Annotate conjunctions.

    // Selection variables -> atoms.
    Map<BooleanFormula, BooleanFormula> selectionVarsInfo = new HashMap<>();
    BooleanFormula annotated = bfmgr.visit(new ConjunctionAnnotator(
        fmgr, new HashMap<BooleanFormula, BooleanFormula>(), selectionVarsInfo),
        noIntermediateNNF);

    // This is possible since the formula does not have any intermediate
    // variables.
    BooleanFormula primed =
        fmgr.instantiate(fmgr.uninstantiate(annotated), transition.getSsa());

    BooleanFormula negated = bfmgr.not(primed);

    logger.log(Level.FINE, "Loop transition: ", transition.getFormula());

    // Inductiveness checking formula.
    BooleanFormula query = bfmgr.and(ImmutableList.of(
        annotated,
        transition.getFormula(),
        negated,
        strengthening
    ));

    if (selectionVarsInfo.size() == 0) {

      // Either everything is inductive, or nothing is inductive.
      if (solver.isUnsat(query)) {
        return noIntermediate;
      } else {

        // Nothing is inductive.
        return bfmgr.makeBoolean(true);
      }
    }

    // Abstracting away every single selector is inductive.
    Set<BooleanFormula> inductiveSlice = ImmutableSet.copyOf(
        selectionVarsInfo.keySet());

    if (runSyntacticSlicing) {
      inductiveSlice = syntacticWeakening(selectionVarsInfo, inductiveSlice,
          transition);

      // Sanity check.
      Verify.verify(solver.isUnsat(bfmgr.and(bfmgr.and(inductiveSlice), query)));
    }

    if (runCounterexampleBasedSlicing) {
      inductiveSlice = counterexampleBasedWeakening(
          selectionVarsInfo, transition.getSsa(), query, inductiveSlice);
    }

    if (runDestructiveSlicing) {
      List<BooleanFormula> orderedList;
      if (sortSelectionVariablesSyntactic) {
        orderedList = sortBySyntacticSimilarity(
                selectionVarsInfo, inductiveSlice, transition.getFormula());
      } else {
        orderedList = new ArrayList<>(selectionVarsInfo.keySet());
      }

      inductiveSlice = destructiveMUS(
          selectionVarsInfo, orderedList, query);
    }

    if (inductiveSlice.size() == selectionVarsInfo.size()) {

      // Everything was abstracted => return a trivial invariant "true".
      return bfmgr.makeBoolean(true);
    }


    // Step 3: Apply the transformation, replace the atoms marked by the
    // selector variables with 'Top'.
    // note: it would be probably better to move those different steps to
    // different subroutines.
    Map<BooleanFormula, BooleanFormula> replacement = new HashMap<>();
    for (BooleanFormula f : selectionVarsInfo.keySet()) {

      if (inductiveSlice.contains(f)) {
        replacement.put(f, bfmgr.makeBoolean(true));
      } else {
        replacement.put(f, bfmgr.makeBoolean(false));
      }
    }

    BooleanFormula sliced = fmgr.substitute(annotated, replacement);
    sliced = fmgr.simplify(sliced);
    logger.log(Level.FINE, "Slice obtained: ", sliced);

    return fmgr.uninstantiate(sliced);
  }

  /**
   * Syntactic formula slicing: slices away all atoms which have variables
   * which were changed (== SSA index changed) by the transition relation.
   * In that case, \phi is exactly the same as \phi',
   * and the formula should be unsatisfiable.
   *
   * @param selectionInfo selection variable -> corresponding atom (instantiated
   * with unprimed SSA).
   * @return Set of selectors which correspond to atoms which *should*
   *         be abstracted.
   */
  private Set<BooleanFormula> syntacticWeakening(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      Collection<BooleanFormula> selectionVars,
      PathFormula transition) {
    Set<BooleanFormula> out = new HashSet<>();
    for (BooleanFormula selector : selectionVars) {
      BooleanFormula atom = selectionInfo.get(selector);

      // Variables which have the SSA index different to the one after the
      // transition.
      Set<String> deadVars = fmgr.getDeadFunctionNames(atom, transition.getSsa());

      if (!deadVars.isEmpty() ||

          // todo: remove this hack.
          atom.toString().contains("z3name")) {
        out.add(selector);
      }
    }
    return out;
  }

  private Set<BooleanFormula> counterexampleBasedWeakening(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      SSAMap finalSSA,
      BooleanFormula query,
      Set<BooleanFormula> inductiveSlice
  ) throws SolverException, InterruptedException {
    query = fmgr.simplify(query);
    Set<BooleanFormula> out = new HashSet<>();

    try (ProverEnvironment env = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      //noinspection ResultOfMethodCallIgnored
      env.push(query);

      while (!env.isUnsat()) {
        Model m = env.getModel();

        List<BooleanFormula> toPush = new ArrayList<>();

        for (Entry<BooleanFormula, BooleanFormula> entry : selectionInfo.entrySet()) {
          BooleanFormula atom = entry.getValue();
          BooleanFormula selector = entry.getKey();

          BooleanFormula primedAtom = fmgr.instantiate(
              fmgr.uninstantiate(atom), finalSSA
          );
          Boolean value = m.evaluate(primedAtom);

          // Exclude the atom by enforcing the selector,
          // only if the atom is contained in the already present refinement.
          if (!value &&
              inductiveSlice.contains(selector)) {

            logger.log(Level.FINE, "Abstracting away",
                selectionInfo.get(selector));
            toPush.add(selector);
            out.add(selector);
          }
        }

        for (BooleanFormula s : toPush) {
          //noinspection ResultOfMethodCallIgnored
          env.push(s);
        }
      }
    }

    return out;
  }

  /**
   * @param selectionInfo Mapping from selection variables
   *    to the atoms (possibly w/ negation) they represent.
   * @param selectionVars List of selection variables, already determined to
   *    be inductive.
   *    The order is very important and determines which MUS we will get out.
   *
   * @return Set of selectors which correspond to atoms which *should*
   *         be abstracted.
   *
   * Implements the destructive algorithm for MUS extraction.
   * Starts with everything abstracted ("true" is inductive),
   * remove selectors which can be removed while keeping the overall query
   * inductive.
   * This is a standard algorithm, however it pays the cost of N SMT calls
   * upfront.
   * Note that since at every iteration the set of abstracted variables is
   * inductive, the algorithm can be terminated early.
   */
  private Set<BooleanFormula> destructiveMUS(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      List<BooleanFormula> selectionVars,
      BooleanFormula query
  ) throws SolverException, InterruptedException {

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

    final Set<String> transitionVars = fmgr.extractFunctionNames(
        fmgr.uninstantiate(transitionRelation));
    List<BooleanFormula> selectorVars = new ArrayList<>(inductiveSlice);
    Collections.sort(selectorVars, new Comparator<BooleanFormula>() {
      @Override
      public int compare(BooleanFormula s1, BooleanFormula s2) {
        BooleanFormula a1 = selectors.get(s1);
        BooleanFormula a2 = selectors.get(s2);

        // todo: incessant re-uninstantiation is inefficient.
        Set<String> a1Vars = fmgr.extractFunctionNames(fmgr.uninstantiate(a1));
        Set<String> a2Vars = fmgr.extractFunctionNames(fmgr.uninstantiate(a2));

        Set<String> intersection1 = Sets.intersection(a1Vars, transitionVars);
        Set<String> intersection2 = Sets.intersection(a2Vars, transitionVars);

        return Integer.compare(intersection1.size(), intersection2.size());
      }
    });
    return selectorVars;
  }

  private static class SlicingPreprocessor
      extends BooleanFormulaManagerView.BooleanFormulaTransformationVisitor {
    private final SSAMap finalSSA;
    private final FormulaManagerView fmgr;

    protected SlicingPreprocessor(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache, SSAMap pFinalSSA) {
      super(pFmgr, pCache);
      finalSSA = pFinalSSA;
      fmgr = pFmgr;
    }

    public static SlicingPreprocessor of(FormulaManagerView fmgr,
        SSAMap ssa) {
      return new SlicingPreprocessor(fmgr,
          new HashMap<BooleanFormula, BooleanFormula>(), ssa);
    }

    /**
     * Replace all atoms containing intermediate variables with "true".
     */
    @Override
    public BooleanFormula visitAtom(BooleanFormula atom, FunctionDeclaration decl) {

      if (!fmgr.getDeadFunctionNames(atom, finalSSA).isEmpty()) {
        return fmgr.getBooleanFormulaManager().makeBoolean(true);
      }
      return atom;
    }
  }

  /**
   * (and a_1 a_2 a_3 ...)
   * -> gets converted to ->
   * (and (or p_1 a_1) ...)
   */
  private class ConjunctionAnnotator
      extends BooleanFormulaManagerView.BooleanFormulaTransformationVisitor {
    private final UniqueIdGenerator controllerIdGenerator =
        new UniqueIdGenerator();
    private final Map<BooleanFormula, BooleanFormula> selectionVars;

    private static final String PROP_VAR = "_FS_SEL_VAR_";

    protected ConjunctionAnnotator(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache,

        // Selection variable -> controlled atom.
        Map<BooleanFormula, BooleanFormula> pSelectionVars) {
      super(pFmgr, pCache);
      selectionVars = pSelectionVars;
    }

    @Override
    public BooleanFormula visitAnd(List<BooleanFormula> pOperands) {
      List<BooleanFormula> args = new ArrayList<>(pOperands.size());
      for (BooleanFormula arg : pOperands) {

        // todo: BUG, missing things inside the argument.
        BooleanFormula controller = makeFreshSelector(arg);
        args.add(bfmgr.or(controller, arg));
      }
      return bfmgr.and(args);
    }

    private BooleanFormula makeFreshSelector(BooleanFormula atom) {
      BooleanFormula selector = bfmgr
          .makeVariable(PROP_VAR + controllerIdGenerator.getFreshId());

      selectionVars.put(selector, atom);
      return selector;
    }
  }
}
