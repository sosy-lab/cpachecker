package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;

/**
 * Abstract domain for policy iteration.
 */
public class PolicyAbstractDomain implements AbstractDomain {

  private final ValueDeterminationFormulaManager vdfmgr;
  private final LogManager logger;
  private final FormulaManagerFactory formulaManagerFactory;
  private final LinearConstraintManager lcmgr;
  private final NumeralFormulaManagerView<NumeralFormula, NumeralFormula.RationalFormula>
      rfmgr;

  /**
   * Scary-hairy global containing all the global data: map
   * from the node and the linear expression to the selected incoming edge.
   */
  private final Table<CFANode, LinearExpression, CFAEdge> policy;
//
  public PolicyAbstractDomain(
     ValueDeterminationFormulaManager vdfmgr,
     FormulaManagerView fmgr,
     FormulaManagerFactory formulaManagerFactory,
     LogManager logger,
     LinearConstraintManager lcmgr
  ) {
    policy = HashBasedTable.create();
    this.vdfmgr = vdfmgr;
    this.logger = logger;
    this.formulaManagerFactory = formulaManagerFactory;
    this.lcmgr = lcmgr;
    rfmgr = fmgr.getRationalFormulaManager();
  }

  void setPolicyForTemplate(CFANode node, LinearExpression template, CFAEdge edge) {
    policy.put(node, template, edge);
  }

  @Override
  public AbstractState join(AbstractState newState, AbstractState prevState)
      throws CPAException {
    return join((PolicyAbstractState) newState, (PolicyAbstractState) prevState);
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
      final PolicyAbstractState prevState) throws CPAException {

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
      PolicyTemplateBound newValue = newState.getBound(template).orNull();
      PolicyTemplateBound oldValue = prevState.getBound(template).orNull();

      // Can't do better than unbounded.
      if (oldValue == null) continue;

      // We are unbounded already, no point in doing value determination for
      // this template.
      if (newValue == null) {
        unbounded.add(template);
        continue;
      }

      if (newValue.bound.compareTo(oldValue.bound) > 0) {
        setPolicyForTemplate(node, template, newValue.edge);
        updated.put(template, newValue);
      } else {
        setPolicyForTemplate(node, template, oldValue.edge);
      }
    }

    if (!node.isLoopStart()) {
      logger.log(Level.FINE, "# Merge not on the loop head, returning "
          + "the updated state");
      return prevState.withUpdates(updated, unbounded, allTemplates);
    }

    // Running value determination only on loop heads.
    return valueDetermination(prevState, newState, updated, node, allTemplates);
  }

  private PolicyAbstractState valueDetermination(
      final PolicyAbstractState prevState,
      final PolicyAbstractState newState,
      Map<LinearExpression, PolicyTemplateBound> updated,
      CFANode node,
      PolicyAbstractState.Templates allTemplates) throws CPAException {

    logger.log(Level.FINE, "# Running value determination");

    Preconditions.checkState(updated.size() > 0,
        "There must exist at least one policy for which the new" +
        "node is strictly larger");

    List<BooleanFormula> valueDeterminationConstraints;
    try {
      valueDeterminationConstraints = vdfmgr.valueDeterminationFormula(
          policy.rowMap()
      );
    } catch (InterruptedException e) {
      throw new CPAException("Exception while computing the formula", e);
    }

    ImmutableMap.Builder<LinearExpression, PolicyTemplateBound> builder;
    builder = ImmutableMap.builder();
    Set<LinearExpression> unbounded = new HashSet<>();

    for (Entry<LinearExpression, PolicyTemplateBound> policyValue : updated.entrySet()) {
      LinearExpression template = policyValue.getKey();
      CFAEdge policyEdge = policyValue.getValue().edge;

      // Maximize for each template subject to the overall constraints.
      try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {
        for (BooleanFormula constraint : valueDeterminationConstraints) {
          solver.addConstraint(constraint);
        }

        logger.log(Level.FINE,
           "# Value determination: optimizing for template" , template);

        NumeralFormula objective = rfmgr.makeVariable(
            vdfmgr.absDomainVarName(node, template));
        ExtendedRational newValue = lcmgr.maximize(solver, objective);

        Preconditions.checkState(newValue != ExtendedRational.NEG_INFTY,
            "Value determination should not be unsatisfiable");
        if (newValue != ExtendedRational.INFTY) {
          builder.put(template, PolicyTemplateBound.of(policyEdge, newValue));
        } else {
          unbounded.add(template);
        }
      } catch (Exception e) {
        throw new CPATransferException("Failed solving", e);
      }
    }

    ImmutableMap<LinearExpression, PolicyTemplateBound> outData = builder.build();
    PolicyAbstractState joinedState = prevState.withUpdates(outData, unbounded, allTemplates);

    // Note: returning an exactly same state is important, due to the issues
    // with .equals() handling.
    if (joinedState.equals(prevState)) return prevState;
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
        (PolicyAbstractState)newState, (PolicyAbstractState)prevState
    );
    boolean ret = (ord == PARTIAL_ORDER.LESS || ord == PARTIAL_ORDER.EQUAL);
    logger.log(Level.FINE, "# Got comparison result = ", ord, ", returning = ", ret);
    return ret;
  }

  PARTIAL_ORDER compare(PolicyAbstractState newState,
                        PolicyAbstractState prevState) {

    if (newState == prevState) return PARTIAL_ORDER.EQUAL;

    boolean less_or_equal = true;
    boolean greater_or_equal = true;

    for (Entry<LinearExpression, PolicyTemplateBound> e : newState) {
      ExtendedRational prevBound, newBound;
      newBound = e.getValue().bound;
      PolicyTemplateBound prevValue = prevState.getBound(e.getKey()).orNull();
      if (prevValue == null) {
        prevBound = ExtendedRational.INFTY;
      } else {
        prevBound = prevValue.bound;
      }

      int cmp = newBound.compareTo(prevBound);

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
}
