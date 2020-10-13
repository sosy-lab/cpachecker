// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class EdgeDefUseData {

  private final ImmutableSet<MemoryLocation> defs;
  private final ImmutableSet<MemoryLocation> uses;

  private final ImmutableSet<CExpression> pointeeDefs;
  private final ImmutableSet<CExpression> pointeeUses;

  private final boolean partialDefs;

  private EdgeDefUseData(
      ImmutableSet<MemoryLocation> pDefs,
      ImmutableSet<MemoryLocation> pUses,
      ImmutableSet<CExpression> pPointeeDefs,
      ImmutableSet<CExpression> pPointeeUses,
      boolean pPartialDefs) {

    defs = pDefs;
    uses = pUses;

    pointeeDefs = pPointeeDefs;
    pointeeUses = pPointeeUses;

    partialDefs = pPartialDefs;
  }

  public ImmutableSet<MemoryLocation> getDefs() {
    return defs;
  }

  public ImmutableSet<MemoryLocation> getUses() {
    return uses;
  }

  public ImmutableSet<CExpression> getPointeeDefs() {
    return pointeeDefs;
  }

  public ImmutableSet<CExpression> getPointeeUses() {
    return pointeeUses;
  }

  public boolean hasPartialDefs() {
    return partialDefs;
  }

  private static EdgeDefUseData createEdgeDefUseData(Collector pCollector) {

    ImmutableSet<MemoryLocation> defs = ImmutableSet.copyOf(pCollector.defs);
    ImmutableSet<MemoryLocation> uses = ImmutableSet.copyOf(pCollector.uses);

    ImmutableSet<CExpression> pointeeDefs = ImmutableSet.copyOf(pCollector.pointeeDefs);
    ImmutableSet<CExpression> pointeeUses = ImmutableSet.copyOf(pCollector.pointeeUses);

    return new EdgeDefUseData(defs, uses, pointeeDefs, pointeeUses, pCollector.partialDefs);
  }

  public static EdgeDefUseData extract(CFAEdge pEdge) {

    Optional<? extends AAstNode> optAstNode = pEdge.getRawAST().toJavaUtil();

    if (optAstNode.isPresent()) {

      AAstNode astNode = optAstNode.orElseThrow();

      if (astNode instanceof CAstNode) {

        CAstNode cAstNode = (CAstNode) astNode;
        Collector collector = new Collector();
        cAstNode.accept(collector);

        return createEdgeDefUseData(collector);
      }
    }

    return new EdgeDefUseData(
        ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(), false);
  }

  public static EdgeDefUseData extract(CExpression pExpression) {

    Collector collector = new Collector();
    pExpression.accept(collector);

    return createEdgeDefUseData(collector);
  }

  @Override
  public String toString() {

    return String.format(
        "[defs: %s, uses: %s, pointee-defs: %s, pointee-uses: %s]",
        defs.toString(), uses.toString(), pointeeDefs.toString(), pointeeUses.toString());
  }

  private static final class EdgeDefUseDataException extends RuntimeException {
    private static final long serialVersionUID = -2034884371415467901L;
  }

  private static class Collector implements CAstNodeVisitor<Void, EdgeDefUseDataException> {

    private final Set<MemoryLocation> defs;
    private final Set<MemoryLocation> uses;

    private final Set<CExpression> pointeeDefs;
    private final Set<CExpression> pointeeUses;

    private boolean partialDefs;

    private Mode mode;

    private Collector() {

      partialDefs = false;

      mode = Mode.USE;

      defs = new HashSet<>();
      uses = new HashSet<>();

      pointeeDefs = new HashSet<>();
      pointeeUses = new HashSet<>();
    }

    @Override
    public Void visit(CArrayDesignator pArrayDesignator) throws EdgeDefUseDataException {

      pArrayDesignator.getSubscriptExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CArrayRangeDesignator pArrayRangeDesignator) throws EdgeDefUseDataException {

      pArrayRangeDesignator.getFloorExpression().accept(this);
      pArrayRangeDesignator.getCeilExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CFieldDesignator pFieldDesignator) throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CInitializerExpression pInitializerExpression)
        throws EdgeDefUseDataException {

      pInitializerExpression.getExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CInitializerList pInitializerList) throws EdgeDefUseDataException {

      for (CInitializer initializer : pInitializerList.getInitializers()) {
        initializer.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CDesignatedInitializer pCStructInitializerPart)
        throws EdgeDefUseDataException {

      pCStructInitializerPart.getRightHandSide().accept(this);

      for (CDesignator designator : pCStructInitializerPart.getDesignators()) {
        designator.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression pIastFunctionCallExpression)
        throws EdgeDefUseDataException {

      pIastFunctionCallExpression.getFunctionNameExpression().accept(this);

      for (CExpression expression : pIastFunctionCallExpression.getParameterExpressions()) {
        expression.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CBinaryExpression pIastBinaryExpression) throws EdgeDefUseDataException {

      pIastBinaryExpression.getOperand1().accept(this);
      pIastBinaryExpression.getOperand2().accept(this);

      return null;
    }

    @Override
    public Void visit(CCastExpression pIastCastExpression) throws EdgeDefUseDataException {

      pIastCastExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CCharLiteralExpression pIastCharLiteralExpression)
        throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression)
        throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
        throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CStringLiteralExpression pIastStringLiteralExpression)
        throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CTypeIdExpression pIastTypeIdExpression) throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pIastUnaryExpression) throws EdgeDefUseDataException {

      pIastUnaryExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CImaginaryLiteralExpression PIastLiteralExpression)
        throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression)
        throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws EdgeDefUseDataException {

      Mode prev = mode;

      pIastArraySubscriptExpression.getArrayExpression().accept(this);

      mode = Mode.USE;
      pIastArraySubscriptExpression.getSubscriptExpression().accept(this);

      mode = prev;

      if (mode == Mode.DEF) {
        partialDefs = true;
      }

      return null;
    }

    @Override
    public Void visit(CFieldReference pIastFieldReference) throws EdgeDefUseDataException {

      if (pIastFieldReference.isPointerDereference()) {

        Mode prev = mode;

        mode = Mode.USE;
        pIastFieldReference.getFieldOwner().accept(this);

        mode = prev;

        Set<CExpression> pointeeSet = (mode == Mode.USE ? pointeeUses : pointeeDefs);
        pointeeSet.add(pIastFieldReference);

      } else {
        pIastFieldReference.getFieldOwner().accept(this);
      }

      if (mode == Mode.DEF) {
        partialDefs = true;
      }

      CType type = pIastFieldReference.getFieldOwner().getExpressionType();

      while (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();
      }

      if (type instanceof CComplexType) {
        String name = ((CComplexType) type).getQualifiedName();
        Set<MemoryLocation> set = (mode == Mode.USE ? uses : defs);
        set.add(MemoryLocation.valueOf(name));
      }

      return null;
    }

    @Override
    public Void visit(CIdExpression pIastIdExpression) throws EdgeDefUseDataException {

      CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();

      if (declaration instanceof CVariableDeclaration
          || declaration instanceof CParameterDeclaration) {

        MemoryLocation memLoc = MemoryLocation.valueOf(declaration.getQualifiedName());
        Set<MemoryLocation> set = (mode == Mode.USE ? uses : defs);
        set.add(memLoc);
      }

      return null;
    }

    @Override
    public Void visit(CPointerExpression pPointerExpression) throws EdgeDefUseDataException {

      Mode prev = mode;

      mode = Mode.USE;
      pPointerExpression.getOperand().accept(this);

      mode = prev;

      Set<CExpression> pointeeSet = (mode == Mode.USE ? pointeeUses : pointeeDefs);
      pointeeSet.add(pPointerExpression);

      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pComplexCastExpression)
        throws EdgeDefUseDataException {

      pComplexCastExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionDeclaration pDecl) throws EdgeDefUseDataException {

      for (CParameterDeclaration declaration : pDecl.getParameters()) {
        declaration.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CComplexTypeDeclaration pDecl) throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CTypeDefDeclaration pDecl) throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CVariableDeclaration pDecl) throws EdgeDefUseDataException {

      MemoryLocation memLoc = MemoryLocation.valueOf(pDecl.getQualifiedName());
      defs.add(memLoc);

      CInitializer initializer = pDecl.getInitializer();
      if (initializer != null) {
        mode = Mode.USE;
        initializer.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CParameterDeclaration pDecl) throws EdgeDefUseDataException {

      pDecl.asVariableDeclaration().accept(this);

      return null;
    }

    @Override
    public Void visit(CEnumerator pDecl) throws EdgeDefUseDataException {
      return null;
    }

    @Override
    public Void visit(CExpressionStatement pIastExpressionStatement)
        throws EdgeDefUseDataException {

      pIastExpressionStatement.getExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
        throws EdgeDefUseDataException {

      mode = Mode.DEF;
      pIastExpressionAssignmentStatement.getLeftHandSide().accept(this);

      mode = Mode.USE;
      pIastExpressionAssignmentStatement.getRightHandSide().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
        throws EdgeDefUseDataException {

      mode = Mode.DEF;
      pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(this);

      mode = Mode.USE;
      pIastFunctionCallAssignmentStatement.getRightHandSide().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pIastFunctionCallStatement)
        throws EdgeDefUseDataException {

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
    public Void visit(CReturnStatement pNode) throws EdgeDefUseDataException {

      Optional<CExpression> optExpression = pNode.getReturnValue().toJavaUtil();

      if (optExpression.isPresent()) {
        return optExpression.orElseThrow().accept(this);
      } else {
        return null;
      }
    }
  }

  private enum Mode {
    DEF,
    USE
  }
}
