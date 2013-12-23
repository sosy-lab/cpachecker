/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.util;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

public class Expression {
  public static class Location extends Expression {
    public static class AliasedLocation extends Location {

      private AliasedLocation(final Formula address) {
        this.address = address;
      }

      public Formula getAddress() {
        return address;
      }

      private final Formula address;
    }

    public static class UnaliasedLocation extends Location {

      private UnaliasedLocation(final String variableName) {
        this.variableName = variableName;
      }

      public String getVariableName() {
        return variableName;
      }

      private final String variableName;
    }

    public static AliasedLocation ofAddress(final Formula address) {
      return new AliasedLocation(address);
    }

    public static UnaliasedLocation ofVariableName(final String variableName) {
      return new UnaliasedLocation(variableName);
    }

    public boolean isAliased() {
      return this instanceof AliasedLocation;
    }

    public AliasedLocation asAliased() {
      if (this instanceof AliasedLocation) {
        return (AliasedLocation) this;
      } else {
        return null;
      }
    }

    public UnaliasedLocation asUnaliased() {
      if (this instanceof UnaliasedLocation) {
        return (UnaliasedLocation) this;
      } else {
        return null;
      }
    }
  }

  public static class Value extends Expression {
    private Value(final Formula value) {
      this.value = value;
    }

    public Formula getValue() {
      return value;
    }

    private final Formula value;
  }

  public static Value ofValue(final Formula value) {
    return new Value(value);
  }

  public boolean isLocation() {
    return this instanceof Location;
  }

  public boolean isValue() {
    return this instanceof Value;
  }

  public Location asLocation() {
    if (this instanceof Location) {
      return (Location) this;
    } else {
      return null;
    }
  }

  public Value asValue() {
    if (this instanceof Value) {
      return (Value) this;
    } else {
      return null;
    }
  }
}
