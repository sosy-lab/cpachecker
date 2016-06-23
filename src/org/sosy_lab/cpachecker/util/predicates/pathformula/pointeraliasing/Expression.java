/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.MoreObjects.toStringHelper;

import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.solver.api.Formula;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

abstract class Expression {
  static abstract class Location extends Expression {
    static final class AliasedLocation extends Location {

      private AliasedLocation(final Formula address) {
        this.address = address;
      }

      Formula getAddress() {
        return address;
      }

      @Override
      Kind getKind() {
        return Kind.ALIASED_LOCATION;
      }

      @Override
      @Deprecated
      AliasedLocation asAliased() {
        return this;
      }

      @Override
      @Deprecated
      AliasedLocation asAliasedLocation() {
        return this;
      }

      @Override
      @Deprecated
      UnaliasedLocation asUnaliased() {
        return null;
      }

      @Override
      @Deprecated
      UnaliasedLocation asUnaliasedLocation() {
        return null;
      }

      @Override
      public String toString() {
        return toStringHelper(this)
                      .add("address", address)
                      .toString();
      }

      private final Formula address;
    }

    static final class UnaliasedLocation extends Location {

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
        return null;
      }

      @Override
      @Deprecated
      AliasedLocation asAliasedLocation() {
        return null;
      }

      @Override
      @Deprecated
      UnaliasedLocation asUnaliased() {
        return this;
      }

      @Override
      @Deprecated
      UnaliasedLocation asUnaliasedLocation() {
        return this;
      }

      @Override
      public String toString() {
        return toStringHelper(this)
                      .add("variable", variableName)
                      .toString();
      }

      private final String variableName;
    }

    static AliasedLocation ofAddress(final @Nonnull Formula address) {
      return new AliasedLocation(address);
    }

    static UnaliasedLocation ofVariableName(final @Nonnull String variableName) {
      return new UnaliasedLocation(variableName);
    }

    boolean isAliased() {
      return this instanceof AliasedLocation;
    }

    @Override
    @Deprecated
    final Location asLocation() {
      return this;
    }

    @Override
    final Value asValue() {
      return null;
    }

    abstract AliasedLocation asAliased();

    abstract UnaliasedLocation asUnaliased();
  }

  static class Value extends Expression {
    private static class Nondet extends Value {
      private Nondet() {
        super(null);
      }

      @Override
      Formula getValue() {
        return null;
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
        return toStringHelper(this)
                      .toString();
      }
    }

    Value(final Formula value) {
      this.value = value;
    }

    Formula getValue() {
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
      return null;
    }

    @Override
    @Deprecated
    final AliasedLocation asAliasedLocation() {
      return null;
    }

    @Override
    @Deprecated
    final UnaliasedLocation asUnaliasedLocation() {
      return null;
    }

    @Override
    @Deprecated
    final Value asValue() {
      return this;
    }

    @Override
    public String toString() {
      return toStringHelper(this)
                    .add("value", value)
                    .toString();
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

    private final Formula value;
    private static final Value nondet = new Nondet();
  }

  static Value ofValue(final @Nullable Formula value) {
    return value != null ? new Value(value) : null;
  }

  static Value nondetValue() {
    return Value.nondet;
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
    return this.isLocation() && this.asLocation().isAliased();
  }

  boolean isUnaliasedLocation() {
    return this.isLocation() && !this.asLocation().isAliased();
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
