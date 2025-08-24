// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class SubstituteUtil {

  public static ImmutableList<MPORThread> extractThreads(
      ImmutableList<MPORSubstitution> pSubstitutions) {

    return pSubstitutions.stream()
        .map(MPORSubstitution::getThread)
        .collect(ImmutableList.toImmutableList());
  }

  public static MPORSubstitution extractMainThreadSubstitution(
      ImmutableList<MPORSubstitution> pSubstitutions) {

    return pSubstitutions.stream().filter(s -> s.thread.isMain()).findAny().orElseThrow();
  }

  /** Function and Type declarations are placed outside {@code main()}. */
  public static boolean isExcludedDeclarationEdge(
      MPOROptions pOptions, CDeclarationEdge pDeclarationEdge) {

    CDeclaration declaration = pDeclarationEdge.getDeclaration();
    if (declaration instanceof CFunctionDeclaration) {
      return !pOptions.inputFunctionDeclarations;

    } else if (declaration instanceof CTypeDeclaration) {
      return !pOptions.inputTypeDeclarations;

    } else if (declaration instanceof CVariableDeclaration variableDeclaration) {
      if (!pOptions.inputTypeDeclarations) {
        // if type declarations are excluded, extern variable declarations are excluded too
        return variableDeclaration.getCStorageClass().equals(CStorageClass.EXTERN);
      }
    }
    return false;
  }

  // Memory Locations ==============================================================================

  public static ImmutableSet<MemoryLocation> getAllMemoryLocations(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSet.Builder<MemoryLocation> rMemoryLocations = ImmutableSet.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rMemoryLocations.addAll(substituteEdge.accessedMemoryLocations);
      rMemoryLocations.addAll(substituteEdge.pointerAssignments.values());
    }
    return rMemoryLocations.build();
  }

  public static ImmutableSet<MemoryLocation> getMemoryLocationsByAccessType(
      MPORSubstitutionTracker pTracker, BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemoryLocations = ImmutableSet.builder();
    for (CVariableDeclaration variableDeclaration :
        pTracker.getGlobalVariablesByAccessType(pAccessType)) {
      rMemoryLocations.add(MemoryLocation.of(variableDeclaration));
    }
    ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration> fieldMembers =
        pTracker.getFieldMembersByAccessType(pAccessType);
    for (CVariableDeclaration fieldOwner : fieldMembers.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember : fieldMembers.get(fieldOwner)) {
        rMemoryLocations.add(MemoryLocation.of(fieldOwner, fieldMember));
      }
    }
    return rMemoryLocations.build();
  }

  // Pointer Assignments ===========================================================================

  /**
   * Maps pointers {@code ptr} to the memory locations e.g. {@code &var} assigned to them based on
   * {@code pSubstituteEdges}, including both global and local memory locations.
   */
  public static ImmutableMap<CVariableDeclaration, MemoryLocation> mapPointerAssignments(
      MPORSubstitutionTracker pTracker) {

    ImmutableMap.Builder<CVariableDeclaration, MemoryLocation> rAssignments =
        ImmutableMap.builder();
    for (var entry : pTracker.getPointerAssignments().entrySet()) {
      rAssignments.put(entry.getKey(), MemoryLocation.of(entry.getValue()));
    }
    ImmutableSet<Cell<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
        cellSet = pTracker.getPointerFieldMemberAssignments().cellSet();
    for (Cell<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration> cell :
        cellSet) {
      rAssignments.put(
          Objects.requireNonNull(cell.getRowKey()),
          MemoryLocation.of(cell.getColumnKey(), cell.getValue()));
    }
    return rAssignments.buildOrThrow();
  }

  public static ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> mapAllPointerAssignments(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSetMultimap.Builder<CVariableDeclaration, MemoryLocation> rAllAssignments =
        ImmutableSetMultimap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rAllAssignments.putAll(substituteEdge.pointerAssignments.asMultimap());
    }
    return rAllAssignments.build();
  }

  // Pointer Parameter Assignments =================================================================

  public static ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
      mapPointerParameterAssignments(ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableTable.Builder<ThreadEdge, CParameterDeclaration, MemoryLocation> rAssignments =
        ImmutableTable.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      // use the original edge, so that we use the original variable declarations
      if (substituteEdge.getOriginalEdge() instanceof CFunctionCallEdge functionCallEdge) {
        // the function call edge is used as the call context, not the actual call context
        ThreadEdge callContext = substituteEdge.getThreadEdge();
        ImmutableList<Cell<ThreadEdge, CParameterDeclaration, MemoryLocation>> assignments =
            buildParameterAssignments(callContext, functionCallEdge);
        for (Cell<ThreadEdge, CParameterDeclaration, MemoryLocation> cell : assignments) {
          rAssignments.put(cell);
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  private static ImmutableList<Cell<ThreadEdge, CParameterDeclaration, MemoryLocation>>
      buildParameterAssignments(ThreadEdge pCallContext, CFunctionCallEdge pFunctionCallEdge) {

    ImmutableList.Builder<Cell<ThreadEdge, CParameterDeclaration, MemoryLocation>> rAssignments =
        ImmutableList.builder();
    List<CExpression> arguments = pFunctionCallEdge.getArguments();
    List<CParameterDeclaration> parameterDeclarations =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration().getParameters();
    // C does not allow optional parameters
    assert arguments.size() == parameterDeclarations.size()
        : "function argument number should be same as parameter declaration number";
    for (int i = 0; i < arguments.size(); i++) {
      CParameterDeclaration leftHandSide = parameterDeclarations.get(i);
      // TODO we also need non-pointers, e.g. for 'global_ptr = &non_ptr_param;'
      if (leftHandSide.getType() instanceof CPointerType) {
        MemoryLocation rhsMemoryLocation = extractMemoryLocation(arguments.get(i));
        if (!rhsMemoryLocation.isEmpty()) {
          rAssignments.add(Tables.immutableCell(pCallContext, leftHandSide, rhsMemoryLocation));
        }
      }
    }
    return rAssignments.build();
  }

  public static MemoryLocation extractMemoryLocation(CExpression pRightHandSide) {
    if (pRightHandSide instanceof CIdExpression idExpression) {
      return MemoryLocation.of(idExpression.getDeclaration());

    } else if (pRightHandSide instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
        return MemoryLocation.of(idExpression.getDeclaration());

      } else if (unaryExpression.getOperand() instanceof CFieldReference fieldReference) {
        return extractFieldReferenceMemoryLocation(fieldReference);
      }

    } else if (pRightHandSide instanceof CFieldReference fieldReference) {
      return extractFieldReferenceMemoryLocation(fieldReference);
    }
    // can e.g. occur with 'param = 4' i.e. literal integer expressions
    return MemoryLocation.empty();
  }

  private static MemoryLocation extractFieldReferenceMemoryLocation(
      CFieldReference pFieldReference) {

    if (pFieldReference.getFieldOwner().getExpressionType() instanceof CTypedefType typedefType) {
      CIdExpression fieldOwner = MPORUtil.recursivelyFindFieldOwner(pFieldReference);
      CCompositeTypeMemberDeclaration fieldMember =
          MPORUtil.getFieldMemberByName(pFieldReference, typedefType);
      return MemoryLocation.of(fieldOwner.getDeclaration(), fieldMember);
    }
    // TODO just making sure
    throw new IllegalArgumentException("pFieldReference owner type must be CTypedefType");
  }

  // Main Function Arg =============================================================================

  public static ImmutableSet<CParameterDeclaration> findAllMainFunctionArgs(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSet.Builder<CParameterDeclaration> rArgs = ImmutableSet.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rArgs.addAll(substituteEdge.accessedMainFunctionArgs);
    }
    return rArgs.build();
  }
}
