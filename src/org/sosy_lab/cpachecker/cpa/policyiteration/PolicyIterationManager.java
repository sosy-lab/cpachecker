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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

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
  private boolean propagateFormulasPastAbstraction = true;

  @Option(secure = true, name = "epsilon",
      description = "Value to substitute for the epsilon")
  private Rational EPSILON = Rational.ONE;

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
  private final ValueDeterminationFormulaManager vdfmgr;
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
      ValueDeterminationFormulaManager pValueDeterminationFormulaManager,
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

    abstractStates = new HashMap<>(pCfa.getAllNodes().size());
  }

  /**
   * Static caches
   */
  // Mapping from loop-heads to the associated loops.
  private final ImmutableMap<CFANode, LoopStructure.Loop> loopStructure;

  /**
   * Scary-hairy global, contains all abstract states.
   */
  private final Map<Location, PolicyState> abstractStates;

  /**
   * Constants
   */
  private static final String INITIAL_CONDITION_FLAG =
      "__INITIAL_CONDITION_TRUE";
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
    abstractStates.put(initialLocation, initial);
    return initial;
  }

  @Override
  public Collection<PolicyState> getAbstractSuccessors(PolicyState oldState,
      CFAEdge edge)
      throws CPATransferException, InterruptedException {

    CFANode node = edge.getSuccessor();
    Location oldLocation = oldState.getLocation();
    Location newLocation = Location.transferRelation(oldLocation, edge);

    PolicyIntermediateState iOldState;

    if (oldState.isAbstract()) {
      iOldState = abstractStateToIntermediate(oldState.asAbstracted());
    } else {
      iOldState = oldState.asIntermediate();
    }

    Multimap<Location, Location> trace =
        HashMultimap.create(iOldState.getTrace());

    // Serialize the choice to trace as well.
    trace.put(newLocation, oldLocation);

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), edge);

    if (simplifyFormulas) {
      outPath = outPath.updateFormula(
          fmgr.simplify(outPath.getFormula()));
    }

    PolicyState out = PolicyIntermediateState.of(
        newLocation,

        // We take the variables alive at the location + templates for the
        // previous location (variable may be not alive at {@code node},
        // but required for the guard associated with {@code edge}
        // nevertheless.
        Sets.union(templateManager.templatesForNode(node),
            oldState.getTemplates()),
        outPath,

        // Redundant variable for path identification.
        trace);

    // NOTE: the abstraction computation and the global update is delayed
    // until the {@code strengthen} call.
    return Collections.singleton(out);
  }

  @Override
  public Collection<PolicyState> strengthen(
      PolicyState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    statistics.startAbstractionTimer();
    try {
      // Perform the abstraction, if necessary.
      if (!state.isAbstract() && shouldPerformAbstraction(
          state.asIntermediate())) {
        PolicyIntermediateState iState = state.asIntermediate();

        logger.log(Level.FINE, ">>> Abstraction from formula",
            iState.getPathFormula());
        logger.log(Level.FINE, "SSA: ", iState.getPathFormula().getSsa());
        Optional<PolicyAbstractedState> abstraction =
            performAbstraction(iState);
        if (!abstraction.isPresent()) {
          return Collections.emptyList();
        }
        state = abstraction.get();
        logger.log(Level.FINE, ">>> Abstraction produced state: ", state);
      }
    } finally {
      abstractStates.put(state.getLocation(), state);
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
    PolicyState out;
    Preconditions.checkState(oldState.getNode() == newState.getNode());
    Preconditions.checkState(oldState.isAbstract() == newState.isAbstract());

    if (oldState.isAbstract()) {
      out = joinAbstractedStates(
          newState.asAbstracted(), oldState.asAbstracted());
    } else {
      out = joinIntermediateStates(
          newState.asIntermediate(), oldState.asIntermediate());
    }
    abstractStates.put(out.getLocation(), out);

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
  ) throws CPATransferException, InterruptedException {
    Preconditions
        .checkState(newState.getLocation().equals(oldState.getLocation()));
    Location location = newState.getLocation();

    // Special logic for checking after the value determination:
    // if two states utilize the same traces (come from the same parent nodes)
    // the new state after the value determination will strictly dominate
    // the old one.
    // Note that the comparison order is reverse of the usual
    // (in #isLessOrEqual).
    if (checkCovering(oldState, newState)) {
      return newState;
    }

    Set<Template> allTemplates = Sets.union(oldState.getTemplates(),
        newState.getTemplates());

    PathFormula newPath = newState.getPathFormula();
    PathFormula oldPath = oldState.getPathFormula();
    Multimap<Location, Location> trace = HashMultimap.create();

    trace.putAll(newState.getTrace());
    trace.putAll(oldState.getTrace());

    // No value determination, no abstraction, simply join incoming edges
    // and the tracked templates.
    return PolicyIntermediateState.of(
        location, allTemplates,
        pfmgr.makeOr(newPath, oldPath),
        trace
    );
  }

  private PolicyAbstractedState joinAbstractedStates(
      PolicyAbstractedState newState,
      PolicyAbstractedState oldState
  ) throws CPATransferException, InterruptedException {
    Preconditions.checkState(
        newState.getLocation().equals(oldState.getLocation()));
    CFANode node = oldState.getNode();
    Location location = oldState.getLocation();
    Set<Template> allTemplates = Sets.union(oldState.getTemplates(),
        newState.getTemplates());
    Map<Template, PolicyBound> updated = new HashMap<>();
    Set<Template> unbounded = new HashSet<>();

    // Simple join:
    // Pick the biggest bound, and keep the biggest trace to match.
    for (Template template : allTemplates) {

      Optional<PolicyBound> oldValue, newValue;
      oldValue = oldState.getBound(template);
      newValue = newState.getBound(template);

      if (!oldValue.isPresent()) {

        // Can't do better than unbounded.
        continue;
      } else if (!newValue.isPresent()) {

        // {@code template} became unbounded.
        unbounded.add(template);
        continue;
      } else if (newValue.get().bound.compareTo(oldValue.get().bound) > 0) {
        updated.put(template, newValue.get());
      }
    }

    PolicyAbstractedState stateWithUpdates =
        oldState.withUpdates(updated, unbounded, allTemplates);
    logger.log(Level.FINE, "# State with updates: ", stateWithUpdates);

    if (!shouldPerformValueDetermination(node, updated)) {
      return stateWithUpdates;

    } else {

      Map<Location, PolicyAbstractedState> related =
          findRelated(stateWithUpdates, updated);

      // Note: this formula contains no disjunctions, as the policy entails
      // the edge selection. Hence it can be used safely for the maximization.
      List<BooleanFormula> constraints = vdfmgr.valueDeterminationFormula(
          related, stateWithUpdates.getLocation(), updated);

      PolicyAbstractedState out = valueDeterminationMaximization(
          oldState,
          allTemplates,
          updated,
          location,
          constraints
      );
      logger.log(Level.FINE, ">>> Value determination out state: ", out);
      return out;
    }
  }

  PolicyAbstractedState valueDeterminationMaximization(
      PolicyAbstractedState prevState,
      Set<Template> templates,
      Map<Template, PolicyBound> updated,
      Location location,
      List<BooleanFormula> pValueDeterminationConstraints
  )
      throws InterruptedException, CPATransferException {

    ImmutableMap.Builder<Template, PolicyBound> builder =
        ImmutableMap.builder();
    Set<Template> unbounded = new HashSet<>();

    // Maximize for each template subject to the overall constraints.
    statistics.startValueDeterminationTimer();
    try (OptEnvironment solver = this.solver.newOptEnvironment()) {
      shutdownNotifier.shutdownIfNecessary();

      for (BooleanFormula constraint : pValueDeterminationConstraints) {
        solver.addConstraint(constraint);
      }

      Map<Template, Integer> objectiveHandles = new HashMap<>(updated.size());
      for (Entry<Template, PolicyBound> policyValue : updated.entrySet()) {
        Template template = policyValue.getKey();

        NumeralFormula objective;
        String varName = vdfmgr.absDomainVarName(location, template);
        if (templateManager.shouldUseRationals(template)) {
          objective = rfmgr.makeVariable(varName);
        } else {
          objective = ifmgr.makeVariable(varName);
        }
        int handle = solver.maximize(objective);
        objectiveHandles.put(template, handle);
      }


      OptEnvironment.OptStatus result;
      try {
        statistics.startOPTTimer();
        result = solver.check();
      } finally {
        statistics.stopOPTTimer();
      }
      if (result != OptEnvironment.OptStatus.OPT) {
        shutdownNotifier.shutdownIfNecessary();
        throw new CPATransferException("Unexpected solver state, " +
            "value determination problem should be feasible");
      }

      for (Entry<Template, PolicyBound> policyValue : updated.entrySet()) {
        Template template = policyValue.getKey();
        PolicyBound bound = policyValue.getValue();

        PathFormula policyFormula = bound.formula;
        Optional<Rational> value = solver.upper(objectiveHandles.get(template),
            EPSILON);

        if (value.isPresent()) {
          builder.put(template, PolicyBound.of(
              policyFormula,
              value.get(),
              bound.predecessor
          ));
        } else {
          unbounded.add(template);
        }
      }
    } catch (SolverException e) {
      throw new CPATransferException("Failed maximization ", e);
    } finally {
      statistics.stopValueDeterminationTimer();
    }

    return prevState.withUpdates(builder.build(), unbounded, templates);
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
    if (!node.isLoopStart()) {
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
      Location location = bound.predecessor;
      if (l.getLoopNodes().contains(location.node)) {
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

    BooleanFormula constraint = state.getPathFormula().getFormula();
    constraint = linearizationManager.enforceChoice(
        constraint,
        Collections.<Entry<Model.AssignableTerm, Object>>emptySet(),
        true
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

  /**
   * Perform the abstract operation on a new state
   *
   * @param state State to abstract
   * @return Abstracted state if the state is reachable, empty optional
   * otherwise.
   */
  private Optional<PolicyAbstractedState> performAbstraction(
      final PolicyIntermediateState state)
      throws CPATransferException, InterruptedException {
    final PathFormula p = state.getPathFormula();
    BooleanFormula transferRelation = p.getFormula();

    // Linearize.
    BooleanFormula linearizedFormula = linearizationManager.linearize(
        transferRelation);

    // Add choice variables.
    BooleanFormula annotatedFormula = linearizationManager.annotateDisjunctions(
        linearizedFormula);

    ImmutableMap.Builder<Template, PolicyBound> abstraction
        = ImmutableMap.builder();

    BooleanFormula formulaWithInitial = linearizationManager.enforceChoice(
        annotatedFormula,
        Collections.<Entry<Model.AssignableTerm, Object>>emptySet(),
        true
    );

    try (OptEnvironment solver = this.solver.newOptEnvironment()) {
      solver.addConstraint(formulaWithInitial);

      shutdownNotifier.shutdownIfNecessary();

      for (Template template : state.getTemplates()) {

        // Optimize for the template subject to the
        // constraints introduced by {@code p}.
        Formula objective = templateManager.toFormula(template, p);

        // We can't use multi-objective semantics as we need a separate model
        // for each optimized objective.
        solver.push();
        logger.log(Level.FINE, "Optimizing for ", objective);
        logger.log(Level.FINE, "Constraints: ", p.getFormula());
        logger.flush();
        int handle = solver.maximize(objective);

        OptEnvironment.OptStatus status;
        try {
          statistics.startOPTTimer();
          status = solver.check();
        } finally {
          statistics.stopOPTTimer();
        }

        // Generate the trace for the single template.
        switch (status) {
          case OPT:
            Optional<Rational> bound = solver.upper(handle, EPSILON);
            if (bound.isPresent()) {
              Model model = solver.getModel();
              Rational boundValue = bound.get();
              if (template.type.isUnsigned() && template.isLowerBound()) {
                boundValue = Rational.max(boundValue, Rational.ZERO);
              }

              // NOTE: it is important to use the formula which does not include
              // the initial condition.
              PolicyBound policyBound = policyBoundFromModel(
                  p, annotatedFormula, model, boundValue);

              abstraction.put(template, policyBound);
            } else {
              if (template.type.isUnsigned() && template.isLowerBound()) {
                Model model = solver.getModel();
                abstraction.put(
                    template, policyBoundFromModel(
                        p, transferRelation, model, Rational.ZERO));
              }
            }
            logger.log(Level.FINE, "Got bound: ", bound);
            break;
          case UNSAT:
            logger.log(Level.FINE, "Got UNSAT");
            return Optional.absent();
          case UNDEF:
            shutdownNotifier.shutdownIfNecessary();
            throw new CPATransferException("Solver returned undefined status");
        }
        solver.pop();
      }
    } catch (SolverException e) {
      throw new CPATransferException("Solver error: ", e);
    }

    return Optional.of(
        PolicyAbstractedState.of(
            abstraction.build(),
            state.getTemplates(),
            state.getLocation(),
            state));
  }

  /**
   * @return Representation of an {@code abstractState} as a
   * {@link PolicyIntermediateState}.
   */
  private PolicyIntermediateState abstractStateToIntermediate(
      PolicyAbstractedState abstractState) throws InterruptedException {

    SSAMap ssa = abstractState.getPathFormula().getSsa();
    List<BooleanFormula> constraints = new ArrayList<>();
    for (Entry<Template, PolicyBound> entry : abstractState) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      Formula t =
          templateManager.toFormula(template, abstractState.getPathFormula());

      BooleanFormula constraint = fmgr.makeLessOrEqual(
          t,
          fmgr.makeNumber(t, bound.bound),
          true
      );
      constraints.add(constraint);
    }

    BooleanFormula initialConstraint = bfmgr.and(constraints);
    initialConstraint = bfmgr.or(
        bfmgr.not(bfmgr.makeVariable(INITIAL_CONDITION_FLAG)),
        bfmgr.and(
            initialConstraint,
            ifmgr.equal(
                ifmgr.makeVariable(START_LOCATION_FLAG),
                ifmgr.makeNumber(abstractState.getLocation().toID())
            )
        )
    );

    // TODO: check that it is correct, that is,
    // pointer information stays invariant under the loop.
    if (propagateFormulasPastAbstraction) {
      BooleanFormula extraBit = abstractState.getPathFormula().getFormula();
      BooleanFormula pointerData = formulaSlicingManager.pointerFormulaSlice(
          extraBit);

      initialConstraint = bfmgr.and(initialConstraint, pointerData);
    }

    PathFormula path = new PathFormula(
        initialConstraint, ssa,
        abstractState.getPathFormula().getPointerTargetSet(),
        0
    );

    return PolicyIntermediateState.of(
        abstractState.getLocation(),
        abstractState.getTemplates(),
        path,
        HashMultimap.<Location, Location>create()
    );
  }

  /**
   * Use the auxiliary variables from the {@code model} to reconstruct the
   * trace which was use for abstracting the state associated with the
   * {@code node}.
   *
   * @return Reconstructed trace
   */
  private PolicyBound policyBoundFromModel(
      PathFormula inputPathFormula,
      BooleanFormula transferRelation,
      Model model,
      Rational bound) {


    BooleanFormula policyFormula = linearizationManager.enforceChoice(
        transferRelation, model.entrySet(), false
    );

    BigInteger prevLocationID = (BigInteger)model.get(
        new Model.Constant(START_LOCATION_FLAG, Model.TermType.Integer));
    int locationID = Ints.checkedCast(prevLocationID.longValue());
    Location prevLocation = Location.ofID(locationID);

    return PolicyBound.of(
        inputPathFormula.updateFormula(policyFormula), bound, prevLocation);
  }

  /**
   * @return Whether to compute the abstraction when creating a new
   * state associated with <code>node</code>.
   */
  private boolean shouldPerformAbstraction(PolicyIntermediateState state) {
    CFANode node = state.getNode();
    if (!pathFocusing) {
      return true;
    }
    if (node.isLoopStart()) {
      return true;
    }
    return false;
  }

  /**
   * @return the subset of {@code abstractStates} required for the update
   * {@code updated}.
   */
  private Map<Location, PolicyAbstractedState> findRelated(
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

      PolicyState state;
      if (loc == focusedLocation) {
        state = newState;

      } else {
        state = abstractStates.get(loc);
      }
      PolicyAbstractedState aState = state.asAbstracted();
      out.put(loc, aState);

      for (Entry<Template, PolicyBound> entry : aState) {
        Template template = entry.getKey();
        PolicyBound bound = entry.getValue();

        // Do not follow the edges which are associated with the focused node
        // but are not in <updated>.
        if (state != newState || updated.containsKey(template)) {
          Location toVisit = bound.predecessor;

          if (!visited.contains(toVisit)) {
            queue.add(toVisit);
          }
        }
      }
    }
    return out;
  }

  /**
   * @return Whether {@code newState} is covered by {@code oldState}
   */
  private boolean checkCovering(
      PolicyIntermediateState newState,
      PolicyIntermediateState oldState
  ) throws CPATransferException, InterruptedException {
    for (Entry<Location, Location> e : newState.getTrace().entries()) {
      if (!oldState.getTrace().containsEntry(e.getKey(), e.getValue())) {
        return false;
      }
    }
    return true;
  }
}
