// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements.SeqFunctionStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record SeqPointerAliasingMapBuilder(
    MPOROptions options,
    ImmutableCollection<SubstituteEdge> substituteEdges,
    ImmutableCollection<SeqFunctionStatements> functionStatements,
    CFA inputCfa,
    MachineModel machineModel) {

  private static final int INITIAL_MEMORY_LOCATION_ID = 0;

  public SeqPointerAliasingMap buildPointerAliasingMap() throws UnsupportedCodeException {
    ImmutableSet<SeqPointerAssignment> pointerAssignments =
        substituteEdges.stream()
            .flatMap(edge -> edge.getPointerAssignments().stream())
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<SeqPointerAssignment> pointerParameterAssignments =
        mapAssignmentsFromFunctionStatements(
            functionStatements.stream()
                .flatMap(s -> s.parameterAssignments().values().stream())
                .collect(ImmutableSet.toImmutableSet()),
            inputCfa,
            SeqPointerAssignmentType.PARAMETER);
    ImmutableSet<SeqPointerAssignment> pointerReturnValueAssignments =
        mapAssignmentsFromFunctionStatements(
            functionStatements.stream()
                .flatMap(s -> s.returnValueAssignments().values().stream())
                .collect(ImmutableSet.toImmutableSet()),
            inputCfa,
            SeqPointerAssignmentType.RETURN_VALUE);

    ImmutableSet<SeqPointerAssignment> startRoutineArgAssignments =
        mapAssignmentsFromFunctionStatements(
            functionStatements.stream()
                .flatMap(s -> s.startRoutineArgAssignments().values().stream())
                .collect(ImmutableSet.toImmutableSet()),
            inputCfa,
            SeqPointerAssignmentType.START_ROUTINE_ARG);
    ImmutableSet<SeqPointerAssignment> startRoutineExitAssignments =
        mapAssignmentsFromFunctionStatements(
            functionStatements.stream()
                .flatMap(s -> s.startRoutineExitAssignments().values().stream())
                .collect(ImmutableSet.toImmutableSet()),
            inputCfa,
            SeqPointerAssignmentType.START_ROUTINE_EXIT);

    ImmutableSet<SeqPointerAssignment> allPointerAssignments =
        ImmutableSet.<SeqPointerAssignment>builder()
            .addAll(pointerAssignments)
            .addAll(pointerParameterAssignments)
            .addAll(pointerReturnValueAssignments)
            .addAll(startRoutineArgAssignments)
            .addAll(startRoutineExitAssignments)
            .build();

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
            // add memory locations that are accessed directly in the edges
            .addAll(
                substituteEdges.stream()
                    .flatMap(
                        substituteEdge ->
                            substituteEdge
                                .getMemoryLocationsByAccessType(SeqMemoryAccessType.ACCESS)
                                .stream())
                    .collect(ImmutableSet.toImmutableSet()))
            .addAll(
                pointerAssignments.stream()
                    .flatMap(a -> a.getAllMemoryLocations().stream())
                    .collect(ImmutableSet.toImmutableSet()))
            // for pointer parameter assignments we only need the right-hand side since that is
            // an actual memory location. the left-hand side is not allocated
            .addAll(
                pointerParameterAssignments.stream()
                    .map(SeqPointerAssignment::rightHandSideMemoryLocation)
                    .collect(ImmutableSet.toImmutableSet()))
            .addAll(
                pointerReturnValueAssignments.stream()
                    .flatMap(a -> a.getAllMemoryLocations().stream())
                    .collect(ImmutableSet.toImmutableSet()))
            // for start_routine arg assignments we only need the right-hand side since that is
            // an actual memory location. the left-hand side is not allocated
            .addAll(
                startRoutineArgAssignments.stream()
                    .map(SeqPointerAssignment::rightHandSideMemoryLocation)
                    .collect(ImmutableSet.toImmutableSet()))
            .addAll(
                startRoutineExitAssignments.stream()
                    .flatMap(a -> a.getAllMemoryLocations().stream())
                    .collect(ImmutableSet.toImmutableSet()))
            // add memory locations from pointer dereferences
            .addAll(pointerDereferences)
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

  /**
   * Checks whether {@code pMemoryLocation} is implicitly global through (transitive) pointer
   * assignments and pointer dereferences. Example:
   *
   * <pre>{@code
   * int global = 0;
   * int main() {
   *   int * local_ptr;
   *   local_ptr = & global;
   *   // dereference is now implicitly global through a pointer assignment
   *   *local_ptr = 1;
   * }
   * }</pre>
   *
   * Note that pointer assignments that are performed on function return are excluded. This is
   * because the memory location on the right-hand side is not implicitly global even if the pointer
   * on the left-hand side is global because it is assigned only after the function returns.
   */
  private static boolean isImplicitGlobalByPointerAssignmentsAndDereferences(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences) {

    // check for explicit pointer assignments
    for (SeqPointerAssignment pointerAssignment : pPointerAssignments) {
      if (!pointerAssignment.type().isPerformedOnFunctionReturn()) {
        if (isExplicitGlobalOrStartRoutineArg(
            pointerAssignment.leftHandSideMemoryLocation(), pPointerAssignments)) {
          return true;
        }
      }
    }

    // check if a global pointer dereference is associated with the memory location
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

    // check on transitive pointer assignments
    for (SeqPointerAssignment pointerAssignment : pPointerAssignments) {
      if (!pointerAssignment.type().isPerformedOnFunctionReturn()) {
        Set<SeqMemoryLocation> transitivePointerMemoryLocations = new HashSet<>();
        recursivelyFindPointerDeclarationsByPointerAssignments(
            pointerAssignment.leftHandSideMemoryLocation(),
            pPointerAssignments,
            transitivePointerMemoryLocations,
            new HashSet<>());
        for (SeqMemoryLocation transitivePointerDeclaration : transitivePointerMemoryLocations) {
          if (isExplicitGlobalOrStartRoutineArg(
              transitivePointerDeclaration, pPointerAssignments)) {
            return true;
          }
        }
      }
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

  private static void recursivelyFindPointerDeclarationsByPointerAssignments(
      SeqMemoryLocation pCurrentMemoryLocation,
      final ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      Set<SeqMemoryLocation> pFound,
      Set<SeqMemoryLocation> pVisited) {

    if (SeqPointerAliasingMap.isLeftHandSideInPointerAssignment(
        pCurrentMemoryLocation, pPointerAssignments)) {
      for (SeqPointerAssignment pointerAssignment : pPointerAssignments) {
        if (!pointerAssignment.type().isPerformedOnFunctionReturn()) {
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
  }

  // Function Statement Assignments

  /**
   * Maps the all assignments from the given {@link SeqFunctionStatement}s, including pointer and
   * non-pointer assignments.
   *
   * <p>Note that it is possible that the given {@code pFunctionStatements} can contain the same
   * left-hand side {@link SeqMemoryLocation} in multiple assignments, but only for {@link
   * SeqPointerAssignmentType#RETURN_VALUE}:
   *
   * <pre>{@code
   * int squared(int x) {
   *    return x * x;
   * }
   * int main() {
   *    int result;
   *    result = squared(1);
   *    result = squared(2);
   * }
   * }</pre>
   */
  private ImmutableSet<SeqPointerAssignment> mapAssignmentsFromFunctionStatements(
      ImmutableCollection<SeqFunctionStatement> pFunctionStatements,
      CFA pInputCfa,
      SeqPointerAssignmentType pType)
      throws UnsupportedCodeException {

    checkArgument(
        !pType.equals(SeqPointerAssignmentType.EXPLICIT),
        "pType cannot be EXPLICIT because pointer assignments from function statements are never"
            + " explicit.");

    ImmutableSet.Builder<SeqPointerAssignment> rAssignments = ImmutableSet.builder();

    for (SeqFunctionStatement functionStatement : pFunctionStatements) {
      CLeftHandSide leftHandSide =
          functionStatement.getExpressionAssignmentStatement().getLeftHandSide();
      CExpression rightHandSide =
          functionStatement.getExpressionAssignmentStatement().getRightHandSide();
      Optional<SeqPointerAssignment> pointerAssignment =
          SeqPointerAliasingUtil.tryBuildPointerAssignment(
              leftHandSide,
              rightHandSide,
              functionStatement.getLeftHandSideCallContext(),
              functionStatement.getRightHandSideCallContext(),
              pInputCfa.getAllFunctions(),
              pType);
      if (pointerAssignment.isPresent()) {
        rAssignments.add(pointerAssignment.orElseThrow());
      }
    }

    return rAssignments.build();
  }
}
