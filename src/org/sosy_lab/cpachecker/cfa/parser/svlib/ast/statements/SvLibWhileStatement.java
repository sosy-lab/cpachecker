// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibWhileStatement extends SvLibControlFlowStatement {

  @Serial private static final long serialVersionUID = 8317989637505431967L;
  private final SvLibTerm condition;
  private final SvLibStatement body;

  public SvLibWhileStatement(
      SvLibTerm pCondition,
      SvLibStatement pBody,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      FileLocation pFileLocation) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    condition = pCondition;
    body = pBody;
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
    return "(while " + condition.toASTString() + " " + body.toASTString() + ")";
  }

  public SvLibTerm getCondition() {
    return condition;
  }

  public SvLibStatement getBody() {
    return body;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + condition.hashCode();
    result = prime * result + body.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibWhileStatement other
        && super.equals(other)
        && condition.equals(other.condition)
        && body.equals(other.body);
  }
}
