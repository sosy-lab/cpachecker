// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
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

/**
 * Helper class that collects all functions referenced by some CFAEdges, not counting those that are
 * called directly. (Only functions that have their address taken (implicitly) are returned.)
 */
class CReferencedFunctionsCollectorWithFieldsMatching extends CReferencedFunctionsCollector {

  public CReferencedFunctionsCollectorWithFieldsMatching() {
    collector = new CollectFunctionsVisitorWithFieldMatching(collectedFunctions);
  }

  Multimap<String, String> getFieldMatching() {
    return ((CollectFunctionsVisitorWithFieldMatching) collector).functionToFieldMatching;
  }

  Multimap<String, String> getGlobalsMatching() {
    return ((CollectFunctionsVisitorWithFieldMatching) collector).functionToGlobalMatching;
  }

  @Override
  public void visitDeclaration(CVariableDeclaration decl) {
    if (decl.getInitializer() != null) {
      decl.getInitializer().accept(collector);
      CInitializer init = decl.getInitializer();
      if (init instanceof CInitializerList) {
        saveStructureDeclaration(decl.getType(), (CInitializerList) init);
      } else if (decl.isGlobal() && init instanceof CInitializerExpression) {
        // Assignment to global variable and not a structure
        saveInitializerExpression(
            getGlobalsMatching(), (CInitializerExpression) init, decl.getName());
      }
    }
  }

  private void saveStructureDeclaration(CType type, CInitializerList init) {
    if (type instanceof CArrayType) {
      for (CInitializer cInit : init.getInitializers()) {
        if (cInit instanceof CInitializerList) {
          // Array of structures
          saveStructureDeclaration(((CArrayType) type).getType(), (CInitializerList) cInit);
        }
      }
    } else if (type instanceof CElaboratedType) {
      saveStructureDeclaration(type.getCanonicalType(), init);
    } else if (type instanceof CTypedefType) {
      saveStructureDeclaration(((CTypedefType) type).getRealType(), init);
    } else if (type instanceof CCompositeType) {
      // Structure found
      List<CCompositeTypeMemberDeclaration> list = ((CCompositeType) type).getMembers();
      List<CInitializer> initList = init.getInitializers();
      // Important to traverse via initList to handle such cases as
      // struct my_struct m = {.field = 1}
      for (int i = 0; i < initList.size(); i++) {
        CCompositeTypeMemberDeclaration decl = list.get(i);
        CInitializer cInit = initList.get(i);
        if (cInit instanceof CDesignatedInitializer) {
          List<CDesignator> des = ((CDesignatedInitializer) cInit).getDesignators();
          assert des.size() == 1;
          CDesignator field = des.get(0);
          CInitializer fieldInit = ((CDesignatedInitializer) cInit).getRightHandSide();
          if (fieldInit instanceof CInitializerExpression && field instanceof CFieldDesignator) {
            saveInitializerExpression(
                getFieldMatching(),
                ((CInitializerExpression) fieldInit),
                ((CFieldDesignator) field).getFieldName());
          }
        }
        if (cInit instanceof CInitializerExpression) {
          saveInitializerExpression(
              getFieldMatching(), (CInitializerExpression) cInit, decl.getName());
        }
      }
    }
  }

  private void saveInitializerExpression(
      Multimap<String, String> map, CInitializerExpression cInit, String fieldName) {

    CExpression initExpression = cInit.getExpression();
    if (initExpression instanceof CCastExpression) {
      // (void*) (&f)
      initExpression = ((CCastExpression) initExpression).getOperand();
    }

    CType type = initExpression.getExpressionType().getCanonicalType();
    if (type instanceof CPointerType) {
      type = ((CPointerType) type).getType();

      if (type instanceof CFunctionType) {
        if (initExpression instanceof CUnaryExpression) {
          // a = &b;
          initExpression = ((CUnaryExpression) initExpression).getOperand();
        }
        if (initExpression instanceof CIdExpression) {
          map.put(fieldName, ((CIdExpression) initExpression).getName());
        }
      }
    }
  }

  private static class CollectFunctionsVisitorWithFieldMatching extends CollectFunctionsVisitor {

    private final Multimap<String, String> functionToFieldMatching = HashMultimap.create();
    private final Multimap<String, String> functionToGlobalMatching = HashMultimap.create();
    private @Nullable String lastFunction;

    public CollectFunctionsVisitorWithFieldMatching(Set<String> collectedFuncs) {
      super(collectedFuncs);
    }

    @Override
    public Void visit(CIdExpression pE) {
      super.visit(pE);
      if (pE.getExpressionType() instanceof CFunctionType) {
        lastFunction = pE.getName();
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {
      int num = collectedFunctions.size();
      pIastExpressionAssignmentStatement.getLeftHandSide().accept(this);
      pIastExpressionAssignmentStatement.getRightHandSide().accept(this);
      if (num < collectedFunctions.size()) {
        // obvious assumption: the assignment is to single variable
        // - one value of 'lastFunction' is enough
        CLeftHandSide left = pIastExpressionAssignmentStatement.getLeftHandSide();
        if (left instanceof CFieldReference) {
          functionToFieldMatching.put(((CFieldReference) left).getFieldName(), lastFunction);
        } else if (left instanceof CIdExpression) {
          CSimpleDeclaration decl = ((CIdExpression) left).getDeclaration();
          if (decl instanceof CVariableDeclaration) {
            if (((CVariableDeclaration) decl).isGlobal()) {
              functionToGlobalMatching.put(((CIdExpression) left).getName(), lastFunction);
            }
          }
        }
      }
      return null;
    }
  }
}
