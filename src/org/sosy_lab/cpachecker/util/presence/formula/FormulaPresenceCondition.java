package org.sosy_lab.cpachecker.util.presence.formula;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.solver.api.BooleanFormula;

public class FormulaPresenceCondition implements PresenceCondition {

  private final BooleanFormula f;

  FormulaPresenceCondition(BooleanFormula pF) {
    f = Preconditions.checkNotNull(pF);
  }

  public BooleanFormula getFormula() {
    return f;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }

    FormulaPresenceCondition that = (FormulaPresenceCondition) pO;

    if (!f.equals(that.f)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return f.hashCode();
  }

  @Override
  public String toString() {
    return f.toString();
  }
}
