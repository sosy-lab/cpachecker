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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CompositeState
    implements AbstractWrapperState, Targetable, Partitionable, PseudoPartitionable, Serializable,
        Graphable {
  private static final long serialVersionUID = -5143296331663510680L;
  private final ImmutableList<AbstractState> states;
  private transient Object partitionKey; // lazily initialized
  private transient Comparable<?> pseudoPartitionKey; // lazily initialized
  private transient Object pseudoHashCode; // lazily initialized

  public CompositeState(List<AbstractState> elements) {
    this.states = ImmutableList.copyOf(elements);
  }

  int getNumberOfStates() {
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
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    Set<Property> properties = Sets.newHashSetWithExpectedSize(states.size());
    for (AbstractState element : states) {
      if ((element instanceof Targetable) && ((Targetable)element).isTarget()) {
        properties.addAll(((Targetable)element).getViolatedProperties());
      }
    }
    return properties;
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

  @Override
  public String toDOTLabel() {
    StringBuilder builder = new StringBuilder();
    for (AbstractState element : states) {
      if (element instanceof Graphable) {
        String label = ((Graphable)element).toDOTLabel();
        if (!label.isEmpty()) {
          builder.append(element.getClass().getSimpleName());
          builder.append(": ");
          builder.append(label);
          builder.append("\\n ");
        }
      }
    }

    return builder.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    for (AbstractState element : states) {
      if (element instanceof Graphable) {
        if (((Graphable)element).shouldBeHighlighted()) {
          return true;
        }
      }
    }
    return false;
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

  @Override
  public Comparable<?> getPseudoPartitionKey() {
    if (pseudoPartitionKey == null) {
      Comparable<?>[] keys = new Comparable<?>[states.size()];

      int i = 0;
      for (AbstractState element : states) {
        if (element instanceof PseudoPartitionable) {
          keys[i] = ((PseudoPartitionable) element).getPseudoPartitionKey();
        }
        i++;
      }

      // wrap array of keys in object to enable overriding of equals and hashCode
      pseudoPartitionKey = new CompositePseudoPartitionKey(keys);
    }

    return pseudoPartitionKey;
  }

  @Override
  public Object getPseudoHashCode() {
    if (pseudoHashCode == null) {
      Object[] keys = new Object[states.size()];

      int i = 0;
      for (AbstractState element : states) {
        if (element instanceof PseudoPartitionable) {
          keys[i] = ((PseudoPartitionable)element).getPseudoHashCode();
        }
        i++;
      }

      // wrap array of keys in object to enable overriding of equals and hashCode
      pseudoHashCode = new CompositePartitionKey(keys);
    }

    return pseudoHashCode;
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

  @SuppressWarnings("rawtypes")
  private static final class CompositePseudoPartitionKey
      implements Comparable<CompositePseudoPartitionKey> {

    private final Comparable<?>[] keys;

    private CompositePseudoPartitionKey(Comparable<?>[] pElements) {
      keys = pElements;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }

      if (!(pObj instanceof CompositePseudoPartitionKey)) {
        return false;
      }

      return Arrays.equals(this.keys, ((CompositePseudoPartitionKey) pObj).keys);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(keys);
    }

    @Override
    public String toString() {
      return "[" + Joiner.on(", ").skipNulls().join(keys) + "]";
    }

    @Override
    public int compareTo(CompositePseudoPartitionKey other) {
      Preconditions.checkArgument(keys.length == other.keys.length);
      ComparisonChain c = ComparisonChain.start();
      for (int i = 0; i < keys.length; i++) {
        c = c.compare(keys[i], other.keys[i], Ordering.natural().nullsFirst());
      }
      return c.result();
    }
  }
}
