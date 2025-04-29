// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AntlrToInternalNotImplementedException;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.BinaryPredicateContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.BinderContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.BindersContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ComparisonPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ExistentialQuantificationPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicalFalsePredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicalTruePredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.OldPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ParenthesesPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.PredicateTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.TernaryConditionPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.UnaryPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.UniversalQuantificationPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ValidPredContext;

class AntrlPredicateToPredicateConverter extends AntlrToInternalAbstractConverter<AcslPredicate> {

  private final AntlrTermToTermConverter antrlToTermConverter;
  private final AntlrTsetToMemorySetConverter antrlTsetToMemorySetConverter;
  private final AntlrLabelToLabelConverter labelConverter;
  private final AntrlTypeExpressionToTypeConverter antrlTypeExpressionToTypeConverter;

  protected AntrlPredicateToPredicateConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
    antrlToTermConverter = new AntlrTermToTermConverter(pCProgramScope, pAcslScope);
    antrlTsetToMemorySetConverter = new AntlrTsetToMemorySetConverter(pCProgramScope, pAcslScope);
    labelConverter = new AntlrLabelToLabelConverter(pCProgramScope, pAcslScope);
    antrlTypeExpressionToTypeConverter =
        new AntrlTypeExpressionToTypeConverter(getCProgramScope(), getAcslScope());
  }

  @Override
  public AcslPredicate visitOldPred(OldPredContext ctx) {
    // The second child is the term inside the old, the others are
    // '\old' '(' term ')'
    AcslPredicate expression = visit(ctx.getChild(2));

    return new AcslOldPredicate(FileLocation.DUMMY, expression);
  }

  @Override
  public AcslPredicate visitLogicalTruePred(LogicalTruePredContext ctx) {
    return new AcslBooleanLiteralPredicate(FileLocation.DUMMY, true);
  }

  @Override
  public AcslPredicate visitLogicalFalsePred(LogicalFalsePredContext ctx) {
    return new AcslBooleanLiteralPredicate(FileLocation.DUMMY, false);
  }

  @Override
  public AcslPredicate visitTernaryConditionPred(TernaryConditionPredContext ctx) {
    // The parsing gives the following structure:
    // [condition, '?', if_true, ':', if_false]
    AcslPredicate condition = visit(ctx.getChild(0));
    AcslPredicate ifTrue = visit(ctx.getChild(2));
    AcslPredicate ifFalse = visit(ctx.getChild(4));

    return new AcslTernaryPredicate(FileLocation.DUMMY, condition, ifTrue, ifFalse);
  }

  @Override
  public AcslPredicate visitParenthesesPred(ParenthesesPredContext ctx) {
    return visit(ctx.getChild(1));
  }

  @Override
  public AcslPredicate visitUnaryPred(UnaryPredContext ctx) {
    AcslUnaryExpressionOperator operator =
        AcslUnaryExpressionOperator.of(ctx.getChild(0).getText());
    AcslPredicate expression = visit(ctx.getChild(1));

    return new AcslUnaryPredicate(FileLocation.DUMMY, expression, operator);
  }

  @Override
  public AcslPredicate visitPredicateTerm(PredicateTermContext ctx) {
    AcslTerm term = antrlToTermConverter.visit(ctx.getChild(0));
    return new AcslBinaryTermPredicate(
        FileLocation.DUMMY,
        term,
        new AcslIntegerLiteralTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
        AcslBinaryTermExpressionOperator.EQUALS);
  }

  @Override
  public AcslPredicate visitValidPred(ValidPredContext ctx) {
    // The parsing gives the following structure:
    // [\valid, '(', term, ')'] or [\valid, label,  '(', term, ')']
    if (ctx.getChildCount() == 4) {
      // We are in the case: [\valid, '(', term, ')']
      AcslMemoryLocationSet memoryLocationSet =
          antrlTsetToMemorySetConverter.visit(ctx.getChild(2));
      return new AcslValidPredicate(FileLocation.DUMMY, memoryLocationSet);
    } else if (ctx.getChildCount() == 5) {
      // We are in the case: [\valid, ['{', label, '}'],  '(', term, ')']
      AcslLabel label = labelConverter.visit(ctx.getChild(1).getChild(1));
      AcslMemoryLocationSet memoryLocationSet =
          antrlTsetToMemorySetConverter.visit(ctx.getChild(3));
      return new AcslValidPredicate(FileLocation.DUMMY, memoryLocationSet, label);
    } else {
      throw new RuntimeException("Unexpected number of children when creating a valid predicate");
    }
  }

  @Override
  public AcslPredicate visitBinaryPredicate(BinaryPredicateContext ctx) {
    AcslPredicate leftExpression = visit(ctx.getChild(0));
    AcslBinaryPredicateExpressionOperator operator =
        AcslBinaryPredicateExpressionOperator.of(ctx.getChild(1).getText());
    AcslPredicate rightExpression = visit(ctx.getChild(2));

    return new AcslBinaryPredicate(FileLocation.DUMMY, leftExpression, rightExpression, operator);
  }

  @Override
  public AcslPredicate visitComparisonPred(ComparisonPredContext ctx) {
    // In ACSL it is allowed to write something like `0 <= i <= n`, which we get as the list
    // [`0`, `<=`, `i`, `<=`, `n`]
    AcslPredicate currentExpression = null;
    int amountOfChildren = ctx.getChildCount();
    int i;
    for (i = 0; i + 1 < amountOfChildren; i += 2) {
      if (i + 3 < amountOfChildren) {
        throw new RuntimeException(
            "Unexpected number of children when creating comparison operator");
      }

      AcslTerm leftTerm = antrlToTermConverter.visit(ctx.getChild(i));
      AcslBinaryTermExpressionOperator operator =
          AcslBinaryTermExpressionOperator.of(ctx.getChild(i + 1).getText());
      AcslTerm righTerm = antrlToTermConverter.visit(ctx.getChild(i + 2));

      AcslPredicate newComparison =
          new AcslBinaryTermPredicate(FileLocation.DUMMY, leftTerm, righTerm, operator);

      if (currentExpression == null) {
        currentExpression = newComparison;
      } else {
        currentExpression =
            new AcslBinaryPredicate(
                FileLocation.DUMMY,
                currentExpression,
                newComparison,
                AcslBinaryPredicateExpressionOperator.AND);
      }
    }

    if (currentExpression == null) {
      throw new RuntimeException("Expected at least one comparison for a comparison predicate");
    }

    if (i + 1 != amountOfChildren) {
      throw new RuntimeException(
          "Not all children were considered during parsing a comparison predicate");
    }

    return currentExpression;
  }

  private AcslPredicate handleQuantifiedPredicate(
      String pQuantifier, BindersContext pBindersContext, ParseTree pPredicate) {
    // The parsing gives the following structure:
    // [quantifier, binders, ';', predicate ]

    // TODO: A visitor would clearly be better here, but it is probably too much overhead and I'm
    // still unsure where else I will need something like this to make a refactoring feasible
    ImmutableList.Builder<AcslParameterDeclaration> parameters = ImmutableList.builder();
    for (ParseTree tree :
        FluentIterable.from(pBindersContext.children)
            .filter(node -> !Objects.equals(node.getText(), ","))) {
      if (tree instanceof BinderContext binder) {
        AcslType type = antrlTypeExpressionToTypeConverter.visit(binder.getChild(0));
        for (int i = 1; i < binder.getChildCount(); i += 2) {
          // TODO: This currently does not handle cases where the variables have pointers or square
          // brackets
          String variableName = binder.getChild(i).getText();
          if (variableName.contains("[")
              || variableName.contains("]")
              || variableName.contains("*")
              || variableName.contains("(")
              || variableName.contains(")")) {
            throw new AntlrToInternalNotImplementedException(
                "Pointer or array types are not supported in the binders list yes");
          }

          parameters.add(new AcslParameterDeclaration(FileLocation.DUMMY, type, variableName));
        }
      } else {
        throw new RuntimeException("Expected a binder in the binders list");
      }
    }

    List<AcslParameterDeclaration> binders = parameters.build();
    for (AcslParameterDeclaration binder : binders) {
      getAcslScope().registerDeclaration(binder);
    }

    AcslPredicate predicate = visit(pPredicate);

    if (pQuantifier.equals("\\exists")) {
      return new AcslExistsPredicate(FileLocation.DUMMY, binders, predicate);
    } else if (pQuantifier.equals("\\forall")) {
      return new AcslForallPredicate(FileLocation.DUMMY, binders, predicate);
    } else {
      throw new RuntimeException("Unknown quantifier: " + pQuantifier);
    }
  }

  @Override
  public AcslPredicate visitExistentialQuantificationPred(
      ExistentialQuantificationPredContext ctx) {
    return handleQuantifiedPredicate(
        ctx.getChild(0).getText(), ctx.binders(), ctx.getChild(ctx.getChildCount() - 1));
  }

  @Override
  public AcslPredicate visitUniversalQuantificationPred(UniversalQuantificationPredContext ctx) {
    // The parsing gives the following structure:
    // [\forall, binders, ';', predicate ]
    return handleQuantifiedPredicate(
        ctx.getChild(0).getText(), ctx.binders(), ctx.getChild(ctx.getChildCount() - 1));
  }
}
