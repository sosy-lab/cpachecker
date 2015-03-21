package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyBound.PolicyBoundImpl;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyBound.PolicyBoundDummy;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
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
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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

  @Option(secure=true, description="Remove redundant constraints from the value"
      + " determination problem")
  private boolean filterValueDeterminationConstraints = true;

  @Option(secure=true,
  description="Use the optimized abstraction, which takes into the account the "
      + "previously obtained bound at the location.")
  private boolean usePreviousBounds = true;

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
      FormulaLinearizationManager pLinearizationManager)
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
    abstractedStates = new HashMap<>(pCfa.getAllNodes().size());
  }

  /**
   * Static caches
   */
  // Mapping from loop-heads to the associated loops.
  private final ImmutableMap<CFANode, LoopStructure.Loop> loopStructure;

  /**
   * Scary-hairy global, contains all abstracted states.
   */
  private final Map<Location, PolicyAbstractedState> abstractedStates;

  /**
   * Constants
   */
  private static final String START_LOCATION_FLAG = "__INITIAL_LOCATION";

  /**
   * @param pNode Initial node
   * @return Initial state for the analysis, assuming the first node
   * is {@code pNode}
   */
  @Override
  public PolicyState getInitialState(CFANode pNode) {
    Location initialLocation = Location.initial(pNode);
    PolicyAbstractedState initial = PolicyAbstractedState.empty(
        initialLocation, pfmgr.makeEmptyPathFormula());
    abstractedStates.put(initialLocation, initial);
    return initial;
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
      outPath = outPath.updateFormula(fmgr.simplify(outPath.getFormula()));
    }

    PolicyState out = PolicyIntermediateState.of(

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

  @Override
  public Collection<PolicyState> strengthen(
      PolicyState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    CFANode toNode = cfaEdge.getSuccessor();
    Location newLocation = Location.of(toNode, otherStates);

    statistics.startAbstractionTimer();
    try {
      // Perform the abstraction, if necessary.
      if (!state.isAbstract() && shouldPerformAbstraction(toNode)) {
        PolicyIntermediateState iState = state.asIntermediate();

        logger.log(Level.FINE, ">>> Abstraction from formula",
            iState.getPathFormula());
        Optional<PolicyAbstractedState> abstraction = performAbstraction(iState,
            newLocation);
        if (!abstraction.isPresent()) {
          return Collections.emptyList();
        }
        state = abstraction.get();
        abstractedStates.put(newLocation, state.asAbstracted());
        logger.log(Level.FINE, ">>> Abstraction produced state: ", state);
      }
    } finally {
      statistics.stopAbstractionTimer();
    }

    // Perform the reachability check for the target states.
    if (state.isAbstract()) {
      return Collections.singleton(state);
    }
    boolean hasTargetState = false;
    for (AbstractState oState : otherStates) {
      if (AbstractStates.isTargetState(oState)) {
        hasTargetState = true;
      }
    }

    if (hasTargetState) {
      boolean isSat = checkSatisfiability(state.asIntermediate());
      if (!isSat) {
        return Collections.emptyList();
      } else {
        return Collections.singleton(state);
      }
    }
    return Collections.singleton(state);
  }

  @Override
  public PolicyState join(PolicyState newState, PolicyState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    Preconditions.checkState(oldState.isAbstract() == newState.isAbstract());
    PolicyState out;

    if (oldState.isAbstract()) {
      out = joinAbstractedStates(
          newState.asAbstracted(), oldState.asAbstracted());
      abstractedStates.put(out.asAbstracted().getLocation(),
          out.asAbstracted());
    } else {
      out = joinIntermediateStates(
          newState.asIntermediate(), oldState.asIntermediate());
    }

    // Note: returning an exactly same state is important, due to the issues
    // with .equals() handling.
    if (out.equals(oldState)) {
      return oldState;
    }
    return out;
  }

  private PolicyIntermediateState joinIntermediateStates(
      PolicyIntermediateState newState,
      PolicyIntermediateState oldState
  ) throws CPATransferException, InterruptedException, SolverException {
    // NOTE: possible optimization before join:
    // check satisfiability, if one of the paths is unsatisfiable =>
    // throw it away (avoid unnecessary diamonds, better get rid of them
    // early).

    if (newState.isMergedInto(oldState)) {
      return oldState;
    } else if (oldState.isMergedInto(newState)) {
      return newState;
    }

    if (oldState.getPathFormula().equals(newState.getPathFormula())
        && checkParentCovering(oldState, newState)) {

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
      mergedPath = mergedPath.updateFormula(
          fmgr.simplify(mergedPath.getFormula()));
    }

    // No value determination, no abstraction, simply join incoming edges
    // and the tracked templates.
    PolicyIntermediateState out = PolicyIntermediateState.of(
        allTemplates,
        mergedPath,
        mergeStartMaps(oldState, newState)
    );

    newState.setMergedInto(out);
    oldState.setMergedInto(out);
    return out;
  }

  private PolicyAbstractedState joinAbstractedStates(
      PolicyAbstractedState newState,
      PolicyAbstractedState oldState
  ) throws CPATransferException, InterruptedException {
    Preconditions.checkState(
        newState.getLocation().equals(oldState.getLocation()));
    CFANode node = oldState.getNode();
    Location location = oldState.getLocation();

    logger.log(Level.FINE, "Merging on location: ", location);
    statistics.abstractMergeCounter.add(location);

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
        logger.log(Level.FINE, "Old value is unbounded, new value: ", newValue);
        continue;
      } else if (!newValue.isPresent()) {

        logger.log(Level.FINE, "New value is unbounded");
        newUnbounded.add(template);
        continue;
      } else if (newValue.get().getBound().compareTo(oldValue.get().getBound()) > 0) {

        logger.log(Level.FINE, "New value is larger");
        updated.put(template, newValue.get());

        statistics.templateUpdateCounter.add(Pair.of(location, template));
      } else {
        logger.log(Level.FINE, "New value is smaller-or-equal");
      }
    }

    if (updated.isEmpty() && newUnbounded.isEmpty()) {
      return oldState;
    }

    if (filterValueDeterminationConstraints) {
      updated = filterAbstraction(updated);
    }
    PolicyAbstractedState stateWithUpdates =
        oldState.withUpdates(updated, newUnbounded, allTemplates);

    if (!shouldPerformValueDetermination(node, updated)) {
      logger.log(Level.FINE, "Returning state with updates");
      return stateWithUpdates;

    } else {
      logger.log(Level.FINE, "Running val. det.");

      Map<Location, PolicyAbstractedState> related =
          getRelated(stateWithUpdates, updated);
      Pair<ImmutableMap<String, FormulaType<?>>, Set<BooleanFormula>>
          constraints;

      if (runHopefulValueDetermination) {

        // Note: this formula contains no disjunctions, as the policy entails
        // the edge selection. Hence it can be used safely for the maximization.
        constraints = vdfmgr.valueDeterminationFormula(
            related, stateWithUpdates, updated, false);
        Optional<PolicyAbstractedState> element =
            valueDeterminationMaximization(
                oldState,
                allTemplates,
                updated,
                location,
                constraints.getFirst(),
                constraints.getSecond());
        if (element.isPresent()) {
          return element.get();
        }
      }

      PolicyAbstractedState out;
      constraints = vdfmgr.valueDeterminationFormula(
          related, stateWithUpdates, updated, true);
      out = valueDeterminationMaximization(
          oldState,
          allTemplates,
          updated,
          location,
          constraints.getFirst(),
          constraints.getSecond()).get();
      return out;
    }
  }

  Optional<PolicyAbstractedState> valueDeterminationMaximization(
      PolicyAbstractedState prevState,
      Set<Template> templates,
      Map<Template, PolicyBound> updated,
      Location location,
      Map<String, FormulaType<?>> types,
      Set<BooleanFormula> pValueDeterminationConstraints
  )
      throws InterruptedException, CPATransferException {

    ImmutableMap.Builder<Template, PolicyBound> builder =
        ImmutableMap.builder();
    Set<Template> unbounded = new HashSet<>();

    // Maximize for each template subject to the overall constraints.
    statistics.startValueDeterminationTimer();
    try (OptEnvironment optSolver = this.solver.newOptEnvironment()) {
      shutdownNotifier.shutdownIfNecessary();

      for (BooleanFormula constraint : pValueDeterminationConstraints) {
        optSolver.addConstraint(constraint);
      }

      Map<Template, Integer> objectiveHandles = new HashMap<>(updated.size());
      for (Entry<Template, PolicyBound> policyValue : updated.entrySet()) {
        Template template = policyValue.getKey();

        Formula objective;
        String varName = vdfmgr.absDomainVarName(location, template);
        logger.log(Level.FINE, "Var name: ", varName);
        if (templateManager.shouldUseRationals(template)) {
          objective = rfmgr.makeVariable(varName);
        } else {
          FormulaType<?> type = types.get(varName);
          objective = fmgr.makeVariable(type, varName);
        }
        int handle = optSolver.maximize(objective);
        objectiveHandles.put(template, handle);
      }

      OptEnvironment.OptStatus result;
      try {
        statistics.startOPTTimer();
        result = optSolver.check();
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

        Optional<Rational> value = optSolver.upper(
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
        unbounded, templates));
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
      Location location = bound.getPredecessor();
      if (l.getLoopNodes().contains(location.getFinalNode())) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return Whether the <code>state</code> is reachable.
   */
  private boolean checkSatisfiability(PolicyIntermediateState state)
      throws CPATransferException, InterruptedException {

    // TODO: we need to be able to return "false" as well, if we know something
    // is definitely "false".
    BooleanFormula startConstraints = getStartConstraints(state);
    BooleanFormula constraint = bfmgr.and(
        state.getPathFormula().getFormula(), startConstraints
    );

    try {
      statistics.startCheckSATTimer();
      return !solver.isUnsat(constraint);
    } catch (SolverException e) {
      throw new CPATransferException("Failed solving", e);
    } finally {
      statistics.stopCheckSATTimer();
    }
  }

  private BooleanFormula getStartConstraints(
      PolicyIntermediateState state) {
    List<BooleanFormula> inputConstraints = new ArrayList<>();
    for (Entry<Location, PolicyAbstractedState> e :
        state.getGeneratingStates().entrySet()) {

      Location loc = e.getKey();
      PathFormula startPath = e.getValue().getPathFormula();
      PolicyAbstractedState startingState = abstractedStates.get(loc);
      List<BooleanFormula> constraints = abstractStateToConstraints(
          startingState, startPath
      );

      BooleanFormula startConstraint = bfmgr.and(
          ifmgr.equal(
              ifmgr.makeVariable(START_LOCATION_FLAG),
              ifmgr.makeNumber(loc.toID())
          ),
          bfmgr.and(constraints)
      );

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
      final Location location)
      throws CPATransferException, InterruptedException {
    final PathFormula p = state.getPathFormula();

    // Linearize.
    final BooleanFormula linearizedFormula = linearizationManager.linearize(
        p.getFormula());

    // Add choice variables.
    final BooleanFormula annotatedFormula = linearizationManager.annotateDisjunctions(
        linearizedFormula);

    final Map<Template, PolicyBound> abstraction = new HashMap<>();
    final BooleanFormula startConstraints = getStartConstraints(state);

    try (
        // Optimizing environment to get the final value.
        OptEnvironment optEnvironment = solver.newOptEnvironment();

        // Prover environment to perform meta-queries.
        ProverEnvironment prover = solver.newProverEnvironment()
    ) {
      optEnvironment.addConstraint(annotatedFormula);
      optEnvironment.addConstraint(startConstraints);

      if (optEnvironment.check() == OptEnvironment.OptStatus.UNSAT) {

        // Bottom => bail early.
        return Optional.absent();
      }

      //noinspection ResultOfMethodCallIgnored
      prover.push(linearizedFormula);

      shutdownNotifier.shutdownIfNecessary();

      for (Template template : state.getTemplates()) {

        // Optimize for the template subject to the
        // constraints introduced by {@code p}.
        Formula objective = templateManager.toFormula(template, p);

        // We only care for a new value if it is the larger than the one
        // we currently have.
        // Skip the iteration if the previous value is already unbounded,
        // add a lemma that the new value has to be strictly larger otherwise.
        BooleanFormula prevStateConstraint = bfmgr.makeBoolean(true);
        PolicyBound prevBound = null;
        if (usePreviousBounds) {
          PolicyAbstractedState prevState = abstractedStates.get(location);
          if (prevState != null) {

            Preconditions.checkState(prevState.isAbstract());
            Optional<PolicyBound> bound = prevState.getBound(template);
            if (!bound.isPresent()) {
              logger.log(Level.FINE, "Old is unbounded, skipping update");

              continue;
            } else {
              prevBound = bound.get();
              Rational prevValue = prevBound.getBound();

              prevStateConstraint = fmgr.makeGreaterThan(
                  objective, fmgr.makeNumber(objective, prevValue), true
              );
            }
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
                  objective,
                  prover, state, p, annotatedFormula, model, boundValue);
              abstraction.put(template, policyBound);
            }
            logger.log(Level.FINE, "Got bound: ", bound);
            break;

          case UNSAT:
            logger.log(Level.FINE, "Got UNSAT, previous value must be unbeatable");

            assert prevBound != null;

            // Use the previous bound for comparison purposes.
            abstraction.put(template, PolicyBoundDummy.of(prevBound.getBound()));
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

    return Optional.of(
        PolicyAbstractedState.of(
            abstraction,
            state.getTemplates(),
            location,
            state));
  }

  private List<BooleanFormula> abstractStateToConstraints(PolicyAbstractedState
      abstractState, PathFormula inputPath) {

    List<BooleanFormula> constraints = new ArrayList<>();
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
    Location location = abstractState.getLocation();
    PathFormula generatingFormula = abstractState.getPathFormula();

    BooleanFormula initialConstraint =  ifmgr.equal(
        ifmgr.makeVariable(START_LOCATION_FLAG),
        ifmgr.makeNumber(location.toID()));

    if (formulaSlicing) {
      BooleanFormula pointerData = formulaSlicingManager.pointerFormulaSlice(
          generatingFormula.getFormula());
      statistics.slicingTimer.start();
      try {
        FormulaInductivenessCheck checker = new FormulaInductivenessCheck(pfmgr, solver);
        BooleanFormula inductivePointerData = checker.getInductiveVersionOf(
            generatingFormula.updateFormula(pointerData), location.getFinalNode());
        initialConstraint = bfmgr.and(initialConstraint, inductivePointerData);
      } finally {
        statistics.slicingTimer.stop();
      }
    }

    PathFormula path = generatingFormula.updateFormula(initialConstraint);

    return PolicyIntermediateState.of(
        abstractState.getTemplates(),
        path,
        ImmutableMap.of(location, abstractState)
    );
  }

  /**
   * Use the auxiliary variables from the {@code model} to reconstruct the
   * trace which was use for abstracting the state associated with the
   * {@code node}.
   *
   * @return Reconstructed trace
   */
  private PolicyBound modelToPolicyBound(
      Formula templateObjective,
      ProverEnvironment prover,
      PolicyIntermediateState inputState,
      PathFormula inputPathFormula,
      BooleanFormula annotatedFormula,
      Model model,
      Rational bound) throws SolverException, InterruptedException {

    // Check whether the bound can change if initial condition is dropped.
    //noinspection ResultOfMethodCallIgnored
    prover.push(fmgr.makeGreaterThan(
        templateObjective,
        fmgr.makeNumber(templateObjective, bound), true));
    boolean dependsOnInitial;
    try {
      statistics.checkIndependenceTimer.start();
      dependsOnInitial = !prover.isUnsat();
    } finally {
      statistics.checkIndependenceTimer.stop();
      prover.pop();
    }

    BooleanFormula policyFormula = linearizationManager.enforceChoice(
        annotatedFormula, model.entrySet());

    BigInteger prevLocationID = (BigInteger)model.get(
        new Model.Constant(START_LOCATION_FLAG, Model.TermType.Integer));
    int locationID = prevLocationID.intValue();
    Location prevLocation = Location.ofID(locationID);

    return PolicyBoundImpl.of(
        inputPathFormula.updateFormula(policyFormula), bound, prevLocation,
        inputState.getGeneratingStates().get(prevLocation).getPathFormula(),
        dependsOnInitial);
  }

  /**
   * @return Whether to compute the abstraction when creating a new
   * state associated with <code>node</code>.
   */
  private boolean shouldPerformAbstraction(CFANode pNode) {
    if (!pathFocusing) {
      return true;
    }
    if (pNode.isLoopStart()) {
      return true;
    }
    return false;
  }

  /**
   * @return the subset of {@code abstractStates} required for the update
   * {@code updated}.
   */
  private Map<Location, PolicyAbstractedState> getRelated(
      PolicyAbstractedState newState,
      Map<Template, PolicyBound> updated) {

    Map<Location, PolicyAbstractedState> out = new HashMap<>();
    Set<Location> visited = Sets.newHashSet();
    Location focusedLocation = newState.getLocation();

    LinkedHashSet<Location> queue = new LinkedHashSet<>();
    queue.add(focusedLocation);
    while (!queue.isEmpty()) {
      Iterator<Location> it = queue.iterator();
      Location loc = it.next();
      it.remove();
      visited.add(loc);

      PolicyAbstractedState state;
      if (loc == focusedLocation) {
        state = newState;
      } else {
        state = abstractedStates.get(loc);
      }
      out.put(loc, state);

      for (Entry<Template, PolicyBound> entry : state) {
        Template template = entry.getKey();
        PolicyBound bound = entry.getValue();

        // Do not follow the edges which are:
        //  * Associated with the focused node but are not in <updated>.
        //  * Marked as not [dependsOnInitial]
        if ((state != newState || updated.containsKey(template))
            && bound.dependsOnInitial()) {
          Location toVisit = bound.getPredecessor();

          if (!visited.contains(toVisit)) {
            queue.add(toVisit);
          }
        }
      }
    }
    return out;
  }

  private boolean checkParentCovering(
      PolicyIntermediateState stateA, PolicyIntermediateState stateB
  ) {

    Map<Location, PolicyAbstractedState> parentsA, parentsB;
    parentsA = stateA.getGeneratingStates();
    parentsB = stateB.getGeneratingStates();

    for (Location loc : Sets.union(parentsA.keySet(), parentsB.keySet())) {
      PolicyAbstractedState parentA = parentsA.get(loc);
      PolicyAbstractedState parentB = parentsB.get(loc);

      if (parentA == null) {
        continue;
      } else if (parentB == null) {
        return false;
      } else {
        if (parentA.getVersion() > parentB.getVersion()) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Join start path formulas: first PathFormula for the beginning of the trace.
   */
  private Map<Location, PolicyAbstractedState> mergeStartMaps(
      PolicyIntermediateState oldState,
      PolicyIntermediateState newState
  ) {
    Map<Location, PolicyAbstractedState> oldParents, newParents;
    oldParents = oldState.getGeneratingStates();
    newParents = newState.getGeneratingStates();

    Map<Location, PolicyAbstractedState> mergedMap = new HashMap<>();
    for (Location loc : Sets.union(newParents.keySet(), oldParents.keySet())) {
      PolicyAbstractedState oldParent = oldParents.get(loc);
      PolicyAbstractedState newParent = newParents.get(loc);
      PolicyAbstractedState merged;
      if (newParent == null) {
        merged = oldParent;
      } else if (oldParent == null) {
        merged = newParent;
      } else {
        if (newParent.getVersion() >= oldParent.getVersion()) {
          merged = newParent;
        } else {
          merged = oldParent;
        }
      }
      mergedMap.put(loc, merged);
    }
    return mergedMap;
  }

  /**
   * Remove redundant constraints.
   * Very naive filtering.
   */
  private Map<Template, PolicyBound> filterAbstraction(
      final Map<Template, PolicyBound> abstraction) {
    return Maps.filterEntries(abstraction,
        new Predicate<Entry<Template, PolicyBound>>() {
          @Override
          public boolean apply(
              Entry<Template, PolicyBound> entry) {
            Template t = entry.getKey();
            LinearExpression<CIdExpression> expr = t.linearExpression;
            CSimpleType type = t.type;

            Rational bound = entry.getValue().getBound();
            if (t.getKind() != Template.Kind.COMPLEX) {
              return true;
            }
            Rational otherBoundsSum = Rational.ZERO;
            for (Entry<CIdExpression, Rational> monomial : expr) {
              CIdExpression part = monomial.getKey();
              Rational monomialCoeff = monomial.getValue();

              Template partTemplate = Template.of(
                  LinearExpression.pair(part, monomialCoeff), type);
              PolicyBound templateValue = abstraction.get(partTemplate);
              if (templateValue == null) {
                return true;
              }
              otherBoundsSum = otherBoundsSum.plus(templateValue.getBound());
            }

            if (!otherBoundsSum.equals(bound)) {
              return true;
            }
            return false;
          }
        });
  }
}
