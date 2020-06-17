// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import com.google.common.base.Optional;

public class StateSpecification {
  public String name;
  public Optional<BooleanCondition> invariant;
  public boolean isInitialState;

  private StateSpecification(
      String pName, Optional<BooleanCondition> pInvariant, boolean pIsInitialState) {
    name = pName;
    invariant = pInvariant;
    isInitialState = pIsInitialState;
  }

  public static class Builder {
    private String name;
    private Optional<BooleanCondition> invariant;
    public boolean isInitialState;

    public Builder name(String pName) {
      name = pName;
      return this;
    }

    public Builder invariant(Optional<BooleanCondition> pInvariant) {
      invariant = pInvariant;
      return this;
    }

    public Builder isInitialState(boolean pIsInitialState) {
      isInitialState = pIsInitialState;
      return this;
    }

    public StateSpecification build() {
      return new StateSpecification(name, invariant, isInitialState);
    }
  }
}
