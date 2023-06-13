// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Longs;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Identifier for basic symbolic values. Symbolic identifiers are used to track equality between
 * variables that have non-deterministic values.
 *
 * <p>
 *
 * <p>Example:
 *
 * <pre>
 *    int a = nondet_int();
 *    int b = a;
 *
 *    if (a != b) {
 * ERROR:
 *    return -1;
 *    }
 * </pre>
 *
 * In the example above, <code>a</code> is assigned a symbolic identifier. <code>b</code> is
 * assigned the same symbolic identifier, so that the condition <code>a != b</code> can be evaluated
 * as <code>false</code>.
 */
public class SymbolicIdentifier implements SymbolicValue, Comparable<SymbolicIdentifier> {

  private static final long serialVersionUID = -3773425414056328601L;

  // this objects unique id for identifying it
  private final long id;

  private final @Nullable MemoryLocation representedLocation;

  public SymbolicIdentifier(final long pId, final MemoryLocation pRepresentedLocation) {
    id = pId;
    representedLocation = pRepresentedLocation;
  }

  @Override
  public <T> T accept(SymbolicValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public Optional<MemoryLocation> getRepresentedLocation() {
    return Optional.ofNullable(representedLocation);
  }

  @Override
  public SymbolicValue copyForLocation(MemoryLocation pLocation) {
    return new SymbolicIdentifier(id, pLocation);
  }

  @Override
  public String getRepresentation() {
    if (representedLocation != null) {
      return representedLocation.toString();
    } else {
      return toString();
    }
  }

  public long getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(representedLocation) + ((int) (id ^ (id >>> 32)));
  }

  @Override
  public boolean equals(Object pOther) {
    return pOther instanceof SymbolicIdentifier
        && ((SymbolicIdentifier) pOther).id == id
        && Objects.equals(representedLocation, ((SymbolicIdentifier) pOther).representedLocation);
  }

  @Override
  public String toString() {
    return "SymbolicIdentifier[" + id + "]";
  }

  @Override
  public boolean isNumericValue() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public boolean isExplicitlyKnown() {
    return false;
  }

  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  @Override
  public Long asLong(CType type) {
    return null;
  }

  @Override
  public int compareTo(SymbolicIdentifier o) {
    return Longs.compare(getId(), o.getId());
  }

  /**
   * Converter for {@link SymbolicIdentifier} objects. Converts SymbolicIdentifiers to and from
   * Strings.
   */
  public static class Converter {

    private static final Converter SINGLETON = new Converter();

    private Converter() {
      // DO NOTHING
    }

    public static Converter getInstance() {
      return SINGLETON;
    }

    /**
     * Converts a given {@link SymbolicIdentifier} to a String. The returned <code>String</code>
     * contains all information necessary for uniquely identifying the given identifier.
     *
     * <p>
     *
     * <p>For a given identifier p, <code>convertToIdentifier(convertToStringEncoding(p)) = p</code>
     * is always true.
     *
     * @param pIdentifier the <code>SymbolicIdentifier</code> to convert to a string
     * @return a <code>String</code> containing all information necessary for converting it to a
     *     identifier
     */
    public String convertToStringEncoding(SymbolicIdentifier pIdentifier) {
      Optional<MemoryLocation> representedLocation = pIdentifier.getRepresentedLocation();
      assert representedLocation.isPresent();
      return representedLocation.orElseThrow().getExtendedQualifiedName()
          + "#"
          + pIdentifier.getId();
    }

    /**
     * Converts a given encoding of a {@link SymbolicIdentifier} to the corresponding <code>
     * SymbolicIdentifier</code>.
     *
     * <p>Only valid encodings, as produced by {@link #convertToStringEncoding(SymbolicIdentifier)},
     * are allowed.
     *
     * @param pIdentifierInformation a <code>String</code> encoding of a <code>SymbolicIdentifier
     *     </code>
     * @return the <code>SymbolicIdentifier</code> representing the given encoding
     * @throws IllegalArgumentException if given String does not match the expected String encoding
     */
    public SymbolicIdentifier convertToIdentifier(String pIdentifierInformation)
        throws IllegalArgumentException {

      final String variableName = FormulaManagerView.parseName(pIdentifierInformation).getFirst();
      final int idStart = variableName.indexOf("#");

      checkArgument(idStart >= 0, "Invalid encoding: %s", pIdentifierInformation);

      final String memLocName = variableName.substring(0, idStart);
      final String identifierIdOnly = variableName.substring(idStart + 1);
      if (!identifierIdOnly.matches("[0-9]+")) {
        throw new AssertionError("Unexpected encoding of symbolic identifier: " + identifierIdOnly);
      }
      final long id = Long.parseLong(identifierIdOnly);

      return new SymbolicIdentifier(id, MemoryLocation.parseExtendedQualifiedName(memLocName));
    }

    /**
     * Returns whether the given string is a valid encoding of a {@link SymbolicIdentifier}.
     *
     * @param pName the string to analyse
     * @return <code>true</code> if the given string is a valid encoding of a <code>
     *     SymbolicIdentifier</code>, <code>false</code> otherwise
     */
    public boolean isSymbolicEncoding(String pName) {
      String variableName = FormulaManagerView.parseName(pName).getFirst();
      return variableName.matches(".*(::)?.*(/.*)?#[0-9]*");
    }
  }
}
