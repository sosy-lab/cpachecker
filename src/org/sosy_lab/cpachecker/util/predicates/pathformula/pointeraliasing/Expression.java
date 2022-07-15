// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.InlineMe;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.java_smt.api.Formula;

abstract class Expression {
  abstract static class Location extends Expression {
    static final class AliasedLocation extends Location {

      static AliasedLocation ofAddress(final Formula address) {
        return new AliasedLocation(checkNotNull(address));
      }

      static AliasedLocation ofAddressWithRegion(final Formula address, final MemoryRegion region) {
        return new AliasedLocation(checkNotNull(address), checkNotNull(region));
      }

      private AliasedLocation(final Formula address) {
        this(address, null);
      }

      private AliasedLocation(final Formula address, @Nullable final MemoryRegion region) {
        this.address = address;
        this.region = region;
      }

      Formula getAddress() {
        return address;
      }

      @Override
      Kind getKind() {
        return Kind.ALIASED_LOCATION;
      }

      @InlineMe(replacement = "this")
      @Override
      @Deprecated
      AliasedLocation asAliased() {
        return this;
      }

      @InlineMe(replacement = "this")
      @Override
      @Deprecated
      AliasedLocation asAliasedLocation() {
        return this;
      }

      @Override
      @Deprecated
      UnaliasedLocation asUnaliased() {
        throw new IllegalStateException();
      }

      @Override
      @Deprecated
      UnaliasedLocation asUnaliasedLocation() {
        throw new IllegalStateException();
      }

      @Override
      public String toString() {
        return toStringHelper(this).add("address", address).toString();
      }

      private final Formula address;

      private final @Nullable MemoryRegion region;

      @Nullable MemoryRegion getMemoryRegion() {
        return region;
      }
    }

    static final class UnaliasedLocation extends Location {

      static UnaliasedLocation ofVariableName(final String variableName) {
        return new UnaliasedLocation(checkNotNull(variableName));
      }

      private UnaliasedLocation(final String variableName) {
        this.variableName = variableName;
      }

      String getVariableName() {
        return variableName;
      }

      @Override
      Kind getKind() {
        return Kind.UNALIASED_LOCATION;
      }

      @Override
      @Deprecated
      AliasedLocation asAliased() {
        throw new IllegalStateException();
      }

      @Override
      @Deprecated
      AliasedLocation asAliasedLocation() {
        throw new IllegalStateException();
      }

      @InlineMe(replacement = "this")
      @Override
      @Deprecated
      UnaliasedLocation asUnaliased() {
        return this;
      }

      @InlineMe(replacement = "this")
      @Override
      @Deprecated
      UnaliasedLocation asUnaliasedLocation() {
        return this;
      }

      @Override
      public String toString() {
        return toStringHelper(this).add("variable", variableName).toString();
      }

      private final String variableName;
    }

    boolean isAliased() {
      return this instanceof AliasedLocation;
    }

    @InlineMe(replacement = "this")
    @Override
    @Deprecated
    final Location asLocation() {
      return this;
    }

    @Override
    final Value asValue() {
      throw new IllegalStateException();
    }

    abstract AliasedLocation asAliased();

    abstract UnaliasedLocation asUnaliased();
  }

  static class Value extends Expression {

    static Value ofValueOrNondet(@Nullable final Formula value) {
      return value == null ? nondetValue() : ofValue(value);
    }

    static Value ofValue(final Formula value) {
      return new Value(checkNotNull(value));
    }

    static Value nondetValue() {
      return Value.nondet;
    }

    private static class Nondet extends Value {
      private Nondet() {
        super(null);
      }

      @Override
      boolean isNondet() {
        return true;
      }

      @Override
      Kind getKind() {
        return Kind.NONDET;
      }

      @Override
      public String toString() {
        return toStringHelper(this).toString();
      }
    }

    Value(final @Nullable Formula value) {
      this.value = value;
    }

    @Nullable Formula getValue() {
      return value;
    }

    boolean isNondet() {
      return false;
    }

    @Override
    Kind getKind() {
      return Kind.DET_VALUE;
    }

    @Override
    @Deprecated
    final Location asLocation() {
      throw new IllegalStateException();
    }

    @Override
    @Deprecated
    final AliasedLocation asAliasedLocation() {
      throw new IllegalStateException();
    }

    @Override
    @Deprecated
    final UnaliasedLocation asUnaliasedLocation() {
      throw new IllegalStateException();
    }

    @InlineMe(replacement = "this")
    @Override
    @Deprecated
    final Value asValue() {
      return this;
    }

    @Override
    public String toString() {
      return toStringHelper(this).add("value", value).toString();
    }

    @Override
    public boolean equals(Object pOther) {
      if (!(pOther instanceof Value)) {
        return false;
      }
      Value otherValue = (Value) pOther;
      if (this instanceof Nondet || otherValue instanceof Nondet) {
        return false;
      }

      return getValue().equals(otherValue.getValue());
    }

    @Override
    public int hashCode() {
      return value != null ? value.hashCode() : 0;
    }

    private final @Nullable Formula value;
    private static final Value nondet = new Nondet();
  }

  boolean isLocation() {
    return this instanceof Location;
  }

  boolean isValue() {
    return this instanceof Value;
  }

  boolean isNondetValue() {
    return this == Value.nondet;
  }

  boolean isAliasedLocation() {
    return isLocation() && asLocation().isAliased();
  }

  boolean isUnaliasedLocation() {
    return isLocation() && !asLocation().isAliased();
  }

  abstract Location asLocation();

  abstract AliasedLocation asAliasedLocation();

  abstract UnaliasedLocation asUnaliasedLocation();

  abstract Value asValue();

  abstract Kind getKind();

  enum Kind {
    ALIASED_LOCATION,
    UNALIASED_LOCATION,
    DET_VALUE,
    NONDET
  }
}
