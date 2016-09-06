package org.sosy_lab.cpachecker.cpa.policyiteration;

import static org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationManager.DecompositionStatus.ABSTRACTION_REQUIRED;
import static org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationManager.DecompositionStatus.BOUND_COMPUTED;
import static org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationManager.DecompositionStatus.UNBOUNDED;
import static org.sosy_lab.cpachecker.util.AbstractStates.asIterable;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationStatistics.TemplateUpdateEvent;
import org.sosy_lab.cpachecker.cpa.policyiteration.ValueDeterminationManager.ValueDeterminationConstraints;
import org.sosy_lab.cpachecker.cpa.policyiteration.polyhedra.PolyhedraWideningManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.RCNFManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.cpachecker.util.templates.Template.Kind;
import org.sosy_lab.cpachecker.util.templates.TemplatePrecision;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.OptimizationProverEnvironment;
import org.sosy_lab.java_smt.api.OptimizationProverEnvironment.OptStatus;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Main logic in a single class.
 */
@Options(prefix = "cpa.lpi", deprecatedPrefix = "cpa.stator.policy")
public class PolicyIterationManager {

  @Option(secure = true,
      description = "Where to perform abstraction")
  private AbstractionLocations abstractionLocations = AbstractionLocations.LOOPHEAD;

  /**
   * Where an abstraction should be performed.
   */
  public enum AbstractionLocations {

    /**
     * At every node.
     */
    ALL,

    /**
     * Only at loop heads (the most sensible choice).
     */
    LOOPHEAD,

    /**
     * Whenever multiple paths are merged.
     */
    MERGE
  }

  @Option(secure = true, name = "epsilon",
      description = "Value to substitute for the epsilon")
  private Rational EPSILON = Rational.ONE;

  @Option(secure=true, description="Run naive value determination first, "
      + "switch to namespaced if it fails.")
  private boolean runHopefulValueDetermination = true;

  @Option(secure=true, description="Check target states reachability")
  private boolean checkTargetStates = true;

  @Option(secure=true, description="Syntactically pre-compute dependencies for "
      + "value determination")
  private boolean valDetSyntacticCheck = true;

  @Option(secure=true, description="Check whether the policy depends on the initial value")
  private boolean checkPolicyInitialCondition = true;

  @Option(secure=true, description="Remove UFs and ITEs from policies.")
  private boolean linearizePolicy = true;

  @Option(secure=true, description="Generate new templates using polyhedra convex hull")
  private boolean generateTemplatesUsingConvexHull = false;

  @Option(secure=true, description="Use caching optimization solver")
  private boolean useCachingOptSolver = false;

  @Option(secure=true, description="Compute abstraction for larger templates "
      + "using decomposition")
  private boolean computeAbstractionByDecomposition = false;

  @Option(secure=true, description="Number of value determination steps allowed before widening is run."
      + " Value of '-1' runs value determination until convergence.")
  private int wideningThreshold = -1;

  @Option(secure=true, description="Algorithm for converting a formula to a "
      + "set of lemmas", toUppercase=true, values={"CNF", "RCNF", "NONE"})
  private String toLemmasAlgorithm = "RCNF";

  @Option(secure=true, description="Do not compute the abstraction until "
      + "strengthen is called. This speeds up the computation, but does not "
      + "let other CPAs use the output of LPI.")
  private boolean delayAbstractionUntilStrengthen = false;

  @Option(secure=true, description="Use the new SSA after the merge operation.")
  private boolean useNewSSAAfterMerge = false;

  private final FormulaManagerView fmgr;
  private final CFA cfa;
  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ValueDeterminationManager vdfmgr;
  private final PolicyIterationStatistics statistics;
  private final FormulaLinearizationManager linearizationManager;
  private final PolyhedraWideningManager pwm;
  private final StateFormulaConversionManager stateFormulaConversionManager;
  private final RCNFManager rcnfManager;
  private final TemplatePrecision initialPrecision;
  private final TemplateToFormulaConversionManager templateToFormulaConversionManager;
  @Nullable private BlockPartitioning partitioning;

  public PolicyIterationManager(
      Configuration pConfig,
      FormulaManagerView pFormulaManager,
      CFA pCfa,
      PathFormulaManager pPfmgr,
      Solver pSolver,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      ValueDeterminationManager pValueDeterminationFormulaManager,
      PolicyIterationStatistics pStatistics,
      FormulaLinearizationManager pLinearizationManager,
      PolyhedraWideningManager pPwm,
      StateFormulaConversionManager pStateFormulaConversionManager,
      TemplateToFormulaConversionManager pTemplateToFormulaConversionManager)
      throws InvalidConfigurationException {
    templateToFormulaConversionManager = pTemplateToFormulaConversionManager;
    pConfig.inject(this, PolicyIterationManager.class);
    pwm = pPwm;
    stateFormulaConversionManager = pStateFormulaConversionManager;
    fmgr = pFormulaManager;
    cfa = pCfa;
    pfmgr = pPfmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    solver = pSolver;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    vdfmgr = pValueDeterminationFormulaManager;
    statistics = pStatistics;
    linearizationManager = pLinearizationManager;
    rcnfManager = new RCNFManager(pConfig);
    initialPrecision = new TemplatePrecision(
        logger, pConfig, cfa, templateToFormulaConversionManager);
  }

  /**
   * Location of the state is defined as follows:
   * if we perform an adjustment, and there's already another state in the
   * same partition (something we are about to get merged with), we take their
   * locationID.
   * Otherwise, we generate a fresh one.
   */
  private final UniqueIdGenerator locationIDGenerator = new UniqueIdGenerator();

  /**
   * @param pNode Initial node.
   * @return Initial state for the analysis, assuming the first node
   * is {@code pNode}.
   */
  public PolicyState getInitialState(CFANode pNode) {
    return PolicyAbstractedState.empty(
        pNode,
        bfmgr.makeBoolean(true), stateFormulaConversionManager);
  }

  public Precision getInitialPrecision() {
    return initialPrecision;
  }

  public Collection<? extends PolicyState> getAbstractSuccessors(
      PolicyState oldState, CFAEdge edge) throws CPATransferException, InterruptedException {

    CFANode node = edge.getSuccessor();
    PolicyIntermediateState iOldState;

    if (oldState.isAbstract()) {
      iOldState = stateFormulaConversionManager.abstractStateToIntermediate(
          oldState.asAbstracted(), false);
    } else {
      iOldState = oldState.asIntermediate();
    }

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), edge);
    PolicyIntermediateState out = PolicyIntermediateState.of(
        node,
        outPath,
        iOldState.getBackpointerState());

    return Collections.singleton(out);
  }

  /**
   * Pre-abstraction strengthening.
   */
  Collection<? extends AbstractState> strengthen(
      PolicyIntermediateState pState, List<AbstractState> pOtherStates)
      throws CPATransferException, InterruptedException {

    // Collect assumptions.
    FluentIterable<CExpression> assumptions =
        FluentIterable.from(pOtherStates)
            .filter(AbstractStateWithAssumptions.class)
            .transformAndConcat(AbstractStateWithAssumptions::getAssumptions)
            .filter(CExpression.class);

    if (assumptions.isEmpty()) {

      // No changes required.
      return Collections.singleton(pState);
    }

    PathFormula pf = pState.getPathFormula();
    for (CExpression assumption : assumptions) {
      pf = pfmgr.makeAnd(pf, assumption);
    }

    return Collections.singleton(pState.withPathFormula(pf));
  }

  public Optional<PrecisionAdjustmentResult> precisionAdjustment(
      final PolicyState inputState,
      final TemplatePrecision inputPrecision,
      final UnmodifiableReachedSet states,
      final AbstractState pArgState) throws CPAException, InterruptedException {
    return precisionAdjustment0(inputState, inputPrecision, states, pArgState)
        .flatMap(
            s -> Optional.of(PrecisionAdjustmentResult.create(
                s, inputPrecision, Action.CONTINUE
            ))
    );
  }

  /**
   * Perform abstraction and reachability checking with precision adjustment
   * operator.
   */
  private Optional<PolicyState> precisionAdjustment0(
      final PolicyState inputState,
      final TemplatePrecision inputPrecision,
      final UnmodifiableReachedSet states,
      final AbstractState pArgState) throws CPAException, InterruptedException {

    if (inputState.isAbstract()) {

      // TODO: might have to change sibling and locationID if we are coming from BAM.
      return Optional.of(inputState);
    }
    Preconditions.checkState(!inputState.isAbstract());

    PolicyIntermediateState iState = inputState.asIntermediate();
    boolean hasTargetState = !AbstractStates.asIterable(pArgState)
        .filter(AbstractStates::isTargetState)
        .isEmpty();

    // Formulas reported by other CPAs.
    BooleanFormula extraInvariant = extractFormula(pArgState);

    CFANode node = iState.getNode();
    final boolean shouldAbstract = shouldPerformAbstraction(iState, pArgState);

    // Perform reachability checking, for property states, or before the abstractions.
    boolean isTarget = hasTargetState && checkTargetStates;
    if (((isTarget) || shouldAbstract)
        && isUnreachable(iState, extraInvariant, isTarget)) {

      logger.log(Level.INFO, "Returning bottom state");
      return Optional.empty();
    }

    // Perform the abstraction, if necessary.
    if (shouldAbstract) {
      Optional<PolicyAbstractedState> sibling = findSibling(iState, states, pArgState);

      PolicyAbstractedState abstraction;
      int locationID = getLocationID(sibling, node);

      if (delayAbstractionUntilStrengthen) {
        return Optional.of(PolicyAbstractedState.top(
            node, locationID, stateFormulaConversionManager,
            iState.getPathFormula().getSsa(),
            iState.getPathFormula().getPointerTargetSet(),
            extraInvariant, iState, sibling
        ));
      }

      statistics.abstractionTimer.start();
      try {
        abstraction = performAbstraction(
            iState, locationID, inputPrecision, extraInvariant, sibling);
        logger.log(Level.FINE, ">>> Abstraction produced a state: ", abstraction);
      } finally {
        statistics.abstractionTimer.stop();
      }

      PolicyAbstractedState outState;
      if (sibling.isPresent()) {

        // Emulate large-step (join followed by value-determination) on the
        // resulting abstraction at the same location.
        outState = emulateLargeStep(abstraction, sibling.get(), inputPrecision, extraInvariant);
      } else {
        outState = abstraction;
      }
      return Optional.of(outState);
    } else {
      return Optional.of(iState);
    }
  }

  /**
   * Post-precision-adjustment strengthening.
   *
   * <p>Injecting new invariants might force us to re-compute the abstraction.
   */
  public Optional<AbstractState> strengthen(
      PolicyState pState, TemplatePrecision pPrecision,
      List<AbstractState> pOtherStates)
      throws CPAException, InterruptedException {
    if (!pState.isAbstract()) {
      return Optional.of(pState);
    }

    // Strengthening only runs on abstracted states.
    PolicyAbstractedState aState = pState.asAbstracted();
    PolicyIntermediateState iState = aState.getGeneratingState().get();

    // We re-perform abstraction and value determination.
    BooleanFormula strengthening =
        bfmgr.and(
            pOtherStates
                .stream()
                .map(state -> AbstractStates.extractReportedFormulas(fmgr, state))
                .filter(state -> !bfmgr.isTrue(state))
                .collect(Collectors.toList())
        );
    if (bfmgr.isTrue(strengthening) && !delayAbstractionUntilStrengthen) {

      // No interesting strengthening.
      return Optional.of(pState);
    }

    if (isUnreachable(iState, strengthening, false)) {

      logger.log(Level.INFO, "Returning bottom state");
      return Optional.empty();
    }

    PolicyAbstractedState abstraction;
    statistics.abstractionTimer.start();
    try {
      abstraction =
          performAbstraction(
              iState, aState.getLocationID(), pPrecision, strengthening, aState.getSibling());
    } finally {
      statistics.abstractionTimer.stop();
    }
    PolicyAbstractedState outState;
    if (aState.getSibling().isPresent()) {

      // Emulate large-step (join followed by value-determination) on the
      // resulting abstraction at the same location.
      outState =
          emulateLargeStep(abstraction, aState.getSibling().get(), pPrecision, strengthening);
    } else {
      outState = abstraction;
    }
    if (outState.equals(pState)) {
      return Optional.of(pState);
    }
    return Optional.of(outState);
  }

  private int getLocationID(Optional<PolicyAbstractedState> sibling, CFANode node) {
    int locationID;
    if (sibling.isPresent()) {
      locationID = sibling.get().getLocationID();
    } else {
      locationID = getFreshLocationID();
      logger.log(Level.INFO, "Generating new location ID", locationID, " for node ", node);
    }
    return locationID;
  }

  int getFreshLocationID() {
    return locationIDGenerator.getFreshId();
  }

  /**
   * Emulate the JOIN step on {@code newState} (recently produced abstracted state)
   * and {@code latestSibling} (state in the same {@link ReachedSet} partition found by following
   * backpointers.
   */
  private PolicyAbstractedState emulateLargeStep(
      PolicyAbstractedState newState,
      PolicyAbstractedState latestSibling,
      TemplatePrecision precision,
      BooleanFormula extraInvariant
      ) throws CPATransferException, InterruptedException {

    Map<Template, PolicyBound> updated = new HashMap<>();
    PolicyAbstractedState merged = unionAbstractedStates(
          newState, latestSibling, precision, updated, extraInvariant);
    PolicyAbstractedState out;
    if (updated.isEmpty()) {
      out = merged;

    } else {
      ValueDeterminationConstraints constraints;
      Optional<PolicyAbstractedState> element = Optional.empty();
      if (runHopefulValueDetermination) {
        constraints = vdfmgr.valueDeterminationFormulaCheap(
            newState, latestSibling, merged, updated.keySet());
        element = performValueDetermination(merged, updated, constraints);
        if (!element.isPresent()) {
          logger.log(Level.INFO, "Switching to more expensive value "
              + "determination strategy.");
        }
      }

      if (!element.isPresent()) {

        // Hopeful value determination failed, run the more expensive version.
        constraints = vdfmgr.valueDeterminationFormula(
            newState, latestSibling, merged, updated.keySet());
        element = performValueDetermination(merged, updated, constraints);
        if (!element.isPresent()) {
          throw new CPATransferException("Value determination problem is "
              + "unfeasible at node " + newState.getNode());
        }
      }
      out = element.get();
    }

    return out;
  }

  /**
   * At every join, update all the references to starting states to the
   * latest ones.
   */
  private PolicyIntermediateState joinIntermediateStates(
      PolicyIntermediateState newState,
      PolicyIntermediateState oldState
  ) throws InterruptedException {

    Preconditions.checkState(newState.getNode() == oldState.getNode());

    if (!newState.getBackpointerState().equals(oldState.getBackpointerState())) {

      // Different parents: do not merge.
      return oldState;
    }

    if (newState.isMergedInto(oldState)) {
      return oldState;
    } else if (oldState.isMergedInto(newState)) {
      return newState;
    }

    PathFormula mergedPath = pfmgr.makeOr(newState.getPathFormula(),
                                          oldState.getPathFormula());
    PolicyIntermediateState out = PolicyIntermediateState.of(
        newState.getNode(),
        mergedPath,
        oldState.getBackpointerState());

    newState.setMergedInto(out);
    oldState.setMergedInto(out);
    return out;
  }

  /**
   * Merge two states, populate the {@code updated} mapping.
   */
  private PolicyAbstractedState unionAbstractedStates(
      final PolicyAbstractedState newState,
      final PolicyAbstractedState oldState,
      final TemplatePrecision precision,
      Map<Template, PolicyBound> updated,
      BooleanFormula extraInvariant) {
    Preconditions.checkState(newState.getNode() == oldState.getNode());
    Preconditions.checkState(
        newState.getLocationID() == oldState.getLocationID());

    if (isLessOrEqualAbstracted(newState, oldState)) {

      // New state does not introduce any updates.
      return oldState;
    }

    statistics.abstractMergeCounter.add(oldState.getLocationID());
    Map<Template, PolicyBound> newAbstraction = new HashMap<>();

    // Pick the biggest bound, and keep the biggest trace to match.
    for (Template template : precision.getTemplatesForNode(newState.getNode())) {
      Optional<PolicyBound> oldValue = oldState.getBound(template);
      Optional<PolicyBound> newValue = newState.getBound(template);

      if (!newValue.isPresent() || !oldValue.isPresent()) {

        // Either is unbounded: no need to do anything.
        continue;
      }
      PolicyBound mergedBound;
      if (newValue.get().getBound().compareTo(oldValue.get().getBound()) > 0) {
        TemplateUpdateEvent updateEvent = TemplateUpdateEvent.of(
            newState.getLocationID(), template);

        if (statistics.templateUpdateCounter.count(updateEvent) == wideningThreshold) {

          // Set the value to infinity if the widening threshold was reached.
          logger.log(Level.FINE, "Widening threshold for template", template,
              "at", newState.getNode(), "was reached, widening to infinity.");
          continue;
        }
        mergedBound = newValue.get();
        updated.put(template, mergedBound);

        logger.log(Level.FINE, "Updating template", template, "at",
            newState.getNode(),
            "to", newValue.get().getBound(),
            "(was: ", oldValue.get().getBound(), ")");
        statistics.templateUpdateCounter.add(updateEvent);
      } else {
        mergedBound = oldValue.get();
      }
      newAbstraction.put(template, mergedBound);
    }

    // Cache coherence for CachingPathFormulaManager is better with oldSSA,
    // but newSSA is required for LPI+BAM.
    SSAMap mergedSSA = useNewSSAAfterMerge ? newState.getSSA() : oldState.getSSA();

    PolicyAbstractedState merged =
        PolicyAbstractedState.of(
            newAbstraction,
            oldState.getNode(),
            newState.getLocationID(),
            stateFormulaConversionManager,
            mergedSSA,
            newState.getPointerTargetSet(),
            extraInvariant,
            newState.getGeneratingState(),

            // Sibling used to emulate the union.
            Optional.of(oldState));

    if (generateTemplatesUsingConvexHull) {
      precision.addGeneratedTemplates(
          pwm.generateWideningTemplates(oldState, newState));
    }

    assert isLessOrEqualAbstracted(newState, merged)
        && isLessOrEqualAbstracted(oldState, merged) :
        "Merged state should be larger than the subsumed one";
    return merged;
  }

  private Optional<PolicyAbstractedState> performValueDetermination(
      PolicyAbstractedState stateWithUpdates,
      Map<Template, PolicyBound> updated,
      ValueDeterminationConstraints valDetConstraints
  ) throws InterruptedException, CPATransferException {
    logger.log(Level.INFO, "Value determination at node",
        stateWithUpdates.getNode(), ", #constraints = ", valDetConstraints.constraints.size());
    Map<Template, PolicyBound> newAbstraction =
        new HashMap<>(stateWithUpdates.getAbstraction());
    int locId = stateWithUpdates.getLocationID();

    // Maximize for each template subject to the overall constraints.
    statistics.valueDeterminationTimer.start();
    try (OptimizationProverEnvironment optEnvironment = solver.newOptEnvironment()) {

      valDetConstraints.constraints.forEach(c -> optEnvironment.addConstraint(c));

      for (Entry<Template, PolicyBound> entry : updated.entrySet()) {
        shutdownNotifier.shutdownIfNecessary();
        optEnvironment.push();

        Template template = entry.getKey();
        PolicyBound mergedBound = entry.getValue();
        Formula objective = valDetConstraints.outVars.get(template, locId);
        BooleanFormula consistencyConstraint = fmgr.makeGreaterOrEqual(
                objective,
                fmgr.makeNumber(objective, mergedBound.getBound()), true);

        optEnvironment.addConstraint(consistencyConstraint);
        int handle = optEnvironment.maximize(objective);

        OptStatus result;
        try {
          statistics.optTimer.start();
          result = optEnvironment.check();
        } finally {
          statistics.optTimer.stop();
        }
        if (result == OptStatus.UNSAT) {
          shutdownNotifier.shutdownIfNecessary();
          return Optional.empty();
        } else if (result == OptStatus.UNDEF) {
          shutdownNotifier.shutdownIfNecessary();
          logger.log(Level.WARNING,
              "Solver returned undefined status on the problem: ");
          logger.log(Level.INFO, optEnvironment);
          throw new CPATransferException("Unexpected solver state");
        }
        assert result == OptStatus.OPT;

        Optional<Rational> value = optEnvironment.upper(handle, EPSILON);

        if (value.isPresent() &&
            !templateToFormulaConversionManager.isOverflowing(template, value.get())) {
          Rational v = value.get();
          logger.log(Level.FINE, "Updating", template, "to value", v);
          newAbstraction.put(template, mergedBound.updateValueFromValueDetermination(v));
        } else {

          // Unbounded.
          newAbstraction.remove(template);
        }
        optEnvironment.pop();
      }
    } catch(SolverException e){
      throw new CPATransferException("Failed maximization ", e);
    } finally{
      statistics.valueDeterminationTimer.stop();
    }

    return Optional.of(stateWithUpdates.withNewAbstraction(newAbstraction));
  }

  /**
   * @return Whether the <code>state</code> is unreachable.
   */
  private boolean isUnreachable(
      PolicyIntermediateState state,
      BooleanFormula extraInvariant,
      boolean pIsTarget)
      throws CPAException, InterruptedException {
    BooleanFormula startConstraints =
        stateFormulaConversionManager.getStartConstraintsWithExtraInvariant(state);
    PathFormula pf = state.getPathFormula();

    BooleanFormula constraint = bfmgr.and(
        startConstraints,
        pf.getFormula(),
        fmgr.instantiate(extraInvariant, pf.getSsa())
    );

    try {
      statistics.checkSATTimer.start();
      boolean out = solver.isUnsat(
          bfmgr.toConjunctionArgs(constraint, true), state.getNode());
      if (!out && pIsTarget) {

        // Set counterexample information for reachable target states for visualization purposes.
        try (ProverEnvironment env = solver.newProverEnvironment
            (ProverOptions.GENERATE_MODELS)) {
          env.push(constraint);
          Preconditions.checkState(!env.isUnsat());
          state.setCounterexample(env.getModelAssignments());
        }
      }
      return out;
    } catch (SolverException e) {
      throw new CPATransferException("Failed solving", e);
    } finally {
      statistics.checkSATTimer.stop();
    }
  }

  /**
   * Derive policy bound from the optimization result.
   */
  private Optional<PolicyBound> getPolicyBound(
      Template template,
      TemplatePrecision precision,
      OptimizationProverEnvironment optEnvironment,
      Optional<Rational> bound,
      BooleanFormula annotatedFormula,
      PathFormula p,
      PolicyIntermediateState state,
      Formula objective
      ) throws SolverException, InterruptedException {

    statistics.getBoundTimer.start();
    try {
      boolean unsignedAndLower = template.isUnsigned() &&
          (template.getKind() == Kind.NEG_LOWER_BOUND ||
              template.getKind() == Kind.NEG_SUM_LOWER_BOUND);
      if ((bound.isPresent()
              && !templateToFormulaConversionManager.isOverflowing(template, bound.get()))
          || unsignedAndLower) {
        Rational boundValue;
        if (bound.isPresent() && unsignedAndLower) {
          boundValue = Rational.max(bound.get(), Rational.ZERO);
        } else if (bound.isPresent()){
          boundValue = bound.get();
        } else {
          boundValue = Rational.ZERO;
        }

        try (Model model = optEnvironment.getModel()) {
          BooleanFormula linearizedFormula = annotatedFormula;
          if (linearizePolicy) {
            statistics.linearizationTimer.start();
            linearizedFormula = linearizationManager.convertToPolicy(
                annotatedFormula, model);
            statistics.linearizationTimer.stop();
          }

          PolicyBound policyBound = modelToPolicyBound(
              objective, state, precision, p, linearizedFormula, model,
              boundValue);
          return Optional.of(policyBound);
        }
      }
      return Optional.empty();
    } finally {
      statistics.getBoundTimer.stop();
    }
  }

  private Set<BooleanFormula> toLemmas(BooleanFormula formula)
      throws InterruptedException {
    switch (toLemmasAlgorithm) {
      case "CNF":
        return bfmgr.toConjunctionArgs(
            fmgr.applyTactic(formula, Tactic.TSEITIN_CNF), true);
      case "RCNF":
        return rcnfManager.toLemmas(formula, fmgr);
      case "NONE":
        return ImmutableSet.of(formula);
      default:
        throw new UnsupportedOperationException("Unexpected state");
    }
  }

  private final Map<Formula, Set<String>> functionNamesCache = new HashMap<>();
  private Set<String> extractFunctionNames(Formula f) {
    Set<String> out = functionNamesCache.get(f);
    if (out == null) {
      out = fmgr.extractFunctionNames(f);
      functionNamesCache.put(f, out);
    }
    return out;
  }

  /**
   * Perform the abstract operation on a new state
   *
   * @param generatorState State to abstract
   * @return Abstracted state if the state is reachable, empty optional
   * otherwise.
   */
  private PolicyAbstractedState performAbstraction(
      final PolicyIntermediateState generatorState,
      int locationID,
      TemplatePrecision precision,
      BooleanFormula extraInvariant,
      Optional<PolicyAbstractedState> pSibling)
      throws CPAException, InterruptedException {

    logger.log(Level.FINE, "Performing abstraction at node: ", generatorState.getNode());

    final CFANode node = generatorState.getNode();
    final PathFormula p = generatorState.getPathFormula();
    final BooleanFormula startConstraints =
        stateFormulaConversionManager.getStartConstraintsWithExtraInvariant(generatorState);

    Set<BooleanFormula> startConstraintLemmas = toLemmas(startConstraints);
    Set<BooleanFormula> lemmas = toLemmas(p.getFormula());

    final Map<Template, PolicyBound> abstraction = new HashMap<>();

    try (OptimizationProverEnvironment optEnvironment = newOptProver()) {

      optEnvironment.push();
      optEnvironment.addConstraint(startConstraints);
      optEnvironment.push();

      for (Template template : precision.getTemplatesForNode(node)) {
        optEnvironment.pop();
        optEnvironment.push();

        // Optimize for the template subject to the
        // constraints introduced by {@code p}.
        Formula objective = templateToFormulaConversionManager.toFormula(
            pfmgr, fmgr, template, p);
        Set<String> objectiveVars = extractFunctionNames(objective);

        if (computeAbstractionByDecomposition) {
          Pair<DecompositionStatus, PolicyBound> res = computeByDecomposition(
              template, p, lemmas, startConstraintLemmas, abstraction);
          switch (res.getFirstNotNull()) {
            case BOUND_COMPUTED:

              // Put the computed bound.
              PolicyBound bound = res.getSecondNotNull();
              if (checkPolicyInitialCondition) {
                bound = updatePolicyBoundDependencies(bound, objective);
              }
              abstraction.put(template, bound);
              continue;
            case UNBOUNDED:

              // Any of the components is unbounded => the sum is unbounded as
              // well.
              continue;
            case ABSTRACTION_REQUIRED:

              // Continue with abstraction.
              break;
            default:
              throw new UnsupportedOperationException("Unexpected case");
          }
        }

        Set<BooleanFormula> slicedConstraint = computeRelevantSubset(
            lemmas, startConstraintLemmas, objectiveVars);
        BooleanFormula f = bfmgr.and(slicedConstraint);

        // Linearize & add choice variables.
        statistics.linearizationTimer.start();
        BooleanFormula annotatedFormula = linearizationManager.annotateDisjunctions(
            linearizationManager.linearize(f)
        );
        statistics.linearizationTimer.stop();

        // Skip updates if the edge does not have any variables mentioned in the
        // template.
        if (bfmgr.isTrue(f)) {
          if (generatorState.getBackpointerState().getAbstraction().get(template) == null) {

            // Unbounded.
            continue;
          }

          PolicyBound bound = generatorState.getBackpointerState().getAbstraction().get(template);
          abstraction.put(template, bound);
        }

        optEnvironment.addConstraint(annotatedFormula);

        int handle = optEnvironment.maximize(objective);

        OptStatus status;
        try {
          statistics.optTimer.start();
          status = optEnvironment.check();
        } finally {
          statistics.optTimer.stop();
        }

        switch (status) {
          case OPT:

            Optional<Rational> bound = optEnvironment.upper(handle, EPSILON);
            Optional<PolicyBound> policyBound = getPolicyBound(
                template, precision, optEnvironment, bound, annotatedFormula,
                p, generatorState, objective);
            if (policyBound.isPresent()) {
              abstraction.put(template, policyBound.get());
            }

            logger.log(Level.FINE, "Got bound: ", bound);
            break;

          case UNSAT:
            throw new CPAException("Unexpected UNSAT");

          case UNDEF:
            logger.log(Level.WARNING, "Solver returned undefined status on the problem: ");
            logger.log(Level.INFO, optEnvironment.toString());
            throw new CPATransferException("Solver returned undefined status");
          default:
            throw new AssertionError("Unhandled enum value in switch: " + status);
        }

      }
    } catch (SolverException e) {
      throw new CPATransferException("Solver error: ", e);
    }

    statistics.updateCounter.add(locationID);

    return PolicyAbstractedState.of(
        abstraction,
        generatorState.getNode(),
        locationID,
        stateFormulaConversionManager,
        generatorState.getPathFormula().getSsa(),
        generatorState.getPathFormula().getPointerTargetSet(),
        extraInvariant,
        Optional.of(generatorState),
        pSibling);
  }

  private OptimizationProverEnvironment newOptProver() {
    if (useCachingOptSolver) {
      return solver.newCachedOptEnvironment();
    } else {
      return solver.newOptEnvironment();
    }
  }

  private PolicyBound updatePolicyBoundDependencies(
      PolicyBound bound, Formula objective
  ) throws SolverException, InterruptedException {
    statistics.checkIndependenceTimer.start();
    try {
      if (solver.isUnsat(bfmgr.and(
          bound.getFormula().getFormula(),
          fmgr.makeGreaterThan(
              objective,
              fmgr.makeNumber(objective, bound.getBound()), true)
      ))) {
        return bound.withNoDependencies();
      } else {
        return bound;
      }
    } finally {
      statistics.checkIndependenceTimer.stop();
    }
  }

  enum DecompositionStatus {
    BOUND_COMPUTED,
    ABSTRACTION_REQUIRED,
    UNBOUNDED,
  }

  /**
   * Tries to shortcut an abstraction computation.
   *
   * @param lemmas Input in RCNF form.
   */
  private Pair<DecompositionStatus, PolicyBound> computeByDecomposition(
      Template pTemplate,
      PathFormula pFormula,
      Set<BooleanFormula> lemmas,
      Set<BooleanFormula> startConstraintLemmas,
      Map<Template, PolicyBound> currentAbstraction) {

    if (pTemplate.size() == 1) {
      return Pair.of(ABSTRACTION_REQUIRED, null);
    }

    // Slices and bounds for all template sub-components.
    List<Set<BooleanFormula>> slices = new ArrayList<>(pTemplate.size());
    List<PolicyBound> policyBounds = new ArrayList<>();
    List<Rational> coefficients = new ArrayList<>();

    for (Entry<CIdExpression, Rational> e : pTemplate.getLinearExpression()) {
      CIdExpression c = e.getKey();
      Rational r = e.getValue();
      LinearExpression<CIdExpression> le = LinearExpression.ofVariable(c);

      Template singleton;
      if (r.signum() < 0) {
        singleton = Template.of(le.negate());
      } else {
        singleton = Template.of(le);
      }

      Set<String> variables = extractFunctionNames(
          templateToFormulaConversionManager.toFormula(pfmgr, fmgr, singleton, pFormula)
      );

      // Subset of lemmas relevant to the set |variables|.
      Set<BooleanFormula> lemmasSubset = computeRelevantSubset(
          lemmas, startConstraintLemmas, variables);
      slices.add(lemmasSubset);
      policyBounds.add(currentAbstraction.get(singleton));
      coefficients.add(r);
    }

    // Fast-fail iff not all lemmas in slices are disjoint: a simple quadratic
    // test.
    for (int sliceIdx=0; sliceIdx<slices.size(); sliceIdx++) {
      for (int otherSliceIdx=sliceIdx+1; otherSliceIdx<slices.size(); otherSliceIdx++) {
        Set<BooleanFormula> sliceA = slices.get(sliceIdx);
        Set<BooleanFormula> sliceB = slices.get(otherSliceIdx);
        if (!Sets.intersection(sliceA, sliceB).isEmpty()) {
          return Pair.of(ABSTRACTION_REQUIRED, null);
        }
      }
    }

    // One unbounded => all unbounded (sign is taken into account).
    if (policyBounds.stream().filter(policyBound -> policyBound == null)
        .iterator().hasNext()) {
      return Pair.of(UNBOUNDED, null);
    }

    // Abstraction required if not all predecessors, SSA forms,
    // and pointer target sets are the same.
    PolicyBound firstBound = policyBounds.get(0);
    for (PolicyBound bound : policyBounds) {
      if (!bound.getPredecessor().equals(firstBound.getPredecessor())
          || !bound.getFormula().getSsa().equals(firstBound.getFormula().getSsa())
          || !bound.getFormula().getPointerTargetSet().equals(firstBound
                .getFormula().getPointerTargetSet())) {
        return Pair.of(ABSTRACTION_REQUIRED, null);
      }
    }

    Set<Template> allDependencies = new HashSet<>();
    Set<BooleanFormula> policies = new HashSet<>();
    Rational combinedBound = Rational.ZERO;
    for (int i=0; i<policyBounds.size(); i++) {
      PolicyBound bound = policyBounds.get(i);
      combinedBound = combinedBound.plus(
          bound.getBound().times(coefficients.get(i)));
      allDependencies.addAll(bound.getDependencies());
      policies.add(bound.getFormula().getFormula());
    }
    BooleanFormula policy = bfmgr.and(policies);

    return Pair.of(BOUND_COMPUTED, PolicyBound.of(
        firstBound.getFormula().updateFormula(policy),
        combinedBound,
        firstBound.getPredecessor(),
        allDependencies
    ));
  }


  /**
   * @param supportingLemmas Closure computation should be done with respect
   *                         to those variables.
   *
   * @return Subset {@code input},
   * which exactly preserves the state-space with respect to all variables in
   * {@code vars}.
   */
  private Set<BooleanFormula> computeRelevantSubset(
      Set<BooleanFormula> input,
      Set<BooleanFormula> supportingLemmas,
      Set<String> vars
  ) {
    final Set<String> closure = relatedClosure(
        Sets.union(input, supportingLemmas), vars);

    return input.stream().filter(
        l -> !Sets.intersection(extractFunctionNames(l), closure).isEmpty()
    ).collect(Collectors.toSet());
  }

  /**
   * @param input Set of lemmas.
   * @param vars Vars to perform the closure with respect to.
   * @return Set of variables contained in the closure.
   */
  private Set<String> relatedClosure(
      Set<BooleanFormula> input,
      Set<String> vars) {
    Set<String> related = new HashSet<>(vars);
    while (true) {
      boolean modified = false;
      for (BooleanFormula atom : input) {
        Set<String> containedVars = extractFunctionNames(atom);
        if (!Sets.intersection(containedVars, related).isEmpty()) {
          modified |= related.addAll(containedVars);
        }
      }
      if (!modified) {
        break;
      }
    }
    return related;
  }

  /**
   * Use the auxiliary variables from the {@code model} to reconstruct the
   * policy which was used for abstracting the state.
   */
  private PolicyBound modelToPolicyBound(
      Formula templateObjective,
      PolicyIntermediateState inputState,
      TemplatePrecision precision,
      PathFormula inputPathFormula,
      BooleanFormula annotatedFormula,
      Model model,
      Rational bound) throws SolverException, InterruptedException {

    statistics.linearizationTimer.start();
    final BooleanFormula policyFormula = linearizationManager.enforceChoice(
        annotatedFormula, model);
    statistics.linearizationTimer.stop();
    final boolean dependsOnInitial;

    if (checkPolicyInitialCondition) {
      statistics.checkIndependenceTimer.start();
      try {
        dependsOnInitial = !solver.isUnsat(bfmgr.and(
            policyFormula,
            fmgr.makeGreaterThan(
                templateObjective,
                fmgr.makeNumber(templateObjective, bound), true)));

      } finally {
        statistics.checkIndependenceTimer.stop();
      }
    } else {
        dependsOnInitial = true;
    }

    PolicyAbstractedState backpointer = inputState.getBackpointerState();

    Set<String> policyVars = extractFunctionNames(policyFormula);
    Collection<Template> dependencies;
    if (!dependsOnInitial) {
      dependencies = new ArrayList<>();
    } else if (!valDetSyntacticCheck) {
      dependencies = precision.getTemplatesForNode(backpointer.getNode());
    } else {
      dependencies = new ArrayList<>();

      // Context for converting the template to formula, used for determining
      // used SSA map and PointerTargetSet.
      PathFormula contextFormula =
          stateFormulaConversionManager.getPathFormula(backpointer, fmgr, false);
      for (Entry<Template, PolicyBound> entry : backpointer) {
        Template t = entry.getKey();
        Set<String> fVars = extractFunctionNames(
            templateToFormulaConversionManager.toFormula(
                pfmgr, fmgr, t, contextFormula));
        if (!Sets.intersection(fVars, policyVars).isEmpty()) {
          dependencies.add(t);
        }
      }
    }

    return PolicyBound.of(
        inputPathFormula.updateFormula(policyFormula), bound, backpointer,
        dependencies);
  }

  /**
   * @param totalState Encloses all other parallel states.
   * @return Whether to compute the abstraction when creating a new
   * state associated with <code>node</code>.
   */
  private boolean shouldPerformAbstraction(
      PolicyIntermediateState iState,
      AbstractState totalState
  ) {

    CFANode node = iState.getNode();

    // If BAM is enabled, and we are at the start/end of the partition,
    // abstraction is necessary.
    if (partitioning != null &&
        (partitioning.isCallNode(node) || partitioning.isReturnNode(node))) {
      return true;
    }

    switch (abstractionLocations) {
      case ALL:
        return true;
      case LOOPHEAD:
        LoopstackState loopState = AbstractStates.extractStateByType(totalState,
            LoopstackState.class);

        return (cfa.getAllLoopHeads().get().contains(node)
            && (loopState == null || loopState.isLoopCounterAbstracted()));
      case MERGE:
        return node.getNumEnteringEdges() > 1;
      default:
        throw new UnsupportedOperationException("Unexpected state");
    }
  }

  /**
   * Find the PolicyAbstractedState sibling:
   * something about-to-be-merged with the argument state.
   *
   * <p>{@link ReachedSet} gives us all elements potentially joinable
   * (in the same partition) with {@code state}.
   * However, we would like to get the <b>latest</b> such element.
   * In ARG terminology, that's the first one we get by following backpointers.
   */
  private Optional<PolicyAbstractedState> findSibling(
      PolicyIntermediateState state,
      UnmodifiableReachedSet states,
      AbstractState pArgState
      ) {

    Set<PolicyAbstractedState> filteredSiblings =
        ImmutableSet.copyOf(
            AbstractStates.projectToType(
                states.getReached(pArgState),
                PolicyAbstractedState.class)
        );
    if (filteredSiblings.isEmpty()) {
      return Optional.empty();
    }

    // We follow the chain of backpointers.
    // The chain is necessary as we might have nested loops.
    PolicyState a = state;
    while (true) {
      if (a.isAbstract()) {
        PolicyAbstractedState aState = a.asAbstracted();

        if (filteredSiblings.contains(aState)) {
          return Optional.of(aState);
        } else {
          Optional<PolicyIntermediateState> genState = aState.getGeneratingState();
          if (!genState.isPresent()) {
            return Optional.empty();
          }
          a = genState.get().getBackpointerState();
        }

      } else {
        PolicyIntermediateState iState = a.asIntermediate();
        a = iState.getBackpointerState();
      }
    }
  }

  public boolean adjustPrecision() {
    return initialPrecision.adjustPrecision();
  }

  void adjustReachedSet(ReachedSet pReachedSet) {
    pReachedSet.clear();
  }

  public boolean isLessOrEqual(PolicyState state1, PolicyState state2) {
    Preconditions.checkState(state1.isAbstract() == state2.isAbstract());
    boolean out;
    if (state1.isAbstract()) {
      out = isLessOrEqualAbstracted(state1.asAbstracted(),
          state2.asAbstracted());
    } else {
      out = isLessOrEqualIntermediate(state1.asIntermediate(),
          state2.asIntermediate());
    }
    return out;
  }

  public PolicyState merge(PolicyState state1, PolicyState state2)
      throws InterruptedException {

    Preconditions.checkState(state1.isAbstract() == state2.isAbstract(),
        "Only states with the same abstraction status should be allowed to merge");
    if (state1.isAbstract()) {

      // No merge.
      return state2;
    }

    return joinIntermediateStates(state1.asIntermediate(), state2.asIntermediate());
  }

  /**
   * @return state1 <= state2
   */
  private boolean isLessOrEqualIntermediate(
      PolicyIntermediateState state1,
      PolicyIntermediateState state2) {
    return state1.isMergedInto(state2)
        || (state1.getPathFormula().getFormula().equals(state2.getPathFormula().getFormula())
            && isLessOrEqualAbstracted(state1.getBackpointerState(), state2.getBackpointerState()));
  }

  /**
   * @return state1 <= state2
   */
  private boolean isLessOrEqualAbstracted(
      PolicyAbstractedState aState1,
      PolicyAbstractedState aState2
  ) {
    for (Entry<Template, PolicyBound> e : aState2) {
      Template t = e.getKey();
      PolicyBound bound2 = e.getValue();

      Optional<PolicyBound> bound1 = aState1.getBound(t);
      if (!bound1.isPresent()
          || bound1.get().getBound().compareTo(bound2.getBound()) >= 1) {
        return false;
      }
    }

    return true;
  }

  private BooleanFormula extractFormula(AbstractState pFormulaState) {
    List<BooleanFormula> constraints = new ArrayList<>();
    for (AbstractState a : asIterable(pFormulaState)) {
      if (!(a instanceof PolicyAbstractedState) && a instanceof FormulaReportingState) {
        constraints.add(((FormulaReportingState) a).getFormulaApproximation(fmgr));
      }
    }
    return bfmgr.and(constraints);
  }

  /**
   * Ugly hack for communicating with
   * {@link org.sosy_lab.cpachecker.cpa.bam.BAMCPA}.
   */
  void setPartitioning(BlockPartitioning pPartitioning) {
    partitioning = pPartitioning;
  }
}
