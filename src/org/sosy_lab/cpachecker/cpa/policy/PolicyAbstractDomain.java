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
import com.google.common.collect.Table;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.ImmutableMapMerger;
import org.sosy_lab.cpachecker.cpa.policy.ValueDeterminationFormulaManager.ValueDeterminationConstraint;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import java.util.*;
import java.util.Map.Entry;

/**
 * Abstract domain for policy iteration.
 */
public class PolicyAbstractDomain implements AbstractDomain {

  private final ValueDeterminationFormulaManager cpfmgr;

  /**
   * Scary-hairy global containing all the global data.
   */
  private final Table<CFANode, LinearExpression, CFAEdge> policy;


  public PolicyAbstractDomain(
     ValueDeterminationFormulaManager cpfmgr
  ) {
    policy = HashBasedTable.create();
    this.cpfmgr = cpfmgr;
  }

  @Override
  public AbstractState join(AbstractState current, AbstractState reached)
       throws CPAException {

    // Perform value determination during the join stage?
    return join((PolicyAbstractState) current, (PolicyAbstractState) reached);
  }

  /**
   * NOTE1: At each iteration we obtain only one new state.
   * Thus we merge only two things: a previous state, and the old one at that node.
   *
   * NOTE2: Cycles can appear only at this step (no cycle can appear if we are updating the invariant
   * for the previously unreached state).
   *
   * At this point our options are:
   * -> It's always easiest to just start out with something extremely naive which works and update
   * it later on.
   *
   * Hence we don't even have to detect cycles at all. Just the global policy in some datastructure,
   * synchronize that manually, and run the value determination (in the old, extremely stupid, way)
   * after every single step.
   * Yay! Decision right there.
   *
   * @param current Newly obtained abstract state.
   * @param reached A previous abstract state for this node (if such exists)
   * @return New abstract state.
   */
  public PolicyAbstractState join(
      final PolicyAbstractState current,
      PolicyAbstractState reached) throws CPAException {

    // NOTE: check. But I think it must be actually the same node.
    Preconditions.checkState(current.node == reached.node);

    // OK so what do we have.
    // if we are performing the merge step there must exist at least one
    // policy for which the new node is strictly larger.

    final CFANode node = current.node;

    // Templates which were updaed.
    final List<LinearExpression> updated = new LinkedList<>();

    // Note: policy is updated inside the merging step.
    PolicyAbstractState out = PolicyAbstractState.withState(
        ImmutableMapMerger.merge(
            current.data,
            reached.data,
            new ImmutableMapMerger.MergeFuncWithKey<LinearExpression,
                PolicyTemplateBound>() {
              @Override
              public PolicyTemplateBound apply(
                  LinearExpression template,
                  PolicyTemplateBound newPolicy,
                  PolicyTemplateBound oldPolicy) {

                // Bound grows, so we are happy when the difference is bigger than zero.
                if (newPolicy.bound.compareTo(oldPolicy.bound) > 0) {

                  // Let's keep a track of templates we had to update.
                  updated.add(template);

                  // Update the policy.
                  policy.put(node, template, newPolicy.edge);

                  return newPolicy;
                } else {
                  return oldPolicy;
                }
              }
            }
        ),
        node
    );

    try {
      ValueDeterminationConstraint constr = cpfmgr.valueDeterminationFormula(
          policy.rowMap()
      );
    } catch (InterruptedException e) {
      throw new CPAException("Exception while computing the formula", e);
    }

    for (LinearExpression template : updated) {
      PolicyTemplateBound currentConstraint = out.data.get(template);

      // TODO: OK at this point we need the bloody interface for the optimization
      // technique.



      // TODO: use the value determination formula here to obtain the new state.

      // TODO: solve the resultant LP to update [out].
    }

    return out;

  }

  enum PARTIAL_ORDER {
    LESS,
    EQUAL,
    UNCOMPARABLE,
    GREATER
  }

  @Override
  public boolean isLessOrEqual(AbstractState current, AbstractState reached)
      throws CPAException {

    PARTIAL_ORDER ord = compare((PolicyAbstractState)current, (PolicyAbstractState)reached);
    return (ord == PARTIAL_ORDER.LESS || ord == PARTIAL_ORDER.EQUAL);
  }

  PARTIAL_ORDER compare(PolicyAbstractState newState, PolicyAbstractState otherState) {
    boolean less_or_equal = true;
    boolean greater_or_equal = true;

    for (Entry<LinearExpression, PolicyTemplateBound> e : newState.data.entrySet()) {
      PolicyTemplateBound value = e.getValue();
      PolicyTemplateBound otherValue = otherState.data.get(e.getKey());

      int cmp = value.bound.compareTo(otherValue.bound);

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
