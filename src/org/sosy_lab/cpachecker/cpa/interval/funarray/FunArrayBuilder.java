// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval.funarray;

import static org.sosy_lab.cpachecker.cfa.ast.FileLocation.DUMMY;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cpa.interval.Interval;

public class FunArrayBuilder {
  private List<Bound> bounds;
  private List<Interval> values;
  private List<Boolean> emptiness;

  private FunArrayBuilder(Bound firstBound) {
    this.bounds = new ArrayList<>();
    this.values = new ArrayList<>();
    this.emptiness = new ArrayList<>();
    this.bounds.add(firstBound);
  }

  public FunArrayBuilder bound(NormalFormExpression... expressions)
      throws FunArrayBuilderException {
    Bound bound = new Bound(Set.of(expressions));
    if (bounds.size() != values.size()) {
      throw new FunArrayBuilderException(
          "Cannot append another bound %s. A value is needed first.".formatted(bound));
    }
    this.bounds.add(bound);
    return this;
  }

  private void ensureEmptiness() {
    while (this.emptiness.size() < this.values.size()) {
      this.emptiness.add(false);
    }
  }

  public FunArrayBuilder value(Interval value) {
    ensureEmptiness();
    this.values.add(value);
    return this;
  }

  public FunArrayBuilder value(Long low, Long high) {
    return value(new Interval(low, high));
  }

  public FunArrayBuilder value(int low, int high) {
    return value(Long.valueOf(low), Long.valueOf(high));
  }

  public FunArrayBuilder value(Long value) {
    return value(value, value);
  }

  public FunArrayBuilder value(int value) {
    return value(Long.valueOf(value));
  }

  public FunArrayBuilder value() {
    return value(null, null);
  }

  public FunArrayBuilder mayBeEmpty() throws FunArrayBuilderException {
    if (this.emptiness.size() != this.values.size() - 1) {
      throw new FunArrayBuilderException("Emptiness value has already been set.");
    }
    this.emptiness.add(true);
    return this;
  }

  public static FunArrayBuilder firstBound(NormalFormExpression... expressions) {
    Bound bound = new Bound(Set.of(expressions));
    return new FunArrayBuilder(bound);
  }

  public FunArray build() {
    ensureEmptiness();
    return new FunArray(this.bounds, this.values, this.emptiness);
  }

  public static NormalFormExpression exp(int constant) {
    return new NormalFormExpression(constant);
  }

  public static NormalFormExpression exp(CIdExpression variable) {
    return new NormalFormExpression(variable);
  }

  public static NormalFormExpression exp(CIdExpression variable, int constant) {
    return new NormalFormExpression(variable, constant);
  }

  public static NormalFormExpression exp(String variableName) {
    return exp(variable(variableName));
  }

  public static NormalFormExpression exp(String variableName, int constant) {
    return exp(variable(variableName), constant);
  }

  public static CIdExpression variable(String name) {
    return new CIdExpression(
        DUMMY,
        null,
        name,
        new CVariableDeclaration(
            DUMMY,
            false,
            CStorageClass.AUTO,
            new CSimpleType(
                CTypeQualifiers.create(false, false, false),
                CBasicType.INT,
                false,
                false,
                true,
                false,
                false,
                false,
                false),
            name,
            name,
            name,
            null));
  }

  public static class FunArrayBuilderException extends Exception {
    @Serial private static final long serialVersionUID = -927793611891304217L;

    public FunArrayBuilderException(String description) {
      super(description);
    }
  }
}
