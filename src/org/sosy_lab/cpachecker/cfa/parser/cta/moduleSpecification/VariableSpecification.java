// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import java.math.BigDecimal;

public class VariableSpecification {
  public static enum VariableType {
    CONST,
    CLOCK,
    SYNC
  }

  public static enum VariableVisibility {
    LOCAL,
    INPUT
  }

  public String name;
  public VariableType type;
  public VariableVisibility visibility;
  public Optional<Number> initialization;

  private VariableSpecification(
      String pName,
      VariableType pType,
      VariableVisibility pVisibility,
      Optional<Number> pInitialization) {
    name = pName;
    type = pType;
    visibility = pVisibility;
    initialization = pInitialization;
  }

  public static class Builder {
    private String name;
    private VariableType type;
    private VariableVisibility visibility;
    private Optional<Number> initialization = Optional.absent();

    public Builder name(String pName) {
      name = checkNotNull(pName);
      checkArgument(!name.isEmpty(), "Empty variable names are not allowed");
      return this;
    }

    public Builder type(VariableType pType) {
      type = checkNotNull(pType);
      return this;
    }

    public Builder visibility(VariableVisibility pVisibility) {
      visibility = checkNotNull(pVisibility);
      return this;
    }

    public Builder initialization(Number pInitialization) {
      initialization = Optional.fromNullable(pInitialization);
      return this;
    }

    public VariableSpecification build() {
      checkNotNull(name);
      checkNotNull(type);
      checkNotNull(visibility);
      checkNotNull(initialization);
      checkState(
          !initialization.isPresent() || type.equals(VariableType.CONST),
          "Initalizations are only allowed for constant variables.");
      checkState(
          !initialization.isPresent()
              || (new BigDecimal(initialization.get().toString()).signum() >= 0),
          "Negative values are not allowed for constants.");

      return new VariableSpecification(name, type, visibility, initialization);
    }
  }
}
