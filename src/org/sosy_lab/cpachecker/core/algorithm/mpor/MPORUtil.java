// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

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

  // reach_error calls =============================================================================

  public static boolean isReachErrorCall(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CFunctionSummaryEdge functionSummaryEdge) {
      return isReachErrorCall(functionSummaryEdge);
    } else if (pCfaEdge instanceof CFunctionCallEdge functionCallEdge) {
      return isReachErrorCall(functionCallEdge);
    }
    return false;
  }

  private static boolean isReachErrorCall(CFunctionSummaryEdge pFunctionSummaryEdge) {
    return pFunctionSummaryEdge
        .getFunctionEntry()
        .getFunction()
        .getOrigName()
        .equals(SeqToken.reach_error);
  }

  private static boolean isReachErrorCall(CFunctionCallEdge pFunctionCallEdge) {
    return pFunctionCallEdge
        .getFunctionCallExpression()
        .getDeclaration()
        .getOrigName()
        .equals(SeqToken.reach_error);
  }

  // assume calls ==================================================================================

  // TODO it becomes a problem if a program uses if (!cond) abort(); in the code itself, instead of
  //  calling one of these functions... should identify the code itself
  /**
   * The set of function names used for assumptions in the SV-Benchmarks:
   *
   * <ul>
   *   <li>{@code assume_abort_if_not} e.g. in pthread/stack_longer-1 (most relevant)
   *   <li>{@code ldv_assume} used by linux device driver programs e.g. ldv-challenges folder
   *   <li>{@code __VERIFIER_assume} seems deprecated, but is still included here
   * </ul>
   */
  private static final ImmutableSet<String> ASSUME_FUNCTION_NAMES =
      ImmutableSet.<String>builder()
          .add(SeqToken.assume_abort_if_not)
          .add(SeqToken.ldv_assume)
          .add(SeqToken.__VERIFIER_assume)
          .build();

  public static boolean isAssumeAbortIfNotCall(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CFunctionSummaryEdge functionSummaryEdge) {
      return isAssumeAbortIfNotCall(functionSummaryEdge);
    } else if (pCfaEdge instanceof CFunctionCallEdge functionCallEdge) {
      return isAssumeAbortIfNotCall(functionCallEdge);
    }
    return false;
  }

  private static boolean isAssumeAbortIfNotCall(CFunctionSummaryEdge pFunctionSummaryEdge) {
    return ASSUME_FUNCTION_NAMES.contains(
        pFunctionSummaryEdge.getFunctionEntry().getFunction().getOrigName());
  }

  private static boolean isAssumeAbortIfNotCall(CFunctionCallEdge pFunctionCallEdge) {
    return ASSUME_FUNCTION_NAMES.contains(
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration().getOrigName());
  }

  // const CPAchecker_TMP ==========================================================================

  public static boolean isConstCpaCheckerTmp(CVariableDeclaration pVarDec) {
    return pVarDec.getType().isConst()
        && !pVarDec.isGlobal()
        && pVarDec.getName().contains(SeqToken.__CPAchecker_TMP_)
        // in tests, const CPAchecker_TMP variables always had initializer
        && pVarDec.getInitializer() != null;
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
   * Extracts e.g. {@code id1} from {@code &id1}, throws a {@link IllegalArgumentException} if the
   * extraction not possible.
   */
  public static CExpression getOperandFromUnaryExpression(CExpression pAddress) {
    if (pAddress instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getExpressionType() instanceof CPointerType) {
        return unaryExpression.getOperand();
      }
    }
    throw new IllegalArgumentException("cannot extract value from pAddress");
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
   * Extracts the {@link CSimpleDeclaration} of {@code pExpression}, if it is a pointer, or returns
   * {@link Optional#empty()} otherwise.
   */
  public static Optional<CSimpleDeclaration> tryGetPointerDeclaration(CExpression pExpression) {
    // unary expression i.e. 'ptr = &var;'
    if (pExpression instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
        if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
          return Optional.of(idExpression.getDeclaration());
        }
      }
      // id expression i.e. another pointer assigned to the pointer 'ptr_a = ptr_b;'
    } else if (pExpression instanceof CIdExpression idExpression) {
      if (idExpression.getDeclaration().getType() instanceof CPointerType) {
        return Optional.of(idExpression.getDeclaration());
      }
      // cast expression e.g. 'ptr = (int *) arg;'
    } else if (pExpression instanceof CCastExpression castExpression) {
      if (castExpression.getCastType() instanceof CPointerType) {
        if (castExpression.getOperand() instanceof CIdExpression idExpression) {
          return Optional.of(idExpression.getDeclaration());
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Returns an {@link Entry} that maps the {@link CSimpleDeclaration} of the outermost field owner
   * to the {@link CCompositeTypeMemberDeclaration} of the innermost field member accessed in {@code
   * pExpression} and {@link Optional#empty()} if it can't be found.
   */
  public static Optional<Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
      tryGetFieldMemberPointer(CExpression pExpression) {

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
      CFieldReference pFieldReference) {

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
      final CFieldReference pFieldReference, CType pType) {

    if (pType instanceof CPointerType pointerType) {
      return recursivelyFindFieldMemberByFieldOwner(pFieldReference, pointerType.getType());
    }
    if (pType instanceof CElaboratedType elaboratedType) {
      // composite type contains the composite type members, e.g. 'amount'
      if (elaboratedType.getRealType() instanceof CCompositeType compositeType) {
        for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
          if (memberDeclaration.getName().equals(pFieldReference.getFieldName())) {
            return memberDeclaration;
          }
        }
      }
    }
    if (pType instanceof CTypedefType typedefType) {
      // elaborated type is e.g. struct __anon_type_QType
      if (typedefType.getRealType() instanceof CElaboratedType elaboratedType) {
        return recursivelyFindFieldMemberByFieldOwner(pFieldReference, elaboratedType);
      }
      if (typedefType.getRealType() instanceof CTypedefType innerTypedefType) {
        return recursivelyFindFieldMemberByFieldOwner(pFieldReference, innerTypedefType);
      }
    }
    throw new IllegalArgumentException("field owner type must be CTypedefType");
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

  public static CFACreator buildCfaCreator(LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    return CFACreator.construct(Configuration.builder().build(), pLogger, pShutdownNotifier);
  }

  public static CFACreator buildCfaCreatorWithPreprocessor(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {

    return CFACreator.construct(
        Configuration.builder().setOption("parser.usePreprocessor", "true").build(),
        pLogger,
        pShutdownNotifier);
  }
}
