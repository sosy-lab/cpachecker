// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class Binder {

  public enum Quantifier {
    FORALL, EXISTS
  }

  private final Type type;
  private final ImmutableSet<String> variables;

  public Binder(Type pType, Set<String> pVariables) {
    type = pType;
    variables = ImmutableSet.copyOf(pVariables);
  }

  public Type getType() {
    return type;
  }

  public Set<String> getVariables() {
    return variables;
  }
}
