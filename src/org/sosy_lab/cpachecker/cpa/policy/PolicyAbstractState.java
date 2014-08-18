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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import java.util.*;
import java.util.Map.Entry;

/**
 * Abstract state for policy iteration: bounds on each expression (from the template),
 * for the given control node.
 *
 * Logic-less container class.
 */
public class PolicyAbstractState implements AbstractState,
    Iterable<Entry<LinearExpression, PolicyTemplateBound>> {

  // NOTE: It should not be there, we are wasting memory.
  // But hey, can't really find an easier way to do that.
  final CFANode node;

  /**
   * The whole class is a storage for this datastructure +
   * a pointer to node.
   */
  final ImmutableMap<LinearExpression, PolicyTemplateBound> data;

  public ImmutableSet<LinearExpression> getTemplates() {
    return data.keySet();
  }

  private PolicyAbstractState(
          ImmutableMap<LinearExpression, PolicyTemplateBound> data,
          CFANode node) {
    this.data = data;
    this.node = node;
  }

  public static PolicyAbstractState withState(
      ImmutableMap<LinearExpression, PolicyTemplateBound> data,
      CFANode node
  ) {
    return new PolicyAbstractState(data, node);
  }

  public static PolicyAbstractState withEmptyState(CFANode node) {
    return new PolicyAbstractState(
        ImmutableMap.<LinearExpression, PolicyTemplateBound>of(),
        node
    );
  }

  @Override
  public Iterator<Entry<LinearExpression, PolicyTemplateBound>> iterator() {
    return data.entrySet().iterator();
  }
}
