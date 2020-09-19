// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public abstract class Statement {
  private static int gotoCounter = 0;

  private boolean isGotoTarget = false;
  private String gotoLabel = null;

  public abstract <E extends Exception> void accept(StatementVisitor<E> pVisitor) throws E;

  public Optional<String> getLabelIfUsed() {
    if (!isGotoTarget) {
      return Optional.empty();
    } else {
      return Optional.of(gotoLabel);
    }
  }

  private static String getNewLabelName() {
    String label = "label_" + gotoCounter;
    gotoCounter++;
    return label;
  }

  public String getLabel() {
    if (!isGotoTarget) {
      gotoLabel = getNewLabelName();
      isGotoTarget = true;
    }
    return gotoLabel;
  }

  /**
   * Creates a String representation of this object. Created strings may get really big, so use with
   * care.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    StatementVisitor<IOException> w = null;
    try {
      w = StatementWriter.getWriter(sb, TranslatorConfig.getDefault());
      accept(w);
    } catch (IOException pE) {
      throw new AssertionError("IOException should not be possible for StringBuilder", pE);
    }
    return sb.toString();
  }

  static class InlinedFunction extends CompoundStatement {
    public InlinedFunction(CompoundStatement pOuterBlock) {
      super(pOuterBlock);
    }
  }

  static class CompoundStatement extends Statement {
    private final List<Statement> statements;
    private final CompoundStatement outerBlock;

    public CompoundStatement() {
      this(null);
    }

    public CompoundStatement(CompoundStatement pOuterBlock) {
      statements = new ArrayList<>();
      outerBlock = pOuterBlock;
    }

    public void addStatement(Statement statement) {
      statements.add(statement);
    }

    @Override
    public <E extends Exception> void accept(StatementVisitor<E> pVisitor) throws E {
      pVisitor.visit(this);
    }

    public ImmutableList<Statement> getStatements() {
      return ImmutableList.copyOf(statements);
    }

    public CompoundStatement getSurroundingBlock() {
      return outerBlock;
    }

    public boolean isEmpty() {
      return statements.isEmpty();
    }

    public Statement getLast() {
      return statements.get(statements.size() - 1);
    }
  }

  static class SimpleStatement extends Statement {
    private final String code;
    private final CFAEdge origin;

    public SimpleStatement(String pCode) {
      this(null, pCode);
    }

    public SimpleStatement(CFAEdge pOrigin, String pCode) {
      origin = pOrigin;
      code = pCode;
    }

    @Override
    public <E extends Exception> void accept(StatementVisitor<E> pVisitor) throws E {
      pVisitor.visit(this);
    }

    public Optional<CFAEdge> getOrigin() {
      return Optional.ofNullable(origin);
    }

    public String getCode() {
      return code;
    }
  }

  static class FunctionDefinition extends Statement {
    private final String functionHeader;
    private final CompoundStatement functionBody;

    public FunctionDefinition(String pFunctionHeader, CompoundStatement pFunctionBody) {
      functionHeader = pFunctionHeader;
      functionBody = pFunctionBody;
    }

    @Override
    public <E extends Exception> void accept(StatementVisitor<E> pVisitor) throws E {
      pVisitor.visit(this);
    }

    public String getFunctionHeader() {
      return functionHeader;
    }

    public CompoundStatement getFunctionBody() {
      return functionBody;
    }
  }

  static class Label extends Statement {

    private final String name;

    public Label(String pLabelName) {
      name = pLabelName;
    }

    @Override
    public <E extends Exception> void accept(StatementVisitor<E> pVisitor) throws E {
      pVisitor.visit(this);
    }

    @Override
    public String getLabel() {
      return name;
    }

    @Override
    public Optional<String> getLabelIfUsed() {
      return Optional.of(name);
    }
  }

  static class EmptyStatement extends Statement {

    @Override
    public <E extends Exception> void accept(StatementVisitor<E> pVisitor) throws E {
      pVisitor.visit(this);
    }
  }
}
