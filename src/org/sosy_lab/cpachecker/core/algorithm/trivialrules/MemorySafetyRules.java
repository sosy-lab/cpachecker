// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalInt;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;

/** The rules for the memory-safety properties. */
final class MemorySafetyRules {

  private MemorySafetyRules() {}

  /** All propositions of valid-memsafety, plus valid-memcleanup. */
  private static final ImmutableSet<Property> ALL_PROPOSITIONS =
      ImmutableSet.of(
          CommonVerificationProperty.VALID_FREE,
          CommonVerificationProperty.VALID_DEREF,
          CommonVerificationProperty.VALID_MEMTRACK,
          CommonVerificationProperty.VALID_MEMCLEANUP);

  /** The propositions that are about an allocated block, i.e., all but valid-deref. */
  private static final ImmutableSet<Property> PROPOSITIONS_ABOUT_BLOCKS =
      ImmutableSet.of(
          CommonVerificationProperty.VALID_FREE,
          CommonVerificationProperty.VALID_MEMTRACK,
          CommonVerificationProperty.VALID_MEMCLEANUP);

  /**
   * The functions that allocate or free memory. A task that only declares them calls a function
   * without a body, which makes the rules abstain anyway; this list is for a task that brings its
   * own definition of them.
   */
  private static final ImmutableSet<String> ALLOCATION_FUNCTIONS =
      ImmutableSet.of(
          "malloc",
          "calloc",
          "realloc",
          "reallocarray",
          "free",
          "alloca",
          "__builtin_alloca",
          "aligned_alloc",
          "valloc",
          "memalign",
          "posix_memalign",
          "strdup",
          "strndup");

  static ImmutableList<TrivialRule> rules() {
    return ImmutableList.of(
        new TrivialRule(
            "no-memory-operation",
            "A program that does not allocate memory and that contains no dereference, no array"
                + " subscript and no address-of operator performs no memory operation that could"
                + " be invalid.",
            ALL_PROPOSITIONS,
            MemorySafetyRules::checkNoMemoryOperation),
        new TrivialRule(
            "no-heap-allocation",
            "A program that never allocates memory has no block that could be freed invalidly, be"
                + " leaked, or still be allocated when the program ends.",
            PROPOSITIONS_ABOUT_BLOCKS,
            MemorySafetyRules::checkNoHeapAllocation),
        new TrivialRule(
            "all-accesses-inside-their-object",
            "A program without pointers can only access the objects it declares, and it can only"
                + " access them through an array subscript. If the value of every subscript is"
                + " inside the bounds of the array it is applied to, every access is inside the"
                + " object it belongs to.",
            ALL_PROPOSITIONS,
            MemorySafetyRules::checkAllAccessesInsideTheirObject),
        new TrivialRule(
            "access-outside-object-on-every-execution",
            "Every execution of the program executes the same sequence of edges as long as every"
                + " location on it has exactly one possible successor. An access outside of an"
                + " object on that sequence is therefore performed by every execution.",
            ImmutableSet.of(CommonVerificationProperty.VALID_DEREF),
            MemorySafetyRules::checkAccessOutsideObjectOnEveryExecution),
        new TrivialRule(
            "free-of-non-heap-object-on-every-execution",
            "Every execution of the program executes the same sequence of edges as long as every"
                + " location on it has exactly one possible successor. A call of free() with the"
                + " address of a declared object on that sequence is therefore performed by every"
                + " execution, and only a block that was allocated may be freed.",
            ImmutableSet.of(CommonVerificationProperty.VALID_FREE),
            MemorySafetyRules::checkFreeOfNonHeapObjectOnEveryExecution));
  }

  private static Optional<RuleVerdict> checkNoMemoryOperation(ProgramFacts pFacts) {
    if (!isApplicable(pFacts)) {
      return Optional.empty();
    }
    for (CFAEdge edge : pFacts.reachableEdges()) {
      for (CAstNode node : ProgramFacts.astNodes(edge)) {
        if (isMemoryOperation(node)) {
          return Optional.empty();
        }
      }
    }
    return RuleVerdict.proven(
        "no reachable edge of the program ("
            + pFacts.reachableEdges().size()
            + " edges) allocates memory, dereferences a pointer, applies an array subscript or"
            + " takes the address of an object");
  }

  private static Optional<RuleVerdict> checkNoHeapAllocation(ProgramFacts pFacts) {
    if (!isApplicable(pFacts)) {
      return Optional.empty();
    }
    return RuleVerdict.proven(
        "no reachable edge of the program ("
            + pFacts.reachableEdges().size()
            + " edges) calls a function that allocates or frees memory");
  }

  private static Optional<RuleVerdict> checkAllAccessesInsideTheirObject(ProgramFacts pFacts) {
    if (!isApplicable(pFacts)) {
      return Optional.empty();
    }
    // Without a pointer the program cannot reach an object other than the ones it declares, and
    // without the address-of operator it cannot create a pointer.
    for (FunctionEntryNode function : pFacts.cfa().getAllFunctions().values()) {
      if (!pFacts.reachableNodes().contains(function)
          || !(function.getFunction() instanceof CFunctionDeclaration declaration)) {
        continue;
      }
      if (hasPointerType(declaration.getType().getReturnType())) {
        return Optional.empty();
      }
      for (CParameterDeclaration parameter : declaration.getParameters()) {
        if (hasPointerType(parameter.getType())) {
          return Optional.empty();
        }
      }
    }
    int accesses = 0;
    for (CFAEdge edge : pFacts.reachableEdges()) {
      for (CAstNode node : ProgramFacts.astNodes(edge)) {
        if (isAddressOf(node) || hasPointerType(node)) {
          return Optional.empty();
        }
        if (node instanceof CArraySubscriptExpression subscript) {
          Boolean inBounds = isInBounds(pFacts, subscript);
          if (inBounds == null || !inBounds) {
            return Optional.empty();
          }
          accesses++;
        }
      }
    }
    return RuleVerdict.proven(
        "the program uses no pointer and takes no address, and each of its "
            + accesses
            + " array accesses is inside the bounds of the array for every value that the types of"
            + " the index allow");
  }

  private static Optional<RuleVerdict> checkAccessOutsideObjectOnEveryExecution(
      ProgramFacts pFacts) {
    if (!pFacts.isCProgram()) {
      return Optional.empty();
    }
    for (CFAEdge edge : pFacts.chain().edges()) {
      for (CAstNode node : ProgramFacts.astNodes(edge)) {
        if (node instanceof CArraySubscriptExpression subscript) {
          Boolean inBounds = isInBounds(pFacts, subscript);
          if (inBounds != null && !inBounds) {
            return RuleVerdict.refuted(
                "every execution evaluates \""
                    + subscript.toASTString()
                    + "\" at "
                    + edge.getFileLocation()
                    + ", whose subscript is outside the bounds of the array for every value that"
                    + " the types of the index allow",
                edge);
          }
        }
        if (node instanceof CPointerExpression pointer && isNullPointer(pointer.getOperand())) {
          return RuleVerdict.refuted(
              "every execution dereferences the null pointer at " + edge.getFileLocation(), edge);
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<RuleVerdict> checkFreeOfNonHeapObjectOnEveryExecution(
      ProgramFacts pFacts) {
    if (!pFacts.isCProgram() || pFacts.isDefinedFunction("free")) {
      return Optional.empty();
    }
    for (CFAEdge edge : pFacts.chain().edges()) {
      if (!"free".equals(ProgramFacts.nameOfCallWithoutBody(edge))) {
        continue;
      }
      for (CAstNode node : ProgramFacts.astNodes(edge)) {
        if (!(node instanceof CFunctionCallExpression call)
            || call.getParameterExpressions().size() != 1) {
          continue;
        }
        CExpression argument = withoutCasts(call.getParameterExpressions().get(0));
        if (isAddressOf(argument)
            || argument.getExpressionType().getCanonicalType() instanceof CArrayType) {
          return RuleVerdict.refuted(
              "every execution calls free(\""
                  + argument.toASTString()
                  + "\") at "
                  + edge.getFileLocation()
                  + ", which frees an object that was not allocated",
              edge);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Whether the rules that prove a proposition may be used: they argue about all memory operations
   * of the program, so we have to see all of them.
   */
  private static boolean isApplicable(ProgramFacts pFacts) {
    if (!pFacts.isCProgram() || !pFacts.unknownFunctions().isEmpty()) {
      // A function without a body can allocate memory and dereference a pointer.
      return false;
    }
    for (CFAEdge edge : pFacts.reachableEdges()) {
      String called = ProgramFacts.nameOfCallWithoutBody(edge);
      if (called != null && ALLOCATION_FUNCTIONS.contains(called)) {
        return false;
      }
      for (CAstNode node : ProgramFacts.astNodes(edge)) {
        if (node instanceof CFunctionCallExpression call
            && call.getDeclaration() != null
            && ALLOCATION_FUNCTIONS.contains(call.getDeclaration().getName())) {
          // The program brings its own definition of an allocation function.
          return false;
        }
      }
    }
    return true;
  }

  private static boolean isMemoryOperation(CAstNode pNode) {
    return pNode instanceof CPointerExpression
        || pNode instanceof CArraySubscriptExpression
        || isAddressOf(pNode)
        || (pNode instanceof CFieldReference field && field.isPointerDereference());
  }

  private static boolean isAddressOf(CAstNode pNode) {
    return pNode instanceof CUnaryExpression unary && unary.getOperator() == UnaryOperator.AMPER;
  }

  /** Whether the type of the given expression or declaration is a pointer type. */
  private static boolean hasPointerType(CAstNode pNode) {
    if (pNode instanceof CExpression expression) {
      return hasPointerType(expression.getExpressionType());
    }
    if (pNode instanceof CSimpleDeclaration declaration) {
      return hasPointerType(declaration.getType());
    }
    return false;
  }

  private static boolean hasPointerType(CType pType) {
    CType type = pType.getCanonicalType();
    if (type instanceof CArrayType array) {
      return hasPointerType(array.getType());
    }
    return type instanceof CPointerType;
  }

  /**
   * Whether the given array access is inside the bounds of its array: {@code true} if it is for
   * every value of the subscript, {@code false} if it is for none, and {@code null} if we cannot
   * say or if the length of the array is unknown.
   */
  private static @Nullable Boolean isInBounds(
      ProgramFacts pFacts, CArraySubscriptExpression pSubscript) {
    CType arrayType = pSubscript.getArrayExpression().getExpressionType().getCanonicalType();
    if (!(arrayType instanceof CArrayType array)) {
      return null;
    }
    OptionalInt length = array.getLengthAsInt();
    if (length.isEmpty()) {
      return null;
    }
    IntegerRange index = pFacts.ranges().rangeOf(pSubscript.getSubscriptExpression());
    if (index == null) {
      return null;
    }
    IntegerRange bounds =
        new IntegerRange(BigInteger.ZERO, BigInteger.valueOf(length.orElseThrow() - 1L));
    if (index.isWithin(bounds)) {
      return true;
    }
    boolean outside =
        index.high().compareTo(bounds.low()) < 0 || index.low().compareTo(bounds.high()) > 0;
    return outside ? Boolean.FALSE : null;
  }

  /** Whether the given expression is the null pointer, i.e., the literal 0. */
  private static boolean isNullPointer(CExpression pExpression) {
    CExpression expression = withoutCasts(pExpression);
    return expression instanceof CIntegerLiteralExpression literal
        && literal.getValue().signum() == 0;
  }

  private static CExpression withoutCasts(CExpression pExpression) {
    CExpression expression = pExpression;
    while (expression instanceof CCastExpression cast) {
      expression = cast.getOperand();
    }
    return expression;
  }
}
