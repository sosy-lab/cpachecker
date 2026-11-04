// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.Serial;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

/**
 * A logic of SMT-LIB. Its name lists its theories after an optional prefix {@code QF_}, e.g. {@code
 * QF_AUFBVLIA} (see https://smt-lib.org/logics.shtml), or it is {@code ALL}.
 */
public final class SmtLibLogic implements Serializable {

  @Serial private static final long serialVersionUID = 4371984465328390151L;

  /** The parts of the name of a logic. */
  private enum Theory {
    // AX has to be matched before A
    ARRAYS_WITH_EXTENSIONALITY("AX"),
    ARRAYS("A"),
    UNINTERPRETED_FUNCTIONS("UF"),
    BITVECTORS("BV"),
    FLOATING_POINT_NUMBERS("FP"),
    DATATYPES("DT"),
    STRINGS("S"),
    INTEGER_DIFFERENCE_LOGIC("IDL"),
    REAL_DIFFERENCE_LOGIC("RDL"),
    LINEAR_INTEGER_ARITHMETIC("LIA"),
    NON_LINEAR_INTEGER_ARITHMETIC("NIA"),
    LINEAR_REAL_ARITHMETIC("LRA"),
    NON_LINEAR_REAL_ARITHMETIC("NRA"),
    LINEAR_INTEGER_REAL_ARITHMETIC("LIRA"),
    NON_LINEAR_INTEGER_REAL_ARITHMETIC("NIRA");

    private final String abbreviation;

    Theory(String pAbbreviation) {
      abbreviation = pAbbreviation;
    }
  }

  private static final ImmutableSet<Theory> INTEGER_ARITHMETIC =
      Sets.immutableEnumSet(
          Theory.INTEGER_DIFFERENCE_LOGIC,
          Theory.LINEAR_INTEGER_ARITHMETIC,
          Theory.NON_LINEAR_INTEGER_ARITHMETIC,
          Theory.LINEAR_INTEGER_REAL_ARITHMETIC,
          Theory.NON_LINEAR_INTEGER_REAL_ARITHMETIC);

  private final String name;
  private final ImmutableSet<Theory> theories;

  private SmtLibLogic(String pName, Set<Theory> pTheories) {
    name = pName;
    theories = Sets.immutableEnumSet(pTheories);
  }

  public static SmtLibLogic fromString(String pLogic) {
    if (pLogic.equals("ALL")) {
      return new SmtLibLogic(pLogic, EnumSet.allOf(Theory.class));
    }

    String rest = pLogic.startsWith("QF_") ? pLogic.substring("QF_".length()) : pLogic;
    checkArgument(!rest.isEmpty(), "Unknown SMT-LIB logic: %s", pLogic);
    Set<Theory> theories = EnumSet.noneOf(Theory.class);
    while (!rest.isEmpty()) {
      Theory theory = null;
      for (Theory candidate : Theory.values()) {
        if (rest.startsWith(candidate.abbreviation)) {
          theory = candidate;
          break;
        }
      }
      checkArgument(theory != null, "Unknown SMT-LIB logic: %s", pLogic);
      theories.add(theory);
      rest = rest.substring(theory.abbreviation.length());
    }
    return new SmtLibLogic(pLogic, theories);
  }

  public boolean containsIntegerArithmetic() {
    return !Sets.intersection(theories, INTEGER_ARITHMETIC).isEmpty();
  }

  public boolean containsBitvectorOperations() {
    return theories.contains(Theory.BITVECTORS);
  }

  public boolean containsFloatingPointOperations() {
    return theories.contains(Theory.FLOATING_POINT_NUMBERS);
  }

  public boolean containsArrayOperations() {
    return theories.contains(Theory.ARRAYS) || theories.contains(Theory.ARRAYS_WITH_EXTENSIONALITY);
  }

  /** Does the logic allow functions with parameters of `declare-fun`? */
  public boolean containsUninterpretedFunctions() {
    return theories.contains(Theory.UNINTERPRETED_FUNCTIONS);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    return this == pOther || (pOther instanceof SmtLibLogic other && name.equals(other.name));
  }
}
