// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

/**
 * Abstract state for policy iteration: bounds on each expression (from the template), for the given
 * control node.
 *
 * <p>Logic-less container class.
 */
public abstract class PolicyState implements AbstractState, Graphable {

  private final CFANode node;

  protected PolicyState(CFANode pNode) {
    node = pNode;
  }

  /** Cast to subclass. Syntax sugar to avoid ugliness. */
  public PolicyIntermediateState asIntermediate() {
    return (PolicyIntermediateState) this;
  }

  public PolicyAbstractedState asAbstracted() {
    return (PolicyAbstractedState) this;
  }

  public CFANode getNode() {
    return node;
  }

  public abstract boolean isAbstract();

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
