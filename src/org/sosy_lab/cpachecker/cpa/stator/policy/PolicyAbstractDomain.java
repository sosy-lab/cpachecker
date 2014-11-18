package org.sosy_lab.cpachecker.cpa.stator.policy;

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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

/**
 * Abstract domain for policy iteration.
 */
@Options(prefix="cpa.stator.policy")
public final class PolicyAbstractDomain implements AbstractDomain {

  @Option(secure=true, name="pathFocusing",
      description="Run (simplified) path focusing")
  private boolean pathFocusing = true;

  @Option(secure=true, name="runAcceleratedValueDetermination",
      description="Maximize for the sum of the templates during value determination")
  private boolean runAcceleratedValueDetermination = true;

  @Option(secure=true, name="useCompactedValueDetermination",
      description="Use only relevant nodes for value determination")
  private boolean useCompactedValueDetermination = true;

  private final ValueDeterminationFormulaManager vdfmgr;
  private final LogManager logger;
  private final FormulaManagerFactory formulaManagerFactory;
  private final LinearConstraintManager lcmgr;
  private final NumeralFormulaManagerView<NumeralFormula, NumeralFormula.RationalFormula>
      rfmgr;
  private final ShutdownNotifier shutdownNotifier;

  private final PolicyIterationStatistics statistics;
  private final PathFocusingManager pathFocusingManager;

  //
  public PolicyAbstractDomain(
      Configuration config,
      ValueDeterminationFormulaManager vdfmgr,
      FormulaManagerView fmgr,
      FormulaManagerFactory formulaManagerFactory,
      LogManager logger,
      LinearConstraintManager lcmgr,
      ShutdownNotifier pShutdownNotifier,
      PolicyIterationStatistics pStatistics
  ) throws InvalidConfigurationException {
    config.inject(this, PolicyAbstractDomain.class);
    this.vdfmgr = vdfmgr;
    this.logger = logger;
    this.formulaManagerFactory = formulaManagerFactory;
    this.lcmgr = lcmgr;
    rfmgr = fmgr.getRationalFormulaManager();
    shutdownNotifier = pShutdownNotifier;
    statistics = pStatistics;
    pathFocusingManager = new PathFocusingManager(pShutdownNotifier, statistics);
  }


  /**
   * Each iteration produces only one step, so after each run of
   * {@link PolicyTransferRelation#getAbstractSuccessors} we merge at most
   * two states (a new one, and potentially an already existing one for
   * this node).
   *
   * @param newState Newly obtained abstract state.
   * @param prevState A previous abstract state for this node (if such exists)
   * @return New abstract state.
   */
  public PolicyAbstractState join(
      final PolicyAbstractState newState,
      final PolicyAbstractState prevState,
      PolicyPrecision precision)
      throws CPAException, InterruptedException {

    // NOTE: check. But I think it must be actually the same node.
    final CFANode node = newState.getNode();

    logger.log(Level.FINE, "# Performing join on node ", node);

    /** Just return the old node if it is strictly larger */
    if (isLessOrEqual(newState, prevState)) {
      return prevState;
    }

    /** Find the templates which were updated */
    final Map<LinearExpression, PolicyTemplateBound> updated = new HashMap<>();

    PolicyAbstractState.Templates allTemplates = newState.getTemplates().merge(
        prevState.getTemplates());
    Set<LinearExpression> unbounded = new HashSet<>();

    for (LinearExpression template : allTemplates) {
      PolicyTemplateBound newValue = newState.getPolicyTemplateBound(template).orNull();
      PolicyTemplateBound oldValue = prevState.getPolicyTemplateBound(template).orNull();

      // Can't do better than unbounded.
      if (oldValue == null) {
        continue;
      }

      // We are unbounded already, no point in doing value determination for
      // this template.
      if (newValue == null) {
        unbounded.add(template);
        continue;
      }

      if (newValue.bound.compareTo(oldValue.bound) > 0) {
        updated.put(template, newValue);
      }
    }

    if (!node.isLoopStart()) {
      logger.log(Level.FINE, "# Merge not on the loop head, returning "
          + "the updated state");
      return prevState.withUpdates(updated, unbounded, allTemplates);
    }

    // Running value determination only on loop heads.
    statistics.valueDeterminationTimer.start();
    try {
      return valueDetermination(
          prevState, newState, updated, node, allTemplates, precision);
    } finally {
      statistics.valueDeterminationTimer.stop();
    }
  }

  private PolicyAbstractState valueDetermination(
      final PolicyAbstractState prevState,
      final PolicyAbstractState newState,
      Map<LinearExpression, PolicyTemplateBound> updated,
      CFANode node,
      PolicyAbstractState.Templates allTemplates,
      PolicyPrecision precision)
      throws CPAException, InterruptedException {

    logger.log(Level.FINE, "# Running value determination");
    Preconditions.checkState(updated.size() > 0,
        "There must exist at least one policy for which the new" +
        "node is strictly larger");

    Table<CFANode, LinearExpression, ? extends CFAEdge> policy;
    policy = reachedToPolicy(precision, node, updated);

    logger.log(Level.FINEST, "# Initial policy: ", policy);
    if (useCompactedValueDetermination) {
      policy = pathFocusingManager.findRelated(policy, node, updated);
    }
    logger.log(Level.FINEST, "# Policy after value determination", policy);
    if (pathFocusing) {
      policy = pathFocusingManager.pathFocusing(policy, node);
    }
    logger.log(Level.FINEST, "# Policy after path focusing", policy);

    List<BooleanFormula> valueDeterminationConstraints =
        vdfmgr.valueDeterminationFormula(policy, node, updated.keySet());
    logger.log(Level.FINE, "# Resulting formula: \n", valueDeterminationConstraints, "\n# end");

    Pair<ImmutableMap<LinearExpression, PolicyTemplateBound>,
        Set<LinearExpression>> p;
    if (runAcceleratedValueDetermination) {
      p = valueDeterminationMaximizationAccelerated(
          updated, node, valueDeterminationConstraints);
    } else {
      p = valueDeterminationMaximization(
          updated, node, valueDeterminationConstraints);
    }
    ImmutableMap<LinearExpression, PolicyTemplateBound> updatedValueDetermination = p.getFirst();
    Set<LinearExpression> unbounded = p.getSecond();

    // TODO: also what happens if the edge reported in the output is the
    // artificially created multi-edge? can we use that later on just fine?
    // This will be required for the iteration through the multiple policies.
    PolicyAbstractState joinedState = prevState.withUpdates(
        updatedValueDetermination, unbounded, allTemplates);

    Preconditions.checkState(isLessOrEqual(newState, joinedState));
    Preconditions.checkState(isLessOrEqual(prevState, joinedState));
    logger.log(Level.FINE, "# New state after merge: ", joinedState);
    return joinedState;
  }

  private Pair<ImmutableMap<LinearExpression, PolicyTemplateBound>,
      Set<LinearExpression>>
  valueDeterminationMaximization(
      Map<LinearExpression, PolicyTemplateBound> updated, CFANode node,
      List<BooleanFormula> pValueDeterminationConstraints)
      throws InterruptedException, CPATransferException {

    ImmutableMap.Builder<LinearExpression, PolicyTemplateBound> builder = ImmutableMap.builder();
    Set<LinearExpression> unbounded = new HashSet<>();

    for (Entry<LinearExpression, PolicyTemplateBound> policyValue : updated.entrySet()) {

      LinearExpression template = policyValue.getKey();
      CFAEdge policyEdge = policyValue.getValue().edge;

      // Maximize for each template subject to the overall constraints.
      try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {
        shutdownNotifier.shutdownIfNecessary();

        for (BooleanFormula constraint : pValueDeterminationConstraints) {
          solver.addConstraint(constraint);
        }

        logger.log(Level.FINE,
            "# Value determination: optimizing for template" , template);

        NumeralFormula objective = rfmgr.makeVariable(
            vdfmgr.absDomainVarName(node, template));

        statistics.valueDeterminationSolverTimer.start();
        statistics.valueDetCalls++;
        ExtendedRational newValue;
        try {
          newValue = lcmgr.maximize(solver, objective);
        } catch (SolverException e) {
          throw new CPATransferException("Failed maximization", e);
        } finally {
          statistics.valueDeterminationSolverTimer.stop();
        }

        Preconditions.checkState(newValue != ExtendedRational.NEG_INFTY,
            "Value determination should not be unsatisfiable");
        if (newValue.isRational()) {
          builder.put(
              template,
              PolicyTemplateBound.of(policyEdge, newValue.getRational()));
        } else {
          unbounded.add(template);
        }
      }
    }

    return Pair.of(builder.build(), unbounded);
  }

  private Pair<ImmutableMap<LinearExpression, PolicyTemplateBound>,
      Set<LinearExpression>>
  valueDeterminationMaximizationAccelerated(
      Map<LinearExpression, PolicyTemplateBound> updated, CFANode node,
      List<BooleanFormula> pValueDeterminationConstraints)
      throws InterruptedException, CPATransferException {

    ImmutableMap.Builder<LinearExpression, PolicyTemplateBound> builder = ImmutableMap.builder();
    Set<LinearExpression> unbounded = new HashSet<>();

    Map<NumeralFormula, Pair<PolicyTemplateBound, LinearExpression>> objectives = new HashMap<>();
    for (Entry<LinearExpression, PolicyTemplateBound> policyValue : updated.entrySet()) {
      LinearExpression template = policyValue.getKey();
      NumeralFormula objective = rfmgr.makeVariable(
          vdfmgr.absDomainVarName(node, template));
      objectives.put(objective, Pair.of(policyValue.getValue(), template));
    }

    try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {
      for (BooleanFormula constraint : pValueDeterminationConstraints) {
        solver.addConstraint(constraint);
      }

      statistics.valueDeterminationSolverTimer.start();
      statistics.valueDetCalls++;
      Map<NumeralFormula, ExtendedRational> model;
      try {
        model = lcmgr.maximizeObjectives(solver,
                Lists.newArrayList(objectives.keySet()));
      } catch (SolverException e) {
        logUnsatCore(pValueDeterminationConstraints);
        throw new CPATransferException("Failed maximization", e);
      } finally {
        statistics.valueDeterminationSolverTimer.stop();
      }

      for (Entry<NumeralFormula, ExtendedRational> e : model.entrySet()) {
        NumeralFormula f = e.getKey();
        ExtendedRational newValue = e.getValue();
        Pair<PolicyTemplateBound, LinearExpression> p = objectives.get(f);
        LinearExpression template = p.getSecond();
        PolicyTemplateBound templateBound = p.getFirst();

        assert (template != null && templateBound != null);
        CFAEdge policyEdge = templateBound.edge;

        if (newValue.isRational()) {
          builder.put(
              template,
              PolicyTemplateBound.of(policyEdge, newValue.getRational()));
        } else {
          unbounded.add(template);
        }
      }
    }
    return Pair.of(builder.build(), unbounded);
  }

  enum PARTIAL_ORDER {
    LESS,
    EQUAL,
    UNCOMPARABLE,
    GREATER
  }

  @Override
  public boolean isLessOrEqual(AbstractState newState, AbstractState prevState)
      throws CPAException {

    logger.log(Level.FINE, "# Comparing state =", newState, " to the state =", prevState);
    PARTIAL_ORDER ord = compare(
        (PolicyAbstractState)newState, (PolicyAbstractState)prevState
    );
    boolean ret = (ord == PARTIAL_ORDER.LESS || ord == PARTIAL_ORDER.EQUAL);
    logger.log(Level.FINE, "# Got comparison result = ", ord, ", returning = ", ret);
    return ret;
  }

  PARTIAL_ORDER compare(PolicyAbstractState newState,
                        PolicyAbstractState prevState) {

    if (newState == prevState) {
      return PARTIAL_ORDER.EQUAL;
    }

    boolean less_or_equal = true;
    boolean greater_or_equal = true;

    for (Entry<LinearExpression, PolicyTemplateBound> e : newState) {
      Rational newBound = e.getValue().bound;

      PolicyTemplateBound prevValue = prevState.getPolicyTemplateBound(
          e.getKey()).orNull();

      int cmp;
      if (prevValue == null) {
        cmp = -1;
      } else {
        cmp = newBound.compareTo(prevValue.bound);
      }

      if (cmp > 0) {
        less_or_equal = false;
      } else if (cmp < 0) {
        greater_or_equal = false;
      }
    }

    PARTIAL_ORDER ret;
    if (less_or_equal && greater_or_equal) {
      ret = PARTIAL_ORDER.EQUAL;
    } else if (less_or_equal) {
      ret = PARTIAL_ORDER.LESS;
    } else if (greater_or_equal) {
      ret = PARTIAL_ORDER.GREATER;
    } else {
      ret = PARTIAL_ORDER.UNCOMPARABLE;
    }

    return ret;
  }

  private Map<CFANode, PolicyAbstractState> toMap(PolicyPrecision p) {
    Map<CFANode, PolicyAbstractState> out = new HashMap<>();
    for (AbstractState s : p.getReached()) {
      PolicyAbstractState state = (PolicyAbstractState) s;
      Preconditions.checkState(!out.containsKey(state.getNode()));
      out.put(state.getNode(), state);
    }
    return out;
  }

  private Table<CFANode, LinearExpression, CFAEdge> reachedToPolicy(
      PolicyPrecision p,
      final CFANode focusedNode,
      Map<LinearExpression, PolicyTemplateBound> updated
  ) {
    Table<CFANode, LinearExpression, CFAEdge> table = HashBasedTable.create();
    Map<CFANode, PolicyAbstractState> map = toMap(p);
    for (Entry<CFANode, PolicyAbstractState> entry : map.entrySet()) {
      CFANode node = entry.getKey();
      PolicyAbstractState state = entry.getValue();

      for (Entry<LinearExpression, PolicyTemplateBound> entry2 : state) {
        LinearExpression template = entry2.getKey();
        PolicyTemplateBound bound = entry2.getValue();
        if (node == focusedNode && updated.containsKey(template)) {
          bound = updated.get(template);
        }
        table.put(node, template, bound.edge);
      }
    }
    return table;
  }

  /**
   * Very useful for debugging.
   */
  @SuppressWarnings("unused")
  private void logUnsatCore(List<BooleanFormula> constraints) throws InterruptedException {
    ProverEnvironment env = formulaManagerFactory.newProverEnvironment(true,
        true);
    for (BooleanFormula constraint : constraints) {
      env.push(constraint);
    }
    try {
      if (env.isUnsat()) {
        List<BooleanFormula> l = env.getUnsatCore();
        logger.log(Level.FINE, "# UNSAT core: ", Joiner.on("\n").join(l));
      }
    } catch (SolverException e) {
      logger.logDebugException(e);
    }
  }

  @Override
  public AbstractState join(AbstractState newState, AbstractState prevState)
      throws CPAException, InterruptedException {
    throw new CPAException(
        "Policy abstract domain can be used only with PolicyMergeOperator");
  }
}
