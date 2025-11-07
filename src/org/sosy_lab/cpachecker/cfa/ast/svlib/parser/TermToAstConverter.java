// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.parser;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.builder.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.ApplicationTermContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.QualIdentifierTermContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.Qual_identiferContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.SpecConstantTermContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.Spec_constantContext;

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
    return new SvLibIdTerm(scope.getVariable(identifier), fileLocation);
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

  private SvLibVariableDeclaration getVariableDeclarationForSymbol(
      String pSymbol, Set<SmtLibLogic> pLogics, List<SvLibTerm> pArguments) {

    if (FluentIterable.from(pLogics).anyMatch(SmtLibLogic::containsIntegerArithmetic)) {
      switch (pSymbol) {
        case "=" -> {
          return SmtLibTheoryDeclarations.INT_EQUALITY;
        }
        case "<" -> {
          return SmtLibTheoryDeclarations.INT_LESS_THAN;
        }
        case "<=" -> {
          return SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN;
        }
        case ">" -> {
          return SmtLibTheoryDeclarations.INT_GREATER_THAN;
        }
        case ">=" -> {
          return SmtLibTheoryDeclarations.INT_GREATER_EQUAL_THAN;
        }
        case "-" -> {
          return SmtLibTheoryDeclarations.INT_MINUS;
        }
        case "+" -> {
          return SmtLibTheoryDeclarations.intAddition(pArguments.size());
        }
      }
    }

    // Match Non-Linear Integer Arithmetic logic
    if (FluentIterable.from(pLogics).anyMatch(SmtLibLogic::containsNonLinearIntegerArithmetic)) {
      switch (pSymbol) {
        case "mod" -> {
          return SmtLibTheoryDeclarations.INT_MOD;
        }
      }
    }

    // Match the core logic of SMT-LIB
    switch (pSymbol) {
      case "not" -> {
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
