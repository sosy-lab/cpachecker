/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;

public abstract class AAstNodeVisitor<R, X extends Exception> extends AExpressionVisitor<R, X>
    implements CAstNodeVisitor<R, X>, JAstNodeVisitor<R, X> {

  /*
   * only language common expressions here, all other have to be implemented by
   * the concrete visitor
   */

  @Override
  public R visit(CFunctionCallExpression exp) throws X {
    return visit((AFunctionCallExpression) exp);
  }

  @Override
  public R visit(JMethodInvocationExpression exp) throws X {
    return visit((AFunctionCallExpression) exp);
  }

  protected abstract R visit(AFunctionCallExpression exp) throws X;

  @Override
  public R visit(CInitializerExpression exp) throws X {
    return visit((AInitializerExpression) exp);
  }

  @Override
  public R visit(JInitializerExpression exp) throws X {
    return visit((AInitializerExpression) exp);
  }

  protected abstract R visit(AInitializerExpression exp) throws X;

  @Override
  public R visit(CFunctionCallStatement stmt) throws X {
    return visit((AFunctionCallStatement) stmt);
  }

  @Override
  public R visit(JMethodInvocationStatement stmt) throws X {
    return visit((AFunctionCallStatement) stmt);
  }

  protected abstract R visit(AFunctionCallStatement stmt) throws X;

  @Override
  public R visit(CFunctionCallAssignmentStatement stmt) throws X {
    return visit((AFunctionCallAssignmentStatement) stmt);
  }

  @Override
  public R visit(JMethodInvocationAssignmentStatement stmt) throws X {
    return visit((AFunctionCallAssignmentStatement) stmt);
  }

  protected abstract R visit(AFunctionCallAssignmentStatement stmt) throws X;

  @Override
  public R visit(CExpressionStatement stmt) throws X {
    return visit((AExpressionStatement) stmt);
  }

  @Override
  public R visit(JExpressionStatement stmt) throws X {
    return visit((AExpressionStatement) stmt);
  }

  protected abstract R visit(AExpressionStatement stmt) throws X;

  @Override
  public R visit(CExpressionAssignmentStatement stmt) throws X {
    return visit((AExpressionAssignmentStatement) stmt);
  }

  @Override
  public R visit(JExpressionAssignmentStatement stmt) throws X {
    return visit((AExpressionAssignmentStatement) stmt);
  }

  protected abstract R visit(AExpressionAssignmentStatement stmt) throws X;

  @Override
  public R visit(CReturnStatement stmt) throws X {
    return visit((AReturnStatement) stmt);
  }

  @Override
  public R visit(JReturnStatement stmt) throws X {
    return visit((AReturnStatement) stmt);
  }

  protected abstract R visit(AReturnStatement stmt) throws X;

  @Override
  public R visit(CFunctionDeclaration decl) throws X {
    return visit((AFunctionDeclaration) decl);
  }

  @Override
  public R visit(JMethodDeclaration decl) throws X {
    return visit((AFunctionDeclaration) decl);
  }

  protected abstract R visit(AFunctionDeclaration decl) throws X;

  @Override
  public R visit(CParameterDeclaration decl) throws X {
    return visit((AParameterDeclaration) decl);
  }

  @Override
  public R visit(JParameterDeclaration decl) throws X {
    return visit((AParameterDeclaration) decl);
  }

  protected abstract R visit(AParameterDeclaration decl) throws X;

  @Override
  public R visit(CVariableDeclaration decl) throws X {
    return visit((AVariableDeclaration) decl);
  }

  @Override
  public R visit(JVariableDeclaration decl) throws X {
    return visit((AVariableDeclaration) decl);
  }

  protected abstract R visit(AVariableDeclaration decl) throws X;
}
