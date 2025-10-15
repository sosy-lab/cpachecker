// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.arg.Splitable;

public class CompositeState
    implements AbstractWrapperState,
        Targetable,
        Partitionable,
        PseudoPartitionable,
        Serializable,
        Graphable,
        Splitable {
  @Serial private static final long serialVersionUID = -5143296331663510680L;
  private final ImmutableList<AbstractState> states;
  private transient @Nullable Object partitionKey; // lazily initialized
  private transient @Nullable Comparable<?> pseudoPartitionKey; // lazily initialized
  private transient @Nullable Object pseudoHashCode; // lazily initialized

  public CompositeState(List<AbstractState> elements) {
    states = ImmutableList.copyOf(elements);
  }

  int getNumberOfStates() {
    return states.size();
  }

  @Override
  public boolean isTarget() {
    for (AbstractState element : states) {
      if ((element instanceof Targetable targetable) && targetable.isTarget()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    checkState(isTarget());
    ImmutableSet.Builder<TargetInformation> properties = ImmutableSet.builder();
    for (AbstractState element : states) {
      if ((element instanceof Targetable targetable) && targetable.isTarget()) {
        properties.addAll(targetable.getTargetInformation());
      }
    }
    return properties.build();
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
      if (element instanceof Graphable graphable) {
        String label = graphable.toDOTLabel();
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
      if (element instanceof Graphable graphable) {
        if (graphable.shouldBeHighlighted()) {
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
  public ImmutableList<AbstractState> getWrappedStates() {
    return states;
  }

  @Override
  public Object getPartitionKey() {
    if (partitionKey == null) {
      Object[] keys = new Object[states.size()];

      int i = 0;
      for (AbstractState element : states) {
        if (element instanceof Partitionable partitionable) {
          keys[i] = partitionable.getPartitionKey();
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
        if (element instanceof PseudoPartitionable pseudoPartitionable) {
          keys[i] = pseudoPartitionable.getPseudoPartitionKey();
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
        if (element instanceof PseudoPartitionable pseudoPartitionable) {
          keys[i] = pseudoPartitionable.getPseudoHashCode();
        }
        i++;
      }

      // wrap array of keys in object to enable overriding of equals and hashCode
      pseudoHashCode = new CompositePartitionKey(keys);
    }

    return pseudoHashCode;
  }

  private static final class CompositePartitionKey {

    private final @Nullable Object[] keys;

    private CompositePartitionKey(Object[] pElements) {
      keys = pElements;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }

      return pObj instanceof CompositePartitionKey compositePartitionKey
          && Arrays.equals(keys, compositePartitionKey.keys);
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

    private final @Nullable Comparable<?>[] keys;

    private CompositePseudoPartitionKey(Comparable<?>[] pElements) {
      keys = pElements;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }

      return pObj instanceof CompositePseudoPartitionKey compositePseudoPartitionKey
          && Arrays.equals(keys, compositePseudoPartitionKey.keys);
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
      return Arrays.compare(keys, other.keys, Ordering.natural().nullsFirst());
    }
  }

  @Override
  public AbstractState forkWithReplacements(Collection<AbstractState> pReplacementStates) {
    List<AbstractState> wrappedStates = getWrappedStates();
    List<AbstractState> newWrappedStates = new ArrayList<>(wrappedStates.size());

    for (AbstractState state : wrappedStates) {
      int targetSize = newWrappedStates.size() + 1;
      // if state was not replaced, add it:
      if (targetSize > newWrappedStates.size()) {
        // recursion might end here if state is not splitable:
        if (state instanceof Splitable splitable) {
          newWrappedStates.add(splitable.forkWithReplacements(pReplacementStates));
        } else {
          newWrappedStates.add(state);
        }
      }
    }
    CompositeState newState = new CompositeState(newWrappedStates);
    return newState;
  }
}
