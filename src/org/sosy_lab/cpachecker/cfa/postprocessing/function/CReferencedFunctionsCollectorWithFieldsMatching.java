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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GlobalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;

/**
 * Helper class that collects all functions referenced by some CFAEdges,
 * not counting those that are called directly.
 * (Only functions that have their address taken (implicitly) are returned.)
 */
class CReferencedFunctionsCollectorWithFieldsMatching extends CReferencedFunctionsCollector {

  private final CollectFunctionsVisitorWithFieldMatching collector;

  public CReferencedFunctionsCollectorWithFieldsMatching() {
    collector = new CollectFunctionsVisitorWithFieldMatching(collectedFunctions);
  }

  Multimap<String, String> getFieldMatching() {
    return collector.functionToFieldMatching;
  }

  Multimap<String, String> getGlobalMatching() {
    return collector.functionToFieldMatching;
  }

  @Override
  public void visitDeclaration(CVariableDeclaration decl) {
    if (decl.getInitializer() != null) {
      decl.getInitializer().accept(collector);
      saveDeclaration(decl.getType(), decl.getInitializer(), decl.isGlobal() ? decl.getName() : null);
    }
  }

  private void saveDeclaration(CType type, CInitializer init, String name) {
    if (init instanceof CInitializerList) {
      //Only structures
      if (type instanceof CArrayType) {
        for (CInitializer cInit : ((CInitializerList)init).getInitializers()) {
          saveDeclaration(((CArrayType)type).getType(), cInit, name);
        }
      } else if (type instanceof CElaboratedType) {
        saveDeclaration(type.getCanonicalType(), init, name);
      } else if (type instanceof CCompositeType) {
        //Structure
        List<CCompositeTypeMemberDeclaration> list = ((CCompositeType) type).getMembers();
        List<CInitializer> initList = ((CInitializerList)init).getInitializers();
        for (int i = 0; i < list.size(); i++) {
          CCompositeTypeMemberDeclaration decl = list.get(i);
          CInitializer cInit = initList.get(i);
          saveInitializerExpression(collector.functionToFieldMatching, cInit, decl.getName());
        }
      } else if (type instanceof CTypedefType) {
        saveDeclaration(((CTypedefType) type).getRealType(), init, name);
      }
    } else {
      //Assignement to global id
      saveInitializerExpression(collector.funcToGlobal, init, name);
    }
  }

  private void saveInitializerExpression(Multimap<String, String> map, CInitializer cInit, String fieldName) {

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
            map.put(fieldName, ((CIdExpression) initExpression).getName());
          }
        }
      }
    }
  }

  private static class CollectFunctionsVisitorWithFieldMatching extends CollectFunctionsVisitor {

    private final Multimap<String, String> functionToFieldMatching = HashMultimap.create();
    private final Multimap<String, String> funcToGlobal = HashMultimap.create();
    private String lastFunction;

    private IdentifierCreator idCreator = new IdentifierCreator();

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
          functionToFieldMatching.put(((CFieldReference) left).getFieldName(), lastFunction);
        } else {
          AbstractIdentifier id = left.accept(idCreator);
          if (id instanceof StructureIdentifier) {
            assert false : "Structures should be handled above";
          } else if (id instanceof GlobalVariableIdentifier) {
            funcToGlobal.put(((SingleIdentifier) id).getName(), lastFunction);
          }
        }
      }
      return null;
    }
  }
}
