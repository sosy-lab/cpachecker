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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNodeVisitor;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagProperty;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagReference;

public final class SvLibSequenceStatement extends SvLibControlFlowStatement {
  @Serial private static final long serialVersionUID = 8121014592707608414L;
  private final ImmutableList<SvLibStatement> statements;
  private final FileLocation fileLocation;

  public SvLibSequenceStatement(
      List<SvLibStatement> pStatements,
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    statements = ImmutableList.copyOf(pStatements);
    fileLocation = pFileLocation;
  }

  public ImmutableList<SvLibStatement> getStatements() {
    return statements;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(" + Joiner.on(" ) ( ").join(statements) + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibSequenceStatement other
        && super.equals(other)
        && statements.equals(other.statements);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + statements.hashCode();
    return result;
  }
}
