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
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class MemoryModelBuilder {

  public static Optional<MemoryModel> tryBuildMemoryModel(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations,
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    if (pOptions.linkReduction) {
      ImmutableMap<MemoryLocation, MemoryLocation> startRoutineArgAssignments =
          mapStartRoutineArgAssignments(
              pOptions, pThreads, pSubstituteEdges, pInitialMemoryLocations);
      ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
          mapParameterAssignments(pOptions, pThreads, pSubstituteEdges, pInitialMemoryLocations);
      ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
          extractPointerParameters(parameterAssignments);
      ImmutableCollection<MemoryLocation> newMemoryLocations =
          ImmutableSet.<MemoryLocation>builder()
              .addAll(parameterAssignments.values())
              .addAll(startRoutineArgAssignments.keySet())
              .addAll(startRoutineArgAssignments.values())
              .build();

      ImmutableSet<MemoryLocation> allMemoryLocations =
          ImmutableSet.<MemoryLocation>builder()
              .addAll(pInitialMemoryLocations)
              .addAll(newMemoryLocations)
              .build();
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pointerAssignments =
          mapPointerAssignments(pSubstituteEdges);
      ImmutableSet<MemoryLocation> pointerDereferences =
          getAllPointerDereferences(pSubstituteEdges);

      MemoryModel memoryModel =
          buildMemoryModel(
              allMemoryLocations,
              pointerAssignments,
              startRoutineArgAssignments,
              parameterAssignments,
              pointerParameterAssignments,
              pointerDereferences);
      return Optional.of(memoryModel);
    } else {
      return Optional.empty();
    }
  }

  public static MemoryModel buildMemoryModel(
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pParameterAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    ImmutableMap<MemoryLocation, Integer> relevantMemoryLocationIds =
        getRelevantMemoryLocationsIds(
            pAllMemoryLocations,
            pPointerAssignments,
            pStartRoutineArgAssignments,
            pPointerParameterAssignments,
            pPointerDereferences);
    return new MemoryModel(
        pAllMemoryLocations,
        relevantMemoryLocationIds,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pParameterAssignments,
        pPointerParameterAssignments,
        pPointerDereferences);
  }

  // Collection helpers ============================================================================

  private static ImmutableMap<MemoryLocation, Integer> getRelevantMemoryLocationsIds(
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    ImmutableMap.Builder<MemoryLocation, Integer> rRelevantIds = ImmutableMap.builder();
    int currentId = BitVectorUtil.RIGHT_INDEX;
    for (MemoryLocation memoryLocation : pAllMemoryLocations) {
      if (isRelevantMemoryLocation(
          memoryLocation,
          pPointerAssignments,
          pStartRoutineArgAssignments,
          pPointerParameterAssignments,
          pPointerDereferences)) {
        rRelevantIds.put(memoryLocation, currentId++);
      }
    }
    return rRelevantIds.buildOrThrow();
  }

  private static boolean isRelevantMemoryLocation(
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    // exclude const CPAchecker_TMP, they do not have any effect in the input program
    if (!MemoryLocationUtil.isConstCpaCheckerTmp(pMemoryLocation)) {
      // relevant locations are either explicitly global or implicitly (e.g. through pointers)
      if (pMemoryLocation.isExplicitGlobal()
          || isImplicitGlobal(
              pMemoryLocation,
              pPointerAssignments,
              pStartRoutineArgAssignments,
              pPointerParameterAssignments,
              pPointerDereferences)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code pMemoryLocation} is implicitly global e.g. through {@code
   * global_ptr = &local_var;}. Returns {@code false} even if the memory location itself is global.
   */
  public static boolean isImplicitGlobal(
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    if (pMemoryLocation.isExplicitGlobal()) {
      return false;
    }
    // e.g. (void*) arg = &local_var -> local_var can be accessed by both threads
    if (pStartRoutineArgAssignments.containsValue(pMemoryLocation)) {
      return true;
    }
    if (isPointedTo(pMemoryLocation, pPointerAssignments, pPointerParameterAssignments)) {
      // inexpensive shortcut: first check for direct assignments
      for (MemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
        if (isExplicitGlobalOrStartRoutineArg(pointerDeclaration, pStartRoutineArgAssignments)) {
          return true;
        }
      }
      // then check if a global pointer deref is associated with the memory location
      for (MemoryLocation pointerDereference : pPointerDereferences) {
        if (pointerDereference.equals(pMemoryLocation)) {
          ImmutableSet<MemoryLocation> memoryLocations =
              MemoryLocationFinder.findMemoryLocationsByPointerDereference(
                  pointerDereference,
                  pPointerAssignments,
                  pStartRoutineArgAssignments,
                  pPointerParameterAssignments);
          for (MemoryLocation memoryLocation : memoryLocations) {
            if (isExplicitGlobalOrStartRoutineArg(memoryLocation, pStartRoutineArgAssignments)) {
              return true;
            }
          }
        }
      }
      // lastly perform most expensive check on transitive pointer assignments
      for (MemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
        ImmutableSet<MemoryLocation> transitivePointerDeclarations =
            MemoryLocationFinder.findPointerDeclarationsByPointerAssignments(
                pointerDeclaration,
                pPointerAssignments,
                pStartRoutineArgAssignments,
                pPointerParameterAssignments);
        for (MemoryLocation otherPointerDeclaration : transitivePointerDeclarations) {
          if (isExplicitGlobalOrStartRoutineArg(
              otherPointerDeclaration, pStartRoutineArgAssignments)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean isExplicitGlobalOrStartRoutineArg(
      MemoryLocation pMemoryLocation,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments) {

    return pMemoryLocation.isExplicitGlobal()
        || pStartRoutineArgAssignments.containsValue(pMemoryLocation);
  }

  /**
   * Returns {@code true} if any pointer (parameter or variable, both local and global) points to
   * {@code pMemoryLocation}.
   */
  private static boolean isPointedTo(
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments) {

    if (pPointerAssignments.values().contains(pMemoryLocation)) {
      return true;
    }
    if (pPointerParameterAssignments.containsValue(pMemoryLocation)) {
      return true;
    }
    return false;
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

  // start_routine arg Assignments =================================================================

  private static ImmutableMap<MemoryLocation, MemoryLocation> mapStartRoutineArgAssignments(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableCollection<SubstituteEdge> pSubstituteEdges,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rAssignments = ImmutableMap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      // use the original edge, so that we use the original variable declarations
      CFAEdge original = substituteEdge.getOriginalCfaEdge();
      if (PthreadUtil.callsPthreadFunction(original, PthreadFunctionType.PTHREAD_CREATE)) {
        ThreadEdge callContext = substituteEdge.getThreadEdge();
        MPORThread thread = ThreadUtil.getThreadById(pThreads, callContext.threadId);
        CExpression startRoutineArg =
            CFAUtils.getParameterAtIndex(
                original, PthreadFunctionType.PTHREAD_CREATE.getStartRoutineArgIndex());
        MemoryLocation rhsMemoryLocation =
            extractMemoryLocation(
                pOptions, thread, callContext, startRoutineArg, pInitialMemoryLocations);
        if (!rhsMemoryLocation.isEmpty()) {
          // use the ID of the created thread for the parameter declaration
          CIdExpression pthreadT = PthreadUtil.extractPthreadT(original);
          MPORThread createdThread = ThreadUtil.getThreadByObject(pThreads, Optional.of(pthreadT));
          CFunctionDeclaration functionDeclaration =
              PthreadUtil.extractStartRoutineDeclaration(original);
          assert functionDeclaration.getParameters().size() == 1
              : "start_routine functions can only have a single parameter";
          CParameterDeclaration parameterDeclaration =
              functionDeclaration.getParameters().getFirst();
          rAssignments.put(
              MemoryLocation.of(
                  pOptions, createdThread.id, Optional.of(callContext), parameterDeclaration),
              rhsMemoryLocation);
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  // Parameter Assignments =========================================================================

  private static ImmutableMap<MemoryLocation, MemoryLocation> mapParameterAssignments(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableCollection<SubstituteEdge> pSubstituteEdges,
      ImmutableSet<MemoryLocation> pInitialMemoryLocations) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rAssignments = ImmutableMap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      // use the original edge, so that we use the original variable declarations
      CFAEdge original = substituteEdge.getOriginalCfaEdge();
      if (original instanceof CFunctionCallEdge functionCallEdge) {
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
            MemoryLocation.of(pOptions, Optional.of(pCallContext), leftHandSide),
            rhsMemoryLocation);
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

    } else if (pRightHandSide instanceof CFieldReference fieldReference) {
      return extractFieldReferenceMemoryLocation(
          pOptions, pThread, pCallContext, fieldReference, pInitialMemoryLocations);

    } else if (pRightHandSide instanceof CUnaryExpression unaryExpression) {
      return extractMemoryLocation(
          pOptions, pThread, pCallContext, unaryExpression.getOperand(), pInitialMemoryLocations);

    } else if (pRightHandSide instanceof CCastExpression castExpression) {
      return extractMemoryLocation(
          pOptions, pThread, pCallContext, castExpression.getOperand(), pInitialMemoryLocations);
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
      if (memoryLocation.declaration.isPresent()) {
        if (memoryLocation.declaration.orElseThrow().equals(pDeclaration)) {
          return memoryLocation;
        }
      }
    }
    return MemoryLocation.of(pOptions, pThread.id, Optional.of(pCallContext), pDeclaration);
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
        return MemoryLocation.of(
            pOptions, pThread.id, Optional.of(pCallContext), pFieldOwner, pFieldMember);
      }
    }
    return MemoryLocation.of(
        pOptions, pThread.id, Optional.of(pCallContext), pFieldOwner, pFieldMember);
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
