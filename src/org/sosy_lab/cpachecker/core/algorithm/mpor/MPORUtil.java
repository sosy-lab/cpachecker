// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Contains static methods that can be reused outside the MPOR context. */
public final class MPORUtil {

  /**
   * Returns the successor PredicateAbstractState when executing pCfaEdge.
   *
   * @param pPtr the PredicateTransferRelation that handles creating a successor AbstractState
   * @param pCurrentState the current PredicateAbstractState in which we execute pCfaEdge
   * @param pCfaEdge the CFAEdge that is executed
   * @return the successor PredicateAbstractState when executing pCfaEdge
   */
  public static @NonNull PredicateAbstractState getNextPredicateAbstractState(
      @NonNull PredicateTransferRelation pPtr,
      @NonNull PredicateAbstractState pCurrentState,
      @NonNull CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    checkNotNull(pPtr);
    checkNotNull(pCurrentState);
    checkNotNull(pCfaEdge);

    // using no precision (null) is fine in the context of MPOR
    Collection<? extends AbstractState> abstractStates =
        pPtr.getAbstractSuccessorsForEdge(pCurrentState, null, pCfaEdge);
    checkState(abstractStates.size() == 1); // should always hold

    PredicateAbstractState rAbstractSuccessor =
        AbstractStates.extractStateByType(
            abstractStates.iterator().next(), PredicateAbstractState.class);
    checkNotNull(rAbstractSuccessor);
    return rAbstractSuccessor;
  }

  // TODO (not sure if important for our algorithm) PredicateAbstractState.abstractLocations
  //  contains all CFANodes visited so far
  /**
   * Checks whether two CFAEdges a and b <i>commute</i>, i.e. if, from the current pAbstractState,
   * executing a then b or b then a result in the same PathFormula.
   *
   * @param pPtr The PredicateTransferRelation instance used to find the next PathFormulas
   * @param pAbstractState the current PredicateAbstractState of the program, i.e. the state
   *     containing the current PathFormula with previous transitions
   * @param pEdgeA CFAEdge a
   * @param pEdgeB CFAEdge b
   * @return true if pEdgeA and pEdgeB commute
   */
  public static boolean doEdgesCommute(
      @NonNull PredicateTransferRelation pPtr,
      @NonNull PredicateAbstractState pAbstractState,
      @NonNull CFAEdge pEdgeA,
      @NonNull CFAEdge pEdgeB)
      throws CPATransferException, InterruptedException {

    checkNotNull(pPtr);
    checkNotNull(pAbstractState);
    checkNotNull(pEdgeA);
    checkNotNull(pEdgeB);

    // TODO this is very costly, leaving it out for now. in tests, the state was always sat
    /*checkArgument(
    !pPtr.unsatCheck(pAbstractState.getAbstractionFormula(), pAbstractState.getPathFormula()),
    "reached abstract state must be sat");*/

    // execute edgeA, then edgeB
    PredicateAbstractState aState = getNextPredicateAbstractState(pPtr, pAbstractState, pEdgeA);
    PredicateAbstractState abState = getNextPredicateAbstractState(pPtr, aState, pEdgeB);
    // execute edgeB, then edgeA
    PredicateAbstractState bState = getNextPredicateAbstractState(pPtr, pAbstractState, pEdgeB);
    PredicateAbstractState baState = getNextPredicateAbstractState(pPtr, bState, pEdgeA);

    return abState.getPathFormula().equals(baState.getPathFormula());
  }

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
    for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pCurrent.getSuccessor())) {
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
    for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pCurrent)) {
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
        idExpression.getDeclaration(), getFieldMemberByFieldReference(pFieldReference, type));
  }

  /**
   * Recursively tries to find the field owner of {@code pFieldReference}, e.g. {@code outer} in
   * {@code outer.intermediary.inner}.
   */
  public static CIdExpression recursivelyFindFieldOwner(CFieldReference pFieldReference) {
    if (pFieldReference.getFieldOwner() instanceof CIdExpression idExpression) {
      return idExpression;
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
  public static CCompositeTypeMemberDeclaration getFieldMemberByFieldReference(
      CFieldReference pFieldReference, CType pType) {

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
        return getFieldMemberByFieldReference(pFieldReference, elaboratedType);
      }
      if (typedefType.getRealType() instanceof CTypedefType innerTypedefType) {
        return getFieldMemberByFieldReference(pFieldReference, innerTypedefType);
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

  /**
   * Returns the symmetric difference of the key sets in {@code pMapA} and {@code pMapB} but retains
   * all values. Assumes that if a key is present in both maps, the value sets are equal.
   */
  public static <K, V> ImmutableSetMultimap<K, V> symmetricDifference(
      ImmutableSetMultimap<K, V> pMapA, ImmutableSetMultimap<K, V> pMapB) {

    ImmutableSetMultimap.Builder<K, V> rSymmetricDifference = ImmutableSetMultimap.builder();
    ImmutableSet<K> keys =
        ImmutableSet.copyOf(Sets.symmetricDifference(pMapA.keySet(), pMapB.keySet()));
    for (K key : keys) {
      if (!pMapB.containsKey(key)) {
        rSymmetricDifference.putAll(key, pMapA.get(key));
      }
      if (pMapA.containsKey(key) && pMapB.containsKey(key)) {
        assert pMapA.get(key).equals(pMapB.get(key))
            : "value sets must be equal if both pMapA and pMapB contain a key";
      }
    }
    return rSymmetricDifference.build();
  }
}
