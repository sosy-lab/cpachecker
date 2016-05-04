package org.sosy_lab.cpachecker.cpa.policyiteration;

import static com.google.common.collect.Iterables.filter;
import static org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationManager.DecompositionStatus.ABSTRACTION_REQUIRED;
import static org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationManager.DecompositionStatus.BOUND_COMPUTED;
import static org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationManager.DecompositionStatus.UNBOUNDED;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationStatistics.TemplateUpdateEvent;
import org.sosy_lab.cpachecker.cpa.policyiteration.Template.Kind;
import org.sosy_lab.cpachecker.cpa.policyiteration.ValueDeterminationManager.ValueDeterminationConstraints;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceManager;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceState;
import org.sosy_lab.cpachecker.cpa.policyiteration.polyhedra.PolyhedraWideningManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.OptimizationProverEnvironment;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;

import java.util.ArrayList;
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
 * Main logic in a single class.
 */
@Options(prefix="cpa.stator.policy")
public class PolicyIterationManager implements IPolicyIterationManager {

  @Option(secure = true,
      description = "Where to perform abstraction")
  private ABSTRACTION_LOCATIONS abstractionLocations = ABSTRACTION_LOCATIONS.LOOPHEAD;

  /**
   * Where an abstraction should be performed.
   */
  private enum ABSTRACTION_LOCATIONS  {

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

  @Option(secure=true, description="Run simple congruence analysis")
  private boolean runCongruence = true;

  @Option(secure=true, description="Use syntactic check to short-circuit"
      + " val. det. and abstraction operations.")
  private boolean shortCircuitSyntactic = true;

  @Option(secure=true, description="Check whether the policy depends on the initial value")
  private boolean checkPolicyInitialCondition = true;

  @Option(secure=true, description="Remove UFs and ITEs from policies.")
  private boolean linearizePolicy = true;

  @Option(secure=true, description="Generate new templates using polyhedra convex hull")
  private boolean generateTemplatesUsingConvexHull = false;

  @Option(secure=true, description="Number of value determination steps allowed before widening is run."
      + " Value of '-1' runs value determination until convergence.")
  private int wideningThreshold = -1;

  private final FormulaManagerView fmgr;
  private final CFA cfa;
  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final TemplateManager templateManager;
  private final ValueDeterminationManager vdfmgr;
  private final PolicyIterationStatistics statistics;
  private final FormulaLinearizationManager linearizationManager;
  private final CongruenceManager congruenceManager;
  private final PolyhedraWideningManager pwm;
  private final InvariantGenerator invariantGenerator;
  private final StateFormulaConversionManager stateFormulaConversionManager;

  public PolicyIterationManager(
      Configuration config,
      FormulaManagerView pFormulaManager,
      CFA pCfa,
      PathFormulaManager pPfmgr,
      Solver pSolver,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      TemplateManager pTemplateManager,
      ValueDeterminationManager pValueDeterminationFormulaManager,
      PolicyIterationStatistics pStatistics,
      FormulaLinearizationManager pLinearizationManager,
      CongruenceManager pCongruenceManager,
      PolyhedraWideningManager pPwm,
      InvariantGenerator pInvariantGenerator,
      StateFormulaConversionManager pStateFormulaConversionManager)
      throws InvalidConfigurationException {
    pwm = pPwm;
    stateFormulaConversionManager = pStateFormulaConversionManager;
    config.inject(this, PolicyIterationManager.class);
    fmgr = pFormulaManager;
    cfa = pCfa;
    pfmgr = pPfmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    solver = pSolver;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    templateManager = pTemplateManager;
    vdfmgr = pValueDeterminationFormulaManager;
    statistics = pStatistics;
    linearizationManager = pLinearizationManager;
    congruenceManager = pCongruenceManager;
    invariantGenerator = pInvariantGenerator;

    /** Compute the cache for loops */
    ImmutableMap.Builder<CFANode, LoopStructure.Loop> loopStructureBuilder =
        ImmutableMap.builder();
    LoopStructure loopStructure1 = pCfa.getLoopStructure().get();
    for (LoopStructure.Loop l : loopStructure1.getAllLoops()) {
      for (CFANode n : l.getLoopHeads()) {
        loopStructureBuilder.put(n, l);
      }
    }
    loopStructure = loopStructureBuilder.build();
  }

  /**
   * Static caches
   */
  // Mapping from loop-heads to the associated loops.
  private final ImmutableMap<CFANode, LoopStructure.Loop> loopStructure;

  /**
   * The concept of a "location" is murky in a CPA.
   * Currently it's defined in a precision adjustment operator:
   * if we perform an adjustment, and there's already another state in the
   * same partition (something we are about to get merged with), we take their
   * locationID.
   * Otherwise, we generate a fresh one.
   */
  private final UniqueIdGenerator locationIDGenerator = new UniqueIdGenerator();

  private boolean invariantGenerationStarted = false;

  /**
   * @param pNode Initial node.
   * @return Initial state for the analysis, assuming the first node
   * is {@code pNode}.
   */
  @Override
  public PolicyState getInitialState(CFANode pNode) {
    // this is somewhat bad, because if we have an expensive
    // invariant generation procedure, it will block for
    // a considerable amount of time before the analysis can even start =(
    startInvariantGeneration(pNode);

    return PolicyAbstractedState.empty(
        pNode,
        bfmgr.makeBoolean(true), stateFormulaConversionManager);
  }



  @Override
  public Collection<? extends PolicyState> getAbstractSuccessors(PolicyState oldState,
      CFAEdge edge) throws CPATransferException, InterruptedException {

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
        iOldState.getGeneratingState());

    return Collections.singleton(out);
  }

  /**
   * Perform abstraction and reachability checking with precision adjustment
   * operator.
   */
  @Override
  public Optional<PrecisionAdjustmentResult> precisionAdjustment(
      final PolicyState inputState,
      final PolicyPrecision inputPrecision,
      final UnmodifiableReachedSet states,
      final AbstractState pArgState) throws CPAException, InterruptedException {

    final PolicyIntermediateState iState;
    if (inputState.isAbstract()) {
      iState = inputState.asAbstracted().getGenerationState().get();
    } else {
      iState = inputState.asIntermediate();
    }

    final boolean hasTargetState = filter(
        AbstractStates.asIterable(pArgState), AbstractStates.IS_TARGET_STATE).iterator().hasNext();
    // Formulas reported by other CPAs.
    BooleanFormula extraInvariant = extractFormula(pArgState);

    final boolean shouldPerformAbstraction = shouldPerformAbstraction(iState, pArgState);

    // Perform reachability checking, either for property states, or when the
    // formula gets too long, or before abstractions.
    if (!inputState.isAbstract() && (hasTargetState && checkTargetStates
        || shouldPerformAbstraction
      ) && isUnreachable(iState, extraInvariant)) {

      logger.log(Level.INFO, "Returning BOTTOM state");
      return Optional.absent();
    }

    // Perform the abstraction, if necessary.
    if (shouldPerformAbstraction) {
      CFANode node = inputState.getNode();
      PolicyPrecision toNodePrecision = templateManager.precisionForNode(inputState.getNode());
      Optional<PolicyAbstractedState> sibling = findSibling(iState, states, pArgState);

      PolicyAbstractedState abstraction;
      if (!inputState.isAbstract()) {
        statistics.abstractionTimer.start();
        try {
          abstraction = performAbstraction(
              iState, getLocationID(sibling, node), toNodePrecision, extraInvariant);
          logger.log(Level.FINE, ">>> Abstraction produced a state: ", abstraction);
        } finally {
          statistics.abstractionTimer.stop();
        }
      } else {

        // Abstraction is as precise as possible with respect to the previously computed state.
        // Strengthening can not make it more precise.
        abstraction = inputState.asAbstracted().withNewExtraInvariant(extraInvariant);
      }

      PolicyAbstractedState outState;
      if (sibling.isPresent()) {

        // Emulate large-step (join followed by value-determination) on the
        // resulting abstraction at the same location.
        outState = emulateLargeStep(abstraction, sibling.get(), inputPrecision, extraInvariant);
      } else {
        outState = abstraction;
      }

      if (inputState.isAbstract()
          && isLessOrEqualAbstracted(inputState.asAbstracted(), outState)
          && inputPrecision.equals(toNodePrecision)) {
        outState = inputState.asAbstracted().withNewExtraInvariant(extraInvariant);
        toNodePrecision = inputPrecision;
      }

      return continueResult(outState, toNodePrecision);
    } else {
      return continueResult(iState, inputPrecision);
    }
  }

  private int getLocationID(Optional<PolicyAbstractedState> sibling, CFANode node) {
    int locationID;
    if (sibling.isPresent()) {
      locationID = sibling.get().getLocationID();
    } else {
      locationID = locationIDGenerator.getFreshId();
      logger.log(Level.INFO, "Generating new location ID", locationID,
          " for node ", node);
    }
    return locationID;
  }

  /**
   * Shortcut to avoid clumsy object creation.
   */
  private Optional<PrecisionAdjustmentResult> continueResult(PolicyState s, Precision p) {
    return Optional.of(
        PrecisionAdjustmentResult.create(
            s, p, PrecisionAdjustmentResult.Action.CONTINUE));
  }

  private PolicyAbstractedState emulateLargeStep(
      PolicyAbstractedState abstraction,
      PolicyAbstractedState latestSibling,
      PolicyPrecision precision,
      BooleanFormula extraInvariant
      ) throws CPATransferException, InterruptedException {

    CFANode node = abstraction.getNode();
    logger.log(Level.INFO, "Emulating large step at node ", node);

    Map<Template, PolicyBound> updated = new HashMap<>();
    PolicyAbstractedState merged = unionAbstractedStates(
          abstraction, latestSibling, precision, updated, extraInvariant);

    PolicyAbstractedState out;
    if (!shouldPerformValueDetermination(node, updated)) {
      out = merged;

    } else {
      logger.log(Level.FINE, "Running val. det.");

      ValueDeterminationConstraints constraints;
      Optional<PolicyAbstractedState> element;
      if (runHopefulValueDetermination) {
        constraints = vdfmgr.valueDeterminationFormulaCheap(
            merged, updated);
        element = performValueDetermination(
            merged, updated, constraints, true);
      } else {
        element = Optional.absent();
      }

      if (!element.isPresent()) {

        // Hopeful value determination failed, run the more expensive version.
        constraints = vdfmgr.valueDeterminationFormula(merged, updated);
        out = performValueDetermination(
            merged,
            updated,
            constraints,
            false).get();
      } else {
        out = element.get();
      }
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

    if (!newState.getGeneratingState().equals(oldState.getGeneratingState())) {

      // Different parents: do not merge.
      return oldState;
    }

    if (newState.isMergedInto(oldState)) {
      return oldState;
    } else if (oldState.isMergedInto(newState)) {
      return newState;
    }

    PathFormula newPath = newState.getPathFormula();
    PathFormula oldPath = oldState.getPathFormula();
    PathFormula mergedPath = pfmgr.makeOr(newPath, oldPath);
    PolicyIntermediateState out = PolicyIntermediateState.of(
        newState.getNode(),
        mergedPath,
        oldState.getGeneratingState()
    );

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
      final PolicyPrecision precision,
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
    for (Template template : precision) {
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

        if (statistics.templateUpdateCounter.count(updateEvent) ==
            wideningThreshold) {
          // Set the value to infinity if the widening threshold was reached.
          logger.log(Level.FINE, "Widening threshold for template", template,
              "at", newState.getNode(), "was reached, widening to infinity.");
          continue;
        }
        mergedBound = newValue.get();
        updated.put(template, newValue.get());

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


    PolicyAbstractedState merged = PolicyAbstractedState.of(
        newAbstraction, oldState.getNode(),
        congruenceManager.join(
            newState.getCongruence(), oldState.getCongruence()),
        newState.getLocationID(),
        stateFormulaConversionManager,
        oldState.getSSA(), // Very important to use the old SSA so that PathFormulaManager
                           // can use the cached values.
        newState.getPointerTargetSet(),
        extraInvariant,
        newState.getGenerationState().get()
    );

    if (generateTemplatesUsingConvexHull) {
      templateManager.addGeneratedTemplates(
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
      ValueDeterminationConstraints valDetConstraints,
      boolean runningCheapValueDetermination
  ) throws InterruptedException, CPATransferException {
    logger.log(Level.INFO, "Value determination at node",
        stateWithUpdates.getNode());

    Map<Template, PolicyBound> newAbstraction =
        new HashMap<>(stateWithUpdates.getAbstraction());

    // Maximize for each template subject to the overall constraints.
    statistics.valueDeterminationTimer.start();
    try (OptimizationProverEnvironment optEnvironment = solver.newOptEnvironment()) {

      for (BooleanFormula constraint : valDetConstraints.constraints) {
        optEnvironment.addConstraint(constraint);
      }

      for (Entry<Template, PolicyBound> policyValue : updated.entrySet()) {
        shutdownNotifier.shutdownIfNecessary();
        optEnvironment.push();

        Template template = policyValue.getKey();
        Formula objective = valDetConstraints.outVars.get(template,
            stateWithUpdates.getLocationID());
        assert objective != null;
        PolicyBound existingBound = policyValue.getValue();

        int handle = optEnvironment.maximize(objective);
        BooleanFormula consistencyConstraint =
            fmgr.makeGreaterOrEqual(
                objective,
                fmgr.makeNumber(objective, existingBound.getBound()),
                true);

        optEnvironment.addConstraint(consistencyConstraint);

        OptimizationProverEnvironment.OptStatus result;
        try {
          statistics.optTimer.start();
          result = optEnvironment.check();
        } finally {
          statistics.optTimer.stop();
        }
        if (result != OptimizationProverEnvironment.OptStatus.OPT) {
          shutdownNotifier.shutdownIfNecessary();

          if (result == OptimizationProverEnvironment.OptStatus.UNSAT) {
            if (!runningCheapValueDetermination) {
              throw new CPATransferException("Inconsistent value determination "
                  + "problem");
            }

            logger.log(Level.INFO, "The val. det. problem is unsat,",
                " switching to a more expensive strategy.");
            return Optional.absent();
          }
          throw new CPATransferException("Unexpected solver state");
        }

        Optional<Rational> value = optEnvironment.upper(handle, EPSILON);

        if (value.isPresent() &&
            !templateManager.isOverflowing(template, value.get())) {
          Rational v = value.get();
          newAbstraction.put(template, existingBound.updateValue(v));
        } else {
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
   * @return Whether to perform the value determination on <code>node</code>.
   * <p/>
   * Returns true iff the <code>node</code> is a loophead and at least one of
   * the bounds in <code>updated</code> has an associated edge coming from
   * outside of the loop.
   * Note that the function returns <code>false</code> is <code>updated</code>
   * is empty.
   */
  private boolean shouldPerformValueDetermination(
      CFANode node,
      Map<Template, PolicyBound> updated) {

    if (updated.isEmpty()) {
      return false;
    }

    // At least one of updated values comes from inside the loop.
    LoopStructure.Loop l = loopStructure.get(node);
    if (l == null) {
      // NOTE: sometimes there is no loop-structure when there's
      // one self-edge.
      return true;
    }
    for (PolicyBound bound : updated.values()) {
      CFANode fromNode = bound.getPredecessor().getNode();
      if (l.getLoopNodes().contains(fromNode)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return Whether the <code>state</code> is unreachable.
   */
  private boolean isUnreachable(PolicyIntermediateState state, BooleanFormula extraInvariant)
      throws CPAException, InterruptedException {
    BooleanFormula startConstraints =
        stateFormulaConversionManager.getStartConstraints(state, true);
    PathFormula pf = state.getPathFormula();

    BooleanFormula constraint = bfmgr.and(
        ImmutableList.of(
            startConstraints,
            pf.getFormula(),
            fmgr.instantiate(extraInvariant, pf.getSsa())
        )
    );

    try {
      statistics.checkSATTimer.start();
      return solver.isUnsat(
          bfmgr.toConjunctionArgs(constraint, true), state.getNode());
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
      OptimizationProverEnvironment optEnvironment,
      Optional<Rational> bound,
      BooleanFormula annotatedFormula,
      PathFormula p,
      PolicyIntermediateState state,
      Formula objective
      ) throws SolverException, InterruptedException {
    boolean unsignedAndLower = template.isUnsigned() &&
        (template.getKind() == Kind.NEG_LOWER_BOUND ||
            template.getKind() == Kind.NEG_SUM_LOWER_BOUND);
    if (bound.isPresent() &&
        !templateManager.isOverflowing(template, bound.get())
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
            objective, state, p, linearizedFormula, model, boundValue);
        return Optional.of(policyBound);
      }
    }
    return Optional.absent();
  }

  /**
   * Perform the abstract operation on a new state
   *
   * @param state State to abstract
   * @return Abstracted state if the state is reachable, empty optional
   * otherwise.
   */
  private PolicyAbstractedState performAbstraction(
      final PolicyIntermediateState state,
      int locationID,
      PolicyPrecision precision,
      BooleanFormula extraInvariant)
      throws CPAException, InterruptedException {

    logger.log(Level.FINE, "Performing abstraction at node: ", state.getNode());

    final PathFormula p = state.getPathFormula();
    final BooleanFormula startConstraints =
        stateFormulaConversionManager.getStartConstraints(state, true);

    Set<BooleanFormula> startConstraintLemmas =
        bfmgr.toConjunctionArgs(fmgr.applyTactic(startConstraints, Tactic
            .TSEITIN_CNF), true);
    Set<BooleanFormula> lemmas = bfmgr.toConjunctionArgs(
      fmgr.applyTactic(p.getFormula(), Tactic.TSEITIN_CNF), true
    );

    final Map<Template, PolicyBound> abstraction = new HashMap<>();

    // TODO: flag for caching.
    try (OptimizationProverEnvironment optEnvironment = solver.newCachedOptEnvironment()) {

      optEnvironment.push();
      optEnvironment.addConstraint(startConstraints);
      optEnvironment.push();

      for (Template template : precision) {
        optEnvironment.pop();
        optEnvironment.push();

        // Optimize for the template subject to the
        // constraints introduced by {@code p}.
        Formula objective = templateManager.toFormula(pfmgr, fmgr, template, p);
        Set<String> objectiveVars = fmgr.extractFunctionNames(objective);

        // TODO: make optional.
        Pair<DecompositionStatus, PolicyBound> res = computeByDecomposition(
                template, p, lemmas, startConstraintLemmas, abstraction);
        switch (res.getFirstNotNull()) {
          case BOUND_COMPUTED:

            // Put the computed bound.
            abstraction.put(template, res.getSecond());
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

        Set<BooleanFormula> slicedConstraint = computeRelevantSubset(
            lemmas,
            startConstraintLemmas,
            objectiveVars);
        BooleanFormula f = bfmgr.and(slicedConstraint);

        // Linearize & add choice variables.
        statistics.linearizationTimer.start();
        BooleanFormula annotatedFormula = linearizationManager.annotateDisjunctions(
            linearizationManager.linearize(f)
        );
        statistics.linearizationTimer.stop();

        // Skip updates if the edge does not have any variables mentioned in the
        // template: this now becomes obvious with the new slicing scheme.
        if (shortCircuitSyntactic && bfmgr.isTrue(f)) {
          if (state.getGeneratingState().getAbstraction().get(template) == null) {

            // Unbounded.
            continue;
          }

          PolicyBound bound = state.getGeneratingState().getAbstraction().get(template);
          abstraction.put(template, bound);
        }

        optEnvironment.addConstraint(annotatedFormula);

        // TODO: evaluate whether we need the code in "usePreviousBound".
        int handle = optEnvironment.maximize(objective);

        OptimizationProverEnvironment.OptStatus status;
        try {
          statistics.optTimer.start();
          status = optEnvironment.check();
        } finally {
          statistics.optTimer.stop();
        }

        switch (status) {
          case OPT:

            Optional<Rational> bound = optEnvironment.upper(handle, EPSILON);
            Optional<PolicyBound> policyBound = getPolicyBound(template,
                optEnvironment, bound, annotatedFormula, p, state, objective);
            if (policyBound.isPresent()) {
              abstraction.put(template, policyBound.get());
            }

            logger.log(Level.FINE, "Got bound: ", bound);
            break;

          case UNSAT:
            throw new CPAException("Unexpected UNSAT");

          case UNDEF:
            throw new CPATransferException("Solver returned undefined status");
          default:
            throw new AssertionError("Unhandled enum value in switch: " + status);
        }

      }
    } catch (SolverException e) {
      throw new CPATransferException("Solver error: ", e);
    }

    statistics.updateCounter.add(locationID);

    // TODO: ideally, congruence should be a separate CPA.
    CongruenceState congruence;
    if (runCongruence) {
      congruence = congruenceManager.performAbstraction(
          state.getNode(), p, startConstraints
      );
    } else {
      congruence = CongruenceState.empty();
    }

    return PolicyAbstractedState.of(
        abstraction,
        state.getNode(),
        congruence,
        locationID,
        stateFormulaConversionManager,
        state.getPathFormula().getSsa(),
        state.getPathFormula().getPointerTargetSet(),
        extraInvariant,
        state
    );
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
      Map<Template, PolicyBound> currentAbstraction
  ) {

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

      Set<String> variables = fmgr.extractFunctionNames(
          templateManager.toFormula(pfmgr, fmgr, singleton, pFormula)
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
    if (Iterables.filter(policyBounds, Predicates.<PolicyBound>isNull())
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

    return Pair.of(BOUND_COMPUTED, PolicyBound.of(
        firstBound.getFormula().updateFormula(bfmgr.and(policies)),
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
    Set<BooleanFormula> out = new HashSet<>();

    for (BooleanFormula lemma : input) {
      if (!Sets.intersection(fmgr.extractFunctionNames(lemma), closure)
          .isEmpty()) {
        out.add(lemma);
      }
    }
    return out;
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
        Set<String> containedVars = fmgr.extractFunctionNames(atom);
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
        try (ProverEnvironment prover = solver.newProverEnvironment()) {

          //noinspection ResultOfMethodCallIgnored
          prover.push(policyFormula);

          //noinspection ResultOfMethodCallIgnored
          prover.push(fmgr.makeGreaterThan(
              templateObjective,
              fmgr.makeNumber(templateObjective, bound), true));
          try {
            dependsOnInitial = !prover.isUnsat();
          } finally {
            statistics.checkIndependenceTimer.stop();
            prover.pop();
          }
        }
    } else {
        dependsOnInitial = true;
    }

    PolicyAbstractedState backpointer = inputState.getGeneratingState();

    Set<String> policyVars = fmgr.extractFunctionNames(policyFormula);
    Set<Template> dependencies;
    if (!dependsOnInitial) {
      dependencies = ImmutableSet.of();
    } else if (!shortCircuitSyntactic) {
      dependencies = templateManager.templatesForNode(backpointer.getNode());
    } else {
      dependencies = new HashSet<>();
      for (Template t : templateManager.templatesForNode(backpointer.getNode())) {
        Set<String> fVars = fmgr.extractFunctionNames(templateManager.toFormula(
            pfmgr, fmgr, t,
            stateFormulaConversionManager.getPathFormula(backpointer, fmgr, false)
        ));
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
      AbstractState totalState) {

    CFANode node = iState.getNode();
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
   * Find the PolicyAbstractedState sibling: something about-to-be-merged
   * with the argument state.
   * ReachedSet gives us all elements potentially joinable
   * (== in the same partition) with {@code state}.
   * However, we would like to get the *latest* such element.
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
      return Optional.absent();
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
          if (!aState.getGenerationState().isPresent()) {
            return Optional.absent();
          }
          a = aState.getGenerationState().get().getGeneratingState();
        }

      } else {
        PolicyIntermediateState iState = a.asIntermediate();
        a = iState.getGeneratingState();
      }
    }
  }

  @Override
  public boolean adjustPrecision() {
    return templateManager.adjustPrecision();
  }

  @Override
  public void adjustReachedSet(ReachedSet pReachedSet) {
    pReachedSet.clear();
  }

  @Override
  public boolean isLessOrEqual(PolicyState state1, PolicyState state2)
      throws CPAException {
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

  @Override
  public PolicyState merge(PolicyState state1, PolicyState state2,
      PolicyPrecision precision)
      throws CPAException, InterruptedException {

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
        || state1.getPathFormula().getFormula().equals(state2.getPathFormula().getFormula())
        && isLessOrEqualAbstracted(state1.getGeneratingState(), state2.getGeneratingState());
  }

  /**
   * @return state1 <= state2
   */
  private boolean isLessOrEqualAbstracted(
      PolicyAbstractedState aState1,
      PolicyAbstractedState aState2
  ) {
    if (!congruenceManager.isLessOrEqual(aState1.getCongruence(), aState2.getCongruence())) {
      return false;
    }

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

  private void startInvariantGeneration(CFANode pNode) {
    if (!invariantGenerationStarted) {
      invariantGenerator.start(pNode);
    }
    invariantGenerationStarted = true;
  }

  private BooleanFormula extractFormula(AbstractState pFormulaState) {
    List<BooleanFormula> constraints = new ArrayList<>();
    for (AbstractState a : AbstractStates.asIterable(pFormulaState)) {
      if (!(a instanceof PolicyAbstractedState) && a instanceof FormulaReportingState) {
        constraints.add(((FormulaReportingState) a).getFormulaApproximation(fmgr, pfmgr));
      }
    }
    return bfmgr.and(constraints);
  }
}
