// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Flow-insensitive targets of pointer variables, computed once from the CFA. */
final class PointerTargets {

  private final Set<String> addressedVariables;
  private final SetMultimap<MemoryLocation, MemoryLocation> targets = HashMultimap.create();
  // Pointers that get the value of another pointer
  private final SetMultimap<MemoryLocation, MemoryLocation> copies = HashMultimap.create();
  private final Set<MemoryLocation> unknown = new HashSet<>();

  private PointerTargets(Set<String> pAddressedVariables) {
    addressedVariables = pAddressedVariables;
  }

  /**
   * Returns the variables that each pointer variable may point into. A pointer variable is missing
   * if its address is taken, if it is a parameter, or if it may get a value that is not the address
   * of a variable, the value of another such pointer, or a null pointer.
   */
  static ImmutableSetMultimap<MemoryLocation, MemoryLocation> compute(
      CFA pCfa, Set<String> pAddressedVariables) {
    PointerTargets pointerTargets = new PointerTargets(pAddressedVariables);
    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      pointerTargets.handleEdge(edge);
    }
    return pointerTargets.propagate();
  }

  private void handleEdge(CFAEdge pEdge) {
    MemoryLocationExtractingVisitor visitor =
        new MemoryLocationExtractingVisitor(
            pEdge.getSuccessor().getFunctionName(), addressedVariables, ImmutableSetMultimap.of());
    switch (pEdge) {
      case CDeclarationEdge declarationEdge
          when declarationEdge.getDeclaration() instanceof CVariableDeclaration declaration
              && declaration.getInitializer() != null -> {
        CInitializer initializer = declaration.getInitializer();
        handleAssignment(
            new CIdExpression(declaration.getFileLocation(), declaration),
            initializer instanceof CInitializerExpression expression
                ? expression.getExpression()
                : null,
            visitor);
      }
      case AStatementEdge statementEdge -> {
        switch (statementEdge.getStatement()) {
          case CExpressionAssignmentStatement assignment ->
              handleAssignment(
                  assignment.getLeftHandSide(), assignment.getRightHandSide(), visitor);
          case CFunctionCallAssignmentStatement assignment ->
              handleAssignment(assignment.getLeftHandSide(), null, visitor);
          default -> {}
        }
      }
      case FunctionSummaryEdge summaryEdge
          when summaryEdge.getExpression() instanceof CFunctionCallAssignmentStatement assignment ->
          handleAssignment(assignment.getLeftHandSide(), null, visitor);
      default -> {}
    }
  }

  /** Records that the given pointer variable gets the given value, or an unknown one if null. */
  private void handleAssignment(
      CLeftHandSide pLeftHandSide,
      @Nullable CExpression pValue,
      MemoryLocationExtractingVisitor pVisitor) {
    if (!(pLeftHandSide instanceof CIdExpression pointer
        && pointer.getExpressionType().getCanonicalType() instanceof CPointerType)) {
      return;
    }
    MemoryLocation location =
        Iterables.getOnlyElement(pVisitor.getMemoryLocation(pointer).getMemoryLocations());
    if (pValue == null
        || pointer.getDeclaration() instanceof CParameterDeclaration
        || !handleValue(location, pValue, pVisitor)) {
      unknown.add(location);
    }
  }

  /** Records the targets of the given value and returns whether they are known. */
  private boolean handleValue(
      MemoryLocation pPointer, CExpression pValue, MemoryLocationExtractingVisitor pVisitor) {
    return switch (pValue) {
      case CCastExpression cast -> handleValue(pPointer, cast.getOperand(), pVisitor);
      case CIntegerLiteralExpression literal -> literal.getValue().equals(BigInteger.ZERO);
      case CBinaryExpression binary
          when binary.getExpressionType().getCanonicalType() instanceof CPointerType ->
          handleValue(
              pPointer,
              MemoryLocationExtractingVisitor.isPointerOrArray(binary.getOperand1())
                  ? binary.getOperand1()
                  : binary.getOperand2(),
              pVisitor);
      case CUnaryExpression unary when unary.getOperator() == UnaryOperator.AMPER ->
          addTargets(pPointer, pVisitor.getTargets(unary));
      case CExpression array when MemoryLocationExtractingVisitor.isArray(array) ->
          addTargets(pPointer, pVisitor.getTargets(array));
      case CIdExpression other
          when other.getExpressionType().getCanonicalType() instanceof CPointerType
              && !(other.getDeclaration() instanceof CParameterDeclaration) -> {
        copies.put(
            pPointer,
            Iterables.getOnlyElement(pVisitor.getMemoryLocation(other).getMemoryLocations()));
        yield true;
      }
      default -> false;
    };
  }

  private boolean addTargets(MemoryLocation pPointer, Set<MemoryLocation> pTargets) {
    targets.putAll(pPointer, pTargets);
    return !pTargets.isEmpty();
  }

  private ImmutableSetMultimap<MemoryLocation, MemoryLocation> propagate() {
    boolean changed = true;
    while (changed) {
      changed = false;
      for (var copy : copies.entries()) {
        if (isUnknown(copy.getValue())) {
          changed |= unknown.add(copy.getKey());
        }
        changed |= targets.putAll(copy.getKey(), ImmutableSet.copyOf(targets.get(copy.getValue())));
      }
    }
    ImmutableSetMultimap.Builder<MemoryLocation, MemoryLocation> result =
        ImmutableSetMultimap.builder();
    for (var target : targets.entries()) {
      if (!isUnknown(target.getKey())) {
        result.put(target);
      }
    }
    return result.build();
  }

  private boolean isUnknown(MemoryLocation pPointer) {
    return unknown.contains(pPointer)
        || addressedVariables.contains(pPointer.getExtendedQualifiedName());
  }
}
