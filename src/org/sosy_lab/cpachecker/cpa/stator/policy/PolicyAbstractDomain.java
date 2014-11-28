package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * Abstract domain for policy iteration.
 */
@Options(prefix="cpa.stator.policy")
public final class PolicyAbstractDomain implements AbstractDomain {

  private final ValueDeterminationFormulaManager vdfmgr;
  private final LogManager logger;
  private final FormulaManagerFactory formulaManagerFactory;
  private final PolicyIterationStatistics statistics;

  //
  public PolicyAbstractDomain(
      Configuration config,
      ValueDeterminationFormulaManager vdfmgr,
      FormulaManagerFactory formulaManagerFactory,
      LogManager logger,
      PolicyIterationStatistics pStatistics
  ) throws InvalidConfigurationException {
    config.inject(this, PolicyAbstractDomain.class);
    this.vdfmgr = vdfmgr;
    this.logger = logger;
    this.formulaManagerFactory = formulaManagerFactory;
    statistics = pStatistics;
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
  public PolicyState join(
      final PolicyState newState,
      final PolicyState prevState,
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
    final Map<LinearExpression, PolicyBound> updated = new HashMap<>();

    Set<Template> allTemplates = Sets.union(
        newState.getTemplates(), prevState.getTemplates()
    );
    Set<LinearExpression> unbounded = new HashSet<>();

    for (Template templateWrapper : allTemplates) {
      LinearExpression template = templateWrapper.linearExpression;
      PolicyBound newValue = newState.getBound(template).orNull();
      PolicyBound oldValue = prevState.getBound(template).orNull();

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

  private PolicyState valueDetermination(
      final PolicyState prevState,
      final PolicyState newState,
      Map<LinearExpression, PolicyBound> updated,
      CFANode node,
      Set<Template> allTemplates,
      PolicyPrecision precision)
      throws CPAException, InterruptedException {

    logger.log(Level.FINE, "# Running value determination");
    Preconditions.checkState(updated.size() > 0,
        "There must exist at least one policy for which the new" +
        "node is strictly larger");

    Map<CFANode, PolicyState> policy = vdfmgr.findRelated(
        newState, toMap(precision), node, updated);
    logger.log(Level.FINE, "# Policy: ", policy);

    List<BooleanFormula> valueDeterminationConstraints =
        vdfmgr.valueDeterminationFormula(policy, node, updated);

    logger.log(Level.FINE, "# Resulting formula: \n", valueDeterminationConstraints, "\n# end");

    PolicyState joinedState = vdfmgr.valueDeterminationMaximization(
          prevState, allTemplates, updated, node, valueDeterminationConstraints);

    Preconditions.checkState(isLessOrEqual(newState, joinedState));
    Preconditions.checkState(isLessOrEqual(prevState, joinedState));
    logger.log(Level.FINE, "# New state after merge: ", joinedState);
    return joinedState;
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
        (PolicyState)newState, (PolicyState)prevState
    );
    boolean ret = (ord == PARTIAL_ORDER.LESS || ord == PARTIAL_ORDER.EQUAL);
    logger.log(Level.FINE, "# Got comparison result = ", ord, ", returning = ", ret);
    return ret;
  }

  PARTIAL_ORDER compare(PolicyState newState,
                        PolicyState prevState) {

    if (newState == prevState) {
      return PARTIAL_ORDER.EQUAL;
    }

    boolean less_or_equal = true;
    boolean greater_or_equal = true;

    for (Entry<LinearExpression, PolicyBound> e : newState) {
      Rational newBound = e.getValue().bound;

      PolicyBound prevValue = prevState.getBound(
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

  private Map<CFANode, PolicyState> toMap(PolicyPrecision p) {
    Map<CFANode, PolicyState> out = new HashMap<>();
    for (AbstractState s : p.getReached()) {
      PolicyState state = (PolicyState) s;
      Preconditions.checkState(!out.containsKey(state.getNode()));
      out.put(state.getNode(), state);
    }
    return out;
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
