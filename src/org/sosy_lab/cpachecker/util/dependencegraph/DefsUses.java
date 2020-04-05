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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

final class DefsUses {

  private DefsUses() {}

  private static Set<MemoryLocation> getDefs(
      CLeftHandSide pLeftHandSide, PointerState pPointerState) throws CPATransferException {
    Set<MemoryLocation> decls;
    Collector collector = new Collector(pPointerState, Optional.empty());
    if (pLeftHandSide instanceof CPointerExpression) {
      // pointers are unsupported (for now)
      return null;
    } else if (pLeftHandSide instanceof CArraySubscriptExpression) {
      decls = ((CArraySubscriptExpression) pLeftHandSide).getArrayExpression().accept(collector);
    } else {
      decls = pLeftHandSide.accept(collector);
    }
    return decls;
  }

  private static Set<MemoryLocation> getUses(CAstNode pExpression, PointerState pPointerState)
      throws CPATransferException {
    Collector collector = new Collector(pPointerState, Optional.empty());
    return pExpression.accept(collector);
  }

  public static DefsUses.Data getCallDefs(FunctionCallEdge pEdge) {
    List<? extends AParameterDeclaration> params = pEdge.getSuccessor().getFunctionParameters();

    Iterator<MemoryLocation> iterator =
        Iterators.transform(
            params.iterator(), param -> MemoryLocation.valueOf(param.getQualifiedName()));
    Set<MemoryLocation> defs =
        ImmutableSet.<MemoryLocation>builderWithExpectedSize(params.size())
            .addAll(iterator)
            .build();
    Set<MemoryLocation> uses = ImmutableSet.of();

    return new Data(pEdge, defs, uses);
  }

  public static DefsUses.Data getEmptyData(CFAEdge pEdge) {
    return new Data(pEdge, ImmutableSet.of(), ImmutableSet.of());
  }

  private static Optional<DefsUses.Data> getDeclarationData(CDeclarationEdge pEdge)
      throws CPATransferException {

    CDeclaration declaration = pEdge.getDeclaration();

    if (declaration instanceof CVariableDeclaration) {

      CVariableDeclaration varDecl = (CVariableDeclaration) declaration;
      CInitializer initializer = varDecl.getInitializer();
      Set<MemoryLocation> defs =
          ImmutableSet.of(MemoryLocation.valueOf(varDecl.getQualifiedName()));

      if (initializer == null) {

        return Optional.of(new Data(pEdge, defs, ImmutableSet.of()));

      } else if (initializer instanceof CInitializerExpression) {

        Set<MemoryLocation> uses =
            getUses(
                ((CInitializerExpression) initializer).getExpression(), PointerState.INITIAL_STATE);

        if (uses != null) {
          return Optional.of(new Data(pEdge, defs, uses));
        }
      } else if (initializer instanceof CInitializerList) {
        CInitializerList initList = (CInitializerList) initializer;

        Set<MemoryLocation> combinedUses = new HashSet<>();

        for (CInitializer init : initList.getInitializers()) {

          if (init instanceof CInitializerExpression) {

            Set<MemoryLocation> uses =
                getUses(
                    ((CInitializerExpression) init).getExpression(), PointerState.INITIAL_STATE);

            if (uses != null) {
              combinedUses.addAll(uses);
            } else {
              break;
            }
          }
        }

        return Optional.of(new Data(pEdge, defs, combinedUses));
      } else {
        throw new AssertionError("Unsupported CInitializer: " + initializer.getClass());
      }
    } else if (declaration instanceof CFunctionDeclaration) {
      return Optional.of(new Data(pEdge, ImmutableSet.of(), ImmutableSet.of()));
    }

    return Optional.empty();
  }

  public static Optional<DefsUses.Data> getData(CFAEdge pEdge) {

    if (pEdge instanceof CStatementEdge) {
      CStatement statement = ((CStatementEdge) pEdge).getStatement();
      if (statement instanceof CAssignment) {

        CAssignment assignment = (CAssignment) statement;

        try {
          Set<MemoryLocation> defs =
              getDefs(assignment.getLeftHandSide(), PointerState.INITIAL_STATE);
          Set<MemoryLocation> uses =
              getUses(assignment.getRightHandSide(), PointerState.INITIAL_STATE);
          if (defs != null && uses != null) {
            return Optional.of(new Data(pEdge, defs, uses));
          } else {
            return Optional.empty();
          }
        } catch (CPATransferException ex) {
          // TODO: handle exception
        }
      }
    } else if (pEdge instanceof CDeclarationEdge) {

      try {
        return getDeclarationData((CDeclarationEdge) pEdge);
      } catch (CPATransferException e) {
        // TODO: handle exception
      }

    } else {
      Optional<? extends AAstNode> optAst = pEdge.getRawAST().toJavaUtil();
      if (optAst.isPresent()) {
        AAstNode ast = optAst.get();

        if (ast instanceof CAstNode) {
          Set<MemoryLocation> defs = ImmutableSet.of();
          try {
            Set<MemoryLocation> uses = getUses((CAstNode) ast, PointerState.INITIAL_STATE);
            if (defs != null && uses != null) {
              return Optional.of(new Data(pEdge, defs, uses));
            } else {
              return Optional.empty();
            }
          } catch (CPATransferException ex) {
            // TODO: handle exception
          }
        } else {
          throw new AssertionError("Only CAstNode supported");
        }
      }
    }

    return Optional.of(new Data(pEdge, ImmutableSet.of(), ImmutableSet.of()));
  }

  public static class Data {

    private final CFAEdge edge;
    private final Set<MemoryLocation> defs;
    private final Set<MemoryLocation> uses;

    private Data(CFAEdge pEdge, Set<MemoryLocation> pDefs, Set<MemoryLocation> pUses) {
      edge = pEdge;
      defs = pDefs;
      uses = pUses;
    }

    public CFAEdge getEdge() {
      return edge;
    }

    public Set<MemoryLocation> getDefs() {
      return defs;
    }

    public Set<MemoryLocation> getUses() {
      return uses;
    }

    @Override
    public String toString() {
      return String.format("(%s, defs: %s, uses: %s)", edge, defs, uses);
    }
  }

  private static class Collector
      implements CAstNodeVisitor<Set<MemoryLocation>, CPATransferException> {

    @SuppressWarnings("unused") // pointers are unsupported (for now)
    private final PointerState pointerState;

    @SuppressWarnings("unused") // pointers are unsupported (for now)
    private final Optional<VariableClassification> varClassification;

    public Collector(
        final PointerState pPointerState,
        final Optional<VariableClassification> pVarClassification) {
      pointerState = pPointerState;
      varClassification = pVarClassification;
    }

    private Set<MemoryLocation> combine(
        final Set<MemoryLocation> pLhs, final Set<MemoryLocation> pRhs) {
      if (pLhs == null || pRhs == null) {
        return null;
      } else {
        return ImmutableSet.<MemoryLocation>builder().addAll(pLhs).addAll(pRhs).build();
      }
    }

    @SuppressWarnings("unused") // pointers are unsupported (for now)
    private static @Nullable Set<MemoryLocation> getPossibePointees(
        CPointerExpression pExp,
        PointerState pPointerState,
        Optional<VariableClassification> pVarClassification) {
      Set<MemoryLocation> pointees = ReachingDefUtils.possiblePointees(pExp, pPointerState);
      if (pointees == null) {
        pointees = new HashSet<>();
        if (pVarClassification.isPresent()) {
          Set<String> addressedVars = pVarClassification.orElseThrow().getAddressedVariables();
          for (String v : addressedVars) {
            MemoryLocation m = MemoryLocation.valueOf(v);
            pointees.add(m);
          }
        } else {
          // if pointees are unknown and we can't derive them through the variable classification,
          // any variable could be used.
          return null;
        }
      }
      return pointees;
    }

    @Override
    public Set<MemoryLocation> visit(CExpressionStatement pStmt) throws CPATransferException {
      return pStmt.getExpression().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CExpressionAssignmentStatement pStmt)
        throws CPATransferException {
      Set<MemoryLocation> lhs = handleLeftHandSide(pStmt.getLeftHandSide());
      Set<MemoryLocation> rhs = pStmt.getRightHandSide().accept(this);
      return combine(lhs, rhs);
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionCallAssignmentStatement pStmt)
        throws CPATransferException {
      Set<MemoryLocation> lhs = handleLeftHandSide(pStmt.getLeftHandSide());
      Set<MemoryLocation> rhs = pStmt.getRightHandSide().accept(this);
      return combine(lhs, rhs);
    }

    private Set<MemoryLocation> handleLeftHandSide(final CLeftHandSide pLhs)
        throws CPATransferException {
      if (pLhs instanceof CPointerExpression) {
        return ((CPointerExpression) pLhs).getOperand().accept(this);
      } else if (pLhs instanceof CArraySubscriptExpression) {
        return ((CArraySubscriptExpression) pLhs).getSubscriptExpression().accept(this);
      } else {
        return ImmutableSet.of();
      }
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionCallStatement pStmt) throws CPATransferException {
      Set<MemoryLocation> paramDecls = new HashSet<>();
      for (CExpression p : pStmt.getFunctionCallExpression().getParameterExpressions()) {
        paramDecls = combine(paramDecls, p.accept(this));
      }
      return paramDecls;
    }

    @Override
    public Set<MemoryLocation> visit(CArrayDesignator pArrayDesignator)
        throws CPATransferException {
      return pArrayDesignator.getSubscriptExpression().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CArrayRangeDesignator pArrayRangeDesignator)
        throws CPATransferException {
      Set<MemoryLocation> fst = pArrayRangeDesignator.getCeilExpression().accept(this);
      Set<MemoryLocation> snd = pArrayRangeDesignator.getFloorExpression().accept(this);
      return combine(fst, snd);
    }

    @Override
    public Set<MemoryLocation> visit(CFieldDesignator pFieldDesignator)
        throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CArraySubscriptExpression pExp) throws CPATransferException {
      Set<MemoryLocation> fst = pExp.getArrayExpression().accept(this);
      Set<MemoryLocation> snd = pExp.getSubscriptExpression().accept(this);

      return combine(fst, snd);
    }

    @Override
    public Set<MemoryLocation> visit(CFieldReference pExp) throws CPATransferException {
      return pExp.getFieldOwner().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CIdExpression pExp) throws CPATransferException {
      CSimpleDeclaration idDeclaration = pExp.getDeclaration();
      if (idDeclaration instanceof CVariableDeclaration
          || idDeclaration instanceof CParameterDeclaration) {
        return Collections.singleton(MemoryLocation.valueOf(idDeclaration.getQualifiedName()));
      } else {
        return ImmutableSet.of();
      }
    }

    @Override
    public Set<MemoryLocation> visit(CPointerExpression pExp) throws CPATransferException {
      // pointers are unsupported (for now)
      return null;
    }

    @Override
    public Set<MemoryLocation> visit(CComplexCastExpression pExp) throws CPATransferException {
      return pExp.getOperand().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CInitializerExpression pExp) throws CPATransferException {
      return pExp.accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CInitializerList pInitializerList)
        throws CPATransferException {
      Set<MemoryLocation> uses = new HashSet<>();
      for (CInitializer i : pInitializerList.getInitializers()) {
        uses = combine(uses, i.accept(this));
      }
      return uses;
    }

    @Override
    public Set<MemoryLocation> visit(CDesignatedInitializer pExp) throws CPATransferException {
      Set<MemoryLocation> used = pExp.getRightHandSide().accept(this);
      for (CDesignator d : pExp.getDesignators()) {
        used = combine(used, d.accept(this));
      }

      return used;
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionCallExpression pExp) throws CPATransferException {
      Set<MemoryLocation> uses = pExp.getFunctionNameExpression().accept(this);
      for (CExpression p : pExp.getParameterExpressions()) {
        uses = combine(uses, p.accept(this));
      }
      return uses;
    }

    @Override
    public Set<MemoryLocation> visit(CBinaryExpression pExp) throws CPATransferException {
      return combine(pExp.getOperand1().accept(this), pExp.getOperand2().accept(this));
    }

    @Override
    public Set<MemoryLocation> visit(CCastExpression pExp) throws CPATransferException {
      return pExp.getOperand().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CCharLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CFloatLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CIntegerLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CStringLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CTypeIdExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CUnaryExpression pExp) throws CPATransferException {
      return pExp.getOperand().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CImaginaryLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CAddressOfLabelExpression pExp) throws CPATransferException {
      return pExp.accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionDeclaration pDecl) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CComplexTypeDeclaration pDecl) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CTypeDefDeclaration pDecl) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CVariableDeclaration pDecl) throws CPATransferException {
      CInitializer init = pDecl.getInitializer();
      if (init != null) {
        return init.accept(this);
      } else {
        return ImmutableSet.of();
      }
    }

    @Override
    public Set<MemoryLocation> visit(CParameterDeclaration pDecl) throws CPATransferException {
      return pDecl.asVariableDeclaration().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CEnumerator pDecl) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CReturnStatement pNode) throws CPATransferException {
      com.google.common.base.Optional<CExpression> ret = pNode.getReturnValue();

      if (ret.isPresent()) {
        return ret.get().accept(this);
      } else {
        return ImmutableSet.of();
      }
    }
  }
}
