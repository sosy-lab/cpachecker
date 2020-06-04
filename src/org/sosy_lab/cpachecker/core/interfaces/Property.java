// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;


/**
 * This interface represents a property that is checked
 * during the analysis.
 *
 * A specification might consist of a set of properties.
 *
 * Instances of this interface...
 *    MUST override the .toString() method to provide a description of the property!
 *    MIGHT override the .equals(...) method! (and implicitly the hashCode() method)
 */
public interface Property {

  /** Return the textual description of the property. */
  @Override
  String toString();
}
