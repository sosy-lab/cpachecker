// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
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
 * order to differentiate between the same variable at different call-sites in a recursive
 * procedure. It is null for bases that do not belong to any stack frame, for example those of
 * global variables. An {@link java.util.OptionalInt} would express this better, but it is not
 * serializable.
 */
public record PointerBase(String name, @Nullable Integer callStackDepth)
    implements Comparable<PointerBase>, Serializable {

  private static final String BASE_PREFIX = "__ADDRESS_OF_";
  private static final String CALL_STACK_DEPTH_SEPARATOR = "__CALL_STACK_DEPTH_";

  /**
   * Create a PointerBase from a plain name. Make sure that this is not the encoded form of the
   * name! Prefer {@link #forVariable(CSimpleDeclaration, int)} for the base of a variable.
   */
  public PointerBase {
    checkNotNull(name);
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
   *
   * <p>The name is expected to be a variable name as it occurs in formulas, i.e., without an SSA
   * index. A name that does not have a function name as prefix is considered to belong to no stack
   * frame and thus gets no call stack depth, just like a global variable. This makes this method
   * applicable to all variable names occurring in formulas, also to those that are not names of C
   * variables, such as the variables that model the results of calls to external functions.
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
    return new PointerBase(qualifiedVariableName, isGlobal ? null : callStackDepth);
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
    if (!isBaseNameInFormulas(potentialEncodedBaseName)) {
      return Optional.empty();
    }

    final String encodedBase = potentialEncodedBaseName.substring(BASE_PREFIX.length());
    // The call stack depth is appended at the end of the name, so it is the part after the last
    // occurrence of the separator and the name is everything before it.
    final int separatorPosition = encodedBase.lastIndexOf(CALL_STACK_DEPTH_SEPARATOR);
    if (separatorPosition < 0) {
      return Optional.of(new PointerBase(encodedBase, null));
    }

    return Optional.of(
        new PointerBase(
            encodedBase.substring(0, separatorPosition),
            Integer.parseInt(
                encodedBase.substring(separatorPosition + CALL_STACK_DEPTH_SEPARATOR.length()))));
  }

  /**
   * Return how to encode this base as a term in formulas. The result should not be used for
   * anything except creating formula terms!
   */
  String formulaEncoding() {
    if (callStackDepth == null) {
      return BASE_PREFIX + name;
    }

    return BASE_PREFIX + name + CALL_STACK_DEPTH_SEPARATOR + callStackDepth;
  }

  @Override
  public int compareTo(PointerBase other) {
    return formulaEncoding().compareTo(other.formulaEncoding());
  }
}
