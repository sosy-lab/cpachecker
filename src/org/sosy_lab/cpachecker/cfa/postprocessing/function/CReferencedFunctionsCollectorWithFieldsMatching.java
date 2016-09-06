/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.postprocessing.function;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class that collects all functions referenced by some CFAEdges,
 * not counting those that are called directly.
 * (Only functions that have their address taken (implicitly) are returned.)
 */
public class CReferencedFunctionsCollectorWithFieldsMatching extends CReferencedFunctionsCollector {

  private final CollectFunctionsVisitorWithFieldMatching collector;


  public CReferencedFunctionsCollectorWithFieldsMatching() {
    collector = new CollectFunctionsVisitorWithFieldMatching(collectedFunctions);
  }

  public Map<String, Collection<String>> getFieldMatching() {
    return collector.functionToFieldMatching;
  }

  @Override
  public void visitEdge(CFAEdge edge) {
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      assumeEdge.getExpression().accept(collector);
      break;
    case BlankEdge:
      //nothing to do
      break;
    case CallToReturnEdge:
      //nothing to do
      assert false;
      break;
    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();
      if (declaration instanceof CVariableDeclaration) {
        CInitializer init = ((CVariableDeclaration)declaration).getInitializer();
        if (init != null) {
          init.accept(collector);
          saveDeclaration(declaration.getType(), init);
        }

      }
      break;
    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)edge;
      if (returnEdge.getExpression().isPresent()) {
        returnEdge.getExpression().get().accept(collector);
      }
      break;
    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      statementEdge.getStatement().accept(collector);
      break;

    default:
      throw new AssertionError();
    }
  }

  @Override
  public void visitDeclaration(CVariableDeclaration decl) {
    if (decl.getInitializer() != null) {
      decl.getInitializer().accept(collector);
      saveDeclaration(decl.getType(), decl.getInitializer());
    }
  }

  private void saveDeclaration(CType type, CInitializer init) {
    if (init instanceof CInitializerList) {
      //Only structures
      if (type instanceof CArrayType) {
        for (CInitializer cInit : ((CInitializerList)init).getInitializers()) {
          saveDeclaration(((CArrayType)type).getType(), cInit);
        }
      } else if (type instanceof CElaboratedType) {
        saveDeclaration(type.getCanonicalType(), init);
      } else if (type instanceof CCompositeType) {
        //Structure
        List<CCompositeTypeMemberDeclaration> list = ((CCompositeType) type).getMembers();
        List<CInitializer> initList = ((CInitializerList)init).getInitializers();
        for (int i = 0; i < list.size(); i++) {
          CCompositeTypeMemberDeclaration decl = list.get(i);
          CInitializer cInit = initList.get(i);
          saveInitializerExpression(cInit, decl.getName());
        }
      } else if (type instanceof CTypedefType) {
        saveDeclaration(((CTypedefType) type).getRealType(), init);
      }
    }
  }

  private void saveInitializerExpression(CInitializer cInit, String fieldName) {

    if (cInit instanceof CInitializerExpression) {
      CInitializerExpression init = (CInitializerExpression) cInit;
      CExpression initExpression = init.getExpression();
      if (initExpression instanceof CCastExpression) {
        // (void*) (&f)
        initExpression = ((CCastExpression)initExpression).getOperand();
      }

      CType type = initExpression.getExpressionType().getCanonicalType();
      if (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();

        if (type instanceof CFunctionType) {
          if (initExpression instanceof CUnaryExpression) {
            // a = &b;
            initExpression = ((CUnaryExpression)initExpression).getOperand();
          }
          if (initExpression instanceof CIdExpression) {
            collector.saveInfoIntoMap(fieldName, ((CIdExpression)initExpression).getName());
          }
        }
      }
    }
  }



  private static class CollectFunctionsVisitorWithFieldMatching extends CollectFunctionsVisitor {

    public final Map<String, Collection<String>> functionToFieldMatching = new HashMap<>();
    private String lastFunction;

    public CollectFunctionsVisitorWithFieldMatching(Set<String> collectedFuncs) {
      super(collectedFuncs);
    }

    @Override
    public Void visit(CIdExpression pE) {
      if (pE.getExpressionType() instanceof CFunctionType) {
        this.collectedFunctions.add(pE.getName());
        lastFunction = pE.getName();
      }
      return null;
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {
      int num = collectedFunctions.size();
      pIastExpressionAssignmentStatement.getLeftHandSide().accept(this);
      pIastExpressionAssignmentStatement.getRightHandSide().accept(this);
      if (num < collectedFunctions.size()) {
        //obvious assumption: the assignment is to single variable
        // - one value of 'lastFunction' is enough
        CLeftHandSide left = pIastExpressionAssignmentStatement.getLeftHandSide();
        if (left instanceof CFieldReference) {
          saveInfoIntoMap(((CFieldReference) left).getFieldName(), lastFunction);
        }
      }
      return null;
    }

    private void saveInfoIntoMap(String field, String funcName) {
      Collection<String> result;
      if (functionToFieldMatching.containsKey(field)) {
        result = functionToFieldMatching.get(field);
      } else {
        result = new HashSet<>();
        functionToFieldMatching.put(field, result);
      }
      result.add(funcName);
    }
  }
}
