/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.location;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.cpa.location.LocationStateWithEdge.ProjectedLocationStateWithEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class LocationApplyOperator implements ApplyOperator {

  private static class GlobalExpressionVisitor
      implements CRightHandSideVisitor<Boolean, NoException> {

    @Override
    public Boolean visit(CBinaryExpression pIastBinaryExpression) throws NoException {
      return pIastBinaryExpression.getOperand1().accept(this)
          || pIastBinaryExpression.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression pIastCastExpression) throws NoException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
        throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CUnaryExpression pIastUnaryExpression) throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws NoException {
      // return pIastArraySubscriptExpression.getArrayExpression().accept(this);
      return true;
    }

    @Override
    public Boolean visit(CFieldReference pIastFieldReference) throws NoException {
      return pIastFieldReference.isPointerDereference()
          || pIastFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public Boolean visit(CIdExpression pIastIdExpression) throws NoException {
      CSimpleDeclaration decl = pIastIdExpression.getDeclaration();
      if (decl instanceof CVariableDeclaration) {
        return ((CVariableDeclaration) decl).isGlobal();
      }
      return false;
    }

    @Override
    public Boolean visit(CPointerExpression pPointerExpression) throws NoException {
      // TODO maybe optimized more, right now just a simple version
      return true;
    }

    @Override
    public Boolean visit(CComplexCastExpression pComplexCastExpression) throws NoException {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CFunctionCallExpression pIastFunctionCallExpression) throws NoException {
      for (CExpression p : pIastFunctionCallExpression.getParameterExpressions()) {
        if (p.accept(this)) {
          return true;
        }
      }
      return false;
    }
  }

  private final Map<CFANode, Boolean> cachedNodes = new TreeMap<>();

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    LocationStateWithEdge state1 = (LocationStateWithEdge) pState1;
    LocationStateWithEdge state2 = (LocationStateWithEdge) pState2;

    if (state2.getAbstractEdge() instanceof WrapperCFAEdge) {
      // Ordinary transition
      return null;
    } else if (state1.getAbstractEdge() == EmptyEdge.getInstance()
        || state2.getAbstractEdge() == EmptyEdge.getInstance()) {
      return null;
    } else {
      return state1.updateEdge(EmptyEdge.getInstance());
    }
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    return ProjectedLocationStateWithEdge.getInstance();
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    LocationStateWithEdge state1 = (LocationStateWithEdge) pParent;

    assert pEdge == state1.getAbstractEdge();
    assert pEdge instanceof WrapperCFAEdge;

    // That is important to remove CFAEdge, to avoid considering it
    // Evil hack!
    return ProjectedLocationStateWithEdge.getInstance();
  }

  @Override
  public boolean isInvariantToEffects(AbstractState pState) {
    return true;
  }

  @Override
  public boolean canBeAnythingApplied(AbstractState pState) {
    LocationState state = (LocationState) pState;
    CFANode node = state.locationNode;

    if (cachedNodes.containsKey(node)) {
      return cachedNodes.get(node);
    }

    boolean result = false;
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFAEdge edge = node.getLeavingEdge(i);
      if (!isRedundantEdge(edge)) {
        result = true;
        break;
      }
    }
    cachedNodes.put(node, result);
    return result;
  }

  private boolean isRedundantEdge(CFAEdge edge) {
    if (edge instanceof BlankEdge) {
      return true;
    } else if (edge instanceof CDeclarationEdge) {
      CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
      if (decl instanceof CVariableDeclaration) {
        CInitializer init = ((CVariableDeclaration) decl).getInitializer();
        if (init == null) {
          return true;
        }
        if (init instanceof CInitializerExpression) {
          CExpression expr = ((CInitializerExpression) init).getExpression();
          if (expr instanceof CLiteralExpression) {
            return true;
          }
        }

      } else {
        return true;
      }
    } else if (edge instanceof FunctionReturnEdge) {
      return true;
    } else if (edge instanceof CAssumeEdge) {
      CExpression expr = ((CAssumeEdge) edge).getExpression();
      GlobalExpressionVisitor visitor = new GlobalExpressionVisitor();
      return !expr.accept(visitor);
    } else if (edge instanceof CStatementEdge) {
      CStatement stmnt = ((CStatementEdge) edge).getStatement();
      if (stmnt instanceof CAssignment) {
        CAssignment asgn = (CAssignment) stmnt;
        // Not only right side, as the node may be an effect itself
        return !asgn.getRightHandSide().accept(new GlobalExpressionVisitor())
            && !asgn.getLeftHandSide().accept(new GlobalExpressionVisitor());
      } else if (stmnt instanceof CFunctionCallStatement) {
        // There may be a specific function, for example, locks, which strongly affect the
        // application
        return false;
      }
    } else if (edge instanceof CFunctionCallEdge) {
      // CFunctionCallExpression fcall =
      // ((CFunctionCallEdge) edge).getSummaryEdge().getExpression().getFunctionCallExpression();
      // return !fcall.accept(new GlobalExpressionVisitor());

      // There may be a specific function, for example, locks, which strongly affect the application
      return false;
    } else if (edge instanceof CReturnStatementEdge) {
      Optional<CExpression> oExp = ((CReturnStatementEdge) edge).getExpression();
      if (oExp.isPresent()) {
        return oExp.get().accept(new GlobalExpressionVisitor());
      } else {
        return true;
      }
    }

    return false;
  }

}
