// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/** Contains static methods that can be reused outside the MPOR context. */
public final class MPORUtil {

  /**
   * Returns {@code true} if pOrigin can be reached through its successor {@link CFANode}. <br>
   * If pStop is encountered in a path, it is not explored further, even if pOrigin may be in the
   * path.
   */
  public static boolean isSelfReachable(
      final CFAEdge pOrigin,
      final Optional<CFAEdge> pStop,
      List<CFAEdge> pVisited,
      CFAEdge pCurrent) {

    pVisited.add(pCurrent);
    boolean foundPath = false;
    for (CFAEdge cfaEdge : pCurrent.getSuccessor().getLeavingEdges()) {
      // only search original call context via summary edge, exclude return edges
      if (!(cfaEdge instanceof CFunctionReturnEdge)) {
        // ignore edges that lead to pStop
        if (!(pStop.isPresent() && cfaEdge.equals(pStop.orElseThrow()))) {
          if (cfaEdge.equals(pOrigin)) {
            // self reach found
            return true;
          } else if (!pVisited.contains(cfaEdge)) {
            // visit edges only once, otherwise we trigger a stack overflow
            foundPath = isSelfReachable(pOrigin, pStop, pVisited, cfaEdge);
            if (foundPath) {
              break;
            }
          }
        }
      }
    }
    return foundPath;
  }

  /**
   * Returns {@code true} if pOrigin can be reached through its leaving edges. <br>
   * If pStop is encountered in a path, it is not explored further, even if pOrigin may be in the
   * path.
   */
  public static boolean isSelfReachable(
      final CFANode pOrigin,
      final Optional<CFANode> pStop,
      List<CFANode> pVisited,
      CFANode pCurrent) {

    pVisited.add(pCurrent);
    boolean foundPath = false;
    for (CFAEdge cfaEdge : pCurrent.getLeavingEdges()) {
      // only search original call context via summary edge, exclude return edges
      if (!(cfaEdge instanceof CFunctionReturnEdge)) {
        CFANode successor = cfaEdge.getSuccessor();
        // ignore edges that lead to pStop
        if (!(pStop.isPresent() && successor.equals(pStop.orElseThrow()))) {
          if (successor.equals(pOrigin)) {
            // self reach found
            return true;
          } else if (!pVisited.contains(successor)) {
            // visit edges only once, otherwise we trigger a stack overflow
            foundPath = isSelfReachable(pOrigin, pStop, pVisited, cfaEdge.getSuccessor());
            if (foundPath) {
              break;
            }
          }
        }
      }
    }
    return foundPath;
  }

  // Functions =====================================================================================

  /**
   * Returns the {@link CParameterDeclaration} at {@code pIndex}, or the last {@link
   * CParameterDeclaration} if the index is out of bounds. Then {@code pFunctionDeclaration} must be
   * a variadic function, where the last {@link CParameterDeclaration} is always the variadic one.
   */
  public static CParameterDeclaration getParameterDeclarationByIndex(
      int pIndex, CFunctionDeclaration pFunctionDeclaration) {

    checkArgument(pIndex >= 0, "pIndex must be at least 0");
    List<CParameterDeclaration> parameterDeclarations = pFunctionDeclaration.getParameters();
    if (pIndex < parameterDeclarations.size()) {
      return parameterDeclarations.get(pIndex);
    } else {
      // handle variadic function (more arguments than parameter declarations)
      checkArgument(
          pFunctionDeclaration.getType().takesVarArgs(),
          "If pIndex >= parameters.size(), then pFunctionDeclaration must be variadic.");
      return parameterDeclarations.getLast();
    }
  }

  // const CPAchecker_TMP ==========================================================================

  public static boolean isConstCpaCheckerTmp(CVariableDeclaration pVariableDeclaration) {
    return pVariableDeclaration.getType().isConst()
        && !pVariableDeclaration.isGlobal()
        && pVariableDeclaration.getName().contains("__CPAchecker_TMP_")
        // in tests, const CPAchecker_TMP variables always had initializer
        && pVariableDeclaration.getInitializer() != null;
  }

  public static boolean isConstCpaCheckerTmpDeclaration(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CDeclarationEdge declarationEdge) {
      if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
        return isConstCpaCheckerTmp(variableDeclaration);
      }
    }
    return false;
  }

  // Pointers ======================================================================================

  /**
   * Extracts e.g. {@code id1} from {@code &id1}, throws a {@link UnsupportedCodeException} if the
   * extraction is not possible.
   */
  public static CExpression getOperandFromUnaryExpression(CExpression pExpression)
      throws UnsupportedCodeException {

    if (pExpression instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getExpressionType() instanceof CPointerType) {
        return unaryExpression.getOperand();
      }
    }
    throw new UnsupportedCodeException(
        "Could not extract operand from pExpression " + pExpression.toASTString(), null);
  }

  public static boolean isFunctionPointer(CInitializer pInitializer) {
    if (pInitializer instanceof CInitializerExpression initializerExpression) {
      if (initializerExpression.getExpression() instanceof CIdExpression idExpression) {
        if (idExpression.getDeclaration() instanceof CFunctionDeclaration) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns an {@link Entry} that maps the {@link CSimpleDeclaration} of the outermost field owner
   * to the {@link CCompositeTypeMemberDeclaration} of the innermost field member accessed in {@code
   * pExpression} and {@link Optional#empty()} if it can't be found.
   */
  public static Optional<Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
      tryGetFieldMemberPointer(CExpression pExpression) throws UnsupportedCodeException {

    // e.g. 'ptr = &field.member;'
    if (pExpression instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getOperand() instanceof CFieldReference fieldReference) {
        return Optional.of(getFieldMemberPointer(fieldReference));
      }

      // e.g. 'ptr = field.member;' where member is a pointer
    } else if (pExpression instanceof CFieldReference fieldReference) {
      return Optional.of(getFieldMemberPointer(fieldReference));
    }
    return Optional.empty();
  }

  private static Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration> getFieldMemberPointer(
      CFieldReference pFieldReference) throws UnsupportedCodeException {

    CIdExpression idExpression = recursivelyFindFieldOwner(pFieldReference);
    CType type = getTypeByIdExpression(idExpression);
    return new AbstractMap.SimpleEntry<>(
        idExpression.getDeclaration(),
        recursivelyFindFieldMemberByFieldOwner(pFieldReference, type));
  }

  /**
   * Recursively tries to find the field owner of {@code pFieldReference}, e.g. {@code outer} in
   * {@code outer.intermediary.inner}.
   */
  public static CIdExpression recursivelyFindFieldOwner(CFieldReference pFieldReference) {
    if (pFieldReference.getFieldOwner() instanceof CIdExpression idExpression) {
      return idExpression;
    }
    if (pFieldReference.getFieldOwner()
        instanceof CArraySubscriptExpression arraySubscriptExpression) {
      if (arraySubscriptExpression.getArrayExpression() instanceof CIdExpression idExpression) {
        return idExpression;
      }
    }
    if (pFieldReference.getFieldOwner() instanceof CFieldReference fieldReference) {
      return recursivelyFindFieldOwner(fieldReference);
    }
    throw new IllegalArgumentException("could not find CIdExpression field owner");
  }

  private static CType getTypeByIdExpression(CIdExpression pIdExpression) {
    if (pIdExpression.getExpressionType() instanceof CPointerType pointerType) {
      return pointerType.getType();
    }
    return pIdExpression.getExpressionType();
  }

  /**
   * Extracts the {@link CCompositeTypeMemberDeclaration} of the field member accessed in {@code
   * pFieldReference}, e.g. {@code member} in {@code owner->member}.
   */
  public static CCompositeTypeMemberDeclaration recursivelyFindFieldMemberByFieldOwner(
      CFieldReference pFieldReference, CType pType) throws UnsupportedCodeException {

    // use getType() on CPointerType/CArrayType since getCanonicalType() returns the
    // CPointerType/CArrayType itself
    if (pType.getCanonicalType() instanceof CPointerType pointerType) {
      return recursivelyFindFieldMemberByFieldOwner(pFieldReference, pointerType.getType());
    }
    if (pType.getCanonicalType() instanceof CArrayType arrayType) {
      return recursivelyFindFieldMemberByFieldOwner(pFieldReference, arrayType.getType());
    }
    if (pType.getCanonicalType() instanceof CCompositeType compositeType) {
      for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (memberDeclaration.getName().equals(pFieldReference.getFieldName())) {
          return memberDeclaration;
        }
      }
    }
    throw new UnsupportedCodeException(
        "could not extract field member from the given CType: " + pType.toASTString(""), null);
  }

  // Collections ===================================================================================

  /**
   * Returns a new {@link ImmutableSet} containing all pElements from the given set except the
   * specified element.
   *
   * <p>If the element is not present in the original set, the original set is returned unchanged.
   */
  public static <T> ImmutableSet<T> withoutElement(ImmutableSet<T> pElements, T pElementToRemove) {
    if (pElements.contains(pElementToRemove)) {
      return ImmutableSet.copyOf(Sets.difference(pElements, ImmutableSet.of(pElementToRemove)));
    }
    return pElements;
  }

  /**
   * Returns a new {@link ImmutableList} containing all pElements from the given list except the
   * specified element.
   *
   * <p>If the element is not present in the original list, the original list is returned unchanged.
   */
  public static <T> ImmutableList<T> withoutElement(
      ImmutableList<T> pElements, T pElementToRemove) {

    if (pElements.contains(pElementToRemove)) {
      ImmutableList<T> toRemove = ImmutableList.of(pElementToRemove);
      return pElements.stream()
          .filter(e -> !toRemove.contains(e))
          .collect(ImmutableList.toImmutableList());
    }
    return pElements;
  }

  // CFA ===========================================================================================

  public static CFACreator buildTestCfaCreator(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {

    return new CFACreator(TestDataTools.configurationForTest().build(), pLogger, pShutdownNotifier);
  }

  public static CFACreator buildTestCfaCreatorWithPreprocessor(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {

    return new CFACreator(
        TestDataTools.configurationForTest().setOption("parser.usePreprocessor", "true").build(),
        pLogger,
        pShutdownNotifier);
  }
}
