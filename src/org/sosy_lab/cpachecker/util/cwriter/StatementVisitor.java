// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.EmptyStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.FunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.Statement.InlinedFunction;
import org.sosy_lab.cpachecker.util.cwriter.Statement.Label;
import org.sosy_lab.cpachecker.util.cwriter.Statement.SimpleStatement;

interface StatementVisitor {
  void visit(SimpleStatement pS);

  void visit(Label pS);

  void visit(FunctionDefinition pS);

  void visit(EmptyStatement pS);

  void visit(CompoundStatement pS);

  void visit(InlinedFunction pS);
}
