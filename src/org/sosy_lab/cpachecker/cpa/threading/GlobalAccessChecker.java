// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * This static analyzer for edges checks whether there exists read- or write-access to global
 * variables or shared memory along edges.
 */
public class GlobalAccessChecker {

  /** cache elements, edges and their content never change. */
  private final IdentityHashMap<AAstNode, Boolean> astCache = new IdentityHashMap<>();

  private final GlobalAccessVisitor globalAccessVisitor;

  /**
   * @param pCfa the CFA, whose variable classification tells which local variables have their
   *     address taken and may thus be accessed by other threads
   */
  GlobalAccessChecker(CFA pCfa) {
    Optional<Set<String>> addressedVariables =
        pCfa.getVarClassification().map(VariableClassification::getAddressedVariables);
    globalAccessVisitor =
        new GlobalAccessVisitor(
            variable ->
                addressedVariables.isEmpty()
                    || addressedVariables.orElseThrow().contains(variable));
  }

  /**
   * check, whether the edge might have a write- or read-access to global variables or shared
   * memory, i.e. whether the edge might influence other threads or uses only scoped variables of
   * the thread.
   */
  boolean hasGlobalAccess(CFAEdge edge) {
    return switch (edge.getEdgeType()) {
      case BlankEdge -> false;
      case AssumeEdge -> hasGlobalAccess(((CAssumeEdge) edge).getExpression());
      case StatementEdge -> hasGlobalAccess(((CStatementEdge) edge).getStatement());
      case DeclarationEdge -> hasGlobalAccess(((CDeclarationEdge) edge).getDeclaration());
      case ReturnStatementEdge ->
          ((CReturnStatementEdge) edge).getExpression().isPresent()
              && hasGlobalAccess(((CReturnStatementEdge) edge).getExpression().orElseThrow());
      case FunctionCallEdge -> hasGlobalAccess(((CFunctionCallEdge) edge).getFunctionCall());
      case FunctionReturnEdge -> hasGlobalAccess(((FunctionReturnEdge) edge).getFunctionCall());
      default -> throw new AssertionError("unexpected edge: " + edge);
    };
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
    return switch (ast) {
      // CRightHandSide
      case CExpression cExpression -> cExpression.accept(globalAccessVisitor);
      case CFunctionCallExpression func -> anyHasGlobalAccess(func.getParameterExpressions());

      // CInitializer
      case CInitializerExpression cInitializerExpression ->
          hasGlobalAccess(cInitializerExpression.getExpression());
      case CInitializerList cInitializerList ->
          anyHasGlobalAccess(cInitializerList.getInitializers());
      case CDesignatedInitializer di ->
          anyHasGlobalAccess(di.getDesignators()) || hasGlobalAccess(di.getRightHandSide());

      // CSimpleDeclaration
      case CVariableDeclaration decl ->
          decl.isGlobal()
              || globalAccessVisitor.mayBeShared(decl)
              || hasGlobalAccess(decl.getInitializer());
      case CFunctionDeclaration decl -> anyHasGlobalAccess(decl.getParameters());
      case CComplexTypeDeclaration decl -> decl.isGlobal();
      case CTypeDefDeclaration decl -> decl.isGlobal();
      case CParameterDeclaration param -> false;
      case CEnumerator enumerator -> false;

      // CStatement
      case CFunctionCallAssignmentStatement stat ->
          hasGlobalAccess(stat.getLeftHandSide()) || hasGlobalAccess(stat.getRightHandSide());
      case CExpressionAssignmentStatement stat ->
          hasGlobalAccess(stat.getLeftHandSide()) || hasGlobalAccess(stat.getRightHandSide());
      case CFunctionCallStatement cFunctionCallStatement ->
          hasGlobalAccess(cFunctionCallStatement.getFunctionCallExpression());
      case CExpressionStatement cExpressionStatement ->
          hasGlobalAccess(cExpressionStatement.getExpression());

      case CReturnStatement cReturnStatement -> {
        Optional<CExpression> returnExp = cReturnStatement.getReturnValue();
        Optional<CAssignment> returnAssignment = cReturnStatement.asAssignment();
        yield (returnExp.isPresent() && hasGlobalAccess(returnExp.orElseThrow()))
            || (returnAssignment.isPresent() && hasGlobalAccess(returnAssignment.orElseThrow()));
      }

      // CDesignator
      case CArrayDesignator cArrayDesignator ->
          hasGlobalAccess(cArrayDesignator.getSubscriptExpression());
      case CArrayRangeDesignator cArrayRangeDesignator ->
          hasGlobalAccess(cArrayRangeDesignator.getFloorExpression())
              || hasGlobalAccess(cArrayRangeDesignator.getCeilExpression());
      case CFieldDesignator fieldDesignator -> false;

      default -> throw new AssertionError("unhandled ASTNode " + ast + " of " + ast.getClass());
    };
  }

  /**
   * returns whether there might be a read- or write-access to global variables or shared memory.
   * Memory accessed through a pointer may be shared, and so may a local variable whose address is
   * taken or that is an array (which is converted to a pointer without taking its address).
   */
  private static class GlobalAccessVisitor extends DefaultCExpressionVisitor<Boolean, NoException>
      implements CRightHandSideVisitor<Boolean, NoException> {

    private final Predicate<String> isAddressed;

    GlobalAccessVisitor(Predicate<String> pIsAddressed) {
      isAddressed = pIsAddressed;
    }

    private boolean mayBeShared(CSimpleDeclaration pDeclaration) {
      return pDeclaration.getType().getCanonicalType() instanceof CArrayType
          || isAddressed.test(pDeclaration.getQualifiedName());
    }

    @Override
    public Boolean visit(CIdExpression pE) {
      CSimpleDeclaration decl = pE.getDeclaration();
      return (decl instanceof AbstractDeclaration abstractDeclaration
              && abstractDeclaration.isGlobal())
          || ((decl instanceof CVariableDeclaration || decl instanceof CParameterDeclaration)
              && mayBeShared(decl));
    }

    @Override
    public Boolean visit(CArraySubscriptExpression pE) {
      return !(pE.getArrayExpression().getExpressionType().getCanonicalType() instanceof CArrayType)
          || pE.getArrayExpression().accept(this)
          || pE.getSubscriptExpression().accept(this);
    }

    @Override
    public Boolean visit(CBinaryExpression pE) {
      return pE.getOperand1().accept(this) || pE.getOperand2().accept(this);
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
      return pE.isPointerDereference() || pE.getFieldOwner().accept(this);
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
      return true;
    }

    @Override
    protected Boolean visitDefault(CExpression pExp) {
      // all further (inherited, not directly implemented) methods only access local data, e.g.
      // IntegerLiteralExpression.
      return false;
    }
  }
}
