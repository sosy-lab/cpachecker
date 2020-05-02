/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.dependencegraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class PointerDefsUses {

  private PointerDefsUses() {}

  private static boolean isPointerType(CSimpleDeclaration pDeclaration) {
    return pDeclaration.getType() instanceof CPointerType;
  }

  private static Optional<Data> getAssignmentData(CAssignment pAssignment) {

    CLeftHandSide lhs = pAssignment.getLeftHandSide();
    CSimpleDeclaration lhsDecl = lhs.accept(Collector.INSTANCE);

    if (lhsDecl != null && isPointerType(lhsDecl)) {

      MemoryLocation lhsVar = MemoryLocation.valueOf(lhsDecl.getQualifiedName());
      CRightHandSide rhs = pAssignment.getRightHandSide();

      if (lhs.getExpressionType() instanceof CPointerType
          && rhs.getExpressionType() instanceof CPointerType) {

        CSimpleDeclaration rhsDecl = rhs.accept(Collector.INSTANCE);

        if (rhsDecl != null) {

          MemoryLocation rhsVar = MemoryLocation.valueOf(rhsDecl.getQualifiedName());
          Data.Type type = isPointerType(rhsDecl) ? Data.Type.COPY : Data.Type.ADDRESS_OF;

          return Optional.of(new Data(type, lhsVar, rhsVar));
        }

      } else {

        return Optional.of(new Data(Data.Type.ASSIGN, lhsVar, null));
      }
    }

    return Optional.empty();
  }

  private static Optional<Data> getVariableDeclarationData(CVariableDeclaration pDeclaration) {

    if (isPointerType(pDeclaration)) {

      MemoryLocation lhsVar = MemoryLocation.valueOf(pDeclaration.getQualifiedName());
      CInitializer initializer = pDeclaration.getInitializer();

      if (initializer != null) {

        if (initializer instanceof CInitializerExpression) {

          CExpression rhs = ((CInitializerExpression) initializer).getExpression();
          CSimpleDeclaration rhsDecl = rhs.accept(Collector.INSTANCE);

          if (rhsDecl != null) {

            MemoryLocation rhsVar = MemoryLocation.valueOf(rhsDecl.getQualifiedName());
            Data.Type type = isPointerType(rhsDecl) ? Data.Type.COPY : Data.Type.ADDRESS_OF;

            return Optional.of(new Data(type, lhsVar, rhsVar));
          }
        }

      } else {
        return Optional.of(new Data(Data.Type.COPY, lhsVar, null));
      }
    }

    return Optional.empty();
  }

  private static Optional<Data> getFunctionCallAssignmentData(
      CFunctionCallAssignmentStatement pAssignment) {

    CLeftHandSide lhs = pAssignment.getLeftHandSide();

    if (lhs.getExpressionType() instanceof CPointerType) {

      CSimpleDeclaration lhsDecl = lhs.accept(Collector.INSTANCE);

      if (lhsDecl != null && isPointerType(lhsDecl)) {
        MemoryLocation lhsVar = MemoryLocation.valueOf(lhsDecl.getQualifiedName());
        return Optional.of(new Data(Data.Type.COPY, lhsVar, null));
      }
    }

    return Optional.empty();
  }

  public static List<Data> getParameterData(CFunctionDeclaration pDeclaration) {

    List<Data> result = new ArrayList<>();

    for (CParameterDeclaration param : pDeclaration.getParameters()) {
      if (isPointerType(param)) {

        MemoryLocation paramVar = MemoryLocation.valueOf(param.getQualifiedName());
        result.add(new Data(Data.Type.COPY, paramVar, null));
      }
    }

    return Collections.unmodifiableList(result);
  }

  public static Optional<Data> getData(CFAEdge pEdge) {

    if (pEdge instanceof CStatementEdge) {

      CStatement statement = ((CStatementEdge) pEdge).getStatement();

      if (statement instanceof CAssignment) {
        return getAssignmentData((CAssignment) statement);
      }

    } else if (pEdge instanceof CDeclarationEdge) {

      CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();

      if (declaration instanceof CVariableDeclaration) {
        return getVariableDeclarationData((CVariableDeclaration) declaration);
      }
    } else if (pEdge instanceof CFunctionSummaryEdge) {

      CStatement statement = ((CFunctionSummaryEdge) pEdge).getExpression();

      if (statement instanceof CFunctionCallAssignmentStatement) {
        return getFunctionCallAssignmentData((CFunctionCallAssignmentStatement) statement);
      }
    }

    return Optional.empty();
  }

  public static final class Data {

    private final Type type;
    private final MemoryLocation defVar;
    private final Optional<MemoryLocation> useVar;

    private Data(Type pType, MemoryLocation pDefVar, MemoryLocation pUseVar) {
      type = pType;
      defVar = pDefVar;
      useVar = (pUseVar != null ? Optional.of(pUseVar) : Optional.empty());
    }

    public Type getType() {
      return type;
    }

    public MemoryLocation getDefVar() {
      return defVar;
    }

    public Optional<MemoryLocation> getUseVar() {
      return useVar;
    }

    private String getUseVarString() {
      return useVar.isPresent() ? useVar.orElseThrow().toString() : "<??>";
    }

    @Override
    public String toString() {
      switch (type) {
        case ADDRESS_OF:
          return String.format("%s := &%s", defVar, getUseVarString());
        case COPY:
          return String.format("%s := %s", defVar, getUseVarString());
        case ASSIGN:
          return String.format("*%s := %s", defVar, getUseVarString());
        default:
          return "INVALID";
      }
    }

    public enum Type {

      /** {@code pointer := &some_variable} */
      ADDRESS_OF,

      /** {@code pointer := some_pointer} */
      COPY,

      /** {@code *pointer := some_value} */
      ASSIGN;
    }
  }

  private static final class PointerDefsUsesException extends RuntimeException {
    private static final long serialVersionUID = 7276917743542681991L;
  }

  private static class Collector
      implements CAstNodeVisitor<CSimpleDeclaration, PointerDefsUsesException> {

    public static final Collector INSTANCE = new Collector();

    @Override
    public CSimpleDeclaration visit(CUnaryExpression pIastUnaryExpression)
        throws PointerDefsUsesException {
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    public CSimpleDeclaration visit(CPointerExpression pPointerExpression)
        throws PointerDefsUsesException {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public CSimpleDeclaration visit(CIdExpression pIastIdExpression)
        throws PointerDefsUsesException {
      return pIastIdExpression.getDeclaration();
    }

    // all methods below this comment just return null

    @Override
    public CSimpleDeclaration visit(CArrayDesignator pArrayDesignator)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CArrayRangeDesignator pArrayRangeDesignator)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CFieldDesignator pFieldDesignator)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CInitializerExpression pInitializerExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CInitializerList pInitializerList)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CDesignatedInitializer pCStructInitializerPart)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CFunctionCallExpression pIastFunctionCallExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CBinaryExpression pIastBinaryExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CCastExpression pIastCastExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CCharLiteralExpression pIastCharLiteralExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CFloatLiteralExpression pIastFloatLiteralExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CStringLiteralExpression pIastStringLiteralExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CTypeIdExpression pIastTypeIdExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CImaginaryLiteralExpression PIastLiteralExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CAddressOfLabelExpression pAddressOfLabelExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CFieldReference pIastFieldReference)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CComplexCastExpression pComplexCastExpression)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CFunctionDeclaration pDecl) throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CComplexTypeDeclaration pDecl) throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CTypeDefDeclaration pDecl) throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CVariableDeclaration pDecl) throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CParameterDeclaration pDecl) throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CEnumerator pDecl) throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CExpressionStatement pIastExpressionStatement)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(
        CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(
        CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CFunctionCallStatement pIastFunctionCallStatement)
        throws PointerDefsUsesException {
      return null;
    }

    @Override
    public CSimpleDeclaration visit(CReturnStatement pNode) throws PointerDefsUsesException {
      return null;
    }
  }
}
