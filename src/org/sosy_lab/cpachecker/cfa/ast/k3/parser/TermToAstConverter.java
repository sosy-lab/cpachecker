// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import com.google.common.collect.FluentIterable;
import java.nio.file.Path;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IDTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ApplicationTermContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.VariableTermContext;

class TermToAstConverter extends AbstractAntlrToAstConverter<K3Term> {
  public TermToAstConverter(K3Scope pScope, Path pFilePath) {
    super(pScope, pFilePath);
  }

  public TermToAstConverter(K3Scope pScope) {
    super(pScope);
  }

  @Override
  public K3Term visitVariableTerm(VariableTermContext ctx) {
    return new K3IDTerm(scope.getVariable(ctx.getText()), fileLocationFromContext(ctx));
  }

  @Override
  public K3Term visitApplicationTerm(ApplicationTermContext ctx) {
    return new K3SymbolApplicationTerm(
        ctx.getChild(1).toString(),
        FluentIterable.from(ctx.term())
            .transform(termContext -> Objects.requireNonNull(termContext).accept(this))
            .toList(),
        fileLocationFromContext(ctx));
  }
}
