// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import java.io.Serializable;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.ValueVisitor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Marker interface for symbolic values.
 *
 * <p>Each class implementing this interface should provide an <code>equals(Object)</code> method
 * that allows checks for equality of symbolic values.
 */
@SuppressWarnings("serial") // we cannot set a UID for an interface
public interface SymbolicValue extends Value, Serializable {

  <T> T accept(SymbolicValueVisitor<T> pVisitor);

  /** Returns the memory location this symbolic value represents. */
  Optional<MemoryLocation> getRepresentedLocation();

  SymbolicValue copyForLocation(MemoryLocation pLocation);

  /**
   * Returns a string representation of this symbolic value with symbolic expressions representing a
   * certain memory locations replaced with these locations.
   */
  String getRepresentation();

  @Override
  default <T> T accept(ValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
}
