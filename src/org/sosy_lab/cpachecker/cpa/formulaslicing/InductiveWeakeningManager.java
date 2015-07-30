package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager.Tactic;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

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

  @Option(secure=true, description="Sort selection variables based on syntactic "
      + "similarity to the transition relation")
  private boolean sortSelectionVariablesSyntactic = true;

  @Option(secure=true, description="Use syntactic formula slicing, which"
      + " uses only the syntactic structure of the formula and does not involve"
      + " any calls to the SMT solver.")
  private boolean useSyntacticFormulaSlicing = false;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final UnsafeFormulaManager ufmgr;
  private final LogManager logger;

  public InductiveWeakeningManager(
      Configuration config,
      FormulaManagerView pFmgr, Solver pSolver,
      UnsafeFormulaManager pUfmgr, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
    solver = pSolver;
    ufmgr = pUfmgr;
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
    BooleanFormula noIntermediate = fmgr.simplify(SlicingPreprocessor
        .of(fmgr, input.getSsa()).visit(input.getFormula()));

    BooleanFormula noIntermediateNNF = bfmgr.applyTactic(noIntermediate,
        Tactic.NNF);

    // Step 2: Annotate conjunctions.

    // Selection variables -> atoms.
    Map<BooleanFormula, BooleanFormula> selectionVars = new HashMap<>();
    BooleanFormula annotated = ConjunctionAnnotator.of(fmgr, selectionVars).visit(
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

    List<BooleanFormula> orderedList;
    if (sortSelectionVariablesSyntactic) {
      orderedList =
          sortBySyntacticSimilarity(selectionVars, transition.getFormula());
    } else {
      orderedList = new ArrayList<>(selectionVars.keySet());
    }

    Set<BooleanFormula> inductiveSlice;

    if (useSyntacticFormulaSlicing) {
      inductiveSlice = syntacticFormulaSlicing(selectionVars, orderedList,
          transition, input);

      // Sanity check. todo: remove/make optional.
      if (!solver.isUnsat(bfmgr.and(bfmgr.and(inductiveSlice), query))) {
        return bfmgr.makeBoolean(true);
      }
    } else {
      inductiveSlice = formulaSlicing(selectionVars,
          orderedList, query);
      if (inductiveSlice.size() == selectionVars.size()) {

        // Everything was abstracted => return a trivial invariant "true".
        return bfmgr.makeBoolean(true);
      }
    }

    // Step 3: Apply the transformation, replace the atoms marked by the
    // selector variables with 'Top'.
    // note: it would be probably better to move those different steps to
    // different subroutines.
    Map<BooleanFormula, BooleanFormula> replacement = new HashMap<>();
    for (BooleanFormula f : selectionVars.keySet()) {

      if (inductiveSlice.contains(f)) {
        replacement.put(f, bfmgr.makeBoolean(true));
      } else {
        replacement.put(f, bfmgr.makeBoolean(false));
      }
    }

    BooleanFormula sliced = ufmgr.substitute(annotated, replacement);
    sliced = fmgr.simplify(sliced);
    logger.log(Level.FINE, "Slice obtained: ", sliced);

    return fmgr.uninstantiate(sliced);
  }

  private Set<BooleanFormula> syntacticFormulaSlicing(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      List<BooleanFormula> selectionVars,
      PathFormula transition,
      PathFormula initial
  ) throws SolverException, InterruptedException {
    Set<BooleanFormula> out = new HashSet<>();
    for (BooleanFormula selector : selectionVars) {
      BooleanFormula atom = selectionInfo.get(selector);
      Set<String> varNames = fmgr.extractFunctionNames(fmgr.uninstantiate(atom), true);

      boolean keepSelector = true;
      for (String var : varNames) {

        if (transition.getSsa().getIndex(var) != initial.getSsa().getIndex(var)) {
          out.add(selector);
          keepSelector = false;
          break;
        }
      }
      if (!keepSelector) {
        break;
      }
    }
    return out;
  }

  /**
   * @param selectionInfo Mapping from selection variables
   *    to the atoms (possibly w/ negation) they represent.
   * @param selectionVars List of selection variables.
   *    The order is very important and determines which MUS we will get out.
   *
   * @return Set of selectors which correspond to atoms which *should*
   *         be abstracted.
   */
  private Set<BooleanFormula> formulaSlicing(
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
   * Sort selectors by syntactic similarity, variables most similar to the
   * transition relation come last.
   *
   * todo: might be a good idea to use the information about the variables
   * which get _changed_ inside the transition as well.
   */
  private List<BooleanFormula> sortBySyntacticSimilarity(
      final Map<BooleanFormula, BooleanFormula> selectors,
      BooleanFormula transitionRelation
  ) {

    final Set<String> transitionVars = fmgr.extractFunctionNames(
        fmgr.uninstantiate(transitionRelation), true);
    List<BooleanFormula> selectorVars = new ArrayList<>(selectors.keySet());
    Collections.sort(selectorVars, new Comparator<BooleanFormula>() {
      @Override
      public int compare(BooleanFormula s1, BooleanFormula s2) {
        BooleanFormula a1 = selectors.get(s1);
        BooleanFormula a2 = selectors.get(s2);

        // todo: incessant re-uninstantiation is inefficient.
        Set<String> a1Vars = fmgr.extractFunctionNames(fmgr.uninstantiate(a1),
            true);
        Set<String> a2Vars = fmgr.extractFunctionNames(fmgr.uninstantiate(a2),
            true);

        Set<String> intersection1 = Sets.intersection(a1Vars, transitionVars);
        Set<String> intersection2 = Sets.intersection(a2Vars, transitionVars);

        return Integer.compare(intersection1.size(), intersection2.size());
      }
    });
    return selectorVars;
  }

  private static class SlicingPreprocessor
      extends BooleanFormulaTransformationVisitor {
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
    protected BooleanFormula visitAtom(BooleanFormula atom) {

      // todo: this does not deal with UFs.
      if (!fmgr.getDeadVariableNames(atom, finalSSA).isEmpty()) {
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
  private static class ConjunctionAnnotator
      extends BooleanFormulaTransformationVisitor {
    private final UniqueIdGenerator controllerIdGenerator =
        new UniqueIdGenerator();
    private final BooleanFormulaManager bfmgr;
    private final Map<BooleanFormula, BooleanFormula> selectionVars;

    private static final String PROP_VAR = "_FS_SEL_VAR_";

    protected ConjunctionAnnotator(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache,

        // Selection variable -> controlled atom.
        Map<BooleanFormula, BooleanFormula> pSelectionVars) {
      super(pFmgr, pCache);
      bfmgr = pFmgr.getBooleanFormulaManager();
      selectionVars = pSelectionVars;
    }

    public static ConjunctionAnnotator of(FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> selectionVars) {
      return new ConjunctionAnnotator(pFmgr,
          new HashMap<BooleanFormula, BooleanFormula>(),
          selectionVars);
    }

    @Override
    protected BooleanFormula visitAnd(BooleanFormula... pOperands) {
      List<BooleanFormula> args = new ArrayList<>(pOperands.length);
      for (BooleanFormula arg : pOperands) {
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
