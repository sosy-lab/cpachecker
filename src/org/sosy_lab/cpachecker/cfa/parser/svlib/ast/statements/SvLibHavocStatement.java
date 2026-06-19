// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;

public final class SvLibHavocStatement extends SvLibStatement {
  @Serial private static final long serialVersionUID = 3102375106958425786L;
  private final ImmutableList<SvLibSimpleParsingDeclaration> variables;

  public SvLibHavocStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      List<SvLibSimpleParsingDeclaration> pVariables) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    variables = ImmutableList.copyOf(pVariables);
  }

  public ImmutableList<SvLibSimpleParsingDeclaration> getVariables() {
    return variables;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTStringWithoutTags() {
    return "(havoc "
        + Joiner.on(" ").join(variables.stream().map(SvLibParsingAstNode::toASTString).toList())
        + ")";
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + variables.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibHavocStatement other
        && super.equals(other)
        && variables.equals(other.variables);
  }
}
