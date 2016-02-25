package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;

import java.util.Collection;
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
  private WEAKENING_STRATEGY weakeningStrategy = WEAKENING_STRATEGY.CEX;

  @Option(description="Granularity of weakening", secure=true)
  private ANNOTATION_MODE selectorAnnotationMode = ANNOTATION_MODE.LITERALS;

  @Option(description="Convert to Semi-CNF form", secure=true)
  private boolean toSemiCNF = false;

  private enum ANNOTATION_MODE {

    /**
     * Introduce a selector per each literal.
     */
    LITERALS,

    /**
     * Introduce only one selector per disjunction. Less granular.
     */
    DISJUNCTIONS
  }

  /**
   * Possible weakening strategies.
   */
  private enum WEAKENING_STRATEGY {

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
  private final SemiCNFConverter semiCNFConverter;

  public InductiveWeakeningManager(
      Configuration config,
      FormulaManagerView pFmgr,
      Solver pSolver,
      LogManager pLogger
  ) throws InvalidConfigurationException {
    config.inject(this);

    statistics = new InductiveWeakeningStatistics();
    fmgr = pFmgr;
    logger = pLogger;
    bfmgr = fmgr.getBooleanFormulaManager();
    syntacticWeakeningManager = new SyntacticWeakeningManager(fmgr);
    destructiveWeakeningManager = new DestructiveWeakeningManager(statistics, pSolver, fmgr,
        logger, config);
    cexWeakeningManager = new CEXWeakeningManager(fmgr, pSolver, logger, statistics, config);
    semiCNFConverter = new SemiCNFConverter(fmgr);
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


    if (toSemiCNF) {
      logger.log(Level.INFO, "Semi-CNF conversion is on, enabling disjunction-level annotation");
      selectorAnnotationMode = ANNOTATION_MODE.DISJUNCTIONS;

      input = input.updateFormula(
          semiCNFConverter.toSemiCNF(input.getFormula())
      );
    } else {

      // Convert to NNF
      input = input.updateFormula(
          fmgr.applyTactic(input.getFormula(), Tactic.NNF)
      );
    }


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
      annotated = annotateLiterals(input.getFormula(), varsInfoBuilder);
      selectionVarsInfo = ImmutableMap.copyOf(varsInfoBuilder);
      assert !selectionVarsInfo.isEmpty();

      selectorsWithIntermediate = markIntermediate(selectionVarsInfo, input);

      // This is possible since the formula does not have any intermediate
      // variables.
      primed = fmgr.instantiate(fmgr.uninstantiate(annotated), transition.getSsa());
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

  private BooleanFormula annotateLiterals(
      BooleanFormula input,
      final Map<BooleanFormula, BooleanFormula> selectionVarsInfoToFill) {
    final UniqueIdGenerator selectorId = new UniqueIdGenerator();
    final String selectorVar = "_FS_SEL_VAR_";
    return bfmgr.visit(new BooleanFormulaTransformationVisitor(fmgr) {
      @Override
      public BooleanFormula visitOr(List<BooleanFormula> args) {
        List<BooleanFormula> processed = visitIfNotSeen(args);
        if (selectorAnnotationMode == ANNOTATION_MODE.DISJUNCTIONS) {
          processed.add(makeSelector(bfmgr.or(processed)));
          return bfmgr.or(processed);
        } else {
          return bfmgr.or(processed);
        }
      }

      @Override
      public BooleanFormula visitAtom(BooleanFormula atom, FunctionDeclaration decl) {
        if (selectorAnnotationMode == ANNOTATION_MODE.LITERALS) {
          return bfmgr.or(makeSelector(atom), atom);
        } else {
          return atom;
        }
      }

      public BooleanFormula makeSelector(BooleanFormula toAnnotate) {
        BooleanFormula selector = bfmgr.makeVariable(selectorVar + selectorId.getFreshId());
        selectionVarsInfoToFill.put(selector, toAnnotate);
        return selector;
      }
    }, input);
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
