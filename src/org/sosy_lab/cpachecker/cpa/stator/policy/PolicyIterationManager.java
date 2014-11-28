package org.sosy_lab.cpachecker.cpa.stator.policy;

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

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PathFormulaReportingState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Main logic in a single class.
 */
public class PolicyIterationManager implements IPolicyIterationManager {

  private final FormulaManagerView fmgr;

  @SuppressWarnings("unused, FieldCanBeLocal")
  private final CFA cfa;
  private final PathFormulaManager pfmgr;
  private final LinearConstraintManager lcmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerFactory formulaManagerFactory;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final NumeralFormulaManagerView<NumeralFormula, NumeralFormula.RationalFormula>
      rfmgr;
  private final TemplateManager templateManager;
  private final ValueDeterminationFormulaManager vdfmgr;
  private final PolicyIterationStatistics statistics;

  public PolicyIterationManager(
      FormulaManagerView pFormulaManager,
      CFA pCfa,
      PathFormulaManager pPfmgr,
      LinearConstraintManager pLcmgr,
      BooleanFormulaManager pBfmgr,
      FormulaManagerFactory pFormulaManagerFactory,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      NumeralFormulaManagerView<NumeralFormula, NumeralFormula.RationalFormula> pRfmgr,
      TemplateManager pTemplateManager,
      ValueDeterminationFormulaManager pValueDeterminationFormulaManager,
      PolicyIterationStatistics pStatistics) {
    fmgr = pFormulaManager;
    cfa = pCfa;
    pfmgr = pPfmgr;
    lcmgr = pLcmgr;
    bfmgr = pBfmgr;
    formulaManagerFactory = pFormulaManagerFactory;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    rfmgr = pRfmgr;
    templateManager = pTemplateManager;
    vdfmgr = pValueDeterminationFormulaManager;
    statistics = pStatistics;

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
    unreachable = new HashSet<>();
  }

  /** Static caches */
  private final ImmutableMap<Integer, CFANode> nodeMap;

  // Mapping from loop-heads to the associated loops.
  private final ImmutableMap<CFANode, LoopStructure.Loop> loopStructure;

  /** Scary-hairy global, contains all abstract states. */
  private final Map<CFANode, PolicyState> abstractStates;

  /** Another scary global, all nodes found to be unreachable */
  private final Set<CFANode> unreachable;

  /** Constants */

  // Temporary variable for edge selection.
  private static final String SELECTION_VAR_TEMPLATE = "__BRANCH_SELECTION_(%d)";

  /**
   * @param pNode Initial node
   * @return Initial state for the analysis, assuming the first node
   * is {@code pNode}
   */
  @Override
  public PolicyState getInitialState(CFANode pNode) {
    PolicyState initial = PolicyState.empty(pNode);
    abstractStates.put(pNode, initial);
    return initial;
  }

  @Override
  public Collection<PolicyState> getAbstractSuccessors(PolicyState oldState, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    Set<Template> newTemplates = Sets.union(
        oldState.getTemplates(),
        templateManager.templatesForEdge(edge)
    );

    PolicyState out = PolicyState.ofIntermediate(
        ImmutableList.<AbstractState>of(),
        ImmutableSet.of(edge),
        edge.getSuccessor(),
        newTemplates
    );

    // NOTE: the abstraction computation and the global update is delayed
    // until the {@code strengthen} call.
    return Collections.singleton(out);
  }

  @Override
  public Collection<PolicyState> strengthen(
      PolicyState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    state = state.withOtherStates(otherStates);

    statistics.abstractionTimer.start();
    try {
      // Perform the abstraction, if necessary.
      CFANode toNode = state.getNode();
      if (shouldPerformAbstraction(toNode) && !state.isAbstract()) {
        Optional<PolicyState> abstraction = performAbstraction(state);
        if (!abstraction.isPresent()) {
          unreachable.add(toNode);
          abstractStates.put(toNode, state);
          return Collections.emptyList();
        }
        state = abstraction.get();
      }
      abstractStates.put(toNode, state);
    } finally {
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
        boolean isSat = checkSatisfiability(state);
        if (!isSat) {
          unreachable.add(state.getNode());
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

  /**
   * @return {@code true} if and only if {@code oldState} represents a state
   * which is smaller-or-equal (with respect to the abstract lattice)
   * than the {@code newState}.
   */
  @Override
  public boolean isLessOrEqual(
       PolicyState oldState, PolicyState newState) {
    Preconditions.checkState(oldState.isAbstract() == newState.isAbstract(),
        "Abstraction state of two states associated with the same node should " +
            "match");

    boolean isAbstract = oldState.isAbstract();

    if (!isAbstract) {

      // Two formulas are incomparable.
      return false;
    } else {

      for (Entry<LinearExpression, PolicyBound> entry : newState) {
        LinearExpression template = entry.getKey();
        PolicyBound newBound = entry.getValue();
        Optional<PolicyBound> oldBound = oldState.getBound(template);

        if (!oldBound.isPresent()) {

          // Old bound is infinity, new bound is not, not covered.
          return false;
        } else if (oldBound.get().bound.compareTo(newBound.bound) > 0) {

          // Note that the trace obtained is irrelevant.
          return false;
        }
      }
      return true;
    }
  }

  @Override
  public PolicyState join(
      PolicyState newState, PolicyState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    statistics.timeInMerge.start();
    try {
      PolicyState out = join0(newState, oldState);
      abstractStates.put(out.getNode(), out);

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

  PolicyState join0(PolicyState newState,
                            PolicyState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    Preconditions.checkState(oldState.getNode() == newState.getNode());
    Preconditions.checkState(oldState.isAbstract() == newState.isAbstract());

    final boolean isAbstract = oldState.isAbstract();
    final CFANode node = oldState.getNode();

    Set<Template> allTemplates = Sets.union(
        oldState.getTemplates(), newState.getTemplates()
    );

    if (!isAbstract) {

      // No value determination, no abstraction, simply join incoming edges
      // and the tracked templates.
      return PolicyState.ofIntermediate(
          Iterables.concat(
              oldState.getOtherStates(), newState.getOtherStates()),
          Sets.union(oldState.getIncomingEdges(), newState.getIncomingEdges()),
          node, allTemplates
      );
    }

    Map<LinearExpression, PolicyBound> updated = new HashMap<>();
    Set<LinearExpression> unbounded = new HashSet<>();

    // Simple join:
    // Pick the biggest bound, and keep the biggest trace to match.
    for (Template templateWrapper : allTemplates) {
      LinearExpression template = templateWrapper.linearExpression;
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

    PolicyState stateWithUpdates =
        oldState.withUpdates(updated, unbounded, allTemplates);
    logger.log(Level.FINE, "# State with updates: ", stateWithUpdates);

    if (!shouldPerformValueDetermination(node, updated)) {
      return stateWithUpdates;

    } else {
      logger.log(Level.FINE, "# Value Determination launched");

      // Restrict the set of nodes to the ones related to the
      // {@code updated} ones.
      // TODO: this is bad code, don't want to pass the global around.
      // Later we can move it back to this class, after we get rid of
      // PolicyCPA.
      Map<CFANode, PolicyState> related =
          vdfmgr.findRelated(stateWithUpdates, abstractStates, node, updated);

      // Note: this formula contains no disjunctions, as the policy entails
      // the edge selection. Hence it can be used safely for the maximization.
      List<BooleanFormula> constraints = vdfmgr.valueDeterminationFormula(
          related, node, updated);

      return vdfmgr.valueDeterminationMaximization(
          oldState,
          allTemplates,
          updated, node, constraints);
    }
  }

  /**
   * @return Whether to perform the value determination on {@param node}.
   *
   * Returns true iff the {@param node} is a loophead and at least one of
   * the bounds in {@param updated} has an associated edge coming from
   * outside of the loop.
   */
  private boolean shouldPerformValueDetermination(
      CFANode node,
      Map<LinearExpression, PolicyBound> updated) {
    if (!node.isLoopStart()) {
      return false;
    }

    // At least one of updated values comes from inside the loop.
    LoopStructure.Loop l = loopStructure.get(node);
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
   * @return Whether the {@param state} is reachable.
   */
  private boolean checkSatisfiability(PolicyState state)
        throws CPATransferException, InterruptedException {

    CFANode node = state.getNode();
    PathFormula p = allPathsToNode(state, node);

    try (ProverEnvironment solver
            = formulaManagerFactory.newProverEnvironment(false, false)) {
      solver.push(p.getFormula());
      if (solver.isUnsat()) {
        return false;
      }
    } catch (SolverException e) {
      throw new CPATransferException("Failed solving", e);
    }

    return true;
  }

  /**
   * Perform the abstract operation on a new state
   *
   * @param state State to abstract
   * @return Abstracted state if the state is reachable, empty optional
   * otherwise.
   */
  private Optional<PolicyState> performAbstraction(PolicyState state)
      throws CPATransferException, InterruptedException {
    final CFANode node = state.getNode();
    final PathFormula p = allPathsToNode(state, node);

    ImmutableMap.Builder<LinearExpression, PolicyBound> abstraction
        = ImmutableMap.builder();

    for (Template templateWrapper : state.getTemplates()) {
      LinearExpression template = templateWrapper.linearExpression;

      // Optimize for the template subject to the
      // constraints introduced by {@code p}.
      try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {
        shutdownNotifier.shutdownIfNecessary();

        solver.addConstraint(p.getFormula());
        NumeralFormula objective =
            lcmgr.linearExpressionToFormula(template, p.getSsa());
        solver.maximize(objective);

        // Generate the trace for the single template.
        OptEnvironment.OptStatus result = solver.check();
        switch (result) {
          case OPT:
            Rational bound = solver.value();
            Model model = solver.getModel();
            logger.log(Level.FINE, "# Model =" + model);

            // Re-arrange the unique policy into the multi-edge.
            // Make sure that the edges meet.
            final List<CFAEdge> traceReversed = new ArrayList<>();
            final Set<CFANode> visitedNodes = new HashSet<>();
            visitedNodes.add(node);

            CFANode successor = node;
            while (true) {
              int toNodeNo = successor.getNodeNumber();
              Object o = model.get(
                  new Model.Constant(
                      String.format(SELECTION_VAR_TEMPLATE, toNodeNo),
                      Model.TermType.Real
                  )
              );
              if (o == null) {

                // Trace has finished.
                break;
              }
              int fromNodeNo = Integer.parseInt(o.toString());
              CFAEdge edge = edgeFromIdentifier(fromNodeNo, toNodeNo);
              assert edge != null;
              traceReversed.add(edge);
              successor = nodeMap.get(fromNodeNo);
              if (visitedNodes.contains(successor)) {

                // Don't loop.
                break;
              }
              visitedNodes.add(successor);
            }

            // Last successor is the ultimate "predecessor"
            CFANode predecessor = successor;
            assert !traceReversed.isEmpty() : model;
            MultiEdge edge = new MultiEdge(predecessor, node,
                Lists.reverse(traceReversed));
            logger.log(Level.FINE, "# Constructed edge: ", edge);
            abstraction.put(template, new PolicyBound(edge, bound));

            break;
          case UNSAT:
            // Short circuit: this point is infeasible.
            return Optional.absent();
          case UNBOUNDED:
            // Skip the constraint: it does not return any additional
            // information.
            continue;
          case UNDEF:
            throw new CPATransferException("Solver returned undefined status");
        }
      } catch (SolverException e) {
        throw new CPATransferException("Solver error: ", e);
      }
    }

    return Optional.of(PolicyState.ofAbstraction(
        abstraction.build(),
        state.getTemplates(),
        node,
        state.getIncomingEdges(),
        state.getOtherStates()));
  }

  /**
   * @return {@link CFAEdge} object connecting the node {@param fromNodeNo}
   * to the node {@param toNodeNo}.
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
   *
   * @return Whether to compute the abstraction when creating a new
   * state associated with {@param node}.
   */
  private boolean shouldPerformAbstraction(CFANode node) {
    // TODO: make configurable.
    // NOTE: we might also want to restrict the length of formulas
    // (e.g. run abstraction after 5 steps).
    return node.isLoopStart();
  }

  /**
  * Recursively compute the formula representing all possible paths
  * to the node {@param current} from the already resolved nodes.
  */
  private PathFormula allPathsToNode(PolicyState toState, CFANode current)
      throws CPATransferException, InterruptedException {
    HashMap<CFANode, PathFormula> memoization = new HashMap<>();
    return recAllPathsToNode(toState, current, memoization, true);
  }

  /**
   * Perform the recursive computation.
   * NOTE: at some point we'll have to re-write the version non-recursively.
   *
   * @param finalToState State corresponding to the node which will be the last
   * in the <b>final</b> {@link PathFormula}, does <b>not</b> change
   * with recursion.
   *
   * NOTE: this computation can be cached.
   */
  private PathFormula recAllPathsToNode(
      PolicyState finalToState,
      CFANode toNode,
      Map<CFANode, PathFormula> memoization,
      boolean firstEntrance)
      throws CPATransferException, InterruptedException {

    PolicyState s;

    // We use the non-abstracted version on the first iteration,
    // and the abstracted version on the second one.
    if (toNode == finalToState.getNode() && firstEntrance) {

      s = finalToState;
      Preconditions.checkState(!finalToState.isAbstract(),
          "Abstraction should not be already available on the final to-state");
    } else {
      s = abstractStates.get(toNode);
    }

    PathFormula out;

    if (s.isAbstract()) {

      // Base case, the constraints for the node are resolved.
      SSAMap empty = SSAMap.emptySSAMap();
      BooleanFormula constraints = abstractStateToFormula(
          s, empty
      );
      out = new PathFormula(
          constraints, empty, PointerTargetSet.emptyPointerTargetSet(), 1);

    } else {

      out = null;
      for (final CFAEdge edge : s.getIncomingEdges()) {
        CFANode predecessor = edge.getPredecessor();
        if (unreachable.contains(predecessor)) {
          continue;
        }

        NumeralFormula branchVar =  rfmgr.makeVariable(
            String.format(SELECTION_VAR_TEMPLATE, toNode.getNodeNumber()));
        BooleanFormula branchConstraint = rfmgr.equal(
            branchVar,
            rfmgr.makeNumber(predecessor.getNodeNumber())
        );
        assert toNode == edge.getSuccessor()
            && toNode == s.getNode();

        // Recursively compute the value for the predecessor.
        // Try the memoization cache first.
        // Issue the recursive call if the cache is empty.
        PathFormula prev = memoization.get(predecessor);
        if (prev == null) {
          prev = recAllPathsToNode(finalToState, predecessor, memoization, false);
        }

        // Use the {@code otherStates} for the formula construction.
        Optional<PathFormulaReportingState> other =
            getPathFormulaReportingState(s);
        PathFormula p = pfmgr.makeAnd(prev, edge);
        if (other.isPresent()) {
          PathFormula otherP = other.get().getFormulaApproximation(
              fmgr,
              p.getSsa(),
              prev.getSsa()
          );

          //
          p = new PathFormula(
              bfmgr.and(p.getFormula(), otherP.getFormula()),
              otherP.getSsa(),
              p.getPointerTargetSet(),
              p.getLength()
          );
        }

        p = pfmgr.makeAnd(p, branchConstraint);

        if (out == null) {
          out = p;
        } else {
          out = pfmgr.makeOr(out, p);
        }
      }

      if (out == null) {
        // Return false if no input is available.
        out = new PathFormula(
            bfmgr.makeBoolean(false),
            SSAMap.emptySSAMap(),
            PointerTargetSet.emptyPointerTargetSet(),
            1
        );
      }
    }

    memoization.put(toNode, out);
    return out;
  }

  private Optional<PathFormulaReportingState> getPathFormulaReportingState(
      PolicyState state
  ) {
    for (AbstractState oState : state.getOtherStates()) {
      if (oState instanceof PathFormulaReportingState) {
        return Optional.of((PathFormulaReportingState)oState);
      }
    }
    return Optional.absent();
  }

  private BooleanFormula abstractStateToFormula(
      PolicyState abstractState, SSAMap ssa) {

    List<BooleanFormula> tokens = new ArrayList<>();
    for (Entry<LinearExpression, PolicyBound> entry : abstractState) {
      LinearExpression template = entry.getKey();
      PolicyBound bound = entry.getValue();
      BooleanFormula constraint = rfmgr.lessOrEquals(
          lcmgr.linearExpressionToFormula(template, ssa),
          rfmgr.makeNumber(bound.bound.toString())
      );
      tokens.add(constraint);
    }
    return bfmgr.and(tokens);
  }
}
