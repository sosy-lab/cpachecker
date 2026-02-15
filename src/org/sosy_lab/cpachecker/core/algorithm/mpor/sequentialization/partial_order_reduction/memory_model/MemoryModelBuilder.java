// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import com.google.common.annotations.VisibleForTesting;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record MemoryModelBuilder(
    MPOROptions options,
    ImmutableList<SeqMemoryLocation> initialMemoryLocations,
    ImmutableCollection<SubstituteEdge> substituteEdges) {

  public Optional<MemoryModel> tryBuildMemoryModel() throws UnsupportedCodeException {
    if (options.linkReduction()) {
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> startRoutineArgAssignments =
          mapStartRoutineArgAssignments();
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> parameterAssignments =
          mapParameterAssignments();
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerParameterAssignments =
          getPointerParameterAssignments(parameterAssignments);
      ImmutableList<SeqMemoryLocation> newMemoryLocations =
          ImmutableList.<SeqMemoryLocation>builder()
              .addAll(parameterAssignments.values())
              .addAll(startRoutineArgAssignments.keySet())
              .addAll(startRoutineArgAssignments.values())
              .build();

      // use distinct list so that sequentialization is deterministic
      ImmutableList<SeqMemoryLocation> allMemoryLocations =
          getAllMemoryLocations(newMemoryLocations);
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
          mapPointerAssignments();
      ImmutableSet<SeqMemoryLocation> pointerDereferences = getAllPointerDereferences();

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

  private MemoryModel buildMemoryModel(
      ImmutableList<SeqMemoryLocation> pAllMemoryLocations,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pParameterAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences)
      throws UnsupportedCodeException {

    ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocationIds =
        getRelevantMemoryLocationsIds(
            pAllMemoryLocations,
            pPointerAssignments,
            pStartRoutineArgAssignments,
            pPointerParameterAssignments,
            pPointerDereferences);
    return new MemoryModel(
        options,
        pAllMemoryLocations,
        relevantMemoryLocationIds,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pParameterAssignments,
        pPointerParameterAssignments,
        pPointerDereferences);
  }

  // All Memory Locations ==========================================================================

  private ImmutableList<SeqMemoryLocation> getAllMemoryLocations(
      ImmutableList<SeqMemoryLocation> pNewMemoryLocations) {

    List<SeqMemoryLocation> rAllMemoryLocations = new ArrayList<>(initialMemoryLocations);
    rAllMemoryLocations.addAll(pNewMemoryLocations);
    return rAllMemoryLocations.stream().distinct().collect(ImmutableList.toImmutableList());
  }

  // Collection helpers ============================================================================

  private ImmutableMap<SeqMemoryLocation, Integer> getRelevantMemoryLocationsIds(
      ImmutableList<SeqMemoryLocation> pAllMemoryLocations,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    ImmutableMap.Builder<SeqMemoryLocation, Integer> rRelevantIds = ImmutableMap.builder();
    int currentId = SeqBitVectorUtil.RIGHT_INDEX;
    for (SeqMemoryLocation memoryLocation : pAllMemoryLocations) {
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

  private boolean isRelevantMemoryLocation(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    // exclude const CPAchecker_TMP, they do not have any effect in the input program
    if (MPORUtil.isConstCpaCheckerTmp(pMemoryLocation.declaration())) {
      return false;
    }
    // relevant locations are either explicit or implicit (e.g. through pointers) global
    if (pMemoryLocation.declaration().isGlobal()
        || isImplicitGlobal(
            pMemoryLocation,
            pPointerAssignments,
            pStartRoutineArgAssignments,
            pPointerParameterAssignments,
            pPointerDereferences)) {
      return true;
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code pMemoryLocation} is implicitly global e.g. through {@code
   * global_ptr = &local_var;}. Returns {@code false} even if the memory location itself is global.
   */
  static boolean isImplicitGlobal(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    if (pMemoryLocation.declaration().isGlobal()) {
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
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

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
      SeqMemoryLocation pMemoryLocation,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments) {

    return pMemoryLocation.declaration().isGlobal()
        || pStartRoutineArgAssignments.containsValue(pMemoryLocation);
  }

  /**
   * Returns {@code true} if any pointer (parameter or variable, both local and global) points to
   * {@code pMemoryLocation}.
   */
  private static boolean isPointedTo(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments) {

    if (pPointerAssignments.values().contains(pMemoryLocation)) {
      return true;
    }
    if (pPointerParameterAssignments.containsValue(pMemoryLocation)) {
      return true;
    }
    return false;
  }

  private static boolean isImplicitGlobalByPointerDereference(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    for (SeqMemoryLocation pointerDereference : pPointerDereferences) {
      if (pointerDereference.equals(pMemoryLocation)) {
        ImmutableSet<SeqMemoryLocation> memoryLocations =
            SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
                pointerDereference,
                pPointerAssignments,
                pStartRoutineArgAssignments,
                pPointerParameterAssignments);
        for (SeqMemoryLocation memoryLocation : memoryLocations) {
          if (isExplicitGlobalOrStartRoutineArg(memoryLocation, pStartRoutineArgAssignments)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean isImplicitGlobalByDirectPointerAssignments(
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments) {

    for (SeqMemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
      if (isExplicitGlobalOrStartRoutineArg(pointerDeclaration, pStartRoutineArgAssignments)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isImplicitGlobalByTransitivePointerAssignments(
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments) {

    for (SeqMemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
      ImmutableSet<SeqMemoryLocation> transitivePointerDeclarations =
          findPointerDeclarationsByPointerAssignments(
              pointerDeclaration,
              pPointerAssignments,
              pStartRoutineArgAssignments,
              pPointerParameterAssignments);
      for (SeqMemoryLocation transitivePointerDeclaration : transitivePointerDeclarations) {
        if (isExplicitGlobalOrStartRoutineArg(
            transitivePointerDeclaration, pStartRoutineArgAssignments)) {
          return true;
        }
      }
    }
    return false;
  }

  // Extraction by Pointer Assignments (including Parameters) ======================================

  private static ImmutableSet<SeqMemoryLocation> findPointerDeclarationsByPointerAssignments(
      SeqMemoryLocation pPointerDeclaration,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments) {

    Set<SeqMemoryLocation> rFound = new HashSet<>();
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
      SeqMemoryLocation pCurrentMemoryLocation,
      final ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      Set<SeqMemoryLocation> pFound,
      Set<SeqMemoryLocation> pVisited) {

    if (MemoryModel.isLeftHandSideInPointerAssignment(
        pCurrentMemoryLocation,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pPointerParameterAssignments)) {
      for (SeqMemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
        if (pVisited.add(pointerDeclaration)) {
          for (SeqMemoryLocation memoryLocation : pPointerAssignments.get(pointerDeclaration)) {
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

  private ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> mapPointerAssignments() {
    ImmutableSetMultimap.Builder<SeqMemoryLocation, SeqMemoryLocation> rAllAssignments =
        ImmutableSetMultimap.builder();
    for (SubstituteEdge substituteEdge : substituteEdges) {
      rAllAssignments.putAll(substituteEdge.pointerAssignments.asMultimap());
    }
    return rAllAssignments.build();
  }

  // start_routine arg Assignments =================================================================

  private ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> mapStartRoutineArgAssignments()
      throws UnsupportedCodeException {

    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rAssignments =
        ImmutableMap.builder();
    for (SubstituteEdge substituteEdge : substituteEdges) {
      // use the original edge, so that we use the original variable declarations
      CFAEdge original = substituteEdge.getOriginalCfaEdge();
      Optional<CFunctionCall> optionalFunctionCall =
          PthreadUtil.tryGetFunctionCallFromCfaEdge(original);
      if (optionalFunctionCall.isPresent()) {
        CFunctionCall functionCall = optionalFunctionCall.orElseThrow();
        if (PthreadUtil.isCallToPthreadFunction(functionCall, PthreadFunctionType.PTHREAD_CREATE)) {
          CFAEdgeForThread callContext = substituteEdge.getThreadEdge();
          int index =
              PthreadFunctionType.PTHREAD_CREATE.getParameterIndex(
                  PthreadObjectType.START_ROUTINE_ARGUMENT);
          CExpression startRoutineArgExpression =
              functionCall.getFunctionCallExpression().getParameterExpressions().get(index);
          Optional<SeqMemoryLocation> rhsMemoryLocation =
              extractMemoryLocation(callContext, startRoutineArgExpression);
          if (rhsMemoryLocation.isPresent()) {
            // use the ID of the created thread for the parameter declaration
            CFunctionDeclaration functionDeclaration =
                PthreadUtil.extractStartRoutineDeclaration(functionCall);
            assert functionDeclaration.getParameters().size() == 1
                : "start_routine functions can only have a single parameter";
            CParameterDeclaration parameterDeclaration =
                functionDeclaration.getParameters().getFirst();
            rAssignments.put(
                SeqMemoryLocation.of(
                    options,
                    Optional.of(callContext),
                    parameterDeclaration.asVariableDeclaration()),
                rhsMemoryLocation.orElseThrow());
          }
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  // Parameter Assignments =========================================================================

  private ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> mapParameterAssignments()
      throws UnsupportedCodeException {

    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rAssignments =
        ImmutableMap.builder();
    for (SubstituteEdge substituteEdge : substituteEdges) {
      // use the original edge, so that we use the original variable declarations
      CFAEdge original = substituteEdge.getOriginalCfaEdge();
      if (original instanceof CFunctionCallEdge functionCallEdge) {
        CFAEdgeForThread callContext = substituteEdge.getThreadEdge();
        rAssignments.putAll(buildParameterAssignments(callContext, functionCallEdge));
      }
    }
    return rAssignments.buildOrThrow();
  }

  private ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> buildParameterAssignments(
      CFAEdgeForThread pCallContext, CFunctionCallEdge pFunctionCallEdge)
      throws UnsupportedCodeException {

    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rAssignments =
        ImmutableMap.builder();
    CFunctionDeclaration functionDeclaration =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration();
    List<CExpression> arguments = pFunctionCallEdge.getArguments();
    for (int i = 0; i < arguments.size(); i++) {
      // we use both pointer and non-pointer parameters, e.g. 'global_ptr = &non_ptr_param;'
      CParameterDeclaration leftHandSide =
          MPORUtil.getParameterDeclarationByIndex(i, functionDeclaration);
      Optional<SeqMemoryLocation> rhsMemoryLocation =
          extractMemoryLocation(pCallContext, arguments.get(i));
      if (rhsMemoryLocation.isPresent()) {
        rAssignments.put(
            SeqMemoryLocation.of(
                options, Optional.of(pCallContext), leftHandSide.asVariableDeclaration()),
            rhsMemoryLocation.orElseThrow());
      }
    }
    return rAssignments.buildOrThrow();
  }

  private Optional<SeqMemoryLocation> extractMemoryLocation(
      CFAEdgeForThread pCallContext, CExpression pRightHandSide) throws UnsupportedCodeException {

    return switch (pRightHandSide) {
      case CIdExpression idExpression ->
          Optional.of(getMemoryLocationByDeclaration(pCallContext, idExpression.getDeclaration()));
      case CFieldReference fieldReference ->
          Optional.of(extractFieldReferenceMemoryLocation(pCallContext, fieldReference));
      case CUnaryExpression unaryExpression ->
          extractMemoryLocation(pCallContext, unaryExpression.getOperand());
      case CCastExpression castExpression ->
          extractMemoryLocation(pCallContext, castExpression.getOperand());
      default ->
          // can e.g. occur with 'param = 4' i.e. literal integer expressions
          Optional.empty();
    };
  }

  private SeqMemoryLocation extractFieldReferenceMemoryLocation(
      CFAEdgeForThread pCallContext, CFieldReference pFieldReference)
      throws UnsupportedCodeException {

    CIdExpression fieldOwner = MPORUtil.recursivelyFindFieldOwner(pFieldReference);
    CCompositeTypeMemberDeclaration fieldMember =
        MPORUtil.recursivelyFindFieldMemberByFieldOwner(
            pFieldReference, pFieldReference.getFieldOwner().getExpressionType());
    return getMemoryLocationByFieldReference(
        pCallContext, fieldOwner.getDeclaration(), fieldMember);
  }

  private SeqMemoryLocation getMemoryLocationByDeclaration(
      CFAEdgeForThread pCallContext, CSimpleDeclaration pDeclaration) {

    for (SeqMemoryLocation memoryLocation : initialMemoryLocations) {
      if (memoryLocation.declaration().equals(pDeclaration)) {
        return memoryLocation;
      }
    }
    return SeqMemoryLocation.of(
        options, Optional.of(pCallContext), (CVariableDeclaration) pDeclaration);
  }

  private SeqMemoryLocation getMemoryLocationByFieldReference(
      CFAEdgeForThread pCallContext,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember) {

    for (SeqMemoryLocation memoryLocation : initialMemoryLocations) {
      if (memoryLocation.fieldMember().isPresent()) {
        CCompositeTypeMemberDeclaration fieldMember = memoryLocation.fieldMember().orElseThrow();
        if (memoryLocation.declaration().equals(pFieldOwner) && fieldMember.equals(pFieldMember)) {
          return memoryLocation;
        }
      }
    }
    return SeqMemoryLocation.of(
        options, Optional.of(pCallContext), (CVariableDeclaration) pFieldOwner, pFieldMember);
  }

  // Pointer Dereferences ==========================================================================

  private ImmutableSet<SeqMemoryLocation> getAllPointerDereferences() {
    return substituteEdges.stream()
        .flatMap(
            substituteEdge ->
                substituteEdge.getPointerDereferencesByAccessType(MemoryAccessType.ACCESS).stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  // Pointer Parameter Assignments =================================================================

  @VisibleForTesting
  static ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> getPointerParameterAssignments(
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pParameterAssignments) {

    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rPointers = ImmutableMap.builder();
    for (var entry : pParameterAssignments.entrySet()) {
      if (entry.getKey().declaration().getType() instanceof CPointerType) {
        rPointers.put(entry);
      }
    }
    return rPointers.buildOrThrow();
  }
}
