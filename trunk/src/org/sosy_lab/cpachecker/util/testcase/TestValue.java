// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.testcase;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;

public abstract class TestValue {

  /**
   * Statements that must be added to the code before the test value is used, so that the test value
   * is well-defined.
   *
   * @param code required code. Must be compilable (e.g., statements must end with a ';' when
   *     required).
   */
  public record AuxiliaryCode(String code) {
    public AuxiliaryCode {
      if (code == null || code.isBlank()) {
        throw new IllegalArgumentException("No valid code given: \"" + code + "\"");
      }
    }
  }

  private final ImmutableList<AuxiliaryCode> auxiliaryCodes;

  private final AAstNode value;

  protected TestValue(ImmutableList<AuxiliaryCode> pAuxiliaryCodes, AAstNode pValue) {
    auxiliaryCodes = pAuxiliaryCodes;
    value = pValue;
  }

  public List<AuxiliaryCode> getAuxiliaryCode() {
    return auxiliaryCodes;
  }

  public AAstNode getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "{ RESULT = " + value.toASTString() + "; }";
  }

  @Override
  public int hashCode() {
    return Objects.hash(auxiliaryCodes, value);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    return pObj instanceof TestValue other
        && value.equals(other.value)
        && auxiliaryCodes.equals(other.auxiliaryCodes);
  }
}
