// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ParametricSortContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.SimpleSortContext;

public class K3SortToAstTypeConverter extends AbstractAntlrToAstConverter<K3Type> {

  public K3SortToAstTypeConverter(K3Scope pScope, Path pFilePath) {
    super(pScope, pFilePath);
  }

  public K3SortToAstTypeConverter(K3Scope pScope) {
    super(pScope);
  }

  @Override
  public K3Type visitSimpleSort(SimpleSortContext pContext) {
    return scope.getTypeForName(pContext.identifier().getText());
  }

  @Override
  public K3Type visitParametricSort(ParametricSortContext pContext) {
    throw new UnsupportedOperationException("Parametric sorts are not supported yet.");
  }
}
