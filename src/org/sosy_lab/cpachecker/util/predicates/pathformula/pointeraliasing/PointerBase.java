// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;

/** Representation of a base, i.e., a memory region. */
public record PointerBase(String name) implements Comparable<PointerBase> {

  private static final String BASE_PREFIX = "__ADDRESS_OF_";

  /**
   * Create a PointerBase from a plain name. Make sure that this is not the encoded form of the
   * name! Prefer {@link #PointerBase(CSimpleDeclaration)} where possible.
   */
  public PointerBase {
    checkNotNull(name);
    assert !isBaseNameInFormulas(name);
  }

  /** Create a PointerBase for a local or global variable. */
  public PointerBase(CSimpleDeclaration decl) {
    this(decl.getQualifiedName());
  }

  /**
   * Check if the given string is the name of a base as it appears in formulas (cf. {@link
   * #formulaEncoding()}).
   */
  private static boolean isBaseNameInFormulas(final String encodedBaseName) {
    return encodedBaseName.startsWith(BASE_PREFIX);
  }

  /**
   * Create a base from the form how it is encoded in formulas (cf. {@link #formulaEncoding()}), if
   * the term is such an encoding.
   */
  public static Optional<PointerBase> fromFormulaEncoding(String potentialEncodedBaseName) {
    if (isBaseNameInFormulas(potentialEncodedBaseName)) {
      return Optional.of(new PointerBase(potentialEncodedBaseName.substring(BASE_PREFIX.length())));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Return how to encode this base as a term in formulas. The result should not be used for
   * anything except creating formula terms!
   */
  String formulaEncoding() {
    return BASE_PREFIX + name;
  }

  @Override
  public int compareTo(PointerBase other) {
    return name.compareTo(other.name);
  }
}
