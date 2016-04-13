package org.sosy_lab.cpachecker.cpa.formulaslicing;


import static org.sosy_lab.cpachecker.cpa.formulaslicing.InductiveWeakeningManager.WEAKENING_STRATEGY.CEX;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

/**
 * Finds inductive weakening of formulas (originally: formula slicing).
 * This class operates on formulas, and should be orthogonal to
 * CFA- and CPA-specific concepts.
 */
@Options(prefix="cpa.slicing")
public class InductiveWeakeningManager implements StatisticsProvider {

  @Option(description="Inductive weakening strategy", secure=true)
  private WEAKENING_STRATEGY weakeningStrategy = CEX;

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

  private static final String SELECTOR_VAR_TEMPLATE = "_FS_SEL_VAR_";

  public InductiveWeakeningManager(
      Configuration config,
      Solver pSolver,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);

    statistics = new InductiveWeakeningStatistics();
    fmgr = pSolver.getFormulaManager();
    logger = pLogger;
    bfmgr = fmgr.getBooleanFormulaManager();
    syntacticWeakeningManager = new SyntacticWeakeningManager(fmgr);
    destructiveWeakeningManager = new DestructiveWeakeningManager(statistics, pSolver, fmgr,
        logger, config);
    cexWeakeningManager = new CEXWeakeningManager(
        fmgr, pSolver, logger, statistics, config, pShutdownNotifier);
  }

  /**
   * This method supports different states of <i>from</i> and <i>to</i> state
   * lemmas. Only the lemmas associated with the <i>to</i> state can be dropped.
   *
   * @param fromStateLemmas Uninstantiated lemmas associated with the
   *                        <i>from</i> state.
   * @param transition Transition from <i>fromState</i> to <i>toState</i>.
   *                   Has to start at {@code startingSSA}.
   * @param toStateLemmas Uninstantiated lemmas associated with the
   *                        <i>to</i> state.
   *
   * @return Subset of {@code toStateLemmas} to which everything in
   * {@code fromStateLemmas} maps.
   */
  public Set<BooleanFormula> findInductiveWeakeningForRCNF(
      SSAMap startingSSA,
      Set<BooleanFormula> fromStateLemmas,
      final PathFormula transition,
      Set<BooleanFormula> toStateLemmas
     )
      throws SolverException, InterruptedException {
    Preconditions.checkState(
        weakeningStrategy != WEAKENING_STRATEGY.DESTRUCTIVE
        && cexWeakeningManager.getRemovalSelectionStrategy() == SELECTION_STRATEGY.ALL);

    // Mapping from selectors to the items they annotate.
    final BiMap<BooleanFormula, BooleanFormula> selectionInfo = HashBiMap.create();

    List<BooleanFormula> fromStateLemmasInstantiated = fmgr.instantiate(
        Lists.newArrayList(fromStateLemmas), startingSSA);

    List<BooleanFormula> toStateLemmasInstantiated = fmgr.instantiate(
        Lists.newArrayList(toStateLemmas), transition.getSsa());
    BooleanFormula toStateLemmasAnnotated = annotateConjunctions(
        toStateLemmasInstantiated, selectionInfo
    );

    final Set<BooleanFormula> toAbstract = findSelectorsToAbstract(
        selectionInfo,
        bfmgr.and(fromStateLemmasInstantiated),
        transition,
        toStateLemmasAnnotated,
        Collections.<BooleanFormula>emptySet(),
        true);

    return Sets.filter(toStateLemmas, new Predicate<BooleanFormula>() {
      @Override
      public boolean apply(BooleanFormula lemma) {
        return (!toAbstract.contains(selectionInfo.inverse().get(
            fmgr.instantiate(lemma, transition.getSsa())
        )));
      }
    });
  }

  /**
   * Find weakening of {@code lemmas} with respect to {@code transition}.
   * This method assumes to- and from- lemmas are the same, and drops both at
   * the same time.
   *
   * @param lemmas Set of uninstantiated lemmas.
   * @return inductive subset of {@code lemmas}
   */
  public Set<BooleanFormula> findInductiveWeakeningForRCNF(
      final SSAMap startingSSA,
      final PathFormula transition,
      Set<BooleanFormula> lemmas
  )
      throws SolverException, InterruptedException {

    // Mapping from selectors to the items they annotate.
    final BiMap<BooleanFormula, BooleanFormula> selectionInfo = HashBiMap.create();

    List<BooleanFormula> fromStateLemmasInstantiated = fmgr.instantiate(
        Lists.newArrayList(lemmas), startingSSA);
    BooleanFormula fromStateLemmasAnnotated = annotateConjunctions(
        fromStateLemmasInstantiated, selectionInfo
    );
    BooleanFormula toStateInstantiated = fmgr.instantiate(
        fromStateLemmasAnnotated, transition.getSsa());

    final Set<BooleanFormula> toAbstract = findSelectorsToAbstract(
        selectionInfo,
        fromStateLemmasAnnotated,
        transition,
        toStateInstantiated,
        Collections.<BooleanFormula>emptySet(),
        false);

    return Sets.filter(lemmas, new Predicate<BooleanFormula>() {
      @Override
      public boolean apply(BooleanFormula lemma) {
        return (!toAbstract.contains(selectionInfo.inverse().get(
            fmgr.instantiate(lemma, startingSSA)
        )));
      }
    });
  }

  /**
   * Find the inductive weakening of {@code input} subject to the loop
   * transition over-approximation shown in {@code transition}.
   *
   * Searches through the space of all literals present in {@code input}.
   */
  public BooleanFormula findInductiveWeakening(
      PathFormula input, PathFormula transition
  ) throws SolverException, InterruptedException {

    // Convert to NNF
    input = input.updateFormula(
        fmgr.applyTactic(input.getFormula(), Tactic.NNF)
    );


    if (bfmgr.isTrue(input.getFormula())) {
      return bfmgr.makeBoolean(true);
    }

    // Step 1: get rid of intermediate variables in "input".

    // ...remove atoms containing intermediate variables.
    final ImmutableMap<BooleanFormula, BooleanFormula> selectionVarsInfo;
    Set<BooleanFormula> selectorsWithIntermediate;
    final BooleanFormula annotated, primed;

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

    Set<BooleanFormula> selectorsToAbstract = findSelectorsToAbstract(
        selectionVarsInfo,
        annotated,
        transition,
        primed,
        selectorsWithIntermediate,
        false
    );

    BooleanFormula out = abstractSelectors(
        annotated,
        selectionVarsInfo,
        selectorsToAbstract
    );
    logger.log(Level.FINE, "Slice obtained: ", out);
    return fmgr.uninstantiate(out);
  }

  /**
   *
   * @param selectionVarsInfo Mapping from the selectors to the already
   *                          instantiated formulas they annotate.
   * @param fromState Instantiated formula representing the state before the
   *                  transition.
   * @param transition Transition under which inductiveness should hold.
   * @param toState Instantiated formula representing the state after the
   *                transition.
   * @param selectorsWithIntermediate Selectors which should be abstracted
   *                                  from the start.
   * @param toAndFromDiffer Whether lemmas associated with the from-
   *                        and to-states differ.
   * @return Set of selectors, subset of {@code selectionVarsInfo} which
   * should be abstracted.
   */
  private Set<BooleanFormula> findSelectorsToAbstract(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      BooleanFormula fromState,
      PathFormula transition,
      BooleanFormula toState,
      Set<BooleanFormula> selectorsWithIntermediate,
      boolean toAndFromDiffer
  ) throws SolverException, InterruptedException {
    switch (weakeningStrategy) {
      case SYNTACTIC:
        // Intermediate variables don't matter.
        if (toAndFromDiffer) {
          final Set<BooleanFormula> fromStateUninstantiatedLemmas =
              bfmgr.toConjunctionArgs(
                  fmgr.uninstantiate(fromState), false
              );
          selectionVarsInfo =
              Maps.filterEntries(selectionVarsInfo,
                  new Predicate<Entry<BooleanFormula, BooleanFormula>>() {
                    @Override
                    public boolean apply(Entry<BooleanFormula, BooleanFormula> e) {
                      BooleanFormula value = e.getValue();
                      return fromStateUninstantiatedLemmas.contains(
                          value
                      );
                    }
                  });
        }
        return syntacticWeakeningManager.performWeakening(
            selectionVarsInfo,
            transition);

      case DESTRUCTIVE:
        return destructiveWeakeningManager.performWeakening(
            selectionVarsInfo,
            fromState,
            transition,
            toState,
            selectorsWithIntermediate);

      case CEX:
        return cexWeakeningManager.performWeakening(
            selectionVarsInfo,
            fromState,
            transition,
            toState,
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
      Map<BooleanFormula, BooleanFormula> selectionInfo, BooleanFormula toAnnotate, int counter) {
    BooleanFormula selector = bfmgr.makeVariable(SELECTOR_VAR_TEMPLATE + counter);
    selectionInfo.put(selector, toAnnotate);
    return selector;
  }

  /**
   * Return a subset of selectors which map to formulas containing intermediate variables.
   */
  private Set<BooleanFormula> markIntermediate(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo, final PathFormula phi) {

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
