// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import java.io.Serial;
import java.util.Optional;

/**
 * Stores an enum constant that can be tracked by the ValueAnalysisCPA for analyses of the Java
 * programming language.
 */
public record JEnumConstantValue(String fullyQualifiedName) implements Value {

  @Serial private static final long serialVersionUID = 2745087444102463717L;

  /**
   * Returns the fully qualified name of the stored enum constant.
   *
   * @return the fully qualified name of this value
   */
  public String getName() {
    return fullyQualifiedName;
  }

  /**
   * Always returns <code>true</code> since every <code>JEnumConstantValue</code> represents one
   * specific value.
   *
   * @return always returns <code>true</code>
   */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
}
