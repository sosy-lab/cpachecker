package org.sosy_lab.cpachecker.cpa.formulaslicing;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.formulaslicing.CEXWeakeningManager.SELECTION_STRATEGY;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Finds inductive weakening of formulas (originally: formula slicing).
 * This class operates on formulas, and should be orthogonal to
 * CFA- and CPA-specific concepts.
 */
@Options(prefix="cpa.slicing")
public class InductiveWeakeningManager implements StatisticsProvider {

  @Option(description="Inductive weakening strategy", secure=true)
  private WEAKENING_STRATEGY weakeningStrategy = WEAKENING_STRATEGY.CEX;

  @Option(description="Granularity of weakening", secure=true)
  private ANNOTATION_MODE selectorAnnotationMode = ANNOTATION_MODE.LITERALS;

  private enum ANNOTATION_MODE {

    /**
     * Introduce a selector per each literal.
     */
    LITERALS,

    /**
     * Introduce only one selector per each argument in the conjunction. Less granular.
     */
    CONJUNCTIONS
  }

  /**
   * Possible weakening strategies.
   */
  enum WEAKENING_STRATEGY {

    /**
     * Remove all atoms containing the literals mentioned in the transition relation.
     */
    SYNTACTIC,

    /**
     * Abstract away all literals, try to un-abstract them one by one.
     */
    DESTRUCTIVE,

    /**
     * Select literals to abstract based on the counterexamples-to-induction.
     */
    CEX
  }

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final LogManager logger;
  private final InductiveWeakeningStatistics statistics;
  private final SyntacticWeakeningManager syntacticWeakeningManager;
  private final DestructiveWeakeningManager destructiveWeakeningManager;
  private final CEXWeakeningManager cexWeakeningManager;
  private final ShutdownNotifier shutdownNotifier;

  private static final String SELECTOR_VAR_TEMPLATE = "_FS_SEL_VAR_";

  public InductiveWeakeningManager(
      Configuration config,
      Solver pSolver,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    shutdownNotifier = pShutdownNotifier;
    config.inject(this);

    statistics = new InductiveWeakeningStatistics();
    fmgr = pSolver.getFormulaManager();
    logger = pLogger;
    bfmgr = fmgr.getBooleanFormulaManager();
    syntacticWeakeningManager = new SyntacticWeakeningManager(fmgr);
    destructiveWeakeningManager = new DestructiveWeakeningManager(statistics, pSolver, fmgr,
        logger, config);
    cexWeakeningManager = new CEXWeakeningManager(
        fmgr, pSolver, logger, statistics, config, shutdownNotifier);
  }


  /**
   * @return Set of semi-clauses which remain inductive under the transition.
   */
  public Set<BooleanFormula> findInductiveWeakeningForSemiCNF(
      final SSAMap startingSSA,
      Set<BooleanFormula> uninstantiatedClauses,
      PathFormula transition,
      BooleanFormula strengthening
      )
      throws SolverException, InterruptedException {
    Preconditions.checkState(weakeningStrategy != WEAKENING_STRATEGY.DESTRUCTIVE,
        "Destructive weakening is not supported for semiCNF mode, use CEX-based one instead");

    // Mapping from selectors to the items they annotate.
    final BiMap<BooleanFormula, BooleanFormula> selectionInfo = HashBiMap.create();

    BooleanFormula input = annotateConjunctions(
        fmgr.instantiate(new ArrayList<>(uninstantiatedClauses), startingSSA),
        selectionInfo
    );

    BooleanFormula primed = fmgr.instantiate(input, transition.getSsa());
    BooleanFormula query = bfmgr.and(
        ImmutableList.of(input, transition.getFormula(), bfmgr.not(primed), strengthening));

    cexWeakeningManager.setRemovalSelectionStrategy(SELECTION_STRATEGY.ALL);
    Set<BooleanFormula> toAbstract = findSelectorsToAbstract(
        selectionInfo, transition, primed, query, ImmutableSet.<BooleanFormula>of());

    HashSet<BooleanFormula> out = new HashSet<>();
    for (BooleanFormula o : uninstantiatedClauses) {
      if (!toAbstract.contains(selectionInfo.inverse().get(
          fmgr.instantiate(o, startingSSA)
      ))) {
        out.add(o);
      } else {
        logger.log(Level.INFO, "Dropping clause", o);
      }
    }
    return out;
  }

  /**
   * Find the inductive weakening of {@code input} subject to the loop
   * transition over-approximation shown in {@code transition}.
   *
   * @param strengthening Strengthening which is guaranteed to be universally
   * true (under the given path) at the given point.
   */
  public BooleanFormula findInductiveWeakening(
      PathFormula input, PathFormula transition,
      BooleanFormula strengthening
  ) throws SolverException, InterruptedException {

    logger.log(Level.FINE, "Transition = " + transition.getFormula());
    logger.log(Level.FINE, "Input = " + input.getFormula());


    // Convert to NNF
    input = input.updateFormula(
        fmgr.applyTactic(input.getFormula(), Tactic.NNF)
    );


    if (input.getFormula().equals(bfmgr.makeBoolean(true))) {
      return bfmgr.makeBoolean(true);
    }

    // Step 1: get rid of intermediate variables in "input".

    // ...remove atoms containing intermediate variables.
    final ImmutableMap<BooleanFormula, BooleanFormula> selectionVarsInfo;
    Set<BooleanFormula> selectorsWithIntermediate;
    final BooleanFormula query, annotated, primed;
    try {
      statistics.annotationTime.start();


      // Annotate conjunctions.
      Map<BooleanFormula, BooleanFormula> varsInfoBuilder = new HashMap<>();
      annotated = annotateWithSelectors(input.getFormula(), varsInfoBuilder);
      logger.log(Level.FINE, "Annotated formula = " + annotated);
      selectionVarsInfo = ImmutableMap.copyOf(varsInfoBuilder);
      assert !selectionVarsInfo.isEmpty();

      selectorsWithIntermediate = markIntermediate(selectionVarsInfo, input);

      // This is possible since the formula does not have any intermediate
      // variables.
      primed = fmgr.instantiate(annotated, transition.getSsa());
      BooleanFormula negated = bfmgr.not(primed);

      // Inductiveness checking formula, injecting the known invariant "strengthening".
      query = bfmgr.and(ImmutableList.of(
          annotated,
          transition.getFormula(),
          negated,
          strengthening
      ));
    } finally {
      statistics.annotationTime.stop();
    }

    Set<BooleanFormula> selectorsToAbstract = findSelectorsToAbstract(
        selectionVarsInfo, transition, primed, query, selectorsWithIntermediate
    );

    BooleanFormula out = abstractSelectors(
        annotated,
        selectionVarsInfo,
        selectorsToAbstract
    );
    logger.log(Level.FINE, "Slice obtained: ", out);
    return fmgr.uninstantiate(out);
  }

  private Set<BooleanFormula> findSelectorsToAbstract(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      PathFormula transition,
      BooleanFormula primed,
      BooleanFormula query,
      Set<BooleanFormula> selectorsWithIntermediate
  ) throws SolverException, InterruptedException {
    switch (weakeningStrategy) {
      case SYNTACTIC:
        // Intermediate variables don't matter.
        return syntacticWeakeningManager.performWeakening(selectionVarsInfo, transition);
      case DESTRUCTIVE:
        return destructiveWeakeningManager.performWeakening(
            selectionVarsInfo,
            transition,
            query,
            selectorsWithIntermediate);
      case CEX:
        return cexWeakeningManager.performWeakening(
            selectionVarsInfo,
            query,
            primed,
            selectorsWithIntermediate);
      default:
        throw new UnsupportedOperationException("Unexpected enum value");
    }
  }

  /**
   * Apply the transformation, replace the atoms marked by the
   * selector variables with 'Top'.
   *
   * @param annotated Annotated input \phi
   * @param selectionVarsInfo Mapping from selectors to the literals they annotate (unprimed \phi)
   * @param selectorsToAbstract Selectors which should be abstracted.
   *
   */
  private BooleanFormula abstractSelectors(
      BooleanFormula annotated,
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      Set<BooleanFormula> selectorsToAbstract) {
    // Step 3:
    // note: it would be probably better to move those different steps to
    // different subroutines.
    Map<BooleanFormula, BooleanFormula> replacement = new HashMap<>();
    for (BooleanFormula f : selectionVarsInfo.keySet()) {

      if (selectorsToAbstract.contains(f)) {
        replacement.put(f, bfmgr.makeBoolean(true));
      } else {
        replacement.put(f, bfmgr.makeBoolean(false));
      }
    }

    BooleanFormula sliced = fmgr.substitute(annotated, replacement);
    return fmgr.simplify(sliced);
  }

  private BooleanFormula annotateWithSelectors(
      BooleanFormula input,
      final Map<BooleanFormula, BooleanFormula> selectionVarsInfoToFill) {

    if (selectorAnnotationMode == ANNOTATION_MODE.LITERALS) {
      return annotateLiterals(input, selectionVarsInfoToFill);
    } else {
      assert selectorAnnotationMode == ANNOTATION_MODE.CONJUNCTIONS;
      return annotateConjunctions(bfmgr.toConjunctionArgs(input, true),
          selectionVarsInfoToFill);
    }
  }

  private BooleanFormula annotateConjunctions(
      Collection<BooleanFormula> pInput,
      final Map<BooleanFormula, BooleanFormula> pSelectionVarsInfoToFill) {

    Set<BooleanFormula> annotated = new HashSet<>(pInput.size());
    int i = -1;
    for (BooleanFormula f : pInput) {
      annotated.add(
          bfmgr.or(
              makeSelector(pSelectionVarsInfoToFill, f, ++i),
              f
          )
      );
    }
    return bfmgr.and(annotated);
  }

  private BooleanFormula annotateLiterals(
      BooleanFormula pInput,
      final Map<BooleanFormula, BooleanFormula> pSelectionVarsInfoToFill) {

    final UniqueIdGenerator selectorId = new UniqueIdGenerator();

    return bfmgr.transformRecursively(new BooleanFormulaTransformationVisitor(fmgr) {
      @Override
      public BooleanFormula visitNot(BooleanFormula negated) {
        return annotate(bfmgr.not(negated));
      }

      @Override
      public BooleanFormula visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> decl) {
        return annotate(atom);
      }

      BooleanFormula annotate(BooleanFormula input) {
        return bfmgr.or(makeSelector(pSelectionVarsInfoToFill, input, selectorId.getFreshId()), input);
      }
    }, pInput);
  }

  private BooleanFormula makeSelector(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      BooleanFormula toAnnotate,
      int counter
  ) {
    BooleanFormula selector = bfmgr.makeVariable(SELECTOR_VAR_TEMPLATE + counter);
    selectionInfo.put(selector, toAnnotate);
    return selector;
  }

  /**
   * Return a subset of selectors which map to formulas containing intermediate variables.
   */
  private Set<BooleanFormula> markIntermediate(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      final PathFormula phi
  ) throws InterruptedException {

    Set<BooleanFormula> hasIntermediate = new HashSet<>();
    for (Entry<BooleanFormula, BooleanFormula> e : selectionVarsInfo.entrySet()) {
      BooleanFormula key = e.getKey();
      BooleanFormula formula = e.getValue();
      if (!fmgr.getDeadFunctionNames(formula, phi.getSsa()).isEmpty()) {
        hasIntermediate.add(key);
      }
    }
    return hasIntermediate;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }
}
