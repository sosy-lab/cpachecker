package org.sosy_lab.cpachecker.cpa.formulaslicing;


import static org.sosy_lab.cpachecker.cpa.formulaslicing.InductiveWeakeningManager.WEAKENING_STRATEGY.CEX;

import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Finds inductive weakening of formulas (originally: formula slicing).
 * This class operates on formulas, and should be orthogonal to
 * CFA- and CPA-specific concepts.
 */
@Options(prefix="cpa.slicing")
public class InductiveWeakeningManager implements StatisticsProvider {

  @Option(description="Inductive weakening strategy", secure=true)
  private WEAKENING_STRATEGY weakeningStrategy = CEX;

  /**
   * Possible weakening strategies.
   */
  public enum WEAKENING_STRATEGY {

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

  @SuppressWarnings("FieldCanBeLocal")
  private final LogManager logger;
  private final InductiveWeakeningStatistics statistics;
  private final SyntacticWeakeningManager syntacticWeakeningManager;
  private final DestructiveWeakeningManager destructiveWeakeningManager;
  private final CEXWeakeningManager cexWeakeningManager;
  private final Solver solver;

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
    solver = pSolver;
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

    Set<BooleanFormula> out =
        Sets.filter(toStateLemmas, new Predicate<BooleanFormula>() {
          @Override
          public boolean apply(BooleanFormula lemma) {
            return (!toAbstract.contains(selectionInfo.inverse().get(
                fmgr.instantiate(lemma, transition.getSsa())
            )));
          }
        });
    assert checkAllMapsTo(fromStateLemmas, startingSSA, out, transition
        .getSsa(), transition.getFormula());
    return out;
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

    Set<BooleanFormula> out =
        Sets.filter(lemmas, new Predicate<BooleanFormula>() {
          @Override
          public boolean apply(BooleanFormula lemma) {
            return (!toAbstract.contains(selectionInfo.inverse().get(
                fmgr.instantiate(lemma, startingSSA)
            )));
          }
        });
    assert checkAllMapsTo(out, startingSSA, out, transition.getSsa(),
        transition.getFormula());

    return out;
  }

  /**
   * Sanity checking on output.
   */
  private boolean checkAllMapsTo(
      Set<BooleanFormula> from,
      SSAMap startSSA,
      Set<BooleanFormula> to,
      SSAMap finishSSA,
      BooleanFormula transition
  ) throws SolverException, InterruptedException {
    return solver.isUnsat(
        bfmgr.and(
            bfmgr.and(fmgr.instantiate(Lists.newArrayList(from), startSSA)),
            transition,
            bfmgr.not(bfmgr.and(fmgr.instantiate(Lists.newArrayList(to),
                finishSSA)))
        )
    );
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
   * @return Set of selectors which should be abstracted.
   *         Subset of {@code selectionVarsInfo}.
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
        if (toAndFromDiffer) {
          // Allow through only those lemmas which appear in both
          // input and output.
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

        // todo: this interface is probably not sufficient.
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

  private BooleanFormula annotateConjunctions(
      Collection<BooleanFormula> pInput,
      final Map<BooleanFormula, BooleanFormula> pSelectionVarsInfoToFill) {

    Set<BooleanFormula> annotated = new HashSet<>(pInput.size());
    int i = -1;
    for (BooleanFormula f : pInput) {
      BooleanFormula selector = bfmgr.makeVariable(SELECTOR_VAR_TEMPLATE + ++i);
      pSelectionVarsInfoToFill.put(selector, f);
      annotated.add(bfmgr.or(selector, f));
    }
    return bfmgr.and(annotated);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }
}
