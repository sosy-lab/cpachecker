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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;

public final class SvLibCorrectnessWitness implements SvLibWitness {
  @Serial private static final long serialVersionUID = 8992655061364998768L;
  private final FileLocation fileLocation;
  private final ImmutableList<SvLibSetInfoCommand> metadataCommands;
  private final ImmutableList<SmtLibCommand> smtLibCommands;
  private final ImmutableList<SvLibAnnotateTagCommand> annotateTagCommands;

  public SvLibCorrectnessWitness(
      FileLocation pFileLocation,
      List<SvLibSetInfoCommand> pMetadataCommands,
      List<SmtLibCommand> pSmtLibCommands,
      List<SvLibAnnotateTagCommand> pAnnotateTagCommands) {
    fileLocation = pFileLocation;
    metadataCommands = ImmutableList.copyOf(pMetadataCommands);
    smtLibCommands = ImmutableList.copyOf(pSmtLibCommands);
    annotateTagCommands = ImmutableList.copyOf(pAnnotateTagCommands);
  }

  public ImmutableList<SvLibSetInfoCommand> getMetadataCommands() {
    return metadataCommands;
  }

  public ImmutableList<SmtLibCommand> getSmtLibCommands() {
    return smtLibCommands;
  }

  public ImmutableList<SvLibAnnotateTagCommand> getAnnotateTagCommands() {
    return annotateTagCommands;
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
                FluentIterable.concat(metadataCommands, smtLibCommands, annotateTagCommands)
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
    result = 31 * result + smtLibCommands.hashCode();
    result = 31 * result + annotateTagCommands.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibCorrectnessWitness other
        && metadataCommands.equals(other.metadataCommands)
        && smtLibCommands.equals(other.smtLibCommands)
        && annotateTagCommands.equals(other.annotateTagCommands);
  }
}
