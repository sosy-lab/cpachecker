/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.overflow;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Abstract state for tracking overflows.
 */
class OverflowState implements AbstractStateWithAssumptions,
                               Targetable,
                               Graphable {

  private final ImmutableList<? extends AExpression> assumptions;
  private final boolean hasOverflow;
  private static final String PROPERTY_OVERFLOW = "overflow";

  public OverflowState(List<? extends AExpression> pAssumptions, boolean pHasOverflow) {
    assumptions = ImmutableList.copyOf(pAssumptions);
    hasOverflow = pHasOverflow;
  }

  public boolean hasOverflow() {
    return hasOverflow;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    return assumptions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(assumptions, hasOverflow);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    OverflowState that = (OverflowState) pO;
    return hasOverflow == that.hasOverflow && Objects.equals(assumptions, that.assumptions);
  }

  @Override
  public String toString() {
    return "OverflowState{" + ", assumeEdges=" + assumptions + ", hasOverflow=" + hasOverflow + '}';
  }

  @Override
  public boolean isTarget() {
    return hasOverflow;
  }

  @Nonnull
  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    return ImmutableSet.of(
        new Property() {
          @Override
          public String toString() {
            return PROPERTY_OVERFLOW;
          }
        }
    );
  }

  @Override
  public String toDOTLabel() {
    if (hasOverflow) {
      return Joiner.on('\n').join(assumptions);
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
