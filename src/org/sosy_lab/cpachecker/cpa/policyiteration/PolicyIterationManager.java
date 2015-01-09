package org.sosy_lab.cpachecker.cpa.policyiteration;

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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Main logic in a single class.
 */
@Options(prefix="cpa.stator.policy")
public class PolicyIterationManager implements IPolicyIterationManager {

  @Option(secure=true,
      description="Call [simplify] on the formulas resulting from the C code")
  private boolean simplifyFormulas = true;

  @Option(secure=true,
      description="Perform abstraction only at the nodes from the cut-set.")
  private boolean pathFocusing = true;

  @Option(secure=true,
    description="Perform formula slicing after abstractions to propagate the" +
        " pointer information")
  private boolean propagateFormulasPastAbstraction = true;

  @Option(secure=true, name="epsilon",
      description="Value to substitute for the epsilon")
  private Rational EPSILON = Rational.ONE;

  private final FormulaManagerView fmgr;

  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  private final CFA cfa;
  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final NumeralFormulaManagerView<NumeralFormula, RationalFormula> rfmgr;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifmgr;
  private final TemplateManager templateManager;
  private final ValueDeterminationFormulaManager vdfmgr;
  private final PolicyIterationStatistics statistics;
  private final FormulaSlicingManager formulaSlicingManager;

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
      FormulaSlicingManager pFormulaSlicingManager)
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

    /** Compute the cache for nodes */
    ImmutableMap.Builder<Integer, CFANode> nodeMapBuilder = ImmutableMap.builder();
    for (CFANode node : pCfa.getAllNodes()) {
      nodeMapBuilder.put(node.getNodeNumber(), node);
    }
    nodeMap = nodeMapBuilder.build();

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

  /** Static caches */
  private final ImmutableMap<Integer, CFANode> nodeMap;

  // Mapping from loop-heads to the associated loops.
  private final ImmutableMap<CFANode, LoopStructure.Loop> loopStructure;

  /** Scary-hairy global, contains all abstract states. */
  private final Map<Location, PolicyState> abstractStates;

  /** Constants */

  // Temporary variable for edge selection.
  private static final String SELECTION_VAR_TEMPLATE = "__BRANCH_SELECTION_(%d)";

  // Option #2: track the location on my own?

  /**
   * @param pNode Initial node
   * @return Initial state for the analysis, assuming the first node
   * is {@code pNode}
   */
  @Override
  public PolicyState getInitialState(CFANode pNode) {
    Location initialLocation = Location.initial(pNode);
    PolicyAbstractedState initial = PolicyState.empty(initialLocation, pfmgr.makeEmptyPathFormula());
    abstractStates.put(initialLocation, initial);
    return initial;
  }

  @Override
  public Collection<PolicyState> getAbstractSuccessors(PolicyState oldState, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    CFANode node = edge.getSuccessor();
    Location oldLocation = oldState.getLocation();
    Location newLocation = newLocation(oldLocation, edge);
    PathFormula prev;

    Multimap<Location, Location> trace = HashMultimap.create();

    if (oldState.isAbstract()) {
      PolicyAbstractedState aOldState = oldState.asAbstracted();
      prev = abstractStateToPathFormula(aOldState);
    } else {
      PolicyIntermediateState iOldState = oldState.asIntermediate();
      prev = oldState.asIntermediate().getPathFormula();
      trace.putAll(iOldState.getTrace());
    }


    // NOTE: possible optimization: only add extra variables
    // if there is a choice (more than one incoming edge).


    // Create path selection variables if there are multiple choices for
    // the entering edge.
    IntegerFormula branchVar = ifmgr.makeVariable(
        String.format(SELECTION_VAR_TEMPLATE, newLocation.toID()));

    BooleanFormula branchConstraint = ifmgr.equal(
        branchVar,
        ifmgr.makeNumber(oldLocation.toID())
    );
    prev = pfmgr.makeAnd(prev, branchConstraint);

    // Serialize the choice to trace as well.
    trace.put(newLocation, oldLocation);

    PathFormula outF = pfmgr.makeAnd(prev, edge);
    if (simplifyFormulas) {
      outF = new PathFormula(
          fmgr.simplify(outF.getFormula()),
          outF.getSsa(), outF.getPointerTargetSet(), outF.getLength());
    }

    PolicyState out = PolicyIntermediateState.of(
        newLocation,

        // We take the variables alive at the location + templates for the
        // previous location (variable may be not alive at {@code node},
        // but required for the guard associated with {@code edge}
        // nevertheless.
        Sets.union(templateManager.templatesForNode(node),
            oldState.getTemplates()),
        outF,

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

    statistics.abstractionTimer.start();
    CFANode toNode = state.getNode();
    try {
      // Perform the abstraction, if necessary.
      if (shouldPerformAbstraction(toNode) && !state.isAbstract()) {
        PolicyIntermediateState iState = state.asIntermediate();

        logger.log(Level.FINE, ">>> Abstraction from formula", iState.getPathFormula());
        logger.log(Level.FINE, "SSA: ", iState.getPathFormula().getSsa());
        Optional<PolicyAbstractedState> abstraction = performAbstraction(iState);
        if (!abstraction.isPresent()) {
          return Collections.emptyList();
        }
        state = abstraction.get();
        logger.log(Level.FINE, ">>> Abstraction produced state: ", state);
      }
    } finally {
      abstractStates.put(state.getLocation(), state);
      statistics.abstractionTimer.stop();
    }

    // Perform the reachability check for the target states.
    statistics.strengthenTimer.start();
    try {
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

    } finally {
      statistics.strengthenTimer.stop();
    }
  }

  @Override
  public PolicyState join(PolicyState newState, PolicyState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    statistics.timeInMerge.start();
    try {
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
    } finally {
      statistics.timeInMerge.stop();
    }
  }

  private PolicyIntermediateState joinIntermediateStates(
      PolicyIntermediateState newState,
      PolicyIntermediateState oldState
  ) throws CPATransferException, InterruptedException {
    Preconditions.checkState(newState.getLocation().equals(oldState.getLocation()));
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

    // TODO: bug, old covers new.... how could that have happened?
    // Should we ever get to that state?
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
        oldState.withUpdates(updated, unbounded, allTemplates,
            oldState.getPathFormula());
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

    ImmutableMap.Builder<Template, PolicyBound> builder = ImmutableMap.builder();
    Set<Template> unbounded = new HashSet<>();

    // Maximize for each template subject to the overall constraints.
    statistics.valueDeterminationSolverTimer.start();
    statistics.valueDetCalls++;
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

      OptEnvironment.OptStatus result = solver.check();
      if (result != OptEnvironment.OptStatus.OPT) {
        throw new CPATransferException("Unexpected solver state, " +
            "value determination problem should be feasible");
      }

      for (Entry<Template, PolicyBound> policyValue : updated.entrySet()) {
        Template template = policyValue.getKey();
        PolicyBound bound = policyValue.getValue();
        MultiEdge policyEdge = bound.trace;
        Optional<Rational> value =  solver.upper(objectiveHandles.get(template),
            EPSILON);

        if (value.isPresent()) {
          builder.put(template, PolicyBound.of(
              policyEdge,
              value.get(),
              bound.updatedFrom
              ));
        } else {
          unbounded.add(template);
        }
      }
    } catch (SolverException e) {
      throw new CPATransferException("Failed maximization ", e);
    } finally {
      statistics.valueDeterminationSolverTimer.stop();
    }

    return prevState.withUpdates(builder.build(), unbounded, templates,
        prevState.getPathFormula());
  }

  /**
   * @return Whether to perform the value determination on <code>node</code>.
   *
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
      CFAEdge edge = bound.trace;
      assert edge.getSuccessor() == node;
      if (l.getLoopNodes().contains(edge.getPredecessor())) {
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

    try {
      return !solver.isUnsat(state.getPathFormula().getFormula());
    } catch (SolverException e) {
      throw new CPATransferException("Failed solving", e);
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
      PolicyIntermediateState state)
      throws CPATransferException, InterruptedException {
    final PathFormula p = state.getPathFormula();

    ImmutableMap.Builder<Template, PolicyBound> abstraction
        = ImmutableMap.builder();

    try (OptEnvironment solver = this.solver.newOptEnvironment()) {
      solver.addConstraint(p.getFormula());

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
        int handle = solver.maximize(objective);

        logger.flush();

        // Generate the trace for the single template.
        switch (solver.check()) {
          case OPT:
            Optional<Rational> bound = solver.upper(handle, EPSILON);
            boolean isLowerBound = (template.type.isUnsigned() &&
                  template.linearExpression.size() == 1 &&
                  template.linearExpression.iterator().next().getValue().equals(Rational.NEG_ONE));
            if (bound.isPresent()) {
              Model model = solver.getModel();
              Pair<MultiEdge, Location> pair = traceFromModel(state, model);
              MultiEdge edge = pair.getFirst();
              Location location = pair.getSecond();

              Rational boundValue = bound.get();
              if  (isLowerBound) {
                boundValue = Rational.max(boundValue, Rational.ZERO);
              }

              abstraction.put(template, new PolicyBound(edge, boundValue, location));
            } else {
              if (isLowerBound) {
                Model model = solver.getModel();
                Pair<MultiEdge, Location> pair = traceFromModel(state, model);
                MultiEdge edge = pair.getFirst();
                Location location = pair.getSecond();
                abstraction.put(template, new PolicyBound(edge, Rational.ZERO, location));
              }
            }
            logger.log(Level.FINE, "Got bound: ", bound);
            break;
          case UNSAT:
            // Short circuit: this point is infeasible.
            logger.log(Level.FINE, "Got UNSAT");
            return Optional.absent();
          case UNDEF:
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
            state.getTemplates(), state.getLocation(), p));
  }

  /**
   * @return Representation of an {@code abstractState} as a {@link PathFormula}.
   */
  private PathFormula abstractStateToPathFormula(
      PolicyAbstractedState abstractState) throws  InterruptedException {

    SSAMap ssa = abstractState.getPathFormula().getSsa();
    List<BooleanFormula> tokens = new ArrayList<>();
    for (Entry<Template, PolicyBound> entry : abstractState) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      Formula t = templateManager.toFormula(template, abstractState.getPathFormula());

      BooleanFormula constraint;
      constraint = fmgr.makeLessOrEqual(
          t,
          fmgr.makeNumber(t, bound.bound),
          true
      );
      tokens.add(constraint);
    }
    BooleanFormula constraint = bfmgr.and(tokens);

    BooleanFormula extraBit = abstractState.getPathFormula().getFormula();

    // TODO: check that it is correct, that is,
    // pointer information stays invariant under the loop.
    if (propagateFormulasPastAbstraction) {
      BooleanFormula pointerData = formulaSlicingManager.pointerFormulaSlice(
          extraBit);
      constraint = bfmgr.and(constraint, pointerData);
    }

    return new PathFormula(
        constraint, ssa,
        abstractState.getPathFormula().getPointerTargetSet(),
        0
    );
  }

  /**
   * Use the auxiliary variables from the {@code model} to reconstruct the
   * trace which was use for abstracting the state associated with the
   * {@code node}.
   *
   * @return Reconstructed trace
   */
  private Pair<MultiEdge, Location> traceFromModel(
      PolicyIntermediateState state,
      Model model) {

    final Location location = state.getLocation();
    final List<CFAEdge> traceReversed = new ArrayList<>();
    final Set<Location> visitedLocations = new HashSet<>();
    visitedLocations.add(location);

    Location successor = location;
    while (true) {
      int toNodeNo = successor.node.getNodeNumber();
      long locationNo = successor.toID();
      CFANode toNode = nodeMap.get(toNodeNo);
      CFAEdge edge;
      int fromNodeNo;

      if (toNode.getNumEnteringEdges() != 0) {

        Object o = model.get(
            new Model.Constant(String.format(SELECTION_VAR_TEMPLATE, locationNo),
                Model.TermType.Integer)
        );
        if (o == null) { // Trace has finished.
          break;
        }
        int fromLocationID = Integer.parseInt(o.toString());
        successor = Location.ofID(fromLocationID);

        fromNodeNo = successor.node.getNodeNumber();
        edge = edgeFromIdentifier(fromNodeNo, toNodeNo);
      } else {

        // Function start.
        break;
      }

      assert edge != null;
      traceReversed.add(edge);
      if (visitedLocations.contains(successor)) {

        // Don't loop.
        break;
      }
      visitedLocations.add(successor);
    }

    // Last successor is the ultimate "predecessor"
    Location predecessor = successor;
    assert !traceReversed.isEmpty() : model;
    return Pair.of(
        new MultiEdge(
            predecessor.node,
            state.getNode(),
            Lists.reverse(traceReversed)
        ), predecessor
    );
  }

  /**
   *
   * @return Whether to compute the abstraction when creating a new
   * state associated with <code>node</code>.
   */
  private boolean shouldPerformAbstraction(CFANode node) {
    if (!pathFocusing) {
      return true;
    }
    if (node.isLoopStart()) {
      return true;
    }
    return false;
  }

  /**
   * @return {@link CFAEdge} object connecting the node <code>fromNodeNo</code>
   * to the node <code>toNodeNo</code>.
   */
  private CFAEdge edgeFromIdentifier(int fromNodeNo, int toNodeNo) {
    CFANode toNode = nodeMap.get(toNodeNo);
    for (int idx=0; idx<toNode.getNumEnteringEdges(); idx++) {
      CFAEdge enteringEdge = toNode.getEnteringEdge(idx);
      if (enteringEdge.getPredecessor().getNodeNumber() == fromNodeNo) {
        return enteringEdge;
      }
    }
    throw new IllegalArgumentException(
        "Pair of nodes corresponds to the non-existent edge: " +
            fromNodeNo + "->" + toNodeNo);
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
      Preconditions.checkState(state.isAbstract());

      PolicyAbstractedState aState = state.asAbstracted();
      out.put(loc, aState);

      for (Entry<Template, PolicyBound> entry : aState) {
        Template template = entry.getKey();
        PolicyBound bound = entry.getValue();

        // Do not follow the edges which are associated with the focused node
        // but are not in <updated>.
        if (!(state == newState && !updated.containsKey(template))) {
          Location toVisit = bound.updatedFrom;

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

  private Location newLocation(Location old, CFAEdge pEdge) {
    if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      return Location.withCallsite(old, pEdge.getPredecessor(), pEdge.getSuccessor());
    } else if (pEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      return Location.popCallsite(old, pEdge.getSuccessor());
    } else {
      return Location.withNode(old, pEdge.getSuccessor());
    }
  }

}
