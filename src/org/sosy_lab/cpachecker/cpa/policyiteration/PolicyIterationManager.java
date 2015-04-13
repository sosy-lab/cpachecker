package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.math.BigInteger;
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

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceManager;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.FormulaInductivenessCheck;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Main logic in a single class.
 */
@Options(prefix="cpa.stator.policy")
public class PolicyIterationManager implements IPolicyIterationManager {

  @Option(secure = true,
      description = "Call [simplify] on the formulas resulting from the C code")
  private boolean simplifyFormulas = true;

  @Option(secure = true,
      description = "Perform abstraction only at the nodes from the cut-set.")
  private boolean pathFocusing = true;

  @Option(secure = true,
      description = "Perform formula slicing after abstractions to propagate the" +
          " pointer information")
  private boolean formulaSlicing = true;

  @Option(secure = true, name = "epsilon",
      description = "Value to substitute for the epsilon")
  private Rational EPSILON = Rational.ONE;

  @Option(secure=true, description="Run naive value determination first, "
      + "switch to namespaced if it fails.")
  private boolean runHopefulValueDetermination = true;

  @Option(secure=true,
  description="Use the optimized abstraction, which takes into the account the "
      + "previously obtained bound at the location.")
  private boolean usePreviousBounds = true;

  @Option(secure=true, description="Any intermediate state with formula length "
      + "bigger than specified will be checked for reachability. "
      + "Set to 0 to disable.")
  private int lengthLimitForSATCheck = 300;

  @Option(secure=true, description="Run simple congruence analysis")
  private boolean runCongruence = false;

  @Option(secure=true, description="Use syntactic check to short-circuit"
      + " val. det. and abstraction operations.")
  private boolean shortCircuitSyntactic = true;

  private final FormulaManagerView fmgr;

  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  private final CFA cfa;
  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final NumeralFormulaManagerView<NumeralFormula, RationalFormula>
      rfmgr;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifmgr;
  private final TemplateManager templateManager;
  private final ValueDeterminationManager vdfmgr;
  private final PolicyIterationStatistics statistics;
  private final FormulaSlicingManager formulaSlicingManager;
  private final FormulaLinearizationManager linearizationManager;
  private final CongruenceManager congruenceManager;

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
      FormulaSlicingManager pFormulaSlicingManager,
      FormulaLinearizationManager pLinearizationManager,
      CongruenceManager pCongruenceManager)
      throws InvalidConfigurationException {
    config.inject(this, PolicyIterationManager.class);
    fmgr = pFormulaManager;
    cfa = pCfa;
    pfmgr = pPfmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    solver = pSolver;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    rfmgr = fmgr.getRationalFormulaManager();
    ifmgr = fmgr.getIntegerFormulaManager();
    templateManager = pTemplateManager;
    vdfmgr = pValueDeterminationFormulaManager;
    statistics = pStatistics;
    formulaSlicingManager = pFormulaSlicingManager;
    linearizationManager = pLinearizationManager;
    congruenceManager = pCongruenceManager;

    /** Compute the cache for nodes */
    ImmutableMap.Builder<Integer, CFANode> nodeMapBuilder =
        ImmutableMap.builder();
    for (CFANode node : pCfa.getAllNodes()) {
      nodeMapBuilder.put(node.getNodeNumber(), node);
    }

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
   * Constants
   */
  private static final String START_LOCATION_FLAG = "__INITIAL_LOCATION";

  /**
   * The concept of a "location" is murky in a CPA.
   * Currently it's defined in a precision adjustment operator:
   * if we perform an adjustment, and there's already another state in the
   * same partition (something we are about to get merged with), we take their
   * locationID.
   * Otherwise, we generate a fresh one.
   */
  private final UniqueIdGenerator locationIDGenerator = new UniqueIdGenerator();

  /**
   * @param pNode Initial node
   * @return Initial state for the analysis, assuming the first node
   * is {@code pNode}
   */
  @Override
  public PolicyState getInitialState(CFANode pNode) {
    return PolicyAbstractedState.empty(
        pNode, pfmgr.makeEmptyPathFormula());
  }


  @Override
  public Collection<PolicyState> getAbstractSuccessors(PolicyState oldState,
      CFAEdge edge) throws CPATransferException, InterruptedException {

    CFANode node = edge.getSuccessor();

    PolicyIntermediateState iOldState;

    if (oldState.isAbstract()) {
      iOldState = abstractStateToIntermediate(oldState.asAbstracted());
    } else {
      iOldState = oldState.asIntermediate();
    }

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), edge);

    if (simplifyFormulas) {
      statistics.simplifyTimer.start();
      outPath = outPath.updateFormula(fmgr.simplify(outPath.getFormula()));
      statistics.simplifyTimer.stop();
    }

    PolicyState out = PolicyIntermediateState.of(
        node,

        // We take the variables alive at the location + templates for the
        // previous location (variable may be not alive at {@code node},
        // but required for the guard associated with {@code edge}
        // nevertheless.
        Sets.union(templateManager.templatesForNode(node),
            oldState.getTemplates()),
        outPath,
        iOldState.getGeneratingStates());

    // NOTE: the abstraction computation and the global update is delayed
    // until the {@code strengthen} call.
    return Collections.singleton(out);
  }

  /**
   * Perform reachability check for "bad" states in strengthen.
   */
  @Override
  public Collection<PolicyState> strengthen(
      PolicyState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    if (!state.isAbstract() && !shouldPerformAbstraction(state.asIntermediate())) {
      // Perform the reachability check for the target states if we are dealing
      // with non-abstracted state.
      boolean hasTargetState = false;
      for (AbstractState oState : otherStates) {
        if (AbstractStates.isTargetState(oState)) {
          hasTargetState = true;
          break;
        }
      }

      if (hasTargetState  || state.asIntermediate().getPathFormula().getLength()
              > lengthLimitForSATCheck) {
        if (isUnreachable(state.asIntermediate())) {
          return Collections.emptyList();
        }
      }
    }
    return Collections.singleton(state);
  }

  /**
   * Perform abstraction with precision adjustment operator.
   */
  @Override
  public Optional<PrecisionAdjustmentResult> prec(PolicyState state,
      Precision precision, UnmodifiableReachedSet states,
      ARGState pArgState)
        throws CPAException, InterruptedException {

    CFANode toNode = state.getNode();

    statistics.startAbstractionTimer();
    try {
      assert !state.isAbstract();
      PolicyIntermediateState iState = state.asIntermediate();

      // Perform the abstraction, if necessary.
      if (shouldPerformAbstraction(iState)) {
        logger.log(Level.FINE, "Performing abstraction on node " + toNode);
        Optional<PolicyAbstractedState> abstraction = performAbstraction(
            iState, findSibling(states.getReached(pArgState)));

        if (!abstraction.isPresent()) {
          logger.log(Level.FINE, "Returning bottom");
          logger.flush();
          return Optional.absent();
        }
        PolicyAbstractedState abstractedState = abstraction.get();
        logger.log(Level.FINE, ">>> Abstraction produced state: ",
            abstractedState);
        return Optional.of(PrecisionAdjustmentResult.create(
            abstractedState, precision, PrecisionAdjustmentResult.Action.CONTINUE));
      } else {
        return Optional.of(PrecisionAdjustmentResult.create(
            state, precision, PrecisionAdjustmentResult.Action.CONTINUE
        ));
      }
    } finally {
      statistics.stopAbstractionTimer();
    }

  }

  @Override
  public PolicyState join(PolicyState newState, PolicyState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    Preconditions.checkState(oldState.isAbstract() == newState.isAbstract());
    PolicyState out;

    if (oldState.isAbstract()) {
      out = joinAbstractedStates(
          newState.asAbstracted(), oldState.asAbstracted());
    } else {
      out = joinIntermediateStates(
          newState.asIntermediate(), oldState.asIntermediate());
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
  ) throws CPATransferException, InterruptedException, SolverException {

    Preconditions.checkState(newState.getNode() == oldState.getNode(),
        "PolicyCPA must run with LocationCPA");

    if (newState.isMergedInto(oldState)) {
      return oldState;
    } else if (oldState.isMergedInto(newState)) {
      return newState;
    }

    if (oldState.getPathFormula().equals(newState.getPathFormula())) {

      // Special logic for checking after the value determination:
      // if two states share the formula,
      // AND the version of the new parent is older,
      // the new state will strictly dominate the old one.
      return newState;
    }

    Set<Template> allTemplates = Sets.union(oldState.getTemplates(),
        newState.getTemplates());

    PathFormula newPath = newState.getPathFormula();
    PathFormula oldPath = oldState.getPathFormula();

    PathFormula mergedPath = pfmgr.makeOr(newPath, oldPath);
    if (simplifyFormulas) {
      statistics.simplifyTimer.start();
      mergedPath = mergedPath.updateFormula(
          fmgr.simplify(mergedPath.getFormula()));
      statistics.simplifyTimer.stop();
    }

    // No value determination, no abstraction, simply join incoming edges
    // and the tracked templates.
    PolicyIntermediateState out = PolicyIntermediateState.of(
        newState.getNode(),
        allTemplates,
        mergedPath,
        updateMergeGenStates(newState, oldState)
    );

    newState.setMergedInto(out);
    oldState.setMergedInto(out);
    return out;
  }

  private PolicyAbstractedState joinAbstractedStates(
      PolicyAbstractedState newState,
      PolicyAbstractedState oldState
  ) throws CPATransferException, InterruptedException {
    Preconditions.checkState(newState.getNode() == oldState.getNode());
    Preconditions.checkState(newState.getLocationID() == oldState.getLocationID());
    CFANode node = oldState.getNode();

    statistics.abstractMergeCounter.add(oldState.getLocationID());

    Set<Template> allTemplates = Sets.union(oldState.getTemplates(),
        newState.getTemplates());
    Map<Template, PolicyBound> updated = new HashMap<>();
    Set<Template> newUnbounded = new HashSet<>();

    // Simple join:
    // Pick the biggest bound, and keep the biggest trace to match.
    for (Template template : allTemplates) {
      Optional<PolicyBound> oldValue, newValue;
      oldValue = oldState.getBound(template);
      newValue = newState.getBound(template);

      if (!oldValue.isPresent()) {
        continue;
      } else if (!newValue.isPresent()) {
        newUnbounded.add(template);
        continue;
      } else if (newValue.get().getBound().compareTo(oldValue.get().getBound()) > 0) {
        updated.put(template, newValue.get());
        statistics.templateUpdateCounter.add(Pair.of(newState.getLocationID(),
            template));
      }
    }

    if (updated.isEmpty() && newUnbounded.isEmpty()) {
      return oldState;
    }

    PolicyAbstractedState stateWithUpdates =
        oldState.withUpdates(updated, newUnbounded, allTemplates,
            congruenceManager.join(newState.congruence, oldState.congruence));
    oldState.setNewVersion(stateWithUpdates);
    newState.setNewVersion(stateWithUpdates);

    PolicyAbstractedState out;
    if (!shouldPerformValueDetermination(node, updated)) {
      logger.log(Level.FINE, "Returning state with updates");
      out = stateWithUpdates;

    } else {
      logger.log(Level.FINE, "Running val. det.");

      Pair<ImmutableMap<String, FormulaType<?>>, Set<BooleanFormula>>
          constraints;

      Optional<PolicyAbstractedState> element;
      if (runHopefulValueDetermination) {
        constraints = vdfmgr.valueDeterminationFormula(
            stateWithUpdates, updated, false);
        element =
            valueDeterminationMaximization(
                stateWithUpdates,
                oldState,
                allTemplates,
                updated,
                constraints.getFirst(),
                constraints.getSecond());
      } else {
        element = Optional.absent();
      }

      if (!element.isPresent()) {
        constraints = vdfmgr.valueDeterminationFormula(
            stateWithUpdates, updated, true);
        out = valueDeterminationMaximization(
            stateWithUpdates,
            oldState,
            allTemplates,
            updated,
            constraints.getFirst(),
            constraints.getSecond()).get();
      } else {
        out = element.get();
      }
    }

    // Set transient update pointers.
    newState.setNewVersion(out);
    oldState.setNewVersion(out);
    return out;
  }

  Optional<PolicyAbstractedState> valueDeterminationMaximization(
      PolicyAbstractedState stateWithUpdates,
      PolicyAbstractedState prevState,
      Set<Template> templates,
      Map<Template, PolicyBound> updated,
      Map<String, FormulaType<?>> types,
      Set<BooleanFormula> pValueDeterminationConstraints
  ) throws InterruptedException, CPATransferException {

    ImmutableMap.Builder<Template, PolicyBound> builder =
        ImmutableMap.builder();
    Set<Template> unbounded = new HashSet<>();

    // Maximize for each template subject to the overall constraints.
    statistics.startValueDeterminationTimer();
    try (OptEnvironment optEnvironment = solver.newOptEnvironment()) {
      shutdownNotifier.shutdownIfNecessary();

      for (BooleanFormula constraint : pValueDeterminationConstraints) {
        optEnvironment.addConstraint(constraint);
      }

      Map<Template, Integer> objectiveHandles = new HashMap<>(updated.size());
      for (Entry<Template, PolicyBound> policyValue : updated.entrySet()) {
        Template template = policyValue.getKey();

        Formula objective;
        String varName = vdfmgr.absDomainVarName(stateWithUpdates, template);
        if (templateManager.shouldUseRationals(template)) {
          objective = rfmgr.makeVariable(varName);
        } else {
          FormulaType<?> type = types.get(varName);
          objective = fmgr.makeVariable(type, varName);
        }
        int handle = optEnvironment.maximize(objective);
        objectiveHandles.put(template, handle);
      }

      OptEnvironment.OptStatus result;
      try {
        statistics.startOPTTimer();
        result = optEnvironment.check();
      } finally {
        statistics.stopOPTTimer();
      }
      if (result != OptEnvironment.OptStatus.OPT) {
        shutdownNotifier.shutdownIfNecessary();

        // Useful for debugging.
        if (result == OptEnvironment.OptStatus.UNSAT) {
          logger.log(Level.INFO, "Val det problem is unsat!");
          logger.flush();
          return Optional.absent();
        }
        throw new CPATransferException("Unexpected solver state");
      }

      for (Entry<Template, PolicyBound> policyValue : updated.entrySet()) {
        Template template = policyValue.getKey();
        PolicyBound bound = policyValue.getValue();

        Optional<Rational> value = optEnvironment.upper(
            objectiveHandles.get(template), EPSILON);

        if (value.isPresent()) {

          // Only the value changes, the rest is constant.
          builder.put(template, bound.updateValue(value.get()));
        } else {
          unbounded.add(template);
        }
      }
    } catch (SolverException e) {
      throw new CPATransferException("Failed maximization ", e);
    } finally {
      statistics.stopValueDeterminationTimer();
    }

    return Optional.of(prevState.withUpdates(builder.build(),
        unbounded, templates, congruenceManager.join(prevState.congruence,
            stateWithUpdates.congruence)));
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
    if (!node.isLoopStart() || updated.isEmpty()) {
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

      // well we can still use NODE for location identification / etc...
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
  private boolean isUnreachable(PolicyIntermediateState state)
      throws CPATransferException, InterruptedException {
    BooleanFormula startConstraints = getStartConstraints(state);

    BooleanFormula constraint = bfmgr.and(
        startConstraints, state.getPathFormula().getFormula()
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

  private BooleanFormula getStartConstraints(
      PolicyIntermediateState state) {
    List<BooleanFormula> inputConstraints = new ArrayList<>();
    for (final PolicyAbstractedState startingState : state.getGeneratingStates().values()) {
      final PolicyAbstractedState latestState = startingState.getLatestVersion();

      PathFormula startPath = latestState.getPathFormula();
      List<BooleanFormula> constraints = abstractStateToConstraints(
          latestState, startPath);

      BooleanFormula startConstraint = bfmgr.and(
          genInitialConstraint(latestState),
          bfmgr.and(constraints));

      inputConstraints.add(startConstraint);
    }
    return bfmgr.or(inputConstraints);
  }


  /**
   * Perform the abstract operation on a new state
   *
   * @param state State to abstract
   * @return Abstracted state if the state is reachable, empty optional
   * otherwise.
   */
  private Optional<PolicyAbstractedState> performAbstraction(
      final PolicyIntermediateState state,
      final Optional<PolicyAbstractedState> otherState)
      throws CPATransferException, InterruptedException {

    int locationID;
    if (otherState.isPresent()) {
      locationID = otherState.get().getLocationID();
    } else {
      locationID = locationIDGenerator.getFreshId();
    }

    final PathFormula p = state.getPathFormula();

    // Linearize.
    final BooleanFormula linearizedFormula = linearizationManager.linearize(
        p.getFormula());

    // Add choice variables.
    final BooleanFormula annotatedFormula = linearizationManager.annotateDisjunctions(
        linearizedFormula);

    final Map<Template, PolicyBound> abstraction = new HashMap<>();
    final BooleanFormula startConstraints = getStartConstraints(state);
    Set<String> formulaVars = fmgr.extractFunctionNames(
        state.getPathFormula().getFormula(), true);

    try (OptEnvironment optEnvironment = solver.newOptEnvironment()) {
      optEnvironment.addConstraint(annotatedFormula);
      optEnvironment.addConstraint(startConstraints);

      if (optEnvironment.check() == OptEnvironment.OptStatus.UNSAT) {

        // Bottom => bail early.
        return Optional.absent();
      }

      for (Template template : state.getTemplates()) {
        shutdownNotifier.shutdownIfNecessary();

        // Optimize for the template subject to the
        // constraints introduced by {@code p}.
        Formula objective = templateManager.toFormula(template, p);

        // We only care for a new value if it is the larger than the one
        // we currently have.
        // Skip the iteration if the previous value is already unbounded,
        // add a lemma that the new value has to be strictly larger otherwise.
        BooleanFormula prevStateConstraint = bfmgr.makeBoolean(true);
        PolicyBound prevBound = null;
        if (usePreviousBounds && otherState.isPresent()) {
          PolicyAbstractedState prevState = otherState.get();
          Optional<PolicyBound> bound = prevState.getBound(template);
          if (!bound.isPresent()) {

            // Can't do better than unbounded.
            continue;
          } else {
            prevBound = bound.get();
            Rational prevValue = prevBound.getBound();

            prevStateConstraint = fmgr.makeGreaterThan(
                objective, fmgr.makeNumber(objective, prevValue), true
            );
          }
        }

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

        OptEnvironment.OptStatus status;
        try {
          statistics.startOPTTimer();
          status = optEnvironment.check();
        } finally {
          statistics.stopOPTTimer();
        }

        // Generate the trace for the single template.
        switch (status) {
          case OPT:
            Optional<Rational> bound = optEnvironment.upper(handle, EPSILON);
            Model model = optEnvironment.getModel();

            // Lower bound on unsigned variables is at least zero.
            boolean unsignedAndLower = template.type.isUnsigned() &&
                template.getKind() == Template.Kind.NEG_LOWER_BOUND;
            if (bound.isPresent() || unsignedAndLower) {
              Rational boundValue;
              if (bound.isPresent() && unsignedAndLower) {
                boundValue = Rational.max(bound.get(), Rational.ZERO);
              } else if (bound.isPresent()){
                boundValue = bound.get();
              } else {
                boundValue = Rational.ZERO;
              }

              // NOTE: it is important to use the formula which does not include
              // the initial condition.
              PolicyBound policyBound = modelToPolicyBound(
                  objective, state, p, annotatedFormula, model, boundValue);
              abstraction.put(template, policyBound);
            }
            logger.log(Level.FINE, "Got bound: ", bound);
            break;

          case UNSAT:
            logger.log(Level.FINE, "Got UNSAT, previous value must be unbeatable");
            assert prevBound != null;

            // Use the previous bound.
            abstraction.put(template, prevBound);
            break;

          case UNDEF:
            shutdownNotifier.shutdownIfNecessary();
            throw new CPATransferException("Solver returned undefined status");
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
              p, startConstraints, state.getTemplates()
          );
    } else {
      congruence = CongruenceState.empty();
    }
    return Optional.of(
        PolicyAbstractedState.of(
            abstraction, state.getTemplates(), state.getNode(), state,
            congruence, locationID));
  }


  private List<BooleanFormula> abstractStateToConstraints(PolicyAbstractedState
      abstractState, PathFormula inputPath) {

    List<BooleanFormula> constraints = new ArrayList<>();
    constraints.add(congruenceManager.toFormula(
        abstractState.congruence, inputPath
    ));
    for (Entry<Template, PolicyBound> entry : abstractState) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      Formula t = templateManager.toFormula(template, inputPath);

      BooleanFormula constraint = fmgr.makeLessOrEqual(
          t, fmgr.makeNumber(t, bound.getBound()), true);
      constraints.add(constraint);
    }
    return constraints;
  }

  /**
   * @return Representation of an {@code abstractState} as a
   * {@link PolicyIntermediateState}.
   */
  private PolicyIntermediateState abstractStateToIntermediate(
      PolicyAbstractedState abstractState)
      throws InterruptedException, CPATransferException {
    CFANode node = abstractState.getNode();
    PathFormula generatingFormula = abstractState.getPathFormula();

    BooleanFormula initialConstraint =
        genInitialConstraint(abstractState);

    if (formulaSlicing) {
      BooleanFormula pointerData = formulaSlicingManager.pointerFormulaSlice(
          generatingFormula.getFormula());
      statistics.slicingTimer.start();
      try {
        FormulaInductivenessCheck checker = new FormulaInductivenessCheck(pfmgr,
            solver);
        BooleanFormula inductivePointerData = checker.getInductiveVersionOf(
            generatingFormula.updateFormula(pointerData), node);
        initialConstraint = bfmgr.and(initialConstraint, inductivePointerData);
      } finally {
        statistics.slicingTimer.stop();
      }
    }

    PathFormula path = generatingFormula.updateFormula(initialConstraint);

    return PolicyIntermediateState.of(
        node, abstractState.getTemplates(), path,
        ImmutableMap.of(abstractState.getLocationID(), abstractState)
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

    BooleanFormula policyFormula = linearizationManager.enforceChoice(
        annotatedFormula, model.entrySet());
    boolean dependsOnInitial;

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

    int prevLocID = ((BigInteger)model.get(
        new Model.Constant(START_LOCATION_FLAG, Model.TermType.Integer))).intValue();

    PolicyAbstractedState backpointer = inputState.getGeneratingStates()
        .get(prevLocID).getLatestVersion();

    Set<String> policyVars = fmgr.extractFunctionNames(policyFormula, true);
    Set<Template> dependencies;
    if (!dependsOnInitial) {
      dependencies = ImmutableSet.of();
    } else if (!shortCircuitSyntactic) {
      dependencies = backpointer.getTemplates();
    } else {
      dependencies = new HashSet<>();
      for (Template t : backpointer.getTemplates()) {
        Formula f = templateManager.toFormula(t, backpointer.getPathFormula());
        Set<String> fVars = fmgr.extractFunctionNames(f, true);
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
   * @return Whether to compute the abstraction when creating a new
   * state associated with <code>node</code>.
   */
  private boolean shouldPerformAbstraction(PolicyIntermediateState iState) {
    if (!pathFocusing) {
      return true;
    }
    CFANode node = iState.getNode();
    if (node.isLoopStart() ||
        cfa.getAllLoopHeads().get().contains(node)) {
      return true;
    }

    return false;
  }

  /** HELPER METHODS BELOW. **/

  /**
   * Perform a syntactic check on whether an abstraction is necessary on a
   * given template.
   *
   * Optional.absent() => abstraction necessary
   * Optional.of(Optional.absent()) => unbounded
   * Optional.of(bound) => fixed bound
   */
  private Optional<Optional<PolicyBound>> shouldPerformOptimization(
      PolicyIntermediateState state,
      Set<String> formulaVars,
      Template pTemplate
  ) {
    Map<Integer, PolicyAbstractedState> generatingStates =
        state.getGeneratingStates();
    if (generatingStates.size() > 1) {
      return Optional.absent();
    }
    PolicyAbstractedState generatingState =
        Iterables.getOnlyElement(generatingStates.values());
    Set<String> templateVars = fmgr.extractFunctionNames(
        templateManager.toFormula(pTemplate, state.getPathFormula()),
        true
    );

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
            generatingState.getPathFormula()
                .updateFormula(bfmgr.makeBoolean(true)),
            genBound.get().getBound(),
            generatingState,
            ImmutableSet.of(pTemplate)
        )
    ));
  }

  private Map<Integer, PolicyAbstractedState> updateMergeGenStates(
      PolicyIntermediateState stateA,
      PolicyIntermediateState stateB
  ) {
    Map<Integer, PolicyAbstractedState> out = new HashMap<>();
    for (int locID : Sets.union(stateA.getGeneratingStates().keySet(),
                                stateB.getGeneratingStates().keySet())) {
      PolicyAbstractedState gen = stateA.getGeneratingStates().get(locID);
      if (gen == null) {
        gen = stateB.getGeneratingStates().get(locID);
      }
      gen = gen.getLatestVersion();
      out.put(locID, gen);
    }
    return out;
  }

  private BooleanFormula genInitialConstraint(PolicyAbstractedState state) {
    int id = state.getLocationID();
    return ifmgr.equal(
        ifmgr.makeVariable(START_LOCATION_FLAG),
        ifmgr.makeNumber(id)
    );
  }

  /**
   * Find the PolicyAbstractedState sibling: something about-to-be-merged
   * with the argument state.
   */
  private Optional<PolicyAbstractedState> findSibling(
      Collection<AbstractState> pSiblings
  ) {
    if (pSiblings.isEmpty()) {
      return Optional.absent();
    }

    ARGState argState = (ARGState)Iterables.getOnlyElement(pSiblings);
    CompositeState compositeState = (CompositeState)argState.getWrappedState();
    List<AbstractState> siblings = compositeState.getWrappedStates();

    Iterable<AbstractState> policySiblings =
        Iterables.filter(
            siblings,
            Predicates.instanceOf(PolicyAbstractedState.class));

    // We always merge policy states, thus there can be only one or zero
    // siblings.
    if (!policySiblings.iterator().hasNext()) {
      return Optional.absent();
    } else {
      return Optional.of(
          (PolicyAbstractedState)Iterables.getOnlyElement(policySiblings)
      );
    }

  }
}
