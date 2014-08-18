/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.policy;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.ImmutableMapMerger;
import org.sosy_lab.cpachecker.cpa.policy.ValueDeterminationFormulaManager.ValueDeterminationConstraint;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 * Abstract domain for policy iteration.
 */
public class PolicyAbstractDomain implements AbstractDomain {

  private final ValueDeterminationFormulaManager vdfmgr;
  private final LogManager logger;
  private final FormulaManagerFactory formulaManagerFactory;
  private final LinearConstraintManager lcmgr;

  /**
   * Scary-hairy global containing all the global data.
   * Internal representation:
   *   {@code Map<CFANode, Map<LinearExpression, CFAEdge>>}
   */
  private final Table<CFANode, LinearExpression, CFAEdge> policy;

  public PolicyAbstractDomain(
     ValueDeterminationFormulaManager vdfmgr,
     FormulaManagerFactory formulaManagerFactory,
     LogManager logger,
     LinearConstraintManager lcmgr
  ) {
    policy = HashBasedTable.create();
    this.vdfmgr = vdfmgr;
    this.logger = logger;
    this.formulaManagerFactory = formulaManagerFactory;
    this.lcmgr = lcmgr;
  }

  void setPolicyForTemplate(CFANode node, LinearExpression template, CFAEdge edge) {
    policy.put(node, template, edge);
  }

  @Override
  public AbstractState join(AbstractState newState, AbstractState prevState)
       throws CPAException {

    // Perform value determination during the join stage?
    return join((PolicyAbstractState) newState, (PolicyAbstractState) prevState);
  }

  /**
   * Each iteration produces only one step, so after each run of
   * {@link PolicyTransferRelation#getAbstractSuccessors} we merge at most
   * two states (a new one, and potentially an alrady existing one for
   * this node).
   *
   * @param newState Newly obtained abstract state.
   * @param prevState A previous abstract state for this node (if such exists)
   * @return New abstract state.
   */
  public PolicyAbstractState join(
      final PolicyAbstractState newState,
      PolicyAbstractState prevState) throws CPAException {

    // NOTE: check. But I think it must be actually the same node.
    Preconditions.checkState(newState.node == prevState.node);

    // OK so what do we have.
    // if we are performing the merge step there must exist at least one
    // policy for which the new node is strictly larger.

    final CFANode node = newState.node;

    /** Find the templates which were updated */
    final Map<LinearExpression, CFAEdge> updated = new HashMap<>();

    for (Entry<LinearExpression, PolicyTemplateBound> entry : prevState) {
      LinearExpression template = entry.getKey();
      PolicyTemplateBound policyValue = entry.getValue();
      PolicyTemplateBound oldValue = prevState.data.get(template);

      if (oldValue == null ||
            oldValue.bound.compareTo(policyValue.bound) < 0) {
        updated.put(template, policyValue.edge);
      }
    }

    ValueDeterminationConstraint valueDeterminationConstraints;
    try {
      valueDeterminationConstraints = vdfmgr.valueDeterminationFormula(
          policy.rowMap()
      );
    } catch (InterruptedException e) {
      throw new CPAException("Exception while computing the formula", e);
    }

    ImmutableMap.Builder<LinearExpression, PolicyTemplateBound> builder;
    builder = ImmutableMap.builder();

    for (Entry<LinearExpression, CFAEdge> policyValue : updated.entrySet()) {
      LinearExpression template = policyValue.getKey();
      CFAEdge policyEdge = policyValue.getValue();

      // Maximize for each template subject to the overall constraints.
      int ssaIdx = valueDeterminationConstraints.ssaTemplateMap.get(node, template);

      try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {
        solver.addConstraint(valueDeterminationConstraints.constraints);

        SSAMap ssaMap = SSAMap.emptySSAMap().withDefault(ssaIdx);

        ExtendedRational newValue = lcmgr.maximize(solver, template, ssaMap);

        builder.put(template, PolicyTemplateBound.of(policyEdge, newValue));
      } catch (Exception e) {
        throw new CPATransferException("Failed solving", e);
      }
    }

    return prevState.withUpdates(builder.build());
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

    PARTIAL_ORDER ord = compare(
        (PolicyAbstractState)newState,
        (PolicyAbstractState)prevState
    );
    return (ord == PARTIAL_ORDER.LESS || ord == PARTIAL_ORDER.EQUAL);
  }

  PARTIAL_ORDER compare(PolicyAbstractState newState,
                        PolicyAbstractState prevState) {
    boolean less_or_equal = true;
    boolean greater_or_equal = true;

    for (Entry<LinearExpression, PolicyTemplateBound> e : newState.data.entrySet()) {
      PolicyTemplateBound newValue = e.getValue();

      PolicyTemplateBound prevValue = prevState.data.get(e.getKey());

      int cmp;
      if (prevValue == null) {
        cmp = 1;
      } else {
        cmp = newValue.bound.compareTo(prevValue.bound);
      }

      if (cmp > 0) {
        less_or_equal = false;
      } else if (cmp < 0) {
        greater_or_equal = false;
      }
    }

    if (less_or_equal && greater_or_equal) {
      return PARTIAL_ORDER.EQUAL;
    } else if (less_or_equal) {
      return PARTIAL_ORDER.LESS;
    } else if (greater_or_equal) {
      return PARTIAL_ORDER.GREATER;
    } else {
      return PARTIAL_ORDER.UNCOMPARABLE;
    }
  }
}
