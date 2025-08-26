// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;

public class MemoryModelBuilder {

  public static Optional<MemoryModel> tryBuildMemoryModel(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations,
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    if (pOptions.linkReduction) {
      ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
          mapParameterAssignments(pOptions, pThreads, pSubstituteEdges, pInitialMemoryLocations);
      ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
          extractPointerParameters(parameterAssignments);
      ImmutableCollection<MemoryLocation> newMemoryLocations = parameterAssignments.values();

      ImmutableMap<MemoryLocation, Integer> memoryLocationIds =
          assignMemoryLocationIds(pInitialMemoryLocations, newMemoryLocations);
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pointerAssignments =
          mapPointerAssignments(pSubstituteEdges);
      ImmutableSet<MemoryLocation> pointerDereferences =
          getAllPointerDereferences(pSubstituteEdges);

      MemoryModel memoryModel =
          new MemoryModel(
              memoryLocationIds,
              pointerAssignments,
              parameterAssignments,
              pointerParameterAssignments,
              pointerDereferences);
      return Optional.of(memoryModel);

    } else {
      return Optional.empty();
    }
  }

  private static ImmutableMap<MemoryLocation, Integer> assignMemoryLocationIds(
      ImmutableSet<MemoryLocation> pInitial, ImmutableCollection<MemoryLocation> pNew) {

    Map<MemoryLocation, Integer> rMemoryLocationIds = new HashMap<>();
    int id = BitVectorUtil.RIGHT_INDEX;
    for (MemoryLocation initialMemoryLocation : pInitial) {
      rMemoryLocationIds.put(initialMemoryLocation, id++);
    }
    for (MemoryLocation newMemoryLocation : pNew) {
      if (!rMemoryLocationIds.containsKey(newMemoryLocation)) {
        rMemoryLocationIds.put(newMemoryLocation, id++);
      }
    }
    return ImmutableMap.copyOf(rMemoryLocationIds);
  }

  // Pointer Assignments ===========================================================================

  private static ImmutableSetMultimap<MemoryLocation, MemoryLocation> mapPointerAssignments(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSetMultimap.Builder<MemoryLocation, MemoryLocation> rAllAssignments =
        ImmutableSetMultimap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rAllAssignments.putAll(substituteEdge.pointerAssignments.asMultimap());
    }
    return rAllAssignments.build();
  }

  // Parameter Assignments =================================================================

  private static ImmutableMap<MemoryLocation, MemoryLocation> mapParameterAssignments(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableCollection<SubstituteEdge> pSubstituteEdges,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rAssignments = ImmutableMap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      // use the original edge, so that we use the original variable declarations
      if (substituteEdge.getOriginalEdge() instanceof CFunctionCallEdge functionCallEdge) {
        // the function call edge is used as the call context, not the actual call context
        ThreadEdge callContext = substituteEdge.getThreadEdge();
        MPORThread thread = ThreadUtil.getThreadById(pThreads, callContext.threadId);
        rAssignments.putAll(
            buildParameterAssignments(
                pOptions, thread, callContext, functionCallEdge, pInitialMemoryLocations));
      }
    }
    return rAssignments.buildOrThrow();
  }

  private static ImmutableMap<MemoryLocation, MemoryLocation> buildParameterAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      ThreadEdge pCallContext,
      CFunctionCallEdge pFunctionCallEdge,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rAssignments = ImmutableMap.builder();
    List<CExpression> arguments = pFunctionCallEdge.getArguments();
    List<CParameterDeclaration> parameterDeclarations =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration().getParameters();
    // C does not allow optional parameters
    assert arguments.size() == parameterDeclarations.size()
        : "function argument number should be same as parameter declaration number";
    for (int i = 0; i < arguments.size(); i++) {
      // we use both pointer and non-pointer parameters, e.g. 'global_ptr = &non_ptr_param;'
      CParameterDeclaration leftHandSide = parameterDeclarations.get(i);
      MemoryLocation rhsMemoryLocation =
          extractMemoryLocation(
              pOptions, pThread, pCallContext, arguments.get(i), pInitialMemoryLocations);
      if (!rhsMemoryLocation.isEmpty()) {
        rAssignments.put(
            MemoryLocation.of(Optional.of(pCallContext), leftHandSide), rhsMemoryLocation);
      }
    }
    return rAssignments.buildOrThrow();
  }

  private static MemoryLocation extractMemoryLocation(
      MPOROptions pOptions,
      MPORThread pThread,
      ThreadEdge pCallContext,
      CExpression pRightHandSide,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations) {

    if (pRightHandSide instanceof CIdExpression idExpression) {
      return getMemoryLocationByDeclaration(
          pOptions, pThread, pCallContext, idExpression.getDeclaration(), pInitialMemoryLocations);

    } else if (pRightHandSide instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
        return getMemoryLocationByDeclaration(
            pOptions,
            pThread,
            pCallContext,
            idExpression.getDeclaration(),
            pInitialMemoryLocations);

      } else if (unaryExpression.getOperand() instanceof CFieldReference fieldReference) {
        return extractFieldReferenceMemoryLocation(
            pOptions, pThread, pCallContext, fieldReference, pInitialMemoryLocations);
      }

    } else if (pRightHandSide instanceof CFieldReference fieldReference) {
      return extractFieldReferenceMemoryLocation(
          pOptions, pThread, pCallContext, fieldReference, pInitialMemoryLocations);
    }
    // can e.g. occur with 'param = 4' i.e. literal integer expressions
    return MemoryLocation.empty();
  }

  private static MemoryLocation extractFieldReferenceMemoryLocation(
      MPOROptions pOptions,
      MPORThread pThread,
      ThreadEdge pCallContext,
      CFieldReference pFieldReference,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations) {

    if (pFieldReference.getFieldOwner().getExpressionType() instanceof CTypedefType typedefType) {
      CIdExpression fieldOwner = MPORUtil.recursivelyFindFieldOwner(pFieldReference);
      CCompositeTypeMemberDeclaration fieldMember =
          MPORUtil.getFieldMemberByName(pFieldReference, typedefType);
      return getMemoryLocationByFieldReference(
          pOptions,
          pThread,
          pCallContext,
          fieldOwner.getDeclaration(),
          fieldMember,
          pInitialMemoryLocations);
    }
    throw new IllegalArgumentException("pFieldReference owner type must be CTypedefType");
  }

  private static MemoryLocation getMemoryLocationByDeclaration(
      MPOROptions pOptions,
      MPORThread pThread,
      ThreadEdge pCallContext,
      CSimpleDeclaration pDeclaration,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations) {

    for (MemoryLocation memoryLocation : pInitialMemoryLocations) {
      if (memoryLocation.variable.isPresent()) {
        if (memoryLocation.variable.orElseThrow().equals(pDeclaration)) {
          return memoryLocation;
        }
      }
    }
    return MemoryLocation.of(pOptions, pThread, Optional.of(pCallContext), pDeclaration);
  }

  private static MemoryLocation getMemoryLocationByFieldReference(
      MPOROptions pOptions,
      MPORThread pThread,
      ThreadEdge pCallContext,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember,
      ImmutableSet<MemoryLocation> pAllMemoryLocations) {

    for (MemoryLocation memoryLocation : pAllMemoryLocations) {
      if (memoryLocation.fieldMember.isPresent()) {
        Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration> fieldMember =
            memoryLocation.fieldMember.orElseThrow();
        if (fieldMember.getKey().equals(pFieldOwner)) {
          if (fieldMember.getValue().equals(pFieldMember)) {
            return memoryLocation;
          }
        }
      }
    }
    if (pFieldOwner instanceof CVariableDeclaration variableDeclaration) {
      if (variableDeclaration.isGlobal()) {
        return MemoryLocation.of(Optional.of(pCallContext), pFieldOwner, pFieldMember);
      }
    }
    return MemoryLocation.of(
        pOptions, pThread, Optional.of(pCallContext), pFieldOwner, pFieldMember);
  }

  // Pointer Parameter Assignments =================================================================

  public static ImmutableMap<MemoryLocation, MemoryLocation> extractPointerParameters(
      ImmutableMap<MemoryLocation, MemoryLocation> pParameterAssignments) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rPointers = ImmutableMap.builder();
    for (var entry : pParameterAssignments.entrySet()) {
      if (entry.getKey().getSimpleDeclaration().getType() instanceof CPointerType) {
        rPointers.put(entry);
      }
    }
    return rPointers.buildOrThrow();
  }

  // Pointer Dereferences ==========================================================================

  private static ImmutableSet<MemoryLocation> getAllPointerDereferences(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    return pSubstituteEdges.stream()
        .flatMap(
            substituteEdge ->
                substituteEdge.getPointerDereferencesByAccessType(MemoryAccessType.ACCESS).stream())
        .collect(ImmutableSet.toImmutableSet());
  }
}
