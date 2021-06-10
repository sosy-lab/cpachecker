// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Represents an array in a program that can be transformed/abstracted.
 *
 * <p>Instances of {@code TransformableArray} are for a specific array (identified by its memory
 * location) and contain information about the array type and all CFA-edges that access the array.
 */
public final class TransformableArray {

  private final MemoryLocation memoryLocation;
  private final CArrayType type;

  private final ImmutableSet<CFAEdge> readEdges;
  private final ImmutableSet<CFAEdge> writeEdges;

  private TransformableArray(
      MemoryLocation pMemoryLocation,
      CArrayType pType,
      ImmutableSet<CFAEdge> pReadEdges,
      ImmutableSet<CFAEdge> pWriteEdges) {

    memoryLocation = pMemoryLocation;
    type = pType;

    readEdges = pReadEdges;
    writeEdges = pWriteEdges;
  }

  private static void collectArrayReadWriteEdges(
      CFA pCfa, Map<MemoryLocation, TransformableArray.Builder> pBuilders) {

    for (CFANode node : pCfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        Optional<? extends AAstNode> optAstNode = edge.getRawAST();
        if (optAstNode.isPresent()) {

          AAstNode astNode = optAstNode.get();

          if (astNode instanceof CAstNode) {

            CAstNode cAstNode = (CAstNode) astNode;
            ArrayOperationFinder arrayFinder =
                new ArrayOperationFinder() {

                  @Override
                  protected void handleArrayOperation(
                      ArrayOperationType pType,
                      MemoryLocation pArrayMemoryLocation,
                      CExpression pIndexExpression) {
                    TransformableArray.Builder builder = pBuilders.get(pArrayMemoryLocation);
                    if (builder != null) {
                      if (pType == ArrayOperationType.READ) {
                        builder.addArrayReadEdge(edge);
                      } else {
                        builder.addArrayWriteEdge(edge);
                      }
                    }
                  }
                };

            cAstNode.accept(arrayFinder);
          }
        }
      }
    }
  }

  /**
   * Returns all transformable arrays in the specified CFA.
   *
   * @param pCfa the CFA to find transformable arrays in
   * @return all transformable arrays in the specified CFA
   * @throws NullPointerException if {@code pCfa == null}
   */
  public static ImmutableSet<TransformableArray> getTransformableArrays(CFA pCfa) {

    Objects.requireNonNull(pCfa, "pCfa must not be null");

    Map<MemoryLocation, TransformableArray.Builder> builders = new HashMap<>();

    for (CFANode node : pCfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (edge instanceof CDeclarationEdge) {
          CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
          CType type = declaration.getType();
          if (declaration instanceof CVariableDeclaration && type instanceof CArrayType) {
            MemoryLocation arrayMemoryLocation =
                MemoryLocation.valueOf(declaration.getQualifiedName());
            TransformableArray.Builder builder =
                new TransformableArray.Builder(arrayMemoryLocation, (CArrayType) type);
            builders.put(arrayMemoryLocation, builder);
          }
        }
      }
    }

    collectArrayReadWriteEdges(pCfa, builders);

    ImmutableSet.Builder<TransformableArray> transformableArrays = ImmutableSet.builder();
    for (TransformableArray.Builder builder : builders.values()) {
      transformableArrays.add(builder.build());
    }

    return transformableArrays.build();
  }

  /**
   * Returns all array operations for the specified CFA-edge.
   *
   * @param pEdge the CFA-edge to get the array operations for
   * @return all array operations for the specified CFA-edge
   */
  public static ImmutableSet<ArrayOperation> getArrayOperations(CFAEdge pEdge) {

    Objects.requireNonNull(pEdge, "pEdge must not be null");

    if (pEdge instanceof CFunctionSummaryEdge) {
      return getArrayOperations(((CFunctionSummaryEdge) pEdge).getExpression());
    }

    Optional<? extends AAstNode> optAstNode = pEdge.getRawAST();
    if (optAstNode.isPresent()) {
      AAstNode astNode = optAstNode.get();
      if (astNode instanceof CAstNode) {
        return getArrayOperations((CAstNode) astNode);
      }
    }

    return ImmutableSet.of();
  }

  /**
   * Returns all array operations for the specified AST-node.
   *
   * @param pCAstNode the AST-node to get the array operations for
   * @return all array operations for the specified AST-node
   */
  public static ImmutableSet<ArrayOperation> getArrayOperations(CAstNode pCAstNode) {

    Objects.requireNonNull(pCAstNode, "pCAstNode must not be null");

    ImmutableSet.Builder<ArrayOperation> builder = ImmutableSet.builder();

    ArrayOperationFinder arrayFinder =
        new ArrayOperationFinder() {

          @Override
          protected void handleArrayOperation(
              ArrayOperationType pType,
              MemoryLocation pArrayMemoryLocation,
              CExpression pIndexExpression) {
            builder.add(new ArrayOperation(pType, pArrayMemoryLocation, pIndexExpression));
          }
        };
    pCAstNode.accept(arrayFinder);

    return builder.build();
  }

  /**
   * Returns the memory location of this transformable array.
   *
   * @return the memory location of this transformable array
   */
  public MemoryLocation getMemoryLocation() {
    return memoryLocation;
  }

  /**
   * Returns the array type of this transformable array.
   *
   * @return the array type of this transformable array
   */
  public CArrayType getArrayType() {
    return type;
  }

  /**
   * Returns the set of CFA-edges that read from this transformable array.
   *
   * @return the set of CFA-edges that read from this transformable array
   */
  public ImmutableSet<CFAEdge> getReadEdges() {
    return readEdges;
  }

  /**
   * Returns the set of CFA-edges that write to this transformable array.
   *
   * @return the set of CFA-edges that write to this transformable array
   */
  public ImmutableSet<CFAEdge> getWriteEdges() {
    return writeEdges;
  }

  @Override
  public String toString() {

    return String.format(
        Locale.ENGLISH,
        "%s[memoryLocation=%s,arrayType=%s,#readEdges=%d,#writeEdges=%d]",
        getClass().getName(),
        memoryLocation,
        type,
        readEdges.size(),
        writeEdges.size());
  }

  /** Represents the type of an array operation (read/write). */
  public enum ArrayOperationType {
    WRITE,
    READ
  }

  /**
   * Represents an operation involving array access (either read or write).
   *
   * <p>Instances of {@code ArrayOperation} are of a specific type (read/write), for a specific
   * array (identified by its memory location), and are on a specific index (defined by the array
   * subscript expression).
   */
  public static final class ArrayOperation {

    private final ArrayOperationType type;
    private final MemoryLocation arrayMemoryLocation;
    private final CExpression indexExpression;

    private ArrayOperation(
        ArrayOperationType pType,
        MemoryLocation pArrayMemoryLocation,
        CExpression pIndexExpression) {
      type = pType;
      arrayMemoryLocation = pArrayMemoryLocation;
      indexExpression = pIndexExpression;
    }

    /**
     * Returns the type of this array operation (read/write).
     *
     * @return the type of this array operation (read/write)
     */
    public ArrayOperationType getType() {
      return type;
    }

    /**
     * Returns the memory location of the array accessed by this operation.
     *
     * @return the memory location of the array accessed by this operation
     */
    public MemoryLocation getArrayMemoryLocation() {
      return arrayMemoryLocation;
    }

    /**
     * Return the array subscript expression for this array operation.
     *
     * @return the array subscript expression for this array operation
     */
    public CExpression getIndexExpression() {
      return indexExpression;
    }

    /**
     * Returns an {@code ArrayOperation} with the same array and index but where the type is {@code
     * ArrayOperationType.READ}.
     *
     * @return an {@code ArrayOperation} with the same array and index but where the type is {@code
     *     ArrayOperationType.READ}.
     */
    public ArrayOperation toReadOperation() {
      return new ArrayOperation(ArrayOperationType.READ, arrayMemoryLocation, indexExpression);
    }

    /**
     * Returns an {@code ArrayOperation} with the same array and index but where the type is {@code
     * ArrayOperationType.WRITE}.
     *
     * @return an {@code ArrayOperation} with the same array and index but where the type is {@code
     *     ArrayOperationType.WRITE}.
     */
    public ArrayOperation toWriteOperation() {
      return new ArrayOperation(ArrayOperationType.WRITE, arrayMemoryLocation, indexExpression);
    }

    @Override
    public int hashCode() {
      return Objects.hash(arrayMemoryLocation, indexExpression, type);
    }

    @Override
    public boolean equals(Object pObject) {
      if (this == pObject) {
        return true;
      }

      if (!(pObject instanceof ArrayOperation)) {
        return false;
      }

      ArrayOperation other = (ArrayOperation) pObject;
      return Objects.equals(arrayMemoryLocation, other.arrayMemoryLocation)
          && Objects.equals(indexExpression, other.indexExpression)
          && type == other.type;
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[type=%s, arrayMemoryLocation=%s, indexExpression=%s]",
          getClass().getName(),
          type,
          arrayMemoryLocation,
          indexExpression);
    }
  }

  /**
   * Builder for creation of {@link TransformableArray} instances.
   *
   * <p>CFA-edges are added using the {@code addArray{Read,Write}Edge} methods.
   */
  private static final class Builder {

    private final MemoryLocation memoryLocation;
    private final CArrayType type;
    private final ImmutableSet.Builder<CFAEdge> readEdges;
    private final ImmutableSet.Builder<CFAEdge> writeEdges;

    private Builder(MemoryLocation pMemoryLocation, CArrayType pType) {
      memoryLocation = pMemoryLocation;
      type = pType;

      readEdges = ImmutableSet.builder();
      writeEdges = ImmutableSet.builder();
    }

    private void addArrayReadEdge(CFAEdge pEdge) {
      readEdges.add(pEdge);
    }

    private void addArrayWriteEdge(CFAEdge pEdge) {
      writeEdges.add(pEdge);
    }

    private TransformableArray build() {
      return new TransformableArray(memoryLocation, type, readEdges.build(), writeEdges.build());
    }
  }

  /** Dummy exception that is never thrown. */
  private static final class DummyException extends RuntimeException {
    private static final long serialVersionUID = 3060142078242154867L;
  }

  /** AST-visitor for finding array operations. */
  private abstract static class ArrayOperationFinder
      implements CAstNodeVisitor<Void, DummyException> {

    private ArrayOperationType mode;

    private ArrayOperationFinder() {
      mode = ArrayOperationType.READ;
    }

    /**
     * Handles an array operation found by this AST-visitor.
     *
     * <p>The method is called for every detected array operation.
     *
     * @param pType the type of the array operation (read/write)
     * @param pArrayMemoryLocation the memory location of the array operated on
     * @param pIndexExpression the array subscript expression
     */
    protected abstract void handleArrayOperation(
        ArrayOperationType pType,
        MemoryLocation pArrayMemoryLocation,
        CExpression pIndexExpression);

    @Override
    public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws DummyException {

      ArrayOperationType prev = mode;

      CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();
      CExpression subscriptExpression = pIastArraySubscriptExpression.getSubscriptExpression();

      if (arrayExpression instanceof CIdExpression) {
        CIdExpression arrayIdExpression = (CIdExpression) arrayExpression;
        String qualifiedName = arrayIdExpression.getDeclaration().getQualifiedName();
        MemoryLocation arrayMemoryLocation = MemoryLocation.valueOf(qualifiedName);
        handleArrayOperation(mode, arrayMemoryLocation, subscriptExpression);
      }

      mode = ArrayOperationType.READ;
      subscriptExpression.accept(this);

      mode = prev;

      return null;
    }

    @Override
    public Void visit(CArrayDesignator pArrayDesignator) throws DummyException {

      pArrayDesignator.getSubscriptExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CArrayRangeDesignator pArrayRangeDesignator) throws DummyException {

      pArrayRangeDesignator.getFloorExpression().accept(this);
      pArrayRangeDesignator.getCeilExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CFieldDesignator pFieldDesignator) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CInitializerExpression pInitializerExpression) throws DummyException {

      pInitializerExpression.getExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CInitializerList pInitializerList) throws DummyException {

      for (CInitializer initializer : pInitializerList.getInitializers()) {
        initializer.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CDesignatedInitializer pCStructInitializerPart) throws DummyException {

      pCStructInitializerPart.getRightHandSide().accept(this);

      for (CDesignator designator : pCStructInitializerPart.getDesignators()) {
        designator.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression pIastFunctionCallExpression) throws DummyException {

      pIastFunctionCallExpression.getFunctionNameExpression().accept(this);

      for (CExpression expression : pIastFunctionCallExpression.getParameterExpressions()) {
        expression.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CBinaryExpression pIastBinaryExpression) throws DummyException {

      pIastBinaryExpression.getOperand1().accept(this);
      pIastBinaryExpression.getOperand2().accept(this);

      return null;
    }

    @Override
    public Void visit(CCastExpression pIastCastExpression) throws DummyException {

      pIastCastExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CCharLiteralExpression pIastCharLiteralExpression) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
        throws DummyException {
      return null;
    }

    @Override
    public Void visit(CStringLiteralExpression pIastStringLiteralExpression) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CTypeIdExpression pIastTypeIdExpression) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pIastUnaryExpression) throws DummyException {

      pIastUnaryExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CImaginaryLiteralExpression PIastLiteralExpression) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CFieldReference pIastFieldReference) throws DummyException {

      if (pIastFieldReference.isPointerDereference()) {

        ArrayOperationType prev = mode;

        mode = ArrayOperationType.READ;
        pIastFieldReference.getFieldOwner().accept(this);

        mode = prev;

      } else {
        pIastFieldReference.getFieldOwner().accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CIdExpression pIastIdExpression) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CPointerExpression pPointerExpression) throws DummyException {

      ArrayOperationType prev = mode;

      mode = ArrayOperationType.READ;
      pPointerExpression.getOperand().accept(this);

      mode = prev;

      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pComplexCastExpression) throws DummyException {

      pComplexCastExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionDeclaration pDecl) throws DummyException {

      for (CParameterDeclaration declaration : pDecl.getParameters()) {
        declaration.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CComplexTypeDeclaration pDecl) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CTypeDefDeclaration pDecl) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CVariableDeclaration pDecl) throws DummyException {

      CInitializer initializer = pDecl.getInitializer();
      if (initializer != null) {
        mode = ArrayOperationType.READ;
        initializer.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CParameterDeclaration pDecl) throws DummyException {

      pDecl.asVariableDeclaration().accept(this);

      return null;
    }

    @Override
    public Void visit(CEnumerator pDecl) throws DummyException {
      return null;
    }

    @Override
    public Void visit(CExpressionStatement pIastExpressionStatement) throws DummyException {

      pIastExpressionStatement.getExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
        throws DummyException {

      mode = ArrayOperationType.WRITE;
      pIastExpressionAssignmentStatement.getLeftHandSide().accept(this);

      mode = ArrayOperationType.READ;
      pIastExpressionAssignmentStatement.getRightHandSide().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
        throws DummyException {

      mode = ArrayOperationType.WRITE;
      pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(this);

      mode = ArrayOperationType.READ;
      pIastFunctionCallAssignmentStatement.getRightHandSide().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pIastFunctionCallStatement) throws DummyException {

      List<CExpression> paramExpressions =
          pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions();

      for (CExpression expression : paramExpressions) {
        expression.accept(this);
      }

      CFunctionDeclaration declaration =
          pIastFunctionCallStatement.getFunctionCallExpression().getDeclaration();
      if (declaration != null) {
        declaration.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CReturnStatement pNode) throws DummyException {

      Optional<CExpression> optExpression = pNode.getReturnValue();

      if (optExpression.isPresent()) {
        return optExpression.get().accept(this);
      } else {
        return null;
      }
    }
  }
}
