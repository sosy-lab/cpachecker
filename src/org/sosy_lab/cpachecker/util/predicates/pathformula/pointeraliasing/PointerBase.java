// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import java.util.Optional;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;

/**
 * Representation of a base, i.e., a memory region.
 *
 * <p>name is the name of this base. This is usually the qualified name of a variable
 *
 * <p>callStackDepth is the depth of the call stack at which this base is considered. This is in
 * order to differentiate between the same variable at different call-sites in a recursive procedure
 */
public record PointerBase(String name, OptionalInt callStackDepth)
    implements Comparable<PointerBase> {

  private static final String BASE_PREFIX = "__ADDRESS_OF_";
  private static final String CALL_STACK_DEPTH_SEPARATOR = "__CALL_STACK_DEPTH_";

  /**
   * Create a PointerBase from a plain name. Make sure that this is not the encoded form of the
   * name! Prefer {@link #PointerBase(CSimpleDeclaration)} where possible.
   */
  public PointerBase {
    checkNotNull(name);
    checkNotNull(callStackDepth);
    assert !isBaseNameInFormulas(name);
  }

  /**
   * Create a PointerBase for a local or global variable identifying it uniquely by using the
   * callstack depth.
   *
   * <p>For a global variable the callstack depth should be empty, for a local variable it should be
   * the depth of the call stack at which the variable is declared.
   */
  public PointerBase(CSimpleDeclaration decl, OptionalInt pCallStackDepth) {
    this(decl.getQualifiedName(), pCallStackDepth);
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
      OptionalInt callStackDepth = OptionalInt.empty();
      if (potentialEncodedBaseName.contains(CALL_STACK_DEPTH_SEPARATOR)) {
        callStackDepth =
            Splitter.on(CALL_STACK_DEPTH_SEPARATOR)
                .splitToStream(potentialEncodedBaseName)
                .skip(1) // skip the part before the separator
                .mapToInt(Integer::parseInt)
                .findFirst();
      }
      return Optional.of(
          new PointerBase(
              potentialEncodedBaseName.substring(BASE_PREFIX.length()), callStackDepth));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Return how to encode this base as a term in formulas. The result should not be used for
   * anything except creating formula terms!
   */
  String formulaEncoding() {
    return BASE_PREFIX + name + CALL_STACK_DEPTH_SEPARATOR + callStackDepth.orElse(-1);
  }

  @Override
  public int compareTo(PointerBase other) {
    return formulaEncoding().compareTo(other.formulaEncoding());
  }
}
