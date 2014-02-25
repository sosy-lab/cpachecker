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
package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.TargetableWithPredicatedAnalysis;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class CompositeState implements AbstractWrapperState, TargetableWithPredicatedAnalysis, Partitionable, Serializable {
  private static final long serialVersionUID = -5143296331663510680L;
  private final ImmutableList<AbstractState> states;
  private transient Object partitionKey; // lazily initialized

  public CompositeState(List<AbstractState> elements) {
    this.states = ImmutableList.copyOf(elements);
  }

  public int getNumberOfStates() {
    return states.size();
  }

  @Override
  public boolean isTarget() {
    for (AbstractState element : states) {
      if ((element instanceof Targetable) && ((Targetable)element).isTarget()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public ViolatedProperty getViolatedProperty() throws IllegalStateException {
    checkState(isTarget());
    // prefer a specific property over the default OTHER property
    for (AbstractState element : states) {
      if ((element instanceof Targetable) && ((Targetable)element).isTarget()) {
        ViolatedProperty property = ((Targetable)element).getViolatedProperty();
        if (property != ViolatedProperty.OTHER) {
          return property;
        }
      }
    }
    return ViolatedProperty.OTHER;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('(');
    for (AbstractState element : states) {
      builder.append(element.getClass().getSimpleName());
      builder.append(": ");
      builder.append(element.toString());
      builder.append("\n ");
    }
    builder.replace(builder.length() - 1, builder.length(), ")");

    return builder.toString();
  }

  public AbstractState get(int idx) {
    return states.get(idx);
  }

  @Override
  public List<AbstractState> getWrappedStates() {
    return states;
  }


  @Override
  public Object getPartitionKey() {
    if (partitionKey == null) {
      Object[] keys = new Object[states.size()];

      int i = 0;
      for (AbstractState element : states) {
        if (element instanceof Partitionable) {
          keys[i] = ((Partitionable)element).getPartitionKey();
        }
        i++;
      }

      // wrap array of keys in object to enable overriding of equals and hashCode
      partitionKey = new CompositePartitionKey(keys);
    }

    return partitionKey;
  }

  private static final class CompositePartitionKey {

    private final Object[] keys;

    private CompositePartitionKey(Object[] pElements) {
      keys = pElements;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }

      if (!(pObj instanceof CompositePartitionKey)) {
        return false;
      }

      return Arrays.equals(this.keys, ((CompositePartitionKey)pObj).keys);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(keys);
    }

    @Override
    public String toString() {
      return "[" + Joiner.on(", ").skipNulls().join(keys) + "]";
    }
  }

  @Override
  public BooleanFormula getErrorCondition(FormulaManagerView fmgr) {
    BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
    if (isTarget()) {
      BooleanFormula f = bfmgr.makeBoolean(false);
      for (AbstractState state : states) {
        if (state instanceof TargetableWithPredicatedAnalysis) {
          f = fmgr.makeOr(f, ((TargetableWithPredicatedAnalysis) state).getErrorCondition(fmgr));
        }
      }
      return f;
    }
    return bfmgr.makeBoolean(false);
  }
}
