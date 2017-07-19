/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;

class StatementCFABuilder extends ASTVisitor
    implements CFABuilderWrapperOfType<StatementCFABuilder> {

  private final CFABuilder builder;

  StatementCFABuilder(final CFABuilder pBuilder) {
    builder = pBuilder;
  }

  public StatementCFABuilder append(final Statement pStatement) {
    pStatement.accept(this);
    return this;
  }

  @Override
  public boolean visit(final FunctionDeclaration statement) {
    final FunctionDeclarationCFABuilder statementCFABuilder =
        new FunctionDeclarationCFABuilder(builder);
    statementCFABuilder.append(statement);
    builder.append(statementCFABuilder.getBuilder());
    return false;
  }

  @Override
  public boolean visit(final FunctionInvocation statement) {
    final FunctionInvocationCFABuilder statementCFABuilder =
        new FunctionInvocationCFABuilder(builder);
    statementCFABuilder.append(statement);
    builder.append(statementCFABuilder.getBuilder());
    return false;
  }

  @Override
  public boolean visit(final IfStatement statement) {
    final IfStatementCFABuilder statementCFABuilder = new IfStatementCFABuilder(builder);
    statementCFABuilder.append(statement);
    builder.append(statementCFABuilder.getBuilder());
    return false;
  }

  @Override
  public boolean visit(final VariableDeclarationStatement statement) {
    final VariableDeclarationStatementCFABuilder statementCFABuilder =
        new VariableDeclarationStatementCFABuilder(builder);
    statementCFABuilder.append(statement);
    builder.append(statementCFABuilder.getBuilder());
    return false;
  }

  @Override
  public CFABuilder getBuilder() {
    return builder;
  }
}
