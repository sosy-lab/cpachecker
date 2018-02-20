/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import java.util.Objects;
import java.util.Optional;

public class SpecificationProperty {

  public static enum PropertyType {
    REACHABILITY_LABEL("G ! label(ERROR)"),

    REACHABILITY("G ! call(__VERIFIER_error())"),

    VALID_FREE("G valid-free"),

    VALID_DEREF("G valid-deref"),

    VALID_MEMTRACK("G valid-memtrack"),

    OVERFLOW("G ! overflow"),

    DEADLOCK("G ! deadlock"),

    TERMINATION("F end"),
    ;

    private final String representation;

    private PropertyType(String pRepresentation) {
      representation = pRepresentation;
    }

    @Override
    public String toString() {
      return representation;
    }
  }

  private final String entryFunction;

  private final PropertyType propertyType;

  private final Optional<String> internalSpecificationPath;

  public SpecificationProperty(
      String pEntryFunction,
      PropertyType pPropertyType,
      Optional<String> pInternalSpecificationPath) {
    entryFunction = Objects.requireNonNull(pEntryFunction);
    propertyType = Objects.requireNonNull(pPropertyType);
    internalSpecificationPath = Objects.requireNonNull(pInternalSpecificationPath);
  }

  /**
   * Gets the function entry.
   *
   * @return the function entry.
   */
  public String getEntryFunction() {
    return entryFunction;
  }

  /**
   * Gets the path to the specification automaton used to represent the property, if it exists.
   *
   * @return the path to the specification automaton used to represent the property, if it exists.
   */
  public Optional<String> getInternalSpecificationPath() {
    return internalSpecificationPath;
  }

  /**
   * Gets the type of the property.
   *
   * @return the type of the property.
   */
  public PropertyType getPropertyType() {
    return propertyType;
  }

  @Override
  public String toString() {
    return String.format(
        "CHECK( init(%s()), LTL(%s) )", getEntryFunction(), getPropertyType().toString());
  }
}
