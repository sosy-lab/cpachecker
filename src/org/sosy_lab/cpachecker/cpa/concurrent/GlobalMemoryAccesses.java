// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * The global variables that an edge reads and writes, and whether it reads or writes memory through
 * a pointer dereference or an array subscript. Partial order reduction uses this to decide whether
 * two edges of different threads are independent, so accesses to local variables and parameters are
 * not collected. Accesses through pointers and arrays are not resolved: they are only flagged, and
 * the users treat such an edge as dependent on every edge that writes.
 *
 * @param reads global variables that are read
 * @param writes global variables that are (fully or partially) written
 * @param readsThroughPointer whether memory is read through a pointer or an array subscript
 * @param writesThroughPointer whether memory is written through a pointer or an array subscript
 */
record GlobalMemoryAccesses(
    ImmutableSet<MemoryLocation> reads,
    ImmutableSet<MemoryLocation> writes,
    boolean readsThroughPointer,
    boolean writesThroughPointer) {

  static final GlobalMemoryAccesses EMPTY =
      new GlobalMemoryAccesses(ImmutableSet.of(), ImmutableSet.of(), false, false);

  boolean isEmpty() {
    return reads.isEmpty() && writes.isEmpty() && !readsThroughPointer && !writesThroughPointer;
  }

  boolean hasWrites() {
    return !writes.isEmpty() || writesThroughPointer;
  }

  boolean accessesThroughPointer() {
    return readsThroughPointer || writesThroughPointer;
  }

  GlobalMemoryAccesses merge(GlobalMemoryAccesses pOther) {
    return new GlobalMemoryAccesses(
        ImmutableSet.<MemoryLocation>builder().addAll(reads).addAll(pOther.reads).build(),
        ImmutableSet.<MemoryLocation>builder().addAll(writes).addAll(pOther.writes).build(),
        readsThroughPointer || pOther.readsThroughPointer,
        writesThroughPointer || pOther.writesThroughPointer);
  }

  /** Extracts the accesses of edges and caches them per edge instance. */
  static final class Extractor {

    private final IdentityHashMap<CFAEdge, GlobalMemoryAccesses> cache = new IdentityHashMap<>();

    GlobalMemoryAccesses extract(CFAEdge pEdge) {
      return cache.computeIfAbsent(pEdge, Extractor::collect);
    }

    private static GlobalMemoryAccesses collect(CFAEdge pEdge) {
      if (pEdge.getRawAST().orElse(null) instanceof CAstNode astNode) {
        Collector collector = new Collector();
        astNode.accept(collector);
        return new GlobalMemoryAccesses(
            ImmutableSet.copyOf(collector.reads),
            ImmutableSet.copyOf(collector.writes),
            collector.readsThroughPointer,
            collector.writesThroughPointer);
      }
      return EMPTY;
    }
  }

  private static final class Collector implements CAstNodeVisitor<Void, NoException> {

    private final Set<MemoryLocation> reads = new HashSet<>();
    private final Set<MemoryLocation> writes = new HashSet<>();
    private boolean readsThroughPointer = false;
    private boolean writesThroughPointer = false;

    /** Whether the currently visited expression is written (otherwise it is read). */
    private boolean writing = false;

    private void visitRead(CAstNode pNode) {
      boolean prev = writing;
      writing = false;
      pNode.accept(this);
      writing = prev;
    }

    private void visitWritten(CAstNode pNode) {
      boolean prev = writing;
      writing = true;
      pNode.accept(this);
      writing = prev;
    }

    private void addPointerAccess() {
      if (writing) {
        writesThroughPointer = true;
      } else {
        readsThroughPointer = true;
      }
    }

    @Override
    public Void visit(CIdExpression pIdExpression) {
      if (pIdExpression.getDeclaration() instanceof CVariableDeclaration declaration
          && declaration.isGlobal()) {
        (writing ? writes : reads).add(MemoryLocation.forDeclaration(declaration));
      }
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pArraySubscriptExpression) {
      visitRead(pArraySubscriptExpression.getArrayExpression());
      visitRead(pArraySubscriptExpression.getSubscriptExpression());
      addPointerAccess();
      return null;
    }

    @Override
    public Void visit(CPointerExpression pPointerExpression) {
      visitRead(pPointerExpression.getOperand());
      addPointerAccess();
      return null;
    }

    @Override
    public Void visit(CFieldReference pFieldReference) {
      if (pFieldReference.isPointerDereference()) {
        visitRead(pFieldReference.getFieldOwner());
        addPointerAccess();
      } else {
        // a write to a field is a (partial) write to the owner
        pFieldReference.getFieldOwner().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression pFunctionCallExpression) {
      List<CExpression> parameters = pFunctionCallExpression.getParameterExpressions();
      if (pFunctionCallExpression.getFunctionNameExpression() instanceof CIdExpression name
          && name.getName().equals("scanf")
          && !parameters.isEmpty()) {
        // scanf writes through all arguments after the format string
        visitRead(parameters.getFirst());
        for (CExpression parameter : parameters.subList(1, parameters.size())) {
          if (parameter instanceof CUnaryExpression unary
              && unary.getOperator() == UnaryOperator.AMPER) {
            visitWritten(unary.getOperand());
          } else {
            visitRead(parameter);
            writesThroughPointer = true;
          }
        }
        return null;
      }
      pFunctionCallExpression.getFunctionNameExpression().accept(this);
      for (CExpression parameter : parameters) {
        parameter.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pStatement) {
      visitWritten(pStatement.getLeftHandSide());
      visitRead(pStatement.getRightHandSide());
      return null;
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pStatement) {
      visitWritten(pStatement.getLeftHandSide());
      visitRead(pStatement.getRightHandSide());
      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pStatement) {
      return pStatement.getFunctionCallExpression().accept(this);
    }

    @Override
    public Void visit(CExpressionStatement pStatement) {
      return pStatement.getExpression().accept(this);
    }

    @Override
    public Void visit(CReturnStatement pStatement) {
      pStatement.getReturnValue().ifPresent(value -> visitRead(value));
      return null;
    }

    @Override
    public Void visit(CVariableDeclaration pDeclaration) {
      if (pDeclaration.isGlobal()) {
        writes.add(MemoryLocation.forDeclaration(pDeclaration));
      }
      CInitializer initializer = pDeclaration.getInitializer();
      if (initializer != null) {
        visitRead(initializer);
      }
      return null;
    }

    @Override
    public Void visit(CInitializerExpression pInitializerExpression) {
      return pInitializerExpression.getExpression().accept(this);
    }

    @Override
    public Void visit(CInitializerList pInitializerList) {
      for (CInitializer initializer : pInitializerList.getInitializers()) {
        initializer.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CDesignatedInitializer pDesignatedInitializer) {
      pDesignatedInitializer.getRightHandSide().accept(this);
      for (CDesignator designator : pDesignatedInitializer.getDesignators()) {
        designator.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CArrayDesignator pArrayDesignator) {
      return pArrayDesignator.getSubscriptExpression().accept(this);
    }

    @Override
    public Void visit(CArrayRangeDesignator pArrayRangeDesignator) {
      pArrayRangeDesignator.getFloorExpression().accept(this);
      return pArrayRangeDesignator.getCeilExpression().accept(this);
    }

    @Override
    public Void visit(CBinaryExpression pBinaryExpression) {
      pBinaryExpression.getOperand1().accept(this);
      return pBinaryExpression.getOperand2().accept(this);
    }

    @Override
    public Void visit(CCastExpression pCastExpression) {
      return pCastExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CComplexCastExpression pComplexCastExpression) {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CUnaryExpression pUnaryExpression) {
      return pUnaryExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CFieldDesignator pFieldDesignator) {
      return null;
    }

    @Override
    public Void visit(CCharLiteralExpression pCharLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CFloatLiteralExpression pFloatLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CIntegerLiteralExpression pIntegerLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CStringLiteralExpression pStringLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CImaginaryLiteralExpression pImaginaryLiteralExpression) {
      return null;
    }

    @Override
    public Void visit(CTypeIdExpression pTypeIdExpression) {
      return null;
    }

    @Override
    public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
      return null;
    }

    @Override
    public Void visit(CFunctionDeclaration pDeclaration) {
      return null;
    }

    @Override
    public Void visit(CParameterDeclaration pDeclaration) {
      return null;
    }

    @Override
    public Void visit(CComplexTypeDeclaration pDeclaration) {
      return null;
    }

    @Override
    public Void visit(CTypeDefDeclaration pDeclaration) {
      return null;
    }

    @Override
    public Void visit(CEnumerator pEnumerator) {
      return null;
    }
  }
}
