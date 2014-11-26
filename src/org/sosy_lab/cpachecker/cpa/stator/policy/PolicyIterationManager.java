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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
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
 * // TODO: extract the interface.
 */
public class PolicyIterationManager {

  /**
   * Dependencies.
   */
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

  public PolicyIterationManager(CFA pCfa,
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
    for (CFANode node : cfa.getAllNodes()) {
      nodeMapBuilder.put(node.getNodeNumber(), node);
    }
    nodeMap = nodeMapBuilder.build();

    /** Compute the cache for loops */
    ImmutableMap.Builder<CFANode, LoopStructure.Loop> loopStructureBuilder =
        ImmutableMap.builder();
    LoopStructure loopStructure1 = cfa.getLoopStructure().get();
    for (LoopStructure.Loop l : loopStructure1.getAllLoops()) {
      for (CFANode n : l.getLoopHeads()) {
        loopStructureBuilder.put(n, l);
      }
    }
    loopStructure = loopStructureBuilder.build();

    abstractStates = new HashMap<>();
  }

  /** Static caches */
  private final ImmutableMap<Integer, CFANode> nodeMap;

  // Mapping from loop-heads to the associated loops.
  private final ImmutableMap<CFANode, LoopStructure.Loop> loopStructure;

  /** Scary-hairy global, contains all abstract states. */
  // TODO: we need a separate cache for "bottom" values, as those
  // can appear during the [strengthen] call, which does not get a chance
  // to invalidate this cache (mark some node as unreachable).
  // Actually, if we just keep a set of nodes which are deemed unreachable
  // and check for those it should be fine.
  Map<CFANode, PolicyAbstractState> abstractStates;

  /** Constants */

  // Constructing and deconstructing the boolean variable names.
  private static final String SELECTION_VAR_PREFIX = "__BRANCH_SELECTION";
  private static final String SELECTION_VAR_TEMPLATE =
      SELECTION_VAR_PREFIX + "(%d)";
  private static final Pattern SELECTION_VAR_PATTERN =
      Pattern.compile(
          SELECTION_VAR_PREFIX +  "\\((\\d+)\\)");

  /**
   * @return Initial state for the analysis, assuming the first node
   * is {@param pNode}
   */
  public AbstractState getInitialState(CFANode pNode) {
    PolicyAbstractState initial = PolicyAbstractState.empty(pNode);
    abstractStates.put(pNode, initial);
    return initial;
  }

  Collection<PolicyAbstractState> getAbstractSuccessors(PolicyAbstractState oldState, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    CFANode toNode = edge.getSuccessor();
    Templates newTemplates = templateManager.updateTemplatesForEdge(
        oldState.getTemplates(), edge
    );

    PolicyAbstractState out = PolicyAbstractState.ofIntermediate(
        ImmutableList.<AbstractState>of(),
        ImmutableSet.of(edge),
        edge.getSuccessor(),
        newTemplates
    );

    if (shouldPerformAbstraction(toNode)) {

      Optional<PolicyAbstractState> abstraction = performAbstraction(out);
      if (!abstraction.isPresent()) {
        return Collections.emptyList();
      }
      out = abstraction.get();
    }

    abstractStates.put(toNode, out);
    return Collections.singleton(out);
  }

  public Collection<? extends AbstractState> strengthen(
      PolicyAbstractState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    // TODO: should we update the global invariant?
    // Also: what about removing state? This
    // whole multi-analysis business is quite complicated.

    statistics.strengthenTimer.start();
    try {
      if (state.isAbstract()) {

        // TODO: clean up the code.
        // The state is already abstracted.
        return null;
      }

      boolean hasErrorState = false;
      for (AbstractState oState : otherStates) {
        if (AbstractStates.isTargetState(oState)) {
          hasErrorState = true;
        }
      }

      if (hasErrorState) {
        boolean isSat = checkSatisfiability(state);
        if (!isSat) {
          return Collections.emptyList();
        } else {
          return null;
        }
      }

      return null;

    } finally {
      statistics.strengthenTimer.stop();
    }
  }

  /**
   * @return {@code true} if and only if {@param oldState} represents a state
   * which is smaller-or-equal (with respect to the abstract lattice)
   * than the {@param newState}.
   */
  boolean isLessOrEqual(PolicyAbstractState oldState, PolicyAbstractState newState) {
    // NOTE: hm this will stop being true if we enforce the abstraction after
    // N states (if it does not depend just on the node).
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

  PolicyAbstractState join(PolicyAbstractState newState, PolicyAbstractState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    statistics.timeInMerge.start();
    try {
      PolicyAbstractState out = join0(newState, oldState);
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

  PolicyAbstractState join0(PolicyAbstractState newState,
                            PolicyAbstractState oldState)
      throws CPATransferException, InterruptedException, SolverException {
    Preconditions.checkState(oldState.getNode() == newState.getNode());
    Preconditions.checkState(oldState.isAbstract() == newState.isAbstract());

    final boolean isAbstract = oldState.isAbstract();
    final CFANode node = oldState.getNode();

    Templates allTemplates = oldState.getTemplates().merge(
        newState.getTemplates());

    if (!isAbstract) {

      // No value determination, no abstraction, simply join incoming edges
      // and the tracked templates.
      return PolicyAbstractState.ofIntermediate(
          Iterables.concat(
              oldState.getOtherStates(), newState.getOtherStates()),
          Sets.union(oldState.getIncomingEdges(), newState.getIncomingEdges()),
          node, allTemplates
      );
    }

    Map<LinearExpression, PolicyBound> updated = new HashMap<>();
    Set<LinearExpression> unbounded = new HashSet<>();

    // Traditional join as currently present in the non-path-focusing code:
    // just pick the biggest bound, and keep the biggest trace to match.
    for (LinearExpression template : allTemplates) {
      Optional<PolicyBound> oldValue, newValue;
      oldValue = oldState.getBound(template);
      newValue = newState.getBound(template);

      if (!oldValue.isPresent()) {

        // Can't do better than unbounded.
        continue;
      } else if (!newValue.isPresent()) {

        // Became unbounded after 1 iteration.
        unbounded.add(template);
        continue;
      } else if (newValue.get().bound.compareTo(oldValue.get().bound) > 0) {
        updated.put(template, newValue.get());
      }
    }

    PolicyAbstractState stateWithUpdates =
        oldState.withUpdates(updated, unbounded, allTemplates);
    logger.log(Level.FINE, "# State with updates: ", stateWithUpdates);

    // TODO: OK, well than the trace we are getting is wrong,
    // We seem to always start from the non-looping edge
    // (which can't be right...).

    if (!shouldPerformValueDetermination(node, updated)) {
      return stateWithUpdates;

    } else {
      logger.log(Level.FINE, "# Value Determination launched");

      // Launching the value determination.
      Map<CFANode, PolicyAbstractState> policy =
          vdfmgr.findRelated(stateWithUpdates, abstractStates, node,
              updated);

      // Note: this formula contains no disjunction, as the policy entails
      // the edge selection. Hence it can be used safely for the maximization.
      List<BooleanFormula> constraints = vdfmgr.valueDeterminationFormula(
          policy, node, updated);

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

  private boolean checkSatisfiability(
      PolicyAbstractState state)
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

  private Optional<PolicyAbstractState> performAbstraction(PolicyAbstractState state)
      throws CPATransferException, InterruptedException {
    final CFANode node = state.getNode();
    final PathFormula p = allPathsToNode(state, node);

    ImmutableMap.Builder<LinearExpression, PolicyBound> abstraction
        = ImmutableMap.builder();

    for (final LinearExpression template : state.getTemplates()) {
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

            // Identify trace from the auxiliary boolean variables in the model.
            // (one entering edge per node.)
            // The hashmap will implicitly filter out the duplicates.
            // The last one will be used.
            Map<CFANode, CFAEdge> policy = new HashMap<>();

            for (Entry<Model.AssignableTerm, Object> e : model.entrySet()) {
              Model.AssignableTerm t = e.getKey();
              Matcher m = SELECTION_VAR_PATTERN.matcher(t.getName());

              if (t.getType() == Model.TermType.Real
                  && m.matches()) {

                int fromNodeNo = Integer.valueOf((e.getValue()).toString());
                int toNodeNo = Integer.parseInt(m.group(1));

                CFANode toNode = nodeMap.get(toNodeNo);
                CFAEdge edge = edgeFromIdentifier(fromNodeNo, toNodeNo);
                assert edge != null;

                policy.put(toNode, edge);
              }
            }
            logger.log(Level.FINE, "# Model =" + model);
            assert !policy.isEmpty();

            // Re-arrange the unique policy into the multi-edge.
            // Make sure that the edges meet.
            final List<CFAEdge> traceReversed = new ArrayList<>(policy.size());

            CFANode predecessor = node;
            // We know the ending point! the variable <node>.
            for (int i=0; i<policy.size(); i++) {
              CFAEdge edge = policy.get(predecessor);
              assert edge != null;
              traceReversed.add(edge);
              predecessor = edge.getPredecessor();
            }

            assert !traceReversed.isEmpty();
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
    
    return Optional.of(PolicyAbstractState.ofAbstraction(
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
        "Pair of nodes corresponds to the non-existent edge.");
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
  private PathFormula allPathsToNode(PolicyAbstractState toState, CFANode current)
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
      PolicyAbstractState finalToState,
      CFANode currentNode,
      Map<CFANode, PathFormula> memoization,
      boolean firstEntrance)
      throws CPATransferException, InterruptedException {

    PolicyAbstractState s;

    // We use the non-abstracted version on the first iteration,
    // and the abstracted version on the second one.
    if (currentNode == finalToState.getNode() && firstEntrance) {

      s = finalToState;
      Preconditions.checkState(!finalToState.isAbstract(),
          "Abstraction should not be already available on the final to-state");
    } else {
      s = abstractStates.get(currentNode);
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

        // Recursively compute the value for the predecessor.
        // Try the memoization cache first.
        PathFormula prev = memoization.get(predecessor);

        // Issue the recursive call if the cache is empty.
        if (prev == null) {
          prev = recAllPathsToNode(finalToState, predecessor, memoization, false);
        }

        PathFormula p = pfmgr.makeAnd(prev, edge);
        if (out == null) {
          out = p;
        } else {
          out = pfmgr.makeOr(out, p);
        }

        // NOTE: easy optimization: do not insert the auxiliary
        // variable when there is only one incoming edge.

        // Add an auxiliary boolean variable which indicates what branch
        // was taken at the disjunction.
        NumeralFormula branchConstraint =  rfmgr.makeVariable(
            String.format(SELECTION_VAR_TEMPLATE,
                edge.getSuccessor().getNodeNumber()));


        out = pfmgr.makeAnd(out, rfmgr.equal(
            branchConstraint, rfmgr.makeNumber(predecessor.getNodeNumber())));
      }
    }

    memoization.put(currentNode, out);
    return out;
  }

  private BooleanFormula abstractStateToFormula(
      PolicyAbstractState abstractState, SSAMap ssa) {

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
