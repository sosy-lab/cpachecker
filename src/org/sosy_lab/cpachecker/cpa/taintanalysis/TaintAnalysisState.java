// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TaintAnalysisState
    implements LatticeAbstractState<TaintAnalysisState>, Targetable, Serializable, Graphable {

  private static final long serialVersionUID = -7715698130795640052L;

  private boolean violatesProperty = false;
  private Set<CIdExpression> taintedVariables;

  public TaintAnalysisState(Set<CIdExpression> pElements) {
    this.taintedVariables = pElements;
  }

  public Set<CIdExpression> getTaintedVariables() {
    return taintedVariables;
  }

  @Override
  public boolean isLessOrEqual(TaintAnalysisState other) {
    return other.getTaintedVariables().containsAll(this.taintedVariables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taintedVariables);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof TaintAnalysisState other)) {
      return false;
    }
    return Objects.equals(taintedVariables, other.taintedVariables);
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  @Override
  public String toDOTLabel() {
    String sb = "{"
        + this.taintedVariables.toString()
        + "}";

    return sb;
  }



  public static AbstractState getInitial(CFANode pNode) {
    return new TaintAnalysisState( new HashSet<>());
  }


  @Override
  public TaintAnalysisState join(TaintAnalysisState pOther) throws CPAException, InterruptedException {
    if (this.isLessOrEqual(pOther)) {
      return pOther;
    } else if (pOther.isLessOrEqual(this)) {
      return this;
    }
    HashSet<CIdExpression> resSet = new HashSet<>();
    resSet.addAll(this.taintedVariables);
    resSet.addAll(pOther.getTaintedVariables());
    return new TaintAnalysisState(resSet);
  }

  @Override
  public boolean shouldBeHighlighted() {
    return this.isTarget();
  }

  @Override
  public boolean isTarget() {
    return this.violatesProperty;
  }

  @Override
  public  Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    if (isTarget()){
      HashSet<TargetInformation> resSet = new HashSet<>();
      resSet.add(SimpleTargetInformation.create("Leaking inforation"));
      return resSet;
    }
    else return new HashSet<>();
  }

@SuppressWarnings("unused")
  public void setViolatesProperty() {
    violatesProperty = true;
  }


}
