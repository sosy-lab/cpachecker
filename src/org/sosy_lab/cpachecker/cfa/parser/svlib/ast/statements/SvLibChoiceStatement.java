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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibChoiceStatement extends SvLibControlFlowStatement {
  @Serial private static final long serialVersionUID = 6120083959916315980L;
  private final ImmutableList<SvLibStatement> choices;

  public SvLibChoiceStatement(
      List<SvLibStatement> pChoices,
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    choices = ImmutableList.copyOf(pChoices);
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
    return "(choice"
        + Joiner.on(" ").join(choices.stream().map(SvLibStatement::toASTString).toList())
        + ")";
  }

  public ImmutableList<SvLibStatement> getChoices() {
    return choices;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + choices.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibChoiceStatement other
        && choices.equals(other.choices)
        && super.equals(other);
  }
}
