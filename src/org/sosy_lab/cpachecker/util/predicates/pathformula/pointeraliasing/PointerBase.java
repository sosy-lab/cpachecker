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
import java.io.Serializable;
import java.util.Optional;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Representation of a base, i.e., a memory region.
 *
 * <p>name is the name of this base. This is usually the qualified name of a variable
 *
 * <p>callStackDepth is the depth of the call stack at which this base is considered. This is in
 * order to differentiate between the same variable at different call-sites in a recursive procedure
 */
public record PointerBase(String name, OptionalInt callStackDepth)
    implements Comparable<PointerBase>, Serializable {

  private static final String BASE_PREFIX = "__ADDRESS_OF_";
  private static final String CALL_STACK_DEPTH_SEPARATOR = "__CALL_STACK_DEPTH_";

  /**
   * Create a PointerBase from a plain name. Make sure that this is not the encoded form of the
   * name! Prefer {@link #forVariable(CSimpleDeclaration, int)} for the base of a variable.
   */
  public PointerBase {
    checkNotNull(name);
    checkNotNull(callStackDepth);
    assert !isBaseNameInFormulas(name);
  }

  /**
   * Create the base of the given variable, which is identified uniquely by the given absolute call
   * stack depth if the variable is local.
   *
   * <p>Prefer {@link #forVariable(String, int)} only if no declaration is available, because a
   * global variable can be detected more reliably from its declaration than from its name.
   */
  public static PointerBase forVariable(CSimpleDeclaration declaration, int callStackDepth) {
    return forVariable(declaration.getQualifiedName(), isGlobal(declaration), callStackDepth);
  }

  /**
   * Create the base of the given variable, which is identified uniquely by the given absolute call
   * stack depth if the variable is local.
   */
  public static PointerBase forVariable(String qualifiedVariableName, int callStackDepth) {
    // Only variables declared inside a function are local, and those are exactly the ones whose
    // qualified name contains a function name.
    boolean isGlobal = CFAUtils.getFunctionName(qualifiedVariableName).isEmpty();
    return forVariable(qualifiedVariableName, isGlobal, callStackDepth);
  }

  /**
   * Global variables do not belong to any stack frame, so in contrast to local variables their
   * bases are not distinguished by the call stack depth.
   */
  private static PointerBase forVariable(
      String qualifiedVariableName, boolean isGlobal, int callStackDepth) {
    return new PointerBase(
        qualifiedVariableName, isGlobal ? OptionalInt.empty() : OptionalInt.of(callStackDepth));
  }

  private static boolean isGlobal(CSimpleDeclaration declaration) {
    return declaration instanceof CEnumerator
        || (declaration instanceof CDeclaration cDeclaration && cDeclaration.isGlobal());
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
    if (callStackDepth.isEmpty()) {
      return BASE_PREFIX + name;
    }

    return BASE_PREFIX + name + CALL_STACK_DEPTH_SEPARATOR + callStackDepth.orElseThrow();
  }

  @Override
  public int compareTo(PointerBase other) {
    return formulaEncoding().compareTo(other.formulaEncoding());
  }
}
