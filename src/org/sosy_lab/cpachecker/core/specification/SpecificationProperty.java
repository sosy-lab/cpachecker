// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.specification.Property.CommonCoverageType;

public class SpecificationProperty {

  private final String entryFunction;

  private final Property property;

  private final Optional<Path> internalSpecificationPath;

  public SpecificationProperty(
      String pEntryFunction, Property pProperty, Optional<Path> pInternalSpecificationPath) {
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
  public Optional<Path> getInternalSpecificationPath() {
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
