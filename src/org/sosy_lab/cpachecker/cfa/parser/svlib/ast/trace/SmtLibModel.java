// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibDefineFunCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibDefineFunRecCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibDefineFunsRecCommand;

public final class SmtLibModel extends SvLibTraceComponent {
  @Serial private static final long serialVersionUID = -7273985178330182242L;

  private final ImmutableList<SmtLibDefineFunCommand> smtLibDefineFunCommands;
  private final ImmutableList<SmtLibDefineFunRecCommand> smtLibDefineFunRecCommands;
  private final ImmutableList<SmtLibDefineFunsRecCommand> smtLibDefineFunsRecCommands;

  public SmtLibModel(
      List<SmtLibDefineFunCommand> pSmtLibDefineFunCommands,
      List<SmtLibDefineFunRecCommand> pSmtLibDefineFunRecCommands,
      List<SmtLibDefineFunsRecCommand> pSmtLibDefineFunsRecCommands,
      FileLocation pFileLocation) {
    super(pFileLocation);
    smtLibDefineFunCommands = ImmutableList.copyOf(pSmtLibDefineFunCommands);
    smtLibDefineFunRecCommands = ImmutableList.copyOf(pSmtLibDefineFunRecCommands);
    smtLibDefineFunsRecCommands = ImmutableList.copyOf(pSmtLibDefineFunsRecCommands);
  }

  @Override
  <R, X extends Exception> R accept(SvLibTraceComponentVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString() {
    return Joiner.on(" ")
        .join(
            FluentIterable.concat(
                    smtLibDefineFunCommands,
                    smtLibDefineFunRecCommands,
                    smtLibDefineFunsRecCommands)
                .transform(SvLibParsingAstNode::toASTString));
  }

  public List<SmtLibDefineFunRecCommand> getSmtLibDefineFunRecCommands() {
    return smtLibDefineFunRecCommands;
  }

  public List<SmtLibDefineFunsRecCommand> getSmtLibDefineFunsRecCommands() {
    return smtLibDefineFunsRecCommands;
  }

  public List<SmtLibDefineFunCommand> getSmtLibDefineFunCommands() {
    return smtLibDefineFunCommands;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + smtLibDefineFunCommands.hashCode();
    result = prime * result + smtLibDefineFunRecCommands.hashCode();
    result = prime * result + smtLibDefineFunsRecCommands.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SmtLibModel other
        && smtLibDefineFunCommands.equals(other.smtLibDefineFunCommands)
        && smtLibDefineFunRecCommands.equals(other.smtLibDefineFunRecCommands)
        && smtLibDefineFunsRecCommands.equals(other.smtLibDefineFunsRecCommands);
  }
}
