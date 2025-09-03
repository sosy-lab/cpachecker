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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class MemoryModelBuilder {

  public static Optional<MemoryModel> tryBuildMemoryModel(
      MPOROptions pOptions,
      ImmutableList<MemoryLocation> pInitialMemoryLocations,
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    if (pOptions.linkReduction) {
      ImmutableMap<MemoryLocation, MemoryLocation> startRoutineArgAssignments =
          mapStartRoutineArgAssignments(pOptions, pSubstituteEdges, pInitialMemoryLocations);
      ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
          mapParameterAssignments(pOptions, pSubstituteEdges, pInitialMemoryLocations);
      ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
          extractPointerParameters(parameterAssignments);
      // TODO ensure order of values / keySets
      ImmutableList<MemoryLocation> newMemoryLocations =
          ImmutableList.<MemoryLocation>builder()
              .addAll(parameterAssignments.values())
              .addAll(startRoutineArgAssignments.keySet())
              .addAll(startRoutineArgAssignments.values())
              .build();

      // use distinct list so that sequentialization is deterministic
      ImmutableList<MemoryLocation> allMemoryLocations =
          getAllMemoryLocations(pInitialMemoryLocations, newMemoryLocations);
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

  private static MemoryModel buildMemoryModel(
      ImmutableList<MemoryLocation> pAllMemoryLocations,
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

  // All Memory Locations ==========================================================================

  private static ImmutableList<MemoryLocation> getAllMemoryLocations(
      ImmutableList<MemoryLocation> pInitialMemoryLocations,
      ImmutableList<MemoryLocation> pNewMemoryLocations) {

    List<MemoryLocation> rAllMemoryLocations = new ArrayList<>(pInitialMemoryLocations);
    rAllMemoryLocations.addAll(pNewMemoryLocations);
    return rAllMemoryLocations.stream().distinct().collect(ImmutableList.toImmutableList());
  }

  // Collection helpers ============================================================================

  private static ImmutableMap<MemoryLocation, Integer> getRelevantMemoryLocationsIds(
      ImmutableList<MemoryLocation> pAllMemoryLocations,
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
      // relevant locations are either explicit or implicit (e.g. through pointers) global
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
  static boolean isImplicitGlobal(
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    if (pMemoryLocation.isExplicitGlobal()) {
      return false;
    }
    // e.g. (void*) arg = &local_var -> local_var can be accessed by creating and created threads
    if (pStartRoutineArgAssignments.containsValue(pMemoryLocation)) {
      return true;
    }
    // if any pointer points to the memory location: check all pointer assignments and derefs
    if (isPointedTo(pMemoryLocation, pPointerAssignments, pPointerParameterAssignments)) {
      if (isImplicitGlobalByPointerAssignmentsAndDereferences(
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

  private static boolean isImplicitGlobalByPointerAssignmentsAndDereferences(
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    // inexpensive shortcut: first check for direct assignments
    if (isImplicitGlobalByDirectPointerAssignments(
        pPointerAssignments, pStartRoutineArgAssignments)) {
      return true;
    }
    // then check if a global pointer deref is associated with the memory location
    if (isImplicitGlobalByPointerDereference(
        pMemoryLocation,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pPointerParameterAssignments,
        pPointerDereferences)) {
      return true;
    }
    // lastly perform most expensive check on transitive pointer assignments
    if (isImplicitGlobalByTransitivePointerAssignments(
        pPointerAssignments, pStartRoutineArgAssignments, pPointerParameterAssignments)) {
      return true;
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

  private static boolean isImplicitGlobalByPointerDereference(
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

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
    return false;
  }

  private static boolean isImplicitGlobalByDirectPointerAssignments(
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments) {

    for (MemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
      if (isExplicitGlobalOrStartRoutineArg(pointerDeclaration, pStartRoutineArgAssignments)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isImplicitGlobalByTransitivePointerAssignments(
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments) {

    for (MemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
      ImmutableSet<MemoryLocation> transitivePointerDeclarations =
          findPointerDeclarationsByPointerAssignments(
              pointerDeclaration,
              pPointerAssignments,
              pStartRoutineArgAssignments,
              pPointerParameterAssignments);
      for (MemoryLocation transitivePointerDeclaration : transitivePointerDeclarations) {
        if (isExplicitGlobalOrStartRoutineArg(
            transitivePointerDeclaration, pStartRoutineArgAssignments)) {
          return true;
        }
      }
    }
    return false;
  }

  // Extraction by Pointer Assignments (including Parameters) ======================================

  private static ImmutableSet<MemoryLocation> findPointerDeclarationsByPointerAssignments(
      MemoryLocation pPointerDeclaration,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments) {

    Set<MemoryLocation> rFound = new HashSet<>();
    recursivelyFindPointerDeclarationsByPointerAssignments(
        pPointerDeclaration,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pPointerParameterAssignments,
        rFound,
        new HashSet<>());
    return ImmutableSet.copyOf(rFound);
  }

  private static void recursivelyFindPointerDeclarationsByPointerAssignments(
      MemoryLocation pCurrentMemoryLocation,
      final ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      final ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      final ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      Set<MemoryLocation> pFound,
      Set<MemoryLocation> pVisited) {

    if (MemoryModel.isLeftHandSideInPointerAssignment(
        pCurrentMemoryLocation,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pPointerParameterAssignments)) {
      for (MemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
        if (pVisited.add(pointerDeclaration)) {
          for (MemoryLocation memoryLocation : pPointerAssignments.get(pointerDeclaration)) {
            pFound.add(pointerDeclaration);
            recursivelyFindPointerDeclarationsByPointerAssignments(
                memoryLocation,
                pPointerAssignments,
                pStartRoutineArgAssignments,
                pPointerParameterAssignments,
                pFound,
                pVisited);
          }
        }
      }
    }
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
      ImmutableCollection<SubstituteEdge> pSubstituteEdges,
      ImmutableList<MemoryLocation> pInitialMemoryLocations) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rAssignments = ImmutableMap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      // use the original edge, so that we use the original variable declarations
      CFAEdge original = substituteEdge.getOriginalCfaEdge();
      if (PthreadUtil.callsPthreadFunction(original, PthreadFunctionType.PTHREAD_CREATE)) {
        ThreadEdge callContext = substituteEdge.getThreadEdge();
        CExpression startRoutineArg =
            CFAUtils.getParameterAtIndex(
                original, PthreadFunctionType.PTHREAD_CREATE.getStartRoutineArgIndex());
        Optional<MemoryLocation> rhsMemoryLocation =
            extractMemoryLocation(pOptions, callContext, startRoutineArg, pInitialMemoryLocations);
        if (rhsMemoryLocation.isPresent()) {
          // use the ID of the created thread for the parameter declaration
          CFunctionDeclaration functionDeclaration =
              PthreadUtil.extractStartRoutineDeclaration(original);
          assert functionDeclaration.getParameters().size() == 1
              : "start_routine functions can only have a single parameter";
          CParameterDeclaration parameterDeclaration =
              functionDeclaration.getParameters().getFirst();
          rAssignments.put(
              MemoryLocation.of(pOptions, Optional.of(callContext), parameterDeclaration),
              rhsMemoryLocation.orElseThrow());
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  // Parameter Assignments =========================================================================

  private static ImmutableMap<MemoryLocation, MemoryLocation> mapParameterAssignments(
      MPOROptions pOptions,
      ImmutableCollection<SubstituteEdge> pSubstituteEdges,
      ImmutableList<MemoryLocation> pInitialMemoryLocations) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rAssignments = ImmutableMap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      // use the original edge, so that we use the original variable declarations
      CFAEdge original = substituteEdge.getOriginalCfaEdge();
      if (original instanceof CFunctionCallEdge functionCallEdge) {
        ThreadEdge callContext = substituteEdge.getThreadEdge();
        rAssignments.putAll(
            buildParameterAssignments(
                pOptions, callContext, functionCallEdge, pInitialMemoryLocations));
      }
    }
    return rAssignments.buildOrThrow();
  }

  private static ImmutableMap<MemoryLocation, MemoryLocation> buildParameterAssignments(
      MPOROptions pOptions,
      ThreadEdge pCallContext,
      CFunctionCallEdge pFunctionCallEdge,
      ImmutableList<MemoryLocation> pInitialMemoryLocations) {

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
      Optional<MemoryLocation> rhsMemoryLocation =
          extractMemoryLocation(pOptions, pCallContext, arguments.get(i), pInitialMemoryLocations);
      if (rhsMemoryLocation.isPresent()) {
        rAssignments.put(
            MemoryLocation.of(pOptions, Optional.of(pCallContext), leftHandSide),
            rhsMemoryLocation.orElseThrow());
      }
    }
    return rAssignments.buildOrThrow();
  }

  private static Optional<MemoryLocation> extractMemoryLocation(
      MPOROptions pOptions,
      ThreadEdge pCallContext,
      CExpression pRightHandSide,
      ImmutableList<MemoryLocation> pInitialMemoryLocations) {

    if (pRightHandSide instanceof CIdExpression idExpression) {
      return Optional.of(
          getMemoryLocationByDeclaration(
              pOptions, pCallContext, idExpression.getDeclaration(), pInitialMemoryLocations));

    } else if (pRightHandSide instanceof CFieldReference fieldReference) {
      return Optional.of(
          extractFieldReferenceMemoryLocation(
              pOptions, pCallContext, fieldReference, pInitialMemoryLocations));

    } else if (pRightHandSide instanceof CUnaryExpression unaryExpression) {
      return extractMemoryLocation(
          pOptions, pCallContext, unaryExpression.getOperand(), pInitialMemoryLocations);

    } else if (pRightHandSide instanceof CCastExpression castExpression) {
      return extractMemoryLocation(
          pOptions, pCallContext, castExpression.getOperand(), pInitialMemoryLocations);
    }
    // can e.g. occur with 'param = 4' i.e. literal integer expressions
    return Optional.empty();
  }

  private static MemoryLocation extractFieldReferenceMemoryLocation(
      MPOROptions pOptions,
      ThreadEdge pCallContext,
      CFieldReference pFieldReference,
      ImmutableList<MemoryLocation> pInitialMemoryLocations) {

    CIdExpression fieldOwner = MPORUtil.recursivelyFindFieldOwner(pFieldReference);
    CCompositeTypeMemberDeclaration fieldMember =
        MPORUtil.getFieldMemberByFieldReference(
            pFieldReference, pFieldReference.getFieldOwner().getExpressionType());
    return getMemoryLocationByFieldReference(
        pOptions, pCallContext, fieldOwner.getDeclaration(), fieldMember, pInitialMemoryLocations);
  }

  private static MemoryLocation getMemoryLocationByDeclaration(
      MPOROptions pOptions,
      ThreadEdge pCallContext,
      CSimpleDeclaration pDeclaration,
      ImmutableList<MemoryLocation> pInitialMemoryLocations) {

    for (MemoryLocation memoryLocation : pInitialMemoryLocations) {
      if (memoryLocation.declaration.equals(pDeclaration)) {
        return memoryLocation;
      }
    }
    return MemoryLocation.of(pOptions, Optional.of(pCallContext), pDeclaration);
  }

  private static MemoryLocation getMemoryLocationByFieldReference(
      MPOROptions pOptions,
      ThreadEdge pCallContext,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember,
      ImmutableList<MemoryLocation> pAllMemoryLocations) {

    for (MemoryLocation memoryLocation : pAllMemoryLocations) {
      if (memoryLocation.fieldMember.isPresent()) {
        CCompositeTypeMemberDeclaration fieldMember = memoryLocation.fieldMember.orElseThrow();
        if (memoryLocation.declaration.equals(pFieldOwner) && fieldMember.equals(pFieldMember)) {
          return memoryLocation;
        }
      }
    }
    if (pFieldOwner instanceof CVariableDeclaration variableDeclaration) {
      if (variableDeclaration.isGlobal()) {
        return MemoryLocation.of(pOptions, Optional.of(pCallContext), pFieldOwner, pFieldMember);
      }
    }
    return MemoryLocation.of(pOptions, Optional.of(pCallContext), pFieldOwner, pFieldMember);
  }

  // Pointer Parameter Assignments =================================================================

  static ImmutableMap<MemoryLocation, MemoryLocation> extractPointerParameters(
      ImmutableMap<MemoryLocation, MemoryLocation> pParameterAssignments) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rPointers = ImmutableMap.builder();
    for (var entry : pParameterAssignments.entrySet()) {
      if (entry.getKey().declaration.getType() instanceof CPointerType) {
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
