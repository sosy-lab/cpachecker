// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSelectTraceCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;

public final class SvLibViolationWitness implements SvLibWitness {
  @Serial private static final long serialVersionUID = -9007403248850000016L;
  private final FileLocation fileLocation;
  private final ImmutableList<SvLibSetInfoCommand> metadataCommands;
  private final ImmutableList<SvLibSelectTraceCommand> selectTraceCommands;

  public SvLibViolationWitness(
      FileLocation pFileLocation,
      List<SvLibSetInfoCommand> pMetadataCommands,
      List<SvLibSelectTraceCommand> pSelectTraceCommands) {
    fileLocation = pFileLocation;
    metadataCommands = ImmutableList.copyOf(pMetadataCommands);
    selectTraceCommands = ImmutableList.copyOf(pSelectTraceCommands);
  }

  public ImmutableList<SvLibSetInfoCommand> getMetadataCommands() {
    return metadataCommands;
  }

  public ImmutableList<SvLibSelectTraceCommand> getSelectTraceCommands() {
    return selectTraceCommands;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "("
        + Joiner.on(System.lineSeparator())
            .join(
                FluentIterable.concat(metadataCommands, selectTraceCommands)
                    .transform(SvLibParsingAstNode::toASTString)
                    .toList())
        + ")";
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    int result = 7;
    result = 31 * result + metadataCommands.hashCode();
    result = 31 * result + selectTraceCommands.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibViolationWitness other
        && metadataCommands.equals(other.metadataCommands)
        && selectTraceCommands.equals(other.selectTraceCommands);
  }
}
