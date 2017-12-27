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
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/**
 * Abstract state for tracking overflows.
 */
class OverflowState implements AbstractStateWithAssumptions,
    Graphable,
    AbstractQueryableState {

  private final ImmutableList<? extends AExpression> assumptions;
  private final boolean hasOverflow;
  private static final String PROPERTY_OVERFLOW = "overflow";
  private PathFormula previousPathFormula;
  private PathFormula currentPathFormula;
  private boolean alreadyStrengthened;

  public OverflowState(List<? extends AExpression> pAssumptions, boolean pHasOverflow) {
    this(pAssumptions, pHasOverflow, null);
  }

  public OverflowState(List<? extends AExpression> pAssumptions, boolean pHasOverflow, OverflowState parent) {
    assumptions = ImmutableList.copyOf(pAssumptions);
    hasOverflow = pHasOverflow;
    alreadyStrengthened = false;
    if (parent != null) {
      previousPathFormula = parent.previousPathFormula;
      currentPathFormula = parent.currentPathFormula;
    } else {
      previousPathFormula = null;
      currentPathFormula = null;
    }
  }

  public boolean hasOverflow() {
    return hasOverflow;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    return ImmutableList.of();
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
    return "OverflowState{" + ", assumeEdges=" + getReadableAssumptions() + ", hasOverflow="
        + hasOverflow + '}';
  }

  @Override
  public String toDOTLabel() {
    if (hasOverflow) {
      return Joiner.on('\n').join(getReadableAssumptions());
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  private List<String> getReadableAssumptions() {
    return assumptions.stream().map(x -> x.toASTString()).collect(Collectors.toList());
  }

  @Override
  public String getCPAName() {
    return "OverflowCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(PROPERTY_OVERFLOW)) { return hasOverflow; }
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }

  @Override
  public PathFormula getPreviousPathFormula(PathFormula pPathFormula) {
    // TODO: The following assertions are needed because this is a hack and needs refactoring.
    // For now we need to get the previous path formula somehow,
    // and communicating it via strengthening operators allows to do this
    // locally here where it is needed, separating concerns
    assert alreadyStrengthened
        : "previous path formula is not availabe before the method OverflowState.updatePathFormulas is called"
            + " (preferably through strengthening in the transfer relation)!"
            + " Maybe you are using PredicateCPA before OverflowCPA? (order is important here)";
    assert pPathFormula.getSsa()
        .equals(currentPathFormula.getSsa()) : "supplied path formula does not match!" +
            " Most likely this means strengthen of the PredicateCPA is called before strengthen of the OverflowCPA!";
    return previousPathFormula;
  }

  @Override
  public List<? extends AExpression> getPreconditionAssumptions() {
    return assumptions;
  }

  public void updatePathFormulas(PathFormula newPathFormula) {
    if (!alreadyStrengthened) {
      previousPathFormula = currentPathFormula;
      currentPathFormula = newPathFormula;
      alreadyStrengthened = true;
    }
  }
}
