// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;

/**
 * Represents an approximation of a node in dependency graph i.e. variable, field or `top' (unknown
 * location).
 */
sealed interface VariableOrField {
  enum Unknown implements VariableOrField {
    INSTANCE;

    @Override
    public String toString() {
      return "<Unknown>";
    }
  }

  record Variable(String scopedName) implements VariableOrField {
    public Variable {
      checkNotNull(scopedName);
    }

    @Override
    public Variable asVariable() {
      return this;
    }

    @Override
    public String toString() {
      return scopedName;
    }
  }

  record Field(CCompositeType compositeType, String name) implements VariableOrField {
    public Field {
      checkNotNull(compositeType);
      checkNotNull(name);
    }

    @Override
    public Field asField() {
      return this;
    }

    @Override
    public String toString() {
      return compositeType + SCOPE_SEPARATOR + name;
    }

    private static final String SCOPE_SEPARATOR = "::";
  }

  static Variable newVariable(final String scopedName) {
    return new Variable(scopedName);
  }

  static Field newField(final CCompositeType composite, final String name) {
    return new Field(composite, name);
  }

  static Unknown unknown() {
    return Unknown.INSTANCE;
  }

  default boolean isVariable() {
    return this instanceof Variable;
  }

  default boolean isField() {
    return this instanceof Field;
  }

  default boolean isUnknown() {
    return this instanceof Unknown;
  }

  default Variable asVariable() {
    throw new ClassCastException(
        "Tried to match " + getClass().getName() + " with " + Variable.class.getName());
  }

  default Field asField() {
    throw new ClassCastException(
        "Tried to match " + getClass().getName() + " with " + Field.class.getName());
  }
}
