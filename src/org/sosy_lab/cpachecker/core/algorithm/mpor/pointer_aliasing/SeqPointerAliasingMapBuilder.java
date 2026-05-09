// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements.SeqFunctionStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record SeqPointerAliasingMapBuilder(
    MPOROptions options,
    ImmutableList<SeqMemoryLocation> initialMemoryLocations,
    ImmutableCollection<SubstituteEdge> substituteEdges,
    ImmutableCollection<SeqFunctionStatements> functionStatements,
    MachineModel machineModel) {

  private static final int INITIAL_MEMORY_LOCATION_ID = 0;

  public SeqPointerAliasingMap buildPointerAliasingMap() throws UnsupportedCodeException {
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        substituteEdges.stream()
            .flatMap(edge -> edge.pointerAssignments.asMultimap().entries().stream())
            .collect(ImmutableSetMultimap.toImmutableSetMultimap(Entry::getKey, Entry::getValue));

    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> parameterAssignments =
        mapAssignmentsFromFunctionStatements(
            functionStatements.stream()
                .flatMap(s -> s.parameterAssignments().values().stream())
                .collect(ImmutableSet.toImmutableSet()));
    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerParameterAssignments =
        extractPointerAssignments(parameterAssignments);

    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> returnValueAssignments =
        mapAssignmentsFromFunctionStatements(
            functionStatements.stream()
                .flatMap(s -> s.returnValueAssignments().values().stream())
                .collect(ImmutableSet.toImmutableSet()));
    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerReturnValueAssignments =
        extractPointerAssignments(returnValueAssignments);

    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> startRoutineArgAssignments =
        mapAssignmentsFromFunctionStatements(
            functionStatements.stream()
                .flatMap(s -> s.startRoutineArgAssignments().values().stream())
                .collect(ImmutableSet.toImmutableSet()));
    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> startRoutineExitAssignments =
        mapAssignmentsFromFunctionStatements(
            functionStatements.stream()
                .flatMap(s -> s.startRoutineExitAssignments().values().stream())
                .collect(ImmutableSet.toImmutableSet()));

    ImmutableSet<SeqPointerAssignment> allPointerAssignments =
        getAllPointerAssignments(
            pointerAssignments,
            pointerParameterAssignments,
            pointerReturnValueAssignments,
            startRoutineArgAssignments,
            startRoutineExitAssignments);

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
                        d, allPointerAssignments)
                        .stream())
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<SeqMemoryLocation> allMemoryLocations =
        ImmutableSet.<SeqMemoryLocation>builder()
            .addAll(initialMemoryLocations)
            .addAll(parameterAssignments.values())
            .addAll(returnValueAssignments.values())
            .addAll(startRoutineArgAssignments.keySet())
            .addAll(startRoutineArgAssignments.values())
            .addAll(startRoutineExitAssignments.keySet())
            .addAll(startRoutineExitAssignments.values())
            .addAll(pointerDereferenceMemoryLocations)
            .build();
    ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocationIds =
        getRelevantMemoryLocationsIds(
            allMemoryLocations, allPointerAssignments, pointerDereferences);

    return new SeqPointerAliasingMap(
        options,
        allMemoryLocations,
        relevantMemoryLocationIds,
        allPointerAssignments,
        pointerDereferences,
        machineModel);
  }

  // Relevant (= Explicit or Implicit Global) Memory Location Extraction

  private ImmutableMap<SeqMemoryLocation, Integer> getRelevantMemoryLocationsIds(
      ImmutableSet<SeqMemoryLocation> pAllMemoryLocations,
      ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    ImmutableMap.Builder<SeqMemoryLocation, Integer> rRelevantIds = ImmutableMap.builder();
    int currentId = INITIAL_MEMORY_LOCATION_ID;
    for (SeqMemoryLocation memoryLocation : pAllMemoryLocations) {
      if (isRelevantMemoryLocation(memoryLocation, pPointerAssignments, pPointerDereferences)) {
        rRelevantIds.put(memoryLocation, currentId++);
      }
    }
    return rRelevantIds.buildOrThrow();
  }

  private boolean isRelevantMemoryLocation(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    // exclude const CPAchecker_TMP, they do not have any effect in the input program
    if (pMemoryLocation.declaration() instanceof CVariableDeclaration variableDeclaration) {
      if (MPORUtil.isConstCpaCheckerTmp(variableDeclaration)) {
        return false;
      }
    }
    // relevant locations are either explicit or implicit (e.g. through pointers) global
    if (pMemoryLocation.isGlobal()
        || isImplicitGlobal(pMemoryLocation, pPointerAssignments, pPointerDereferences)) {
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
      ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    if (pMemoryLocation.isGlobal()) {
      return false;
    }
    // if any pointer points to the memory location: check all pointer assignments and derefs
    if (pPointerAssignments.stream()
        .anyMatch(a -> a.rightHandSideMemoryLocation().equals(pMemoryLocation))) {
      if (isImplicitGlobalByPointerAssignmentsAndDereferences(
          pMemoryLocation, pPointerAssignments, pPointerDereferences)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isImplicitGlobalByPointerAssignmentsAndDereferences(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    // inexpensive shortcut: first check for direct assignments
    if (isImplicitGlobalByDirectPointerAssignments(pPointerAssignments)) {
      return true;
    }
    // then check if a global pointer deref is associated with the memory location
    if (isImplicitGlobalByPointerDereference(
        pMemoryLocation, pPointerAssignments, pPointerDereferences)) {
      return true;
    }
    // lastly perform most expensive check on transitive pointer assignments
    if (isImplicitGlobalByTransitivePointerAssignments(pPointerAssignments)) {
      return true;
    }
    return false;
  }

  private static boolean isExplicitGlobalOrStartRoutineArg(
      SeqMemoryLocation pMemoryLocation, ImmutableSet<SeqPointerAssignment> pPointerAssignments) {

    return pMemoryLocation.isGlobal()
        || pPointerAssignments.stream()
            .filter(a -> a.type().equals(SeqPointerAssignmentType.START_ROUTINE_ARG))
            .anyMatch(a -> a.leftHandSideMemoryLocation().equals(pMemoryLocation));
  }

  private static boolean isImplicitGlobalByPointerDereference(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    for (SeqMemoryLocation pointerDereference : pPointerDereferences) {
      if (pointerDereference.equals(pMemoryLocation)) {
        ImmutableSet<SeqMemoryLocation> memoryLocations =
            SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
                pointerDereference, pPointerAssignments);
        for (SeqMemoryLocation memoryLocation : memoryLocations) {
          if (isExplicitGlobalOrStartRoutineArg(memoryLocation, pPointerAssignments)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean isImplicitGlobalByDirectPointerAssignments(
      ImmutableSet<SeqPointerAssignment> pPointerAssignments) {

    for (SeqPointerAssignment pointerAssignment : pPointerAssignments) {
      if (isExplicitGlobalOrStartRoutineArg(
          pointerAssignment.leftHandSideMemoryLocation(), pPointerAssignments)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isImplicitGlobalByTransitivePointerAssignments(
      ImmutableSet<SeqPointerAssignment> pPointerAssignments) {

    for (SeqPointerAssignment pointerAssignment : pPointerAssignments) {
      ImmutableSet<SeqMemoryLocation> transitivePointerDeclarations =
          findPointerDeclarationsByPointerAssignments(
              pointerAssignment.leftHandSideMemoryLocation(), pPointerAssignments);
      for (SeqMemoryLocation transitivePointerDeclaration : transitivePointerDeclarations) {
        if (isExplicitGlobalOrStartRoutineArg(transitivePointerDeclaration, pPointerAssignments)) {
          return true;
        }
      }
    }
    return false;
  }

  private static ImmutableSet<SeqMemoryLocation> findPointerDeclarationsByPointerAssignments(
      SeqMemoryLocation pPointerDeclaration,
      ImmutableSet<SeqPointerAssignment> pPointerAssignments) {

    Set<SeqMemoryLocation> rFound = new HashSet<>();
    recursivelyFindPointerDeclarationsByPointerAssignments(
        pPointerDeclaration, pPointerAssignments, rFound, new HashSet<>());
    return ImmutableSet.copyOf(rFound);
  }

  private static void recursivelyFindPointerDeclarationsByPointerAssignments(
      SeqMemoryLocation pCurrentMemoryLocation,
      final ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      Set<SeqMemoryLocation> pFound,
      Set<SeqMemoryLocation> pVisited) {

    if (SeqPointerAliasingMap.isLeftHandSideInPointerAssignment(
        pCurrentMemoryLocation, pPointerAssignments)) {
      for (SeqPointerAssignment pointerAssignment : pPointerAssignments) {
        if (pVisited.add(pointerAssignment.leftHandSideMemoryLocation())) {
          pFound.add(pointerAssignment.leftHandSideMemoryLocation());
          recursivelyFindPointerDeclarationsByPointerAssignments(
              pointerAssignment.rightHandSideMemoryLocation(),
              pPointerAssignments,
              pFound,
              pVisited);
        }
      }
    }
  }

  // Function Statement Assignments

  private ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> mapAssignmentsFromFunctionStatements(
      ImmutableCollection<SeqFunctionStatement> pFunctionStatements)
      throws UnsupportedCodeException {

    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rAssignments =
        ImmutableMap.builder();

    for (SeqFunctionStatement functionStatement : pFunctionStatements) {
      CLeftHandSide leftHandSide =
          functionStatement.getExpressionAssignmentStatement().getLeftHandSide();
      CExpression rightHandSide =
          functionStatement.getExpressionAssignmentStatement().getRightHandSide();
      Optional<Map.Entry<SeqMemoryLocation, SeqMemoryLocation>> pointerAssignment =
          SeqPointerAliasingUtil.tryMapPointerAssignment(
              leftHandSide, rightHandSide, functionStatement.getCallContext());
      if (pointerAssignment.isPresent()) {
        rAssignments.put(
            pointerAssignment.orElseThrow().getKey(), pointerAssignment.orElseThrow().getValue());
      }
    }

    return rAssignments.buildOrThrow();
  }

  // Helper

  @VisibleForTesting
  static ImmutableSet<SeqPointerAssignment> getAllPointerAssignments(
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pParameterAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pReturnValueAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineExitAssignments) {

    ImmutableSet.Builder<SeqPointerAssignment> pointerAssignments = ImmutableSet.builder();

    pPointerAssignments
        .entries()
        .forEach(
            a ->
                pointerAssignments.add(
                    new SeqPointerAssignment(
                        SeqPointerAssignmentType.EXPLICIT, a.getKey(), a.getValue())));
    pParameterAssignments.forEach(
        (key, value) ->
            pointerAssignments.add(
                new SeqPointerAssignment(SeqPointerAssignmentType.PARAMETER, key, value)));
    pReturnValueAssignments.forEach(
        (key, value) ->
            pointerAssignments.add(
                new SeqPointerAssignment(SeqPointerAssignmentType.RETURN_VALUE, key, value)));
    pStartRoutineArgAssignments.forEach(
        (key, value) ->
            pointerAssignments.add(
                new SeqPointerAssignment(SeqPointerAssignmentType.START_ROUTINE_ARG, key, value)));
    pStartRoutineExitAssignments.forEach(
        (key, value) ->
            pointerAssignments.add(
                new SeqPointerAssignment(SeqPointerAssignmentType.START_ROUTINE_EXIT, key, value)));

    return pointerAssignments.build();
  }

  @VisibleForTesting
  static ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> extractPointerAssignments(
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pAssignments) {

    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rPointerAssignments =
        ImmutableMap.builder();
    for (var entry : pAssignments.entrySet()) {
      if (entry.getKey().declaration().getType() instanceof CPointerType) {
        rPointerAssignments.put(entry);
      }
    }
    return rPointerAssignments.buildOrThrow();
  }
}
