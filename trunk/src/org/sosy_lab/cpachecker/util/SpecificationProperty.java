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
import org.sosy_lab.cpachecker.util.Property.CommonCoverageType;

public class SpecificationProperty {

  private final String entryFunction;

  private final Property property;

  private final Optional<String> internalSpecificationPath;

  public SpecificationProperty(
      String pEntryFunction, Property pProperty, Optional<String> pInternalSpecificationPath) {
    entryFunction = Objects.requireNonNull(pEntryFunction);
    property = Objects.requireNonNull(pProperty);
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
   * Gets the property.
   *
   * @return the property.
   */
  public Property getProperty() {
    return property;
  }

  @Override
  public String toString() {
    return (property instanceof CommonCoverageType)
        ? String.format(
            "COVER( init(%s()), FQL(%s) )", getEntryFunction(), getProperty().toString())
        : String.format(
            "CHECK( init(%s()), LTL(%s) )", getEntryFunction(), getProperty().toString());
  }
}
