// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.taint")
class TaintTransferRelation extends ForwardingTransferRelation<TaintState, TaintState, Precision> {

  private static final MemoryLocation UNKNOWN = MemoryLocation.forIdentifier("__UNKNOWN");

  @Option(secure = true, name = "sourceFunctions", description = "Set of taint-source functions.")
  private Set<String> taintSourceFunctions = ImmutableSet.of();

  @Option(secure = true, name = "sinkFunctions", description = "Set of taint-sink functions.")
  private Set<String> taintSinkFunctions = ImmutableSet.of();

  private final CSystemDependenceGraph sdg;
  private final Multimap<CFAEdge, CSystemDependenceGraph.Node> nodesPerCfaNode =
      ArrayListMultimap.create();

  TaintTransferRelation(Configuration pConfig, CSystemDependenceGraph pSdg)
      throws InvalidConfigurationException {
    sdg = pSdg;
    for (CSystemDependenceGraph.Node node : sdg.getNodes()) {
      Optional<CFAEdge> optCfaEdge = node.getStatement();
      if (optCfaEdge.isPresent()) {
        nodesPerCfaNode.put(optCfaEdge.orElseThrow(), node);
      }
    }

    pConfig.inject(this);
  }

  private static boolean containsTaintedUse(TaintState pTaintState, Set<MemoryLocation> pUses) {
    for (MemoryLocation use : pUses) {
      if (pTaintState.isTainted(use)) {
        return true;
      }
    }
    return false;
  }

  private TaintState handleEdge(TaintState pTaintState, CFAEdge pEdge) {
    TaintState newTaintState = pTaintState;
    Optional<AAstNode> optAstNode = pEdge.getRawAST();
    if (optAstNode.isPresent()) {
      AAstNode astNode = optAstNode.orElseThrow();
      if (astNode instanceof CAstNode) {
        CAstNode cAstNode = (CAstNode) astNode;
        DefUseCollector defUseCollector = new DefUseCollector();
        cAstNode.accept(defUseCollector);

        if (newTaintState.isTainted(pEdge)
            || containsTaintedUse(newTaintState, defUseCollector.getUses())) {
          // taint defs
          for (MemoryLocation def : defUseCollector.getDefs()) {
            newTaintState = newTaintState.taint(def);
          }
        } else if (!defUseCollector.hasPartialDefs()) {
          // untaint defs
          for (MemoryLocation def : defUseCollector.getDefs()) {
            newTaintState = newTaintState.untaint(def);
          }
        }
      }
    }
    return newTaintState;
  }

  @Override
  protected @Nullable TaintState handleAssumption(
      CAssumeEdge pEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException, InterruptedException {

    TaintState newTaintState = handleEdge(state, pEdge);
    DefUseCollector defUseCollector = new DefUseCollector();
    pExpression.accept(defUseCollector);

    if (newTaintState.isTainted(pEdge)
        || containsTaintedUse(newTaintState, defUseCollector.getUses())) {
      // taint control-dependent edges
      var sdgVisitor = new ControlDependentEdgeCollectingSdgVisitor();
      sdg.traverse(nodesPerCfaNode.get(pEdge), sdgVisitor);

      for (CFAEdge dependentEdge : sdgVisitor.getDependentEdges()) {
        newTaintState = newTaintState.taint(dependentEdge);
      }
    }

    return newTaintState;
  }

  @Override
  protected TaintState handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleStatementEdge(CStatementEdge pEdge, CStatement pStatement)
      throws CPATransferException {
    TaintState newTaintState = state;
    if (pStatement instanceof CFunctionCall) {
      CFunctionCall functionCall = (CFunctionCall) pStatement;
      CFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
      CExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
      if (functionNameExpression instanceof CIdExpression) {
        String calledFunctionName = ((CIdExpression) functionNameExpression).getName();
        if (taintSourceFunctions.contains(calledFunctionName)) {
          newTaintState = newTaintState.taint(pEdge);
        }
        if (taintSinkFunctions.contains(calledFunctionName)) {
          newTaintState = newTaintState.taintError();
        }
      }
    }
    return handleEdge(newTaintState, pEdge);
  }

  private static final class ControlDependentEdgeCollectingSdgVisitor
      implements CSystemDependenceGraph.ForwardsVisitor {

    private final Set<CFAEdge> dependentEdges;

    private ControlDependentEdgeCollectingSdgVisitor() {
      dependentEdges = new LinkedHashSet<>();
    }

    private Set<CFAEdge> getDependentEdges() {
      return dependentEdges;
    }

    @Override
    public SystemDependenceGraph.VisitResult visitNode(CSystemDependenceGraph.Node pNode) {
      return SystemDependenceGraph.VisitResult.CONTINUE;
    }

    @Override
    public SystemDependenceGraph.VisitResult visitEdge(
        SystemDependenceGraph.EdgeType pType,
        CSystemDependenceGraph.Node pPredecessor,
        CSystemDependenceGraph.Node pSuccessor) {

      if (pType == SystemDependenceGraph.EdgeType.CONTROL_DEPENDENCY) {
        pSuccessor.getStatement().ifPresent(dependentEdges::add);
      }

      return SystemDependenceGraph.VisitResult.SKIP;
    }
  }

  private static class DefUseCollector implements CAstNodeVisitor<Void, NoException> {

    private final Set<MemoryLocation> defs = new LinkedHashSet<>();
    private final Set<MemoryLocation> uses = new LinkedHashSet<>();

    private boolean partialDefs = false;

    private Mode mode = Mode.USE;

    private Set<MemoryLocation> getDefs() {
      return defs;
    }

    private Set<MemoryLocation> getUses() {
      return uses;
    }

    private boolean hasPartialDefs() {
      return partialDefs;
    }

    @Override
    public Void visit(CArrayDesignator pArrayDesignator) {

      pArrayDesignator.getSubscriptExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CArrayRangeDesignator pArrayRangeDesignator) {

      pArrayRangeDesignator.getFloorExpression().accept(this);
      pArrayRangeDesignator.getCeilExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CFieldDesignator pFieldDesignator) {
      return null;
    }

    @Override
    public Void visit(CInitializerExpression pInitializerExpression) {

      pInitializerExpression.getExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CInitializerList pInitializerList) {

      for (CInitializer initializer : pInitializerList.getInitializers()) {
        initializer.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CDesignatedInitializer pCStructInitializerPart) {

      pCStructInitializerPart.getRightHandSide().accept(this);

      for (CDesignator designator : pCStructInitializerPart.getDesignators()) {
        designator.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression pIastFunctionCallExpression) {

      pIastFunctionCallExpression.getFunctionNameExpression().accept(this);

      for (CExpression expression : pIastFunctionCallExpression.getParameterExpressions()) {
        expression.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CBinaryExpression pIastBinaryExpression) {

      pIastBinaryExpression.getOperand1().accept(this);
      pIastBinaryExpression.getOperand2().accept(this);

      return null;
    }

    @Override
    public Void visit(CCastExpression pIastCastExpression) {

      pIastCastExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CCharLiteralExpression pIastCharLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CStringLiteralExpression pIastStringLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CTypeIdExpression pIastTypeIdExpression) {
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pIastUnaryExpression) {

      pIastUnaryExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CImaginaryLiteralExpression PIastLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression) {

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
    public Void visit(CFieldReference pIastFieldReference) {

      if (pIastFieldReference.isPointerDereference()) {

        Mode prev = mode;

        mode = Mode.USE;
        pIastFieldReference.getFieldOwner().accept(this);

        mode = prev;

        // TODO: handle pointers properly
        if (mode == Mode.DEF) {
          defs.add(UNKNOWN);
        } else {
          uses.add(UNKNOWN);
        }

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
        set.add(MemoryLocation.parseExtendedQualifiedName(name));
      }

      return null;
    }

    @Override
    public Void visit(CIdExpression pIastIdExpression) {

      CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();

      if (declaration instanceof CVariableDeclaration
          || declaration instanceof CParameterDeclaration) {

        MemoryLocation memLoc = MemoryLocation.forDeclaration(declaration);
        Set<MemoryLocation> set = (mode == Mode.USE ? uses : defs);
        set.add(memLoc);
      }

      return null;
    }

    @Override
    public Void visit(CPointerExpression pPointerExpression) {

      Mode prev = mode;

      mode = Mode.USE;
      pPointerExpression.getOperand().accept(this);

      mode = prev;

      // TODO: handle pointers
      if (mode == Mode.DEF) {
        defs.add(UNKNOWN);
      } else {
        uses.add(UNKNOWN);
      }

      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pComplexCastExpression) {

      pComplexCastExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionDeclaration pDecl) {

      for (CParameterDeclaration declaration : pDecl.getParameters()) {
        declaration.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CComplexTypeDeclaration pDecl) {
      return null;
    }

    @Override
    public Void visit(CTypeDefDeclaration pDecl) {
      return null;
    }

    @Override
    public Void visit(CVariableDeclaration pDecl) {

      MemoryLocation memLoc = MemoryLocation.forDeclaration(pDecl);
      defs.add(memLoc);

      CInitializer initializer = pDecl.getInitializer();
      if (initializer != null) {
        mode = Mode.USE;
        initializer.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CParameterDeclaration pDecl) {

      // undeclared functions don't have qualified parameter names, so we ignore them
      if (pDecl.getQualifiedName() != null) {
        pDecl.asVariableDeclaration().accept(this);
      }

      return null;
    }

    @Override
    public Void visit(CEnumerator pDecl) {
      return null;
    }

    @Override
    public Void visit(CExpressionStatement pIastExpressionStatement) {

      pIastExpressionStatement.getExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {

      mode = Mode.DEF;
      pIastExpressionAssignmentStatement.getLeftHandSide().accept(this);

      mode = Mode.USE;
      pIastExpressionAssignmentStatement.getRightHandSide().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) {

      mode = Mode.DEF;
      pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(this);

      mode = Mode.USE;
      pIastFunctionCallAssignmentStatement.getRightHandSide().accept(this);

      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pIastFunctionCallStatement) {

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
    public Void visit(CReturnStatement pNode) {

      Optional<CExpression> optExpression = pNode.getReturnValue();

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
