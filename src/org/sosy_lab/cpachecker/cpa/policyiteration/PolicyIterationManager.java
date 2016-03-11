package org.sosy_lab.cpachecker.cpa.policyiteration;

import static com.google.common.collect.Iterables.filter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
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
      description = "Perform abstraction only at the nodes from the cut-set.")
  private boolean pathFocusing = true;

  @Option(secure = true, name = "epsilon",
      description = "Value to substitute for the epsilon")
  private Rational EPSILON = Rational.ONE;

  @Option(secure=true, description="Run naive value determination first, "
      + "switch to namespaced if it fails.")
  private boolean runHopefulValueDetermination = true;

  @Option(secure=true, description="Check target states reachability")
  private boolean checkTargetStates = true;

  @Option(secure=true,
  description="Use the optimized abstraction, which takes into the account the "
      + "previously obtained bound at the location. Interferes with:"
      + "obtaining templates using convex hull and split-sep merge configuration.")
  private boolean usePreviousBounds = true;

  @Option(secure=true, description="Any intermediate state with formula length "
      + "bigger than specified will be checked for reachability. "
      + "Set to -1 for no limit.")
  private int lengthLimitForSATCheck = 300;

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

  @Option(secure=true, description="Use extra invariant during abstraction")
  private boolean useExtraPredicateDuringAbstraction = true;

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
  public Optional<PrecisionAdjustmentResult> precisionAdjustment(PolicyState state,
      PolicyPrecision precision, UnmodifiableReachedSet states,
      AbstractState pArgState)
        throws CPAException, InterruptedException {

    final PolicyIntermediateState iState;
    if (state.isAbstract()) {
      // TODO: predecessor isn't quite the right name.
      iState = state.asAbstracted().getPredecessor().get();
    } else {
      iState = state.asIntermediate();
    }
    final boolean hasTargetState = filter(
        AbstractStates.asIterable(pArgState),
        AbstractStates.IS_TARGET_STATE).iterator().hasNext();
    final boolean shouldPerformAbstraction = shouldPerformAbstraction(iState, pArgState);

    // Formulas reported by other CPAs.
    BooleanFormula extraInvariant = extractFormula(pArgState);
    logger.log(Level.FINE, "Reported formulas: ", extraInvariant);

    // Perform reachability checking, either for property states, or when the
    // formula gets too long, or before abstractions.
    if ((hasTargetState && checkTargetStates
        || (lengthLimitForSATCheck != -1 &&
              iState.getPathFormula().getLength() > lengthLimitForSATCheck)
        || shouldPerformAbstraction
      ) && isUnreachable(iState, extraInvariant)) {

      logger.log(Level.INFO, "Returning BOTTOM state");
      return Optional.absent();
    }

    // Perform the abstraction, if necessary.
    if (shouldPerformAbstraction) {
      PolicyPrecision toNodePrecision = templateManager.precisionForNode(state.getNode());


      Optional<PolicyAbstractedState> sibling = findSibling(iState, states, pArgState);

      statistics.startAbstractionTimer();
      PolicyAbstractedState abstraction;
      try {
        abstraction = performAbstraction(iState, sibling, toNodePrecision, extraInvariant);
        logger.log(Level.FINE, ">>> Abstraction produced a state: ", abstraction);
      } finally {
        statistics.stopAbstractionTimer();
      }

      PolicyAbstractedState outState;
      if (sibling.isPresent()) {

        // Emulate large-step (join followed by value-determination) on the
        // resulting abstraction at the same location.
        outState = emulateLargeStep(abstraction, sibling.get(), precision, extraInvariant);
      } else {
        outState = abstraction;
      }
      return Optional.of(PrecisionAdjustmentResult.create(
          outState,
          toNodePrecision,
          PrecisionAdjustmentResult.Action.CONTINUE));
    } else {
      return Optional.of(PrecisionAdjustmentResult.create(
          iState,
          precision,
          PrecisionAdjustmentResult.Action.CONTINUE));
    }
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
    PolicyAbstractedState merged;
    try {
      merged = unionAbstractedStates(
          abstraction, latestSibling, precision, updated, extraInvariant);
    } catch (SolverException e) {
      throw new CPATransferException("Solver failed", e);
    }

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

    latestSibling.setLatestVersion(out);

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
      BooleanFormula extraInvariant
  ) throws InterruptedException, SolverException {
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
        oldState.getSSA(),
        newState.getPointerTargetSet(),
        extraInvariant,
        newState.getPredecessor().get()
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
    statistics.startValueDeterminationTimer();
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
          statistics.startOPTTimer();
          result = optEnvironment.check();
        } finally {
          statistics.stopOPTTimer();
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
      statistics.stopValueDeterminationTimer();
    }

    return Optional.of(stateWithUpdates.replaceAbstraction(newAbstraction));
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
      statistics.startCheckSATTimer();
      return solver.isUnsat(constraint);
    } catch (SolverException e) {
      throw new CPATransferException("Failed solving", e);
    } finally {
      statistics.stopCheckSATTimer();
    }
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
      final Optional<PolicyAbstractedState> otherState,
      PolicyPrecision precision,
      BooleanFormula extraInvariant)
      throws CPAException, InterruptedException {

    logger.log(Level.FINE, "Performing abstraction at node: ", state.getNode());

    int locationID;
    if (otherState.isPresent()) {
      locationID = otherState.get().getLocationID();
    } else {
      locationID = locationIDGenerator.getFreshId();
      statistics.latestLocationID = locationID;
      logger.log(Level.INFO, "Generating new location ID", locationID,
          " for node ", state.getNode());
    }

    final PathFormula p = state.getPathFormula();

    // Linearize.
    statistics.linearizationTimer.start();
    final BooleanFormula linearizedFormula = linearizationManager.linearize(
        p.getFormula());

    // Add choice variables.
    BooleanFormula annotatedFormula = linearizationManager.annotateDisjunctions(
        linearizedFormula);
    statistics.linearizationTimer.stop();

    final Map<Template, PolicyBound> abstraction = new HashMap<>();
    final BooleanFormula startConstraints =
        stateFormulaConversionManager.getStartConstraints(state, true);

    try (OptimizationProverEnvironment optEnvironment = solver.newOptEnvironment()) {
      optEnvironment.addConstraint(annotatedFormula);
      optEnvironment.addConstraint(startConstraints);

      if (useExtraPredicateDuringAbstraction) {

        // Invariant from other CPAs.
        optEnvironment.addConstraint(
            fmgr.instantiate(extraInvariant, state.getPathFormula().getSsa())
        );
      }

      // Invariant from the invariant generator.
      optEnvironment.addConstraint(
          fmgr.instantiate(
              stateFormulaConversionManager.getInvariantFor(state.getNode()),
              state.getPathFormula().getSsa()
          )
      );

      Set<String> formulaVars = fmgr.extractFunctionNames(
          state.getPathFormula().getFormula());
      for (Template template : precision) {
        shutdownNotifier.shutdownIfNecessary();

        // Optimize for the template subject to the
        // constraints introduced by {@code p}.
        Formula objective = templateManager.toFormula(pfmgr, fmgr, template, p);

        // We only care for a new value if it is the larger than the one
        // we currently have.
        // Skip the iteration if the previous value is already unbounded,
        // add a lemma that the new value has to be strictly larger otherwise.
        BooleanFormula prevStateConstraint = bfmgr.makeBoolean(true);
        PolicyBound prevBound = null;
        if (usePreviousBounds && otherState.isPresent()) {
          PolicyAbstractedState prevState = otherState.get()
              .getLatestVersion();
          Optional<PolicyBound> bound = prevState.getBound(template);
          if (!bound.isPresent()) {

            // Can't do better than unbounded.
            continue;
          } else {
            prevBound = bound.get();
            Rational prevValue = prevBound.getBound();

            prevStateConstraint = fmgr.makeGreaterThan(
                objective, fmgr.makeNumber(objective, prevValue), true);
          }
        }

        // Skip updates if the edge does not have any variables mentioned in the
        // template.
        if (shortCircuitSyntactic) {
          Optional<Optional<PolicyBound>> syntacticCheckResult =
              shouldPerformOptimization(state, formulaVars, template);
          if (syntacticCheckResult.isPresent()) {
            Optional<PolicyBound> inner = syntacticCheckResult.get();
            if (inner.isPresent()) {
              abstraction.put(template, inner.get());
            }
            continue;
          }
        }

        optEnvironment.push();
        optEnvironment.addConstraint(prevStateConstraint);

        logger.log(Level.FINE, "Optimizing for ", objective);
        int handle = optEnvironment.maximize(objective);

        OptimizationProverEnvironment.OptStatus status;
        try {
          statistics.startOPTTimer();
          status = optEnvironment.check();
        } finally {
          statistics.stopOPTTimer();
        }

        switch (status) {
          case OPT:
            Optional<Rational> bound = optEnvironment.upper(handle, EPSILON);
            Model model = optEnvironment.getModel();

            // Lower bound on unsigned variables is at least zero.
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

              if (linearizePolicy) {
                statistics.linearizationTimer.start();
                annotatedFormula = linearizationManager.convertToPolicy(
                    annotatedFormula, model);
                statistics.linearizationTimer.stop();
              }

              PolicyBound policyBound = modelToPolicyBound(
                  objective, state, p, annotatedFormula, model, boundValue);
              abstraction.put(template, policyBound);
            }
            logger.log(Level.FINE, "Got bound: ", bound);
            break;

          case UNSAT:
            logger.log(Level.FINE, "Got UNSAT, previous value must be unbeatable");
            assert prevBound != null : "Got UNSAT during abstraction, no previous value supplied";

            // Use the previous bound.
            abstraction.put(template, prevBound);
            break;

          case UNDEF:
            shutdownNotifier.shutdownIfNecessary();
            throw new CPATransferException("Solver returned undefined status");
          default:
            throw new AssertionError("Unhandled enum value in switch: " + status);
        }
        optEnvironment.pop();
      }
    } catch (SolverException e) {
      throw new CPATransferException("Solver error: ", e);
    }

    statistics.updateCounter.add(locationID);
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

    PolicyAbstractedState backpointer = inputState.getGeneratingState()
        .getLatestVersion();

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
  private boolean shouldPerformAbstraction(PolicyIntermediateState iState,
      AbstractState totalState) {

    if (!pathFocusing) {
      return true;
    }
    LoopstackState loopState = AbstractStates.extractStateByType(totalState,
        LoopstackState.class);

    CFANode node = iState.getNode();
    return (cfa.getAllLoopHeads().get().contains(node)
        && (loopState == null || loopState.isLoopCounterAbstracted()));
  }

  /** HELPER METHODS BELOW. **/

  /**
   * Perform a syntactic check on whether an abstraction is necessary on a
   * given template.
   *
   * Optional.absent() => abstraction necessary
   * Optional.of(Optional.absent()) => unbounded
   * Optional.of(Optional.of(bound)) => fixed bound
   */
  private Optional<Optional<PolicyBound>> shouldPerformOptimization(
      PolicyIntermediateState state,
      Set<String> formulaVars,
      Template pTemplate
  ) {
    PolicyAbstractedState generatingState = state.getGeneratingState().getLatestVersion();
    Set<String> templateVars = fmgr.extractFunctionNames(
        templateManager.toFormula(pfmgr, fmgr, pTemplate, state.getPathFormula()));

    if (!Sets.intersection(formulaVars, templateVars).isEmpty()) {
      return Optional.absent();
    }

    // Otherwise compute the bound from the previous value.
    Optional<PolicyBound> genBound = generatingState.getBound(pTemplate);
    if (!genBound.isPresent()) {
      return Optional.of(Optional.<PolicyBound>absent());
    }
    return Optional.of(Optional.of(
        PolicyBound.of(
            stateFormulaConversionManager
                .getPathFormula(generatingState, fmgr, true)
                .updateFormula(bfmgr.makeBoolean(true)),
            genBound.get().getBound(),
            generatingState,
            ImmutableSet.of(pTemplate)
        )
    ));
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
          if (!aState.getPredecessor().isPresent()) {
            return Optional.absent();
          }
          a = aState.getPredecessor().get().getGeneratingState();
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
    try {
      statistics.comparisonTimer.start();
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
    } catch (SolverException | InterruptedException e) {
      throw new CPAException("Solver failed", e);
    } finally {
      statistics.comparisonTimer.stop();
    }
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
      PolicyIntermediateState state2)
      throws SolverException, InterruptedException {
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
