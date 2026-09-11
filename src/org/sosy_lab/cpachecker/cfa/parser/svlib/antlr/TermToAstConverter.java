// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ApplicationTermContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.QualIdentifierTermContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Qual_identiferContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SpecConstantTermContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Spec_constantContext;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibAnyType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;

class TermToAstConverter extends AbstractAntlrToAstConverter<SvLibTerm> {
  public TermToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
  }

  public TermToAstConverter(SvLibScope pScope) {
    super(pScope);
  }

  @Override
  public SvLibTerm visitQualIdentifierTerm(QualIdentifierTermContext ctx) {
    String identifier = ctx.qual_identifer().getText();
    FileLocation fileLocation = fileLocationFromContext(ctx);

    // We handle the case that it is a pre-defined constant like true or false
    if (ImmutableSet.of("true", "false").contains(identifier)) {
      return new SvLibBooleanConstantTerm(identifier.equals("true"), fileLocation);
    }

    // We now handle the general case of a variable
    return new SvLibIdTerm(scope.getVariable(identifier).toSimpleDeclaration(), fileLocation);
  }

  @Override
  public SvLibTerm visitSpecConstantTerm(SpecConstantTermContext ctx) {
    Spec_constantContext specConstantContext = ctx.spec_constant();
    if (specConstantContext.numeral() != null) {
      return new SvLibIntegerConstantTerm(
          new BigInteger(specConstantContext.numeral().getText()), fileLocationFromContext(ctx));
    } else {
      throw new IllegalArgumentException(
          "The constant %s is currently not supported.".formatted(ctx.getText()));
    }
  }

  private SvLibFunctionDeclaration getVariableDeclarationForSymbol(
      String pSymbol, Set<SmtLibLogic> pLogics, List<SvLibTerm> pArguments) {

    // Match Integer Arithmetic logic
    if (FluentIterable.from(pLogics).anyMatch(SmtLibLogic::containsIntegerArithmetic)) {
      switch (pSymbol) {
        case "=" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_EQUALITY;
        }
        case "<" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_LESS_THAN;
        }
        case "<=" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN;
        }
        case ">" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_GREATER_THAN;
        }
        case ">=" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_GREATER_EQUAL_THAN;
        }
        case "-" -> {
          return SmtLibTheoryDeclarations.intSubtraction(pArguments.size());
        }
        case "+" -> {
          return SmtLibTheoryDeclarations.intAddition(pArguments.size());
        }
        case "*" -> {
          // Only a special case of multiplication is actually supported in LIA
          // but we do not care, because we either way use all theories together.
          // In case this becomes a problem, we can introduce a separate check for LIA here.
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_MULTIPLICATION;
        }
      }
    }

    // Match Non-Linear Integer Arithmetic logic
    if (FluentIterable.from(pLogics).anyMatch(SmtLibLogic::containsNonLinearIntegerArithmetic)) {
      switch (pSymbol) {
        case "mod" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_MOD;
        }
        case "div" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_DIV;
        }
      }
    }

    // Match the core logic of SMT-LIB
    switch (pSymbol) {
      case "not" -> {
        Verify.verify(pArguments.size() == 1);
        return SmtLibTheoryDeclarations.BOOL_NEGATION;
      }
      case "and" -> {
        return SmtLibTheoryDeclarations.boolConjunction(pArguments.size());
      }
      case "or" -> {
        return SmtLibTheoryDeclarations.boolDisjunction(pArguments.size());
      }
      case "=>" -> {
        return SmtLibTheoryDeclarations.boolImplication(pArguments.size());
      }
    }

    // Match all array stuff
    switch (pSymbol) {
      case "select" -> {
        Verify.verify(pArguments.size() == 2);
        if (pArguments.getFirst().getExpressionType() instanceof SvLibSmtLibArrayType arrayType) {

          return SmtLibTheoryDeclarations.arraySelect(
              arrayType.getKeysType(), arrayType.getValuesType());
        } else if (pArguments.getFirst().getExpressionType().equals(new SvLibAnyType())) {
          return SmtLibTheoryDeclarations.arraySelect(new SvLibAnyType(), new SvLibAnyType());
        }

        throw new IllegalArgumentException(
            "The first argument of 'select' must be of array type, but was: "
                + pArguments.getFirst().getExpressionType());
      }
      case "store" -> {
        Verify.verify(pArguments.size() == 3);
        return SmtLibTheoryDeclarations.arrayStore(
            (SvLibSmtLibType) pArguments.get(1).getExpressionType(),
            (SvLibSmtLibType) pArguments.get(2).getExpressionType());
      }
    }

    throw new IllegalArgumentException(
        "Unsupported logic for the resolution of symbol: " + pSymbol);
  }

  @Override
  public SvLibTerm visitApplicationTerm(ApplicationTermContext ctx) {
    Qual_identiferContext functionSymbolContext = ctx.qual_identifer();
    List<SvLibTerm> arguments =
        transformedImmutableListCopy(
            ctx.term(), termContext -> Objects.requireNonNull(termContext).accept(this));
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(
            getVariableDeclarationForSymbol(
                functionSymbolContext.getText(), scope.getLogics(), arguments),
            fileLocationFromContext(functionSymbolContext)),
        arguments,
        fileLocationFromContext(ctx));
  }
}
