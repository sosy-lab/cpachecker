// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Represents an array that can be transformed by an array abstraction. */
final class TransformableArray {

  private static final String VALUE_VARIABLE_PREFIX = "__array_value_";
  private static final String INDEX_VARIABLE_PREFIX = "__array_index_";

  private final CDeclarationEdge arrayDeclarationEdge;
  private final CDeclarationEdge valueDeclarationEdge;
  private final CDeclarationEdge indexDeclarationEdge;

  private final CExpression lengthExpression;

  private TransformableArray(CDeclarationEdge pArrayDeclarationEdge) {
    arrayDeclarationEdge = pArrayDeclarationEdge;
    valueDeclarationEdge = createValueDeclarationEdge(pArrayDeclarationEdge);
    indexDeclarationEdge = createIndexDeclarationEdge(pArrayDeclarationEdge);
    lengthExpression = createLengthExpression(pArrayDeclarationEdge).orElseThrow();
  }

  private static CDeclarationEdge createValueDeclarationEdge(
      CDeclarationEdge pArrayDeclarationEdge) {

    CVariableDeclaration arrayDeclaration =
        (CVariableDeclaration) pArrayDeclarationEdge.getDeclaration();

    String valueName = VALUE_VARIABLE_PREFIX + arrayDeclaration.getName();
    String functionName = pArrayDeclarationEdge.getSuccessor().getFunctionName();
    String valueQualifiedName =
        MemoryLocation.forLocalVariable(functionName, valueName).getExtendedQualifiedName();

    CType arrayType = arrayDeclaration.getType();
    CType valueType;
    if (arrayType instanceof CArrayType) {
      valueType = ((CArrayType) arrayType).getType();
    } else if (arrayType instanceof CPointerType) {
      valueType = ((CPointerType) arrayType).getType();
    } else {
      throw new AssertionError("Unknown array type");
    }

    CVariableDeclaration valueDeclaration =
        new CVariableDeclaration(
            arrayDeclaration.getFileLocation(),
            arrayDeclaration.isGlobal(),
            arrayDeclaration.getCStorageClass(),
            valueType,
            valueName,
            valueName,
            valueQualifiedName,
            null);

    return new CDeclarationEdge(
        pArrayDeclarationEdge.getRawStatement(),
        pArrayDeclarationEdge.getFileLocation(),
        new CFANode(pArrayDeclarationEdge.getPredecessor().getFunction()),
        new CFANode(pArrayDeclarationEdge.getSuccessor().getFunction()),
        valueDeclaration);
  }

  private static CDeclarationEdge createIndexDeclarationEdge(
      CDeclarationEdge pArrayDeclarationEdge) {

    CVariableDeclaration arrayDeclaration =
        (CVariableDeclaration) pArrayDeclarationEdge.getDeclaration();

    String indexName = INDEX_VARIABLE_PREFIX + arrayDeclaration.getName();
    String functionName = pArrayDeclarationEdge.getSuccessor().getFunctionName();
    String indexQualifiedName =
        MemoryLocation.forLocalVariable(functionName, indexName).getExtendedQualifiedName();

    CVariableDeclaration valueDeclaration =
        new CVariableDeclaration(
            arrayDeclaration.getFileLocation(),
            arrayDeclaration.isGlobal(),
            arrayDeclaration.getCStorageClass(),
            CNumericTypes.INT,
            indexName,
            indexName,
            indexQualifiedName,
            null);

    return new CDeclarationEdge(
        pArrayDeclarationEdge.getRawStatement(),
        pArrayDeclarationEdge.getFileLocation(),
        new CFANode(pArrayDeclarationEdge.getPredecessor().getFunction()),
        new CFANode(pArrayDeclarationEdge.getSuccessor().getFunction()),
        valueDeclaration);
  }

  public CDeclarationEdge getArrayDeclarationEdge() {
    return arrayDeclarationEdge;
  }

  public CSimpleDeclaration getArrayDeclaration() {
    return arrayDeclarationEdge.getDeclaration();
  }

  public CDeclarationEdge getValueDeclarationEdge() {
    return valueDeclarationEdge;
  }

  public CSimpleDeclaration getValueDeclaration() {
    return valueDeclarationEdge.getDeclaration();
  }

  public CDeclarationEdge getIndexDeclarationEdge() {
    return indexDeclarationEdge;
  }

  public CSimpleDeclaration getIndexDeclaration() {
    return indexDeclarationEdge.getDeclaration();
  }

  public CExpression getLengthExpression() {
    return lengthExpression;
  }

  private static Optional<CExpression> createLengthExpression(CDeclarationEdge pDeclarationEdge) {

    CDeclaration declaration = pDeclarationEdge.getDeclaration();
    if (declaration instanceof CVariableDeclaration) {
      CType type = declaration.getType();
      if (type instanceof CArrayType) {
        return Optional.of(((CArrayType) type).getLength());
      }
    }

    return Optional.empty();
  }

  private static boolean isArrayDeclarationEdge(CDeclarationEdge pDeclarationEdge) {

    CDeclaration declaration = pDeclarationEdge.getDeclaration();
    if (declaration instanceof CVariableDeclaration) {
      CType type = declaration.getType();
      if ((type instanceof CArrayType || type instanceof CPointerType)
          && createLengthExpression(pDeclarationEdge).isPresent()) {
        return true;
      }
    }

    return false;
  }

  private static ImmutableSet<CDeclarationEdge> findArrayDeclarationEdges(CFA pCfa) {

    return pCfa.edges().stream()
        .filter(edge -> edge instanceof CDeclarationEdge)
        .map(edge -> (CDeclarationEdge) edge)
        .filter(TransformableArray::isArrayDeclarationEdge)
        .collect(ImmutableSet.toImmutableSet());
  }

  private static boolean isRelevantArrayAccessOfArray(
      ArrayAccess pArrayAccess, CSimpleDeclaration pArrayDeclaration) {

    CExpression arrayExpression = pArrayAccess.getArrayExpression();
    CExpression subscriptExpression = pArrayAccess.getSubscriptExpression();

    if (arrayExpression instanceof CIdExpression) {
      CSimpleDeclaration arrayExpressDeclaration =
          ((CIdExpression) arrayExpression).getDeclaration();
      if (arrayExpressDeclaration.equals(pArrayDeclaration)) {
        if (subscriptExpression instanceof CIntegerLiteralExpression integerExpression) {
          // we don't consider arrays as relevant if they are only accessed at index 0
          // (these accesses could come from pointers that point to a single element)
          return !integerExpression.getValue().equals(BigInteger.ZERO);
        } else {
          return true;
        }
      }
    }

    return false;
  }

  public static ImmutableSet<TransformableArray> findTransformableArrays(CFA pCfa) {

    Set<CDeclarationEdge> unproblematicArrayDeclarationEdges =
        new LinkedHashSet<>(findArrayDeclarationEdges(pCfa));
    Set<CDeclarationEdge> relevantArrayDeclarationEdges = new LinkedHashSet<>();

    for (CFAEdge edge : pCfa.edges()) {

      Iterator<CDeclarationEdge> iterator = unproblematicArrayDeclarationEdges.iterator();
      while (iterator.hasNext()) {

        CDeclarationEdge arrayDeclarationEdge = iterator.next();
        CDeclaration declaration = arrayDeclarationEdge.getDeclaration();

        // we skip the array declaration edge itself (it would otherwise be a problematic usage)
        if (arrayDeclarationEdge.equals(edge)) {
          continue;
        }

        if (ProblematicArrayUsageFinder.containsProblematicUsage(
            edge, arrayDeclarationEdge.getDeclaration())) {
          iterator.remove();
        }

        // is array declaration already relevant?
        if (relevantArrayDeclarationEdges.contains(arrayDeclarationEdge)) {
          continue;
        }

        for (ArrayAccess arrayAccess : ArrayAccess.findArrayAccesses(edge)) {
          if (isRelevantArrayAccessOfArray(arrayAccess, declaration)) {
            relevantArrayDeclarationEdges.add(arrayDeclarationEdge);
          }
        }
      }
    }

    return Sets.intersection(unproblematicArrayDeclarationEdges, relevantArrayDeclarationEdges)
        .stream()
        .map(TransformableArray::new)
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public int hashCode() {
    return arrayDeclarationEdge.hashCode();
  }

  @Override
  public boolean equals(Object pObject) {

    if (this == pObject) {
      return true;
    }

    if (!(pObject instanceof TransformableArray)) {
      return false;
    }

    TransformableArray other = (TransformableArray) pObject;
    return arrayDeclarationEdge.equals(other.arrayDeclarationEdge);
  }

  @Override
  public String toString() {
    return getClass().getName() + '[' + arrayDeclarationEdge + ']';
  }

  /**
   * Visitor that returns whether an AST node contains problematic uses of a specified array that
   * prohibit its transformation. {@code true} is returned if any problematic uses are found.
   */
  private static final class ProblematicArrayUsageFinder
      implements CAstNodeVisitor<Boolean, NoException> {

    private final CSimpleDeclaration arrayDeclaration;

    private ProblematicArrayUsageFinder(CSimpleDeclaration pArrayDeclaration) {
      arrayDeclaration = pArrayDeclaration;
    }

    private static boolean containsProblematicUsage(
        CFAEdge pEdge, CSimpleDeclaration pArrayDeclaration) {

      CAstNode astNode = null;

      if (pEdge instanceof CFunctionSummaryEdge) {
        astNode = ((CFunctionSummaryEdge) pEdge).getExpression();
      }

      Optional<? extends AAstNode> optAstNode = pEdge.getRawAST();
      if (optAstNode.isPresent()) {
        AAstNode aAstNode = optAstNode.get();
        if (aAstNode instanceof CAstNode) {
          astNode = (CAstNode) aAstNode;
        }
      }

      if (astNode != null) {
        return astNode.accept(new ProblematicArrayUsageFinder(pArrayDeclaration));
      } else {
        return false;
      }
    }

    @Override
    public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) {

      if (pIastArraySubscriptExpression.getArrayExpression() instanceof CIdExpression) {
        return pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
      }

      return pIastArraySubscriptExpression.getArrayExpression().accept(this)
          || pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    }

    @Override
    public Boolean visit(CArrayDesignator pArrayDesignator) {
      return pArrayDesignator.getSubscriptExpression().accept(this);
    }

    @Override
    public Boolean visit(CArrayRangeDesignator pArrayRangeDesignator) {
      return pArrayRangeDesignator.getFloorExpression().accept(this)
          || pArrayRangeDesignator.getCeilExpression().accept(this);
    }

    @Override
    public Boolean visit(CFieldDesignator pFieldDesignator) {
      return false;
    }

    @Override
    public Boolean visit(CInitializerExpression pInitializerExpression) {
      return pInitializerExpression.getExpression().accept(this);
    }

    @Override
    public Boolean visit(CInitializerList pInitializerList) {

      for (CInitializer initializer : pInitializerList.getInitializers()) {
        if (initializer.accept(this)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public Boolean visit(CDesignatedInitializer pCStructInitializerPart) {

      pCStructInitializerPart.getRightHandSide().accept(this);

      for (CDesignator designator : pCStructInitializerPart.getDesignators()) {
        if (designator.accept(this)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public Boolean visit(CFunctionCallExpression pIastFunctionCallExpression) {

      pIastFunctionCallExpression.getFunctionNameExpression().accept(this);

      for (CExpression expression : pIastFunctionCallExpression.getParameterExpressions()) {
        if (expression.accept(this)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public Boolean visit(CBinaryExpression pIastBinaryExpression) {
      return pIastBinaryExpression.getOperand1().accept(this)
          || pIastBinaryExpression.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression pIastCastExpression) {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) {
      return false;
    }

    @Override
    public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
      return false;
    }

    @Override
    public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
      return false;
    }

    @Override
    public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) {
      return false;
    }

    @Override
    public Boolean visit(CTypeIdExpression pIastTypeIdExpression) {
      return false;
    }

    @Override
    public Boolean visit(CUnaryExpression pIastUnaryExpression) {

      if (pIastUnaryExpression.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
        for (ArrayAccess arrayAccess : ArrayAccess.findArrayAccesses(pIastUnaryExpression)) {
          if (arrayAccess.getArrayExpression().accept(this)) {
            return true;
          }
        }
      }

      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) {
      return false;
    }

    @Override
    public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
      return false;
    }

    @Override
    public Boolean visit(CFieldReference pIastFieldReference) {
      return pIastFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public Boolean visit(CIdExpression pIastIdExpression) {
      return arrayDeclaration.equals(pIastIdExpression.getDeclaration());
    }

    @Override
    public Boolean visit(CPointerExpression pPointerExpression) {

      if (!ArrayAccess.findArrayAccesses(pPointerExpression).isEmpty()
          && ArrayAccess.findArrayAccesses(pPointerExpression.getOperand()).isEmpty()) {

        ArrayAccess arrayAccess =
            ArrayAccess.findArrayAccesses(pPointerExpression).stream().findAny().orElseThrow();
        if (arrayAccess.getArrayExpression() instanceof CIdExpression) {
          return arrayAccess.getSubscriptExpression().accept(this);
        }
      }

      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CComplexCastExpression pComplexCastExpression) {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CFunctionDeclaration pDecl) {

      for (CParameterDeclaration declaration : pDecl.getParameters()) {
        if (declaration.accept(this)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public Boolean visit(CComplexTypeDeclaration pDecl) {
      return false;
    }

    @Override
    public Boolean visit(CTypeDefDeclaration pDecl) {
      return false;
    }

    @Override
    public Boolean visit(CVariableDeclaration pDecl) {

      CInitializer initializer = pDecl.getInitializer();
      if (initializer != null) {
        return initializer.accept(this);
      }

      return false;
    }

    @Override
    public Boolean visit(CParameterDeclaration pDecl) {
      return pDecl.asVariableDeclaration().accept(this);
    }

    @Override
    public Boolean visit(CEnumerator pDecl) {
      return false;
    }

    @Override
    public Boolean visit(CExpressionStatement pIastExpressionStatement) {
      return pIastExpressionStatement.getExpression().accept(this);
    }

    @Override
    public Boolean visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {
      return pIastExpressionAssignmentStatement.getLeftHandSide().accept(this)
          || pIastExpressionAssignmentStatement.getRightHandSide().accept(this);
    }

    @Override
    public Boolean visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) {
      return pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(this)
          || pIastFunctionCallAssignmentStatement.getRightHandSide().accept(this);
    }

    @Override
    public Boolean visit(CFunctionCallStatement pIastFunctionCallStatement) {

      List<CExpression> paramExpressions =
          pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions();

      for (CExpression expression : paramExpressions) {
        if (expression.accept(this)) {
          return true;
        }
      }

      CFunctionDeclaration declaration =
          pIastFunctionCallStatement.getFunctionCallExpression().getDeclaration();
      if (declaration != null) {
        if (declaration.accept(this)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public Boolean visit(CReturnStatement pNode) {

      Optional<CExpression> optExpression = pNode.getReturnValue();

      if (optExpression.isPresent()) {
        return optExpression.orElseThrow().accept(this);
      } else {
        return false;
      }
    }
  }
}
