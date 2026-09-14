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
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;

/**
 * The rules for the no-overflow property, i.e., for the absence of a signed integer overflow. The
 * wrap-around of unsigned arithmetic is defined behavior, and the property allows it explicitly.
 *
 * <p>A signed integer overflow happens exactly when the mathematical result of an operation is
 * outside the range of the type the operation is computed in, so both rules enumerate the
 * operations with a signed calculation type and compare the values their result can have (cf.
 * {@link RangeEstimator}) with that range. The rules are the two sides of the same comparison: if
 * every result is inside the range, the program has no overflow; if a result is outside the range
 * on an edge that every execution executes, the program does overflow.
 */
final class OverflowRules {

  private OverflowRules() {}

  private static final ImmutableSet<Property> PROPOSITIONS =
      ImmutableSet.of(CommonVerificationProperty.OVERFLOW);

  /**
   * The option that lets the parser evaluate the constant expressions of the program. The value it
   * computes has wrapped around already, so an overflow in such an expression is not visible in the
   * CFA any more.
   */
  private static final String SIMPLIFY_CONST_EXPRESSIONS_OPTION = "cfa.simplifyConstExpressions";

  /**
   * An operation of the program that can overflow.
   *
   * @param operation the AST node of the operation
   * @param type the type the operation is computed in
   * @param allowed the values that this type can represent
   * @param result the values that the mathematical result of the operation can have, or {@code
   *     null} if we cannot say
   */
  private record Operation(
      CAstNode operation, CType type, IntegerRange allowed, @Nullable IntegerRange result) {

    /** Whether the result of the operation leaves the range of its type for every value. */
    boolean alwaysOverflows() {
      return result != null
          && (result.high().compareTo(allowed.low()) < 0
              || result.low().compareTo(allowed.high()) > 0);
    }
  }

  static ImmutableList<TrivialRule> rules() {
    return ImmutableList.of(
        new TrivialRule(
            "no-signed-overflow-possible",
            "An operation overflows if its mathematical result is outside the range of the type it"
                + " is computed in, and the types of the operands bound the values of that result."
                + " A program in which no operation can leave the range of its type therefore has"
                + " no overflow. This covers a program without arithmetic, a program whose"
                + " arithmetic is unsigned, and a program whose operands are narrow enough.",
            PROPOSITIONS,
            OverflowRules::checkNoSignedOverflowPossible),
        new TrivialRule(
            "signed-overflow-on-every-execution",
            "Every execution of the program executes the same sequence of edges as long as every"
                + " location on it has exactly one possible successor. If an operation on that"
                + " sequence has a result that is outside the range of its type for every value of"
                + " its operands, every execution has an overflow.",
            PROPOSITIONS,
            OverflowRules::checkOverflowOnEveryExecution));
  }

  private static RuleVerdict checkNoSignedOverflowPossible(ProgramFacts pFacts) {
    if (!pFacts.isCProgram()) {
      return RuleVerdict.abstained();
    }
    if (!pFacts.isOptionExplicitlyDisabled(SIMPLIFY_CONST_EXPRESSIONS_OPTION)) {
      pFacts
          .logger()
          .log(
              Level.INFO,
              "Not using the rule no-signed-overflow-possible because the option",
              SIMPLIFY_CONST_EXPRESSIONS_OPTION,
              "is not disabled, so an overflow in a constant expression is not visible in the"
                  + " CFA.");
      return RuleVerdict.abstained();
    }
    if (!pFacts.unknownFunctions().isEmpty()) {
      // A function without a body could have an overflow of its own.
      return RuleVerdict.abstained();
    }

    int checked = 0;
    for (CFAEdge edge : pFacts.reachableEdges()) {
      for (Operation operation : operationsOf(pFacts, edge)) {
        if (operation.result() == null || !operation.result().isWithin(operation.allowed())) {
          return RuleVerdict.abstained();
        }
        checked++;
      }
    }

    if (checked == 0) {
      return RuleVerdict.proven(
          "no reachable edge of the program ("
              + pFacts.reachableEdges().size()
              + " edges) contains an operation on a signed integer type");
    }
    return RuleVerdict.proven(
        "the result of each of the "
            + checked
            + " operations on a signed integer type is inside the range of that type for every"
            + " value that the types of the operands allow");
  }

  private static RuleVerdict checkOverflowOnEveryExecution(ProgramFacts pFacts) {
    if (!pFacts.isCProgram()) {
      return RuleVerdict.abstained();
    }
    for (CFAEdge edge : pFacts.chain().edges()) {
      for (Operation operation : operationsOf(pFacts, edge)) {
        if (operation.alwaysOverflows()) {
          return RuleVerdict.refuted(
              "every execution evaluates \""
                  + operation.operation().toASTString()
                  + "\" at "
                  + edge.getFileLocation()
                  + ", whose result is in "
                  + operation.result()
                  + " and thus outside the range "
                  + operation.allowed()
                  + " of the type "
                  + operation.type()
                  + " it is computed in",
              edge);
        }
      }
    }
    return RuleVerdict.abstained();
  }

  /**
   * The operations of the given edge that can overflow, i.e., the ones that are computed in a
   * signed integer type. Bit operations and comparisons cannot leave the range of their type, and
   * neither can an operation on an unsigned type.
   */
  private static ImmutableList<Operation> operationsOf(ProgramFacts pFacts, CFAEdge pEdge) {
    RangeEstimator ranges = pFacts.ranges();
    ImmutableList.Builder<Operation> result = ImmutableList.builder();
    for (CAstNode node : ProgramFacts.astNodes(pEdge)) {
      CType type = calculationTypeOf(node);
      if (type == null || !ranges.isSignedIntegerType(type)) {
        continue;
      }
      IntegerRange allowed = ranges.rangeOfType(type);
      if (allowed == null) {
        continue;
      }
      IntegerRange value =
          node instanceof CBinaryExpression binary
              ? ranges.resultOfOperation(binary)
              : ranges.resultOfOperation((CUnaryExpression) node);
      result.add(new Operation(node, type, allowed, value));
    }
    return result.build();
  }

  /**
   * The type that the given AST node is computed in, or {@code null} if the node is not an
   * operation that can overflow.
   */
  private static @Nullable CType calculationTypeOf(CAstNode pNode) {
    return switch (pNode) {
      case CBinaryExpression binary -> binary.getCalculationType();
      case CUnaryExpression unary -> unary.getExpressionType();
      default -> null;
    };
  }
}
