// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslQuantifiedPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslQuantifiedPredicate.AcslQuantifier;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.BinaryPredicateContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.BinderContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.BindersContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.ComparisonPredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.ExistentialQuantificationPredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LogicalFalsePredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LogicalTruePredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.OldPredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.ParenthesesPredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.PredicateApplicationPredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.PredicateTermContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.PredicateVariableContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TermContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TernaryConditionPredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.UnaryPredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.UniversalQuantificationPredContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.ValidPredContext;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

class PredicateToAstConverter extends AbstractAntlrToAstConverter<AcslPredicate> {

  private final TermToAstConverter termConverter;
  private final MemoryLocationSetToAstConverter memoryLocationSetConverter;
  private final LabelToAstConverter labelConverter;
  private final TypeToAstConverter typeConverter;

  PredicateToAstConverter(
      CProgramScope pCProgramScope, AcslScope pAcslScope, FileLocation pInitialFileLocation) {
    super(pCProgramScope, pAcslScope, pInitialFileLocation);
    termConverter = new TermToAstConverter(pCProgramScope, pAcslScope, pInitialFileLocation);
    memoryLocationSetConverter =
        new MemoryLocationSetToAstConverter(pCProgramScope, pAcslScope, pInitialFileLocation);
    labelConverter = new LabelToAstConverter(pCProgramScope, pAcslScope, pInitialFileLocation);
    typeConverter = new TypeToAstConverter(getCProgramScope(), getAcslScope());
  }

  @Override
  public AcslPredicate visitOldPred(OldPredContext ctx) {
    // The second child is the term inside the old, the others are
    // '\old' '(' term ')'
    AcslPredicate expression = visit(ctx.getChild(2));

    return new AcslOldPredicate(fileLocationFromContext(ctx), expression);
  }

  @Override
  public AcslPredicate visitLogicalTruePred(LogicalTruePredContext ctx) {
    return new AcslBooleanLiteralPredicate(fileLocationFromContext(ctx), true);
  }

  @Override
  public AcslPredicate visitLogicalFalsePred(LogicalFalsePredContext ctx) {
    return new AcslBooleanLiteralPredicate(fileLocationFromContext(ctx), false);
  }

  @Override
  public AcslPredicate visitTernaryConditionPred(TernaryConditionPredContext ctx) {
    // The parsing gives the following structure:
    // [condition, '?', if_true, ':', if_false]
    AcslPredicate condition = visit(ctx.getChild(0));
    AcslPredicate ifTrue = visit(ctx.getChild(2));
    AcslPredicate ifFalse = visit(ctx.getChild(4));

    return new AcslTernaryPredicate(fileLocationFromContext(ctx), condition, ifTrue, ifFalse);
  }

  @Override
  public AcslPredicate visitParenthesesPred(ParenthesesPredContext ctx) {
    return visit(ctx.getChild(1));
  }

  @Override
  public AcslPredicate visitUnaryPred(UnaryPredContext ctx) {
    AcslUnaryPredicateOperator operator = AcslUnaryPredicateOperator.of(ctx.getChild(0).getText());
    AcslPredicate expression = visit(ctx.getChild(1));

    return new AcslUnaryPredicate(fileLocationFromContext(ctx), expression, operator);
  }

  @Override
  public AcslPredicate visitPredicateTerm(PredicateTermContext ctx) {
    return termAsPredicate(termConverter.visit(ctx.getChild(0)), ctx);
  }

  @Override
  public AcslPredicate visitPredicateVariable(PredicateVariableContext ctx) {
    String name = ctx.getText();
    AcslPredicateDeclaration predicate = getAcslScope().lookupPredicate(name);
    if (predicate == null) {
      return termAsPredicate(
          new AcslIdTerm(fileLocationFromContext(ctx), getVariableDeclarationForName(name)), ctx);
    }
    if (!predicate.getParameters().isEmpty()) {
      throw new AcslParseRuntimeException("Predicate " + name + " is applied to no arguments");
    }
    return new AcslIdPredicate(fileLocationFromContext(ctx), predicate);
  }

  /** A boolean term as predicate holds if it is true, and any other term if it is not zero. */
  private AcslPredicate termAsPredicate(AcslTerm pTerm, ParserRuleContext pContext) {
    FileLocation location = fileLocationFromContext(pContext);
    if (pTerm.getExpressionType().equals(AcslBuiltinLogicType.BOOLEAN)) {
      return new AcslBinaryTermPredicate(
          location,
          pTerm,
          new AcslBooleanLiteralTerm(location, true),
          AcslBinaryTermPredicateOperator.EQUALS);
    }
    return new AcslBinaryTermPredicate(
        location,
        pTerm,
        new AcslIntegerLiteralTerm(location, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
        AcslBinaryTermPredicateOperator.NOT_EQUALS);
  }

  @Override
  public AcslPredicate visitValidPred(ValidPredContext ctx) {
    // The parsing gives the following structure:
    // [\valid, '(', term, ')'] or [\valid, label,  '(', term, ')']
    if (ctx.getChildCount() == 4) {
      // We are in the case: [\valid, '(', term, ')']
      AcslMemoryLocationSet memoryLocationSet = memoryLocationSetConverter.visit(ctx.getChild(2));
      return new AcslValidPredicate(fileLocationFromContext(ctx), memoryLocationSet);
    }
    // We are in the case: [\valid, ['{', label, '}'],  '(', term, ')']
    verify(
        ctx.getChildCount() == 5, "Unexpected number of children when creating a valid predicate");
    AcslLabel label = labelConverter.visit(ctx.getChild(1).getChild(1));
    AcslMemoryLocationSet memoryLocationSet = memoryLocationSetConverter.visit(ctx.getChild(3));
    return new AcslValidPredicate(fileLocationFromContext(ctx), memoryLocationSet, label);
  }

  @Override
  public AcslPredicate visitBinaryPredicate(BinaryPredicateContext ctx) {
    AcslPredicate leftExpression = visit(ctx.getChild(0));
    AcslBinaryPredicateOperator operator =
        AcslBinaryPredicateOperator.of(ctx.getChild(1).getText());
    AcslPredicate rightExpression = visit(ctx.getChild(2));

    return new AcslBinaryPredicate(
        fileLocationFromContext(ctx), leftExpression, rightExpression, operator);
  }

  @Override
  public AcslPredicate visitComparisonPred(ComparisonPredContext ctx) {
    // In ACSL it is allowed to write something like `0 <= i <= n`, which we get as the list
    // [`0`, `<=`, `i`, `<=`, `n`]
    AcslPredicate currentExpression = null;
    int amountOfChildren = ctx.getChildCount();
    int i;
    for (i = 0; i + 1 < amountOfChildren; i += 2) {
      verify(
          i + 3 <= amountOfChildren,
          "Unexpected number of children when creating comparison operator");

      AcslTerm leftTerm = termConverter.visit(ctx.getChild(i));
      AcslBinaryTermPredicateOperator operator =
          AcslBinaryTermPredicateOperator.of(ctx.getChild(i + 1).getText());
      AcslTerm righTerm = termConverter.visit(ctx.getChild(i + 2));

      AcslPredicate newComparison =
          new AcslBinaryTermPredicate(fileLocationFromContext(ctx), leftTerm, righTerm, operator);

      if (currentExpression == null) {
        currentExpression = newComparison;
      } else {
        currentExpression =
            new AcslBinaryPredicate(
                fileLocationFromContext(ctx),
                newComparison,
                currentExpression,
                AcslBinaryPredicateOperator.AND);
      }
    }

    verify(
        currentExpression != null, "Expected at least one comparison for a comparison predicate");
    verify(
        i + 1 == amountOfChildren,
        "Not all children were considered during parsing a comparison predicate");

    return currentExpression;
  }

  @Override
  public AcslPredicate visitPredicateApplicationPred(PredicateApplicationPredContext ctx) {
    String name = ctx.ident().getText();
    AcslPredicateDeclaration declaration = getPredicateDeclarationForName(name);
    ImmutableList.Builder<AcslTerm> termBuilder = ImmutableList.builder();
    for (TermContext t : ctx.term()) {
      AcslTerm term = termConverter.visit(t);
      termBuilder.add(term);
    }
    ImmutableList<AcslTerm> parameters = termBuilder.build();
    return new AcslPredicateApplicationPredicate(
        fileLocationFromContext(ctx), declaration, parameters);
  }

  private AcslPredicate handleQuantifiedPredicate(
      String pQuantifier, BindersContext pBindersContext, ParseTree pPredicate) {
    // The parsing gives the following structure:
    // [quantifier, binders, ';', predicate ]

    ImmutableList.Builder<AcslParameterDeclaration> parameters = ImmutableList.builder();
    for (BinderContext binder : pBindersContext.binder()) {
      AcslType type = typeConverter.visit(binder.getChild(0));
      for (int i = 1; i < binder.getChildCount(); i += 2) {
        // TODO: This currently does not handle cases where the variables have pointers or square
        // brackets
        String variableName = binder.getChild(i).getText();
        if (variableName.contains("[")
            || variableName.contains("]")
            || variableName.contains("*")
            || variableName.contains("(")
            || variableName.contains(")")) {
          throw new AcslParseRuntimeException(
              "Pointer or array types are not supported in the binders list yes");
        }

        parameters.add(
            new AcslParameterDeclaration(fileLocationFromContext(binder), type, variableName));
      }
    }

    List<AcslParameterDeclaration> binders = parameters.build();
    // The binders are only visible in the quantified predicate
    AcslPredicate predicate =
        new PredicateToAstConverter(
                getCProgramScope(),
                getAcslScope().withDeclarations(binders),
                getInitialFileLocation())
            .visit(pPredicate);

    return new AcslQuantifiedPredicate(
        fileLocationFromContext(pBindersContext),
        AcslQuantifier.of(pQuantifier),
        binders,
        predicate);
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
