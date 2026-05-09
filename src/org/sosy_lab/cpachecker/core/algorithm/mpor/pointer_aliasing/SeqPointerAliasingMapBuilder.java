// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil.CLeftHandSideSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record SeqPointerAliasingMapBuilder(
    MPOROptions options,
    ImmutableList<SeqMemoryLocation> initialMemoryLocations,
    ImmutableCollection<SubstituteEdge> substituteEdges,
    ImmutableCollection<SeqFunctionStatements> functionStatements,
    MachineModel machineModel) {

  private static final int INITIAL_MEMORY_LOCATION_ID = 0;

  public SeqPointerAliasingMap buildPointerAliasingMap() throws UnsupportedCodeException {
    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> startRoutineArgAssignments =
        mapStartRoutineArgAssignments();
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        substituteEdges.stream()
            .flatMap(edge -> edge.pointerAssignments.asMultimap().entries().stream())
            .collect(ImmutableSetMultimap.toImmutableSetMultimap(Entry::getKey, Entry::getValue));
    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> parameterAssignments =
        mapParameterAssignments();
    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerParameterAssignments =
        getPointerParameterAssignments(parameterAssignments);

    ImmutableSet<SeqMemoryLocation> pointerDereferences =
        substituteEdges.stream()
            .flatMap(
                substituteEdge ->
                    substituteEdge
                        .getPointerDereferencesByAccessType(SeqMemoryAccessType.ACCESS)
                        .stream())
            .collect(ImmutableSet.toImmutableSet());
    // Dereference all pointers once to find the memory locations the pointers point to. This is
    // useful e.g. when a pointer points to a memory location that is a member of a struct. If the
    // struct member is never accessed directly but e.g. only the struct owner, then the struct
    // member is not part of initialMemoryLocations and can be found through the dereference.
    ImmutableSet<SeqMemoryLocation> pointerDereferenceMemoryLocations =
        pointerDereferences.stream()
            .flatMap(
                d ->
                    SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
                        d,
                        pointerAssignments,
                        startRoutineArgAssignments,
                        pointerParameterAssignments)
                        .stream())
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<SeqMemoryLocation> allMemoryLocations =
        ImmutableSet.<SeqMemoryLocation>builder()
            .addAll(initialMemoryLocations)
            .addAll(parameterAssignments.values())
            .addAll(startRoutineArgAssignments.keySet())
            .addAll(startRoutineArgAssignments.values())
            .addAll(pointerDereferenceMemoryLocations)
            .build();
    ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocationIds =
        getRelevantMemoryLocationsIds(
            allMemoryLocations,
            pointerAssignments,
            startRoutineArgAssignments,
            pointerParameterAssignments,
            pointerDereferences);

    return new SeqPointerAliasingMap(
        options,
        allMemoryLocations,
        relevantMemoryLocationIds,
        pointerAssignments,
        startRoutineArgAssignments,
        parameterAssignments,
        pointerParameterAssignments,
        pointerDereferences,
        machineModel);
  }

  // Collection helpers ============================================================================

  private ImmutableMap<SeqMemoryLocation, Integer> getRelevantMemoryLocationsIds(
      ImmutableSet<SeqMemoryLocation> pAllMemoryLocations,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    ImmutableMap.Builder<SeqMemoryLocation, Integer> rRelevantIds = ImmutableMap.builder();
    int currentId = INITIAL_MEMORY_LOCATION_ID;
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
    if (pMemoryLocation.declaration() instanceof CVariableDeclaration variableDeclaration) {
      if (MPORUtil.isConstCpaCheckerTmp(variableDeclaration)) {
        return false;
      }
    }
    // relevant locations are either explicit or implicit (e.g. through pointers) global
    if (pMemoryLocation.isGlobal()
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

    if (pMemoryLocation.isGlobal()) {
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

    return pMemoryLocation.isGlobal() || pStartRoutineArgAssignments.containsValue(pMemoryLocation);
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

    if (SeqPointerAliasingMap.isLeftHandSideInPointerAssignment(
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
            // start_routine functions can only have a single parameter
            CParameterDeclaration parameterDeclaration =
                Iterables.getOnlyElement(functionDeclaration.getParameters());
            rAssignments.put(
                SeqMemoryLocation.of(
                    Optional.of(callContext), parameterDeclaration.asVariableDeclaration()),
                rhsMemoryLocation.orElseThrow());
          }
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  // Parameter Assignments =========================================================================

  private ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> mapParameterAssignments() {
    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rAssignments =
        ImmutableMap.builder();
    for (SubstituteEdge substituteEdge : substituteEdges) {
      // use the substitute edge, so that we use the substituted declarations
      if (substituteEdge.cfaEdge instanceof CFunctionCallEdge) {
        CFAEdgeForThread callContext = substituteEdge.getThreadEdge();
        rAssignments.putAll(buildParameterAssignments(callContext, substituteEdge));
      }
    }
    return rAssignments.buildOrThrow();
  }

  private ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> buildParameterAssignments(
      CFAEdgeForThread pCallContext, SubstituteEdge pSubstituteEdge) {

    checkArgument(
        pSubstituteEdge.cfaEdge instanceof CFunctionCallEdge,
        "pSubstituteEdge.cfaEdge must be CFunctionCallEdge.");

    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rAssignments =
        ImmutableMap.builder();

    CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pSubstituteEdge.cfaEdge;
    CFunctionDeclaration functionDeclaration =
        functionCallEdge.getFunctionCallExpression().getDeclaration();
    ImmutableList<CExpression> arguments = functionCallEdge.getArguments();

    for (int i = 0; i < arguments.size(); i++) {
      // we use both pointer and non-pointer parameters, e.g. 'global_ptr = &non_ptr_param;'
      CExpression argumentExpression = arguments.get(i);
      Optional<SeqMemoryLocation> rhsMemoryLocation =
          extractMemoryLocation(pCallContext, argumentExpression);
      if (rhsMemoryLocation.isPresent()) {
        CParameterDeclaration lhsDeclaration =
            MPORUtil.getParameterDeclarationByIndex(i, functionDeclaration);
        // adjust the argument index, in case the function is variadic
        int variadicArgumentIndex;
        if (i > functionDeclaration.getParameters().size()) {
          checkState(functionDeclaration.getType().takesVarArgs());
          variadicArgumentIndex = i - functionDeclaration.getParameters().size();
        } else {
          variadicArgumentIndex = 0;
        }
        SeqMemoryLocation lhsMemoryLocation =
            getParameterMemoryLocation(pCallContext, lhsDeclaration, variadicArgumentIndex);
        rAssignments.put(lhsMemoryLocation, rhsMemoryLocation.orElseThrow());
      }
    }
    return rAssignments.buildOrThrow();
  }

  private SeqMemoryLocation getParameterMemoryLocation(
      CFAEdgeForThread pCallContext,
      CParameterDeclaration pParameterDeclaration,
      int pVariadicArgumentIndex) {

    for (SubstituteEdge substituteEdge : substituteEdges) {
      if (substituteEdge.getCallContext().equals(Optional.of(pCallContext))) {
        if (substituteEdge.parameterSubstitutes.containsKey(pParameterDeclaration)) {
          ImmutableList<CIdExpression> argumentExpressions =
              substituteEdge.parameterSubstitutes.get(pParameterDeclaration);
          CIdExpression idExpression = argumentExpressions.get(pVariadicArgumentIndex);
          return SeqMemoryLocation.of(
              Optional.of(pCallContext),
              MPORUtil.convertToVariableDeclaration(idExpression.getDeclaration()));
        }
      }
    }
    // this should never occur, even if a parameter is declared but never used inside a function
    throw new IllegalArgumentException(
        "Could not find memory location for the given pCallContext, pParameterDeclaration and"
            + " pVariadicArgumentIndex.");
  }

  // Memory Location Extraction

  private Optional<SeqMemoryLocation> extractMemoryLocation(
      CFAEdgeForThread pCallContext, CExpression pRightHandSide) {

    return switch (pRightHandSide) {
      case CIdExpression idExpression ->
          Optional.of(
              getMemoryLocationByDeclaration(
                  pCallContext,
                  MPORUtil.convertToVariableDeclaration(idExpression.getDeclaration())));
      case CFieldReference fieldReference ->
          Optional.of(extractFieldReferenceMemoryLocation(pCallContext, fieldReference));
      case CUnaryExpression unaryExpression ->
          extractMemoryLocation(pCallContext, unaryExpression.getOperand());
      case CCastExpression castExpression ->
          extractMemoryLocation(pCallContext, castExpression.getOperand());
      // can e.g. occur with 'param = 4' i.e. literal integer expressions
      default -> Optional.empty();
    };
  }

  private SeqMemoryLocation extractFieldReferenceMemoryLocation(
      CFAEdgeForThread pCallContext, CFieldReference pFieldReference) {

    CSimpleDeclaration fieldOwner =
        pFieldReference.accept(new CLeftHandSideSimpleDeclarationVisitor());
    CCompositeTypeMemberDeclaration fieldMember =
        SeqPointerAliasingUtil.getCompositeTypeMemberDeclarationByFieldName(
            pFieldReference.getFieldOwner().getExpressionType(), pFieldReference.getFieldName());
    return getMemoryLocationByFieldReference(
        pCallContext, MPORUtil.convertToVariableDeclaration(fieldOwner), fieldMember);
  }

  private SeqMemoryLocation getMemoryLocationByDeclaration(
      CFAEdgeForThread pCallContext, CVariableDeclaration pVariableDeclaration) {

    for (SeqMemoryLocation memoryLocation : initialMemoryLocations) {
      if (memoryLocation.declaration().equals(pVariableDeclaration)) {
        return memoryLocation;
      }
    }
    return SeqMemoryLocation.of(Optional.of(pCallContext), pVariableDeclaration);
  }

  private SeqMemoryLocation getMemoryLocationByFieldReference(
      CFAEdgeForThread pCallContext,
      CVariableDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember) {

    for (SeqMemoryLocation memoryLocation : initialMemoryLocations) {
      if (memoryLocation.fieldMember().isPresent()) {
        CCompositeTypeMemberDeclaration fieldMember = memoryLocation.fieldMember().orElseThrow();
        if (memoryLocation.declaration().equals(pFieldOwner) && fieldMember.equals(pFieldMember)) {
          return memoryLocation;
        }
      }
    }
    return SeqMemoryLocation.of(Optional.of(pCallContext), pFieldOwner, Optional.of(pFieldMember));
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
