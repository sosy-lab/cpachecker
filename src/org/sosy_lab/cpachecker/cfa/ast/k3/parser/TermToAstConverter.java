// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3NumeralConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ApplicationTermContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.QualIdentifierTermContext;
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
    return new K3IdTerm(scope.getVariable(ctx.getText()), fileLocationFromContext(ctx));
  }

  @Override
  public K3Term visitSpecConstantTerm(SpecConstantTermContext ctx) {
    Spec_constantContext specConstantContext = ctx.spec_constant();
    if (specConstantContext.numeral() != null) {
      return new K3NumeralConstantTerm(
          new BigInteger(specConstantContext.numeral().getText()), fileLocationFromContext(ctx));
    } else {
      throw new IllegalArgumentException(
          "The constant %s is currently not supported.".formatted(ctx.getText()));
    }
  }

  @Override
  public K3Term visitApplicationTerm(ApplicationTermContext ctx) {
    return new K3SymbolApplicationTerm(
        ctx.getChild(1).getText(),
        transformedImmutableListCopy(
            ctx.term(), termContext -> Objects.requireNonNull(termContext).accept(this)),
        fileLocationFromContext(ctx));
  }
}
