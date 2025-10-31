// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ApplicationTermContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.QualIdentifierTermContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.Qual_identiferContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.SpecConstantTermContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.Spec_constantContext;

class TermToAstConverter extends AbstractAntlrToAstConverter<K3Term> {
  public TermToAstConverter(K3Scope pScope, Path pFilePath) {
    super(pScope, pFilePath);
  }

  public TermToAstConverter(K3Scope pScope) {
    super(pScope);
  }

  @Override
  public K3Term visitQualIdentifierTerm(QualIdentifierTermContext ctx) {
    String identifier = ctx.qual_identifer().getText();
    FileLocation fileLocation = fileLocationFromContext(ctx);

    // We handle the case that it is a pre-defined constant like true or false
    if (ImmutableSet.of("true", "false").contains(identifier)) {
      return new K3BooleanConstantTerm(identifier.equals("true"), fileLocation);
    }

    // We now handle the general case of a variable
    return new K3IdTerm(scope.getVariable(identifier), fileLocation);
  }

  @Override
  public K3Term visitSpecConstantTerm(SpecConstantTermContext ctx) {
    Spec_constantContext specConstantContext = ctx.spec_constant();
    if (specConstantContext.numeral() != null) {
      return new K3IntegerConstantTerm(
          new BigInteger(specConstantContext.numeral().getText()), fileLocationFromContext(ctx));
    } else {
      throw new IllegalArgumentException(
          "The constant %s is currently not supported.".formatted(ctx.getText()));
    }
  }

  private K3VariableDeclaration getVariableDeclarationForSymbol(
      String pSymbol, Set<SmtLibLogic> pLogics, List<K3Term> pArguments) {

    if (pLogics.contains(SmtLibLogic.LIA)) {
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
        case "-" -> {
          return SmtLibTheoryDeclarations.INT_MINUS;
        }
        case "+" -> {
          return SmtLibTheoryDeclarations.intAddition(pArguments.size());
        }
      }
    }

    // Match the core logic of SMT-LIB
    switch (pSymbol) {
      case "not" -> {
        return SmtLibTheoryDeclarations.BOOL_NEGATION;
      }
    }

    throw new IllegalArgumentException(
        "Unsupported logic for the resolution of symbol: " + pSymbol);
  }

  @Override
  public K3Term visitApplicationTerm(ApplicationTermContext ctx) {
    Qual_identiferContext functionSymbolContext = ctx.qual_identifer();
    List<K3Term> arguments =
        transformedImmutableListCopy(
            ctx.term(), termContext -> Objects.requireNonNull(termContext).accept(this));
    return new K3SymbolApplicationTerm(
        new K3IdTerm(
            getVariableDeclarationForSymbol(
                functionSymbolContext.getText(), scope.getLogics(), arguments),
            fileLocationFromContext(functionSymbolContext)),
        arguments,
        fileLocationFromContext(ctx));
  }
}
