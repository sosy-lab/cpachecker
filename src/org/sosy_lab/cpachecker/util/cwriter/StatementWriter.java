// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Preconditions.checkState;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.TargetTreatment;
import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.EmptyStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.FunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.Statement.InlinedFunction;
import org.sosy_lab.cpachecker.util.cwriter.Statement.Label;
import org.sosy_lab.cpachecker.util.cwriter.Statement.SimpleStatement;

public class StatementWriter implements StatementVisitor<IOException>, Closeable {

  private final Appendable sb;
  private int currentIndent = 0;

  /** Whether this writer has been closed. * */
  private boolean closed = false;

  protected StatementWriter(final Appendable pDestination, final TranslatorConfig pConfig)
      throws IOException {
    sb = pDestination;

    if (pConfig.doIncludeHeader()) {
      sb.append("#include <stdio.h>\n");
    }
    if (pConfig.doIncludeHeader() || pConfig.getTargetStrategy() == TargetTreatment.ASSERTFALSE) {
      sb.append("#include <assert.h>\n");
    }
    if (pConfig.getTargetStrategy() == TargetTreatment.VERIFIERERROR) {
      sb.append("extern void __VERIFIER_error();\n");
    }
    sb.append("extern void __VERIFIER_assume();\n");
    sb.append("extern _Bool __VERIFIER_nondet_bool();\n");
  }

  public static StatementWriter getWriter(
      final Appendable pDestination, final TranslatorConfig pConfig) throws IOException {
    if (pConfig.getMetadataOutput() != null) {
      return new StatementWriterWithMetadata(pDestination, pConfig.getMetadataOutput(), pConfig);
    }
    return new StatementWriter(pDestination, pConfig);
  }

  public void write(String pString) throws IOException {
    sb.append(pString).append("\n");
  }

  private void addIndent() throws IOException {
    for (int i = 0; i < currentIndent; i++) {
      // sb.append(" ");
    }
    sb.append(" ");
  }

  private void increaseIndent() {
    checkState(currentIndent >= 0);
    currentIndent += 4;
    checkState(currentIndent >= 0);
  }

  private void decreaseIndent() {
    checkState(currentIndent >= 0);
    currentIndent -= 4;
    checkState(currentIndent >= 0);
  }

  private void addLabelIfNecessary(Statement pS) throws IOException {
    Optional<String> label = pS.getLabelIfUsed();
    if (label.isPresent()) {
      sb.append(label.orElseThrow()).append(":;\n");
    }
  }

  @Override
  public void visit(SimpleStatement pS) throws IOException {
    addLabelIfNecessary(pS);
    addIndent();
    sb.append(pS.getCode());
    sb.append("\n");
  }

  @Override
  public void visit(Label pS) throws IOException {
    // in contrast to the other statements, we do not call 'addLabelIfNecessary',
    // but always add the label below.
    addIndent();
    sb.append(pS.getLabel()).append(":;");
  }

  @Override
  public void visit(FunctionDefinition pS) throws IOException {
    addLabelIfNecessary(pS);
    addIndent();
    sb.append(pS.getFunctionHeader()).append("\n");
    pS.getFunctionBody().accept(this);
    sb.append("\n");
  }

  @Override
  public void visit(EmptyStatement pS) throws IOException {
    addLabelIfNecessary(pS);
  }

  @Override
  public void visit(CompoundStatement pS) throws IOException {
    visitCompound(pS);
  }

  private void visitCompound(CompoundStatement pS) throws IOException {
    addLabelIfNecessary(pS);
    addIndent();
    sb.append("{\n");

    for (Statement statement : pS.getStatements()) {
      int indentBefore = currentIndent;
      increaseIndent();
      statement.accept(this);
      decreaseIndent();
      checkState(currentIndent == indentBefore);
    }

    addIndent();
    sb.append("}\n");
  }

  @Override
  public void visit(InlinedFunction pS) throws IOException {
    visitCompound(pS);
  }

  @Override
  public void close() throws IOException {
    checkState(!closed);
    closed = true;
  }
}
