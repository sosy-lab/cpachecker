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
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class PowersetAutomatonState implements AbstractWrapperState,
    Targetable, Serializable, Graphable, Iterable<AutomatonState> {

  private static class TopPowersetAutomatonState extends PowersetAutomatonState {

    private static final long serialVersionUID = -3468579071210217351L;

    public TopPowersetAutomatonState() {
      super(ImmutableSet.<AutomatonState>of());
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {
      return (pObj instanceof TopPowersetAutomatonState);
    }
  }

  final static TopPowersetAutomatonState TOP = new TopPowersetAutomatonState();

  private static final long serialVersionUID = -8033111447137153782L;
  private final Set<AutomatonState> states;

  public PowersetAutomatonState(Iterable<AutomatonState> elements) {
    this.states = ImmutableSet.copyOf(elements);
  }

  public int getNumberOfStates() {
    return states.size();
  }

  @Override
  public boolean isTarget() {
    for (AutomatonState e : states) {
      if (e.isTarget()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    Set<Property> properties = Sets.newHashSetWithExpectedSize(states.size());
    for (AutomatonState e : states) {
      if (e.isTarget()) {
        properties.addAll(e.getViolatedProperties());
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

  @Override
  public List<AbstractState> getWrappedStates() {
    return ImmutableList.<AbstractState>copyOf(states);
  }

  public Set<AutomatonState> getAutomataStates() {
    return states;
  }

  @Override
  public Iterator<AutomatonState> iterator() {
    return states.iterator();
  }

}
