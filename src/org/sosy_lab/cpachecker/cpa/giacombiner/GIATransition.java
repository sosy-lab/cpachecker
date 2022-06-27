// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giacombiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr;

public class GIATransition {

  private AutomatonBoolExpr trigger;
  private List<AExpression> assumptions;
  private final String scope;

  public GIATransition(
      AutomatonBoolExpr pTrigger,
      List<AExpression> pAssertions, String pScope) {
    trigger = pTrigger;
    assumptions = pAssertions;
    this.scope = pScope;
  }

  public String getScope() {
    return scope;
  }

  public AutomatonBoolExpr getTrigger() {
    return trigger;
  }

  public List<AExpression> getAssumptions() {
    return assumptions;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (! (pO instanceof GIATransition) ){
      return false;
    }
    GIATransition that = (GIATransition) pO;
    return Objects.equals(trigger, that.trigger) && Objects.equals(assumptions,
        that.assumptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trigger, assumptions);
  }
}
