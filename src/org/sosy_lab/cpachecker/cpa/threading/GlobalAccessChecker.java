/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.threading;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;

import java.util.Optional;

/** This static analyzer for edges checks whether there exists
 * read- or write-access to global variables or shared memory along edges. */
public class GlobalAccessChecker {

  /** cache elements, edges and their content never change. */
  private final Map<AAstNode, Boolean> astCache = new IdentityHashMap<>();

  /** check, whether the edge might have a write- or read-access to
   * global variables or shared memory, i.e. whether the edge might
   * influence other threads or uses only scoped variables of the thread. */
  boolean hasGlobalAccess(CFAEdge edge) {
    switch (edge.getEdgeType()) {
    case BlankEdge:
      return false;
    case AssumeEdge:
      return hasGlobalAccess(((CAssumeEdge) edge).getExpression());
    case StatementEdge:
      return hasGlobalAccess(((CStatementEdge) edge).getStatement());
    case DeclarationEdge:
      return hasGlobalAccess(((CDeclarationEdge)edge).getDeclaration());
    case ReturnStatementEdge:
      return ((CReturnStatementEdge) edge).getExpression().isPresent()
          && hasGlobalAccess(((CReturnStatementEdge) edge).getExpression().get());
    case FunctionCallEdge:
      return ((CFunctionCallEdge) edge).getRawAST().isPresent()
          && hasGlobalAccess(((CFunctionCallEdge) edge).getRawAST().get());
    case FunctionReturnEdge:
      return hasGlobalAccess(((FunctionReturnEdge) edge).getSummaryEdge().getExpression());
    default:
      throw new AssertionError("unexpected edge: " + edge);
    }
  }

  private <T extends AAstNode> boolean hasGlobalAccess(final T ast) {

    if (ast == null) {
      return false;
    }

    if (astCache.containsKey(ast)) {
      return astCache.get(ast);
    }

    final boolean hasGlobalAccess = hasGlobalAccessDirect(ast);

    astCache.put(ast, hasGlobalAccess);

    return hasGlobalAccess;
  }

  private <T extends AAstNode> Boolean anyHasGlobalAccess(final List<T> astList) {
    for (T ast : astList) {
      if (hasGlobalAccess(ast)) {
        return true;
      }
    }
    return false;
  }

  // TODO check each line, code is just written, but not tested.
  private Boolean hasGlobalAccessDirect(AAstNode ast) {

    if (ast instanceof CRightHandSide) {

      if (ast instanceof CExpression) {
        return ((CExpression) ast).accept(GlobalAccessVisitor.INSTANCE);

      } else if (ast instanceof CFunctionCallExpression) {
        CFunctionCallExpression func = (CFunctionCallExpression) ast;
        return anyHasGlobalAccess(func.getParameterExpressions());
      }

    } else if (ast instanceof CInitializer) {

      if (ast instanceof CInitializerExpression) {
        return hasGlobalAccess(((CInitializerExpression) ast).getExpression());

      } else if (ast instanceof CInitializerList) {
        return anyHasGlobalAccess(((CInitializerList) ast).getInitializers());

      } else if (ast instanceof CDesignatedInitializer) {
        CDesignatedInitializer di = (CDesignatedInitializer) ast;
        return anyHasGlobalAccess(di.getDesignators()) || hasGlobalAccess(di.getRightHandSide());
      }

    } else if (ast instanceof CSimpleDeclaration) {

      if (ast instanceof CVariableDeclaration) {
        CVariableDeclaration decl = (CVariableDeclaration) ast;
        return decl.isGlobal() || hasGlobalAccess(decl.getInitializer());

      } else if (ast instanceof CFunctionDeclaration) {
        CFunctionDeclaration decl = (CFunctionDeclaration) ast;
        return anyHasGlobalAccess(decl.getParameters());

      } else if (ast instanceof CComplexTypeDeclaration) {
        CComplexTypeDeclaration decl = (CComplexTypeDeclaration) ast;
        return decl.isGlobal();

      } else if (ast instanceof CTypeDefDeclaration) {
        CTypeDefDeclaration decl = (CTypeDefDeclaration) ast;
        return decl.isGlobal();

      } else if (ast instanceof CParameterDeclaration) {
        return false;

      } else if (ast instanceof CEnumType.CEnumerator) {
        return false;
      }

    } else if (ast instanceof CStatement) {

      if (ast instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement stat = (CFunctionCallAssignmentStatement) ast;
        return hasGlobalAccess(stat.getLeftHandSide()) || hasGlobalAccess(stat.getRightHandSide());

      } else if (ast instanceof CExpressionAssignmentStatement) {
        CExpressionAssignmentStatement stat = (CExpressionAssignmentStatement) ast;
        return hasGlobalAccess(stat.getLeftHandSide()) || hasGlobalAccess(stat.getRightHandSide());

      } else if (ast instanceof CFunctionCallStatement) {
        return hasGlobalAccess(((CFunctionCallStatement) ast).getFunctionCallExpression());

      } else if (ast instanceof CExpressionStatement) {
        return hasGlobalAccess(((CExpressionStatement) ast).getExpression());
      }

    } else if (ast instanceof CReturnStatement) {
      Optional<CExpression> returnExp = ((CReturnStatement) ast).getReturnValue();
      Optional<CAssignment> returnAssignment = ((CReturnStatement) ast).asAssignment();
      return (returnExp.isPresent() && hasGlobalAccess(returnExp.get()))
          || (returnAssignment.isPresent() && hasGlobalAccess(returnAssignment.get()));

    } else if (ast instanceof CDesignator) {

      if (ast instanceof CArrayDesignator) {
        return hasGlobalAccess(((CArrayDesignator) ast).getSubscriptExpression());

      } else if (ast instanceof CArrayRangeDesignator) {
        return hasGlobalAccess(((CArrayRangeDesignator) ast).getFloorExpression())
            || hasGlobalAccess(((CArrayRangeDesignator) ast).getCeilExpression());

      } else if (ast instanceof CFieldDesignator) {
        return false;
      }
    }

    throw new AssertionError("unhandled ASTNode " + ast + " of " + ast.getClass());
  }

  /** returns whether there might be a read- or write-access to global variables. */
  private static class GlobalAccessVisitor
      extends DefaultCExpressionVisitor<Boolean, RuntimeException>
      implements CRightHandSideVisitor<Boolean, RuntimeException> {

    // we can use singleton, because there is no internal storage or state.
    final static GlobalAccessVisitor INSTANCE = new GlobalAccessVisitor();

    @Override
    public Boolean visit(CIdExpression pE) {
      CSimpleDeclaration decl = pE.getDeclaration();
      return decl != null
          && decl instanceof AbstractDeclaration
          && ((AbstractDeclaration)decl).isGlobal();
    }

    @Override
    public Boolean visit(CArraySubscriptExpression pE) {
      return pE.getArrayExpression().accept(this)
          || pE.getSubscriptExpression().accept(this);
    }

    @Override
    public Boolean visit(CBinaryExpression pE) {
      return pE.getOperand1().accept(this)
          || pE.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression pE) {
      return pE.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CComplexCastExpression pE) {
      return pE.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CFieldReference pE) {
      return pE.getFieldOwner().accept(this);
    }

    @Override
    public Boolean visit(CFunctionCallExpression pE) {
      if (pE.getFunctionNameExpression().accept(this)) {
        return true;
      }
      for (CExpression param : pE.getParameterExpressions()) {
        if (param.accept(this)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Boolean visit(CUnaryExpression pE) {
      return pE.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CPointerExpression pE) {
      return pE.getOperand().accept(this);
    }

    @Override
    protected Boolean visitDefault(CExpression pExp) {
      // all further (inherited, not directly implemented) methods only access local data, e.g. IntegerLiteralExpression.
      return false;
    }
  }
}
