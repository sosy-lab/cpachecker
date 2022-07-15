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
abstract class VariableOrField implements Comparable<VariableOrField> {
  private static final class Unknown extends VariableOrField {
    private Unknown() {}

    @Override
    public String toString() {
      return "<Unknown>";
    }

    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Unknown)) {
        return false;
      } else {
        return true;
      }
    }

    @Override
    public int compareTo(final VariableOrField other) {
      if (this == other) {
        return 0;
      } else if (other instanceof Variable) {
        return -1;
      } else if (other instanceof Field) {
        return -1;
      } else {
        checkNotNull(other);
        throw new AssertionError("Should not happen: all cases are covered above");
      }
    }

    @Override
    public int hashCode() {
      return 7;
    }

    private static final Unknown INSTANCE = new Unknown();
  }

  static final class Variable extends VariableOrField {
    private Variable(final String scopedName) {
      this.scopedName = checkNotNull(scopedName);
    }

    public String getScopedName() {
      return scopedName;
    }

    @Override
    public String toString() {
      return getScopedName();
    }

    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Variable)) {
        return false;
      } else {
        final Variable other = (Variable) o;
        return scopedName.equals(other.scopedName);
      }
    }

    @Override
    public int compareTo(final VariableOrField other) {
      if (this == other) {
        return 0;
      } else if (other instanceof Unknown) {
        return 1;
      } else if (other instanceof Field) {
        return -1;
      } else if (other instanceof Variable) {
        return scopedName.compareTo(((Variable) other).scopedName);
      } else {
        checkNotNull(other);
        throw new AssertionError("Should not happen: all cases are covered above");
      }
    }

    @Override
    public int hashCode() {
      return scopedName.hashCode();
    }

    private final String scopedName;
  }

  static final class Field extends VariableOrField {
    private Field(final CCompositeType composite, final String name) {
      this.composite = checkNotNull(composite);
      this.name = checkNotNull(name);
    }

    public CCompositeType getCompositeType() {
      return composite;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return composite + SCOPE_SEPARATOR + name;
    }

    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Field)) {
        return false;
      } else {
        final Field other = (Field) o;
        return composite.equals(other.composite) && name.equals(other.name);
      }
    }

    @Override
    public int compareTo(final VariableOrField other) {
      if (this == other) {
        return 0;
      } else if (other instanceof Unknown) {
        return 1;
      } else if (other instanceof Variable) {
        return 1;
      } else if (other instanceof Field) {
        final Field o = (Field) other;
        final int result = composite.getQualifiedName().compareTo(o.composite.getQualifiedName());
        return result != 0 ? result : name.compareTo(o.name);
      } else {
        checkNotNull(other);
        throw new AssertionError("Should not happen: all cases are covered above");
      }
    }

    @Override
    public int hashCode() {
      final int prime = 67;
      return prime * composite.hashCode() + name.hashCode();
    }

    private final CCompositeType composite;
    private final String name;
    private static final String SCOPE_SEPARATOR = "::";
  }

  private VariableOrField() {}

  public static Variable newVariable(final String scopedName) {
    return new Variable(scopedName);
  }

  public static Field newField(final CCompositeType composite, final String name) {
    return new Field(composite, name);
  }

  public static Unknown unknown() {
    return Unknown.INSTANCE;
  }

  public boolean isVariable() {
    return this instanceof Variable;
  }

  public boolean isField() {
    return this instanceof Field;
  }

  public boolean isUnknown() {
    return this instanceof Unknown;
  }

  public Variable asVariable() {
    if (this instanceof Variable) {
      return (Variable) this;
    } else {
      throw new ClassCastException(
          "Tried to match " + this.getClass().getName() + " with " + Variable.class.getName());
    }
  }

  public Field asField() {
    if (this instanceof Field) {
      return (Field) this;
    } else {
      throw new ClassCastException(
          "Tried to match " + this.getClass().getName() + " with " + Field.class.getName());
    }
  }

  @Override
  public abstract boolean equals(final Object other);

  @Override
  public abstract int hashCode();

  @Override
  public abstract int compareTo(final VariableOrField other);
}
