package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.google.common.collect.Lists;

public class InductiveWeakeningManager {
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final UnsafeFormulaManager ufmgr;

  public InductiveWeakeningManager(FormulaManagerView pFmgr, Solver pSolver,
      UnsafeFormulaManager pUfmgr) {
    fmgr = pFmgr;
    solver = pSolver;
    ufmgr = pUfmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  public BooleanFormula slice(PathFormula input, PathFormula transition)
      throws SolverException, InterruptedException {

    // Step 0: (optional): add quantifiers next to intermediate variables,
    // perform quantification, run QE_LIGHT to remove the ones we can.

    // Step 1: get rid of intermediate variables in "input".


    // ...remove atoms containing intermediate variables.
    BooleanFormula noIntermediate = SlicingPreprocessor
        .of(fmgr, input.getSsa()).visit(input.getFormula());

    BooleanFormula noIntermediateNNF = bfmgr.applyTactic(noIntermediate,
        Tactic.NNF);

    // Step 2: Annotate conjunctions.
    Set<BooleanFormula> selectionVars = new HashSet<>();
    BooleanFormula annotated = ConjunctionAnnotator.of(fmgr, selectionVars).visit(
        noIntermediateNNF);

    // This is possible since the formula does not have any intermediate
    // variables, hence the whole renaming would work just as expected.
    BooleanFormula primed =
        fmgr.instantiate(fmgr.uninstantiate(annotated),
            transition.getSsa());
    BooleanFormula negated = bfmgr.not(primed);

    // Inductiveness checking formula.
    BooleanFormula query = bfmgr.and(ImmutableList.of(annotated,
        transition.getFormula(),
        negated));
    List<BooleanFormula> orderedList = ImmutableList.copyOf(selectionVars);

    Set<BooleanFormula> inductiveSlice = formulaSlicing(orderedList, query);

    Map<BooleanFormula, BooleanFormula> replacement = new HashMap<>();
    for (BooleanFormula f : inductiveSlice) {
      replacement.put(f, bfmgr.makeBoolean(true));
    }

    BooleanFormula sliced = ufmgr.substitute(input.getFormula(), replacement);

    return fmgr.uninstantiate(sliced);
  }

  /**
   * @param selectionVars List of selection variables.
   *    The order is very important and determines which MUS we will get out.
   *
   * @return An assignment to boolean variables:
   *         returned as a set of abstracted {@code selectionVars}
   */
  private Set<BooleanFormula> formulaSlicing(
      List<BooleanFormula> selectionVars,
      BooleanFormula query
  ) throws SolverException, InterruptedException {

    List<BooleanFormula> selection = selectionVars;


    try (ProverEnvironment env = solver.newProverEnvironment()) {
      env.push(query);

      // Make everything abstracted.
      BooleanFormula selectionFormula = bfmgr.and(selection);
      env.push(selectionFormula);
      Verify.verify(env.isUnsat());


      while (true) {

        // Remove the selection constraint.
        env.pop();

        boolean removed = false;
        for (int i=0; i<selectionVars.size(); i++) {

          // Remove this variable from the selection.
          List<BooleanFormula> newSelection = Lists.newArrayList(selectionVars);
          newSelection.remove(i);

          env.push(bfmgr.and(newSelection));

          if (env.isUnsat()) {
            // Still unsat: keep that element non-abstracted.
            selection = newSelection;
            removed = true;
            break; // break out of the variable selection loop.
          } else {

            // Try to abstract away some other element.
            continue;
          }
        }

        if (!removed) {
          break;
        }
      }
    }


    return new HashSet<>(selection);
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
    private final Set<BooleanFormula> selectionVars;

    private static final String PROP_VAR = "_FS_PROP_";

    protected ConjunctionAnnotator(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache,
        Set<BooleanFormula> pSelectionVars) {
      super(pFmgr, pCache);
      bfmgr = pFmgr.getBooleanFormulaManager();
      selectionVars = pSelectionVars;
    }

    public static ConjunctionAnnotator of(FormulaManagerView pFmgr,
        Set<BooleanFormula> selectionVars) {
      return new ConjunctionAnnotator(pFmgr,
          new HashMap<BooleanFormula, BooleanFormula>(),
          selectionVars);
    }

    @Override
    protected BooleanFormula visitAnd(BooleanFormula... pOperands) {
      List<BooleanFormula> args = new ArrayList<>(pOperands.length);
      for (BooleanFormula arg : pOperands) {
        BooleanFormula controller = makeFreshSelector();
        args.add(bfmgr.or(controller, arg));
      }
      return bfmgr.and(args);
    }

    private BooleanFormula makeFreshSelector() {
      BooleanFormula selector = bfmgr
          .makeVariable(PROP_VAR + controllerIdGenerator.getFreshId());
      selectionVars.add(selector);
      return selector;
    }
  }
}
