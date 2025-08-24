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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
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

  public static ImmutableSet<MemoryLocation> getAllMemoryLocations(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSet.Builder<MemoryLocation> rMemoryLocations = ImmutableSet.builder();
    Set<CVariableDeclaration> visited = new HashSet<>();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      ImmutableSet<CVariableDeclaration> allAccessedVariables =
          getAccessedVariablesBySubstituteEdge(substituteEdge);
      for (CVariableDeclaration variableDeclaration : allAccessedVariables) {
        if (visited.add(variableDeclaration)) {
          rMemoryLocations.add(MemoryLocation.of(variableDeclaration));
          if (variableDeclaration.getType() instanceof CTypedefType typedefType) {
            // for structs, add all field members (including members of inner structs).
            // we assume that each field member is accessed at least once in the input program.
            ImmutableSet<CCompositeTypeMemberDeclaration> allFieldMembers =
                recursivelyFindFieldMembers(typedefType);
            for (CCompositeTypeMemberDeclaration fieldMember : allFieldMembers) {
              rMemoryLocations.add(MemoryLocation.of(variableDeclaration, fieldMember));
            }
          }
        }
      }
    }
    return rMemoryLocations.build();
  }

  private static ImmutableSet<CVariableDeclaration> getAccessedVariablesBySubstituteEdge(
      SubstituteEdge pSubstituteEdge) {

    return ImmutableSet.<CVariableDeclaration>builder()
        .addAll(pSubstituteEdge.accessedGlobalVariables)
        .addAll(pSubstituteEdge.accessedFieldMembers.keySet())
        .build();
  }

  private static ImmutableSet<CCompositeTypeMemberDeclaration> recursivelyFindFieldMembers(
      CTypedefType pTypedefType) {

    Set<CCompositeTypeMemberDeclaration> rFound = new HashSet<>();
    recursivelyFindFieldMembers(pTypedefType, rFound);
    return ImmutableSet.copyOf(rFound);
  }

  private static void recursivelyFindFieldMembers(
      CTypedefType pTypedefType, Set<CCompositeTypeMemberDeclaration> pFound) {

    // elaborated type is e.g. struct __anon_type_QType
    if (pTypedefType.getRealType() instanceof CElaboratedType elaboratedType) {
      // composite type contains the composite type members, e.g. 'amount'
      if (elaboratedType.getRealType() instanceof CCompositeType compositeType) {
        pFound.addAll(compositeType.getMembers());
        for (CCompositeTypeMemberDeclaration fieldMember : compositeType.getMembers()) {
          if (fieldMember.getType() instanceof CTypedefType typedefType) {
            recursivelyFindFieldMembers(typedefType, pFound);
          }
        }
      }
    }
  }

  // Pointer Assignments ===========================================================================

  /**
   * Maps pointers {@code ptr} to the memory locations e.g. {@code &var} assigned to them based on
   * {@code pSubstituteEdges}, including both global and local memory locations.
   */
  public static ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration>
      mapPointerAssignments(ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSetMultimap.Builder<CVariableDeclaration, CSimpleDeclaration> rPointerAssignments =
        ImmutableSetMultimap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      if (!substituteEdge.pointerAssignment.isEmpty()) {
        assert substituteEdge.pointerAssignment.size() == 1
            : "a single edge can have at most 1 pointer assignments";
        Map.Entry<CVariableDeclaration, CSimpleDeclaration> singleEntry =
            substituteEdge.pointerAssignment.entrySet().iterator().next();
        rPointerAssignments.put(singleEntry.getKey(), singleEntry.getValue());
      }
    }
    return rPointerAssignments.build();
  }

  // Pointer Parameter Assignments =================================================================

  public static ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
      mapParameterAssignments(ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableTable.Builder<ThreadEdge, CParameterDeclaration, CSimpleDeclaration> rAssignments =
        ImmutableTable.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      // use the original edge, so that we use the original variable declarations
      if (substituteEdge.getOriginalEdge() instanceof CFunctionCallEdge functionCallEdge) {
        // the function call edge is used as the call context, not the actual call context
        ThreadEdge callContext = substituteEdge.getThreadEdge();
        ImmutableList<Cell<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>> assignments =
            buildParameterAssignments(callContext, functionCallEdge);
        for (Cell<ThreadEdge, CParameterDeclaration, CSimpleDeclaration> cell : assignments) {
          rAssignments.put(cell);
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  private static ImmutableList<Cell<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>>
      buildParameterAssignments(ThreadEdge pCallContext, CFunctionCallEdge pFunctionCallEdge) {

    ImmutableList.Builder<Cell<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>>
        rAssignments = ImmutableList.builder();
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
        Optional<CSimpleDeclaration> rightHandSide = extractSimpleDeclaration(arguments.get(i));
        if (rightHandSide.isPresent()) {
          rAssignments.add(
              Tables.immutableCell(pCallContext, leftHandSide, rightHandSide.orElseThrow()));
        }
      }
    }
    return rAssignments.build();
  }

  public static Optional<CSimpleDeclaration> extractSimpleDeclaration(CExpression pRightHandSide) {
    if (pRightHandSide instanceof CIdExpression idExpression) {
      return Optional.of(idExpression.getDeclaration());

    } else if (pRightHandSide instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
        return Optional.of(idExpression.getDeclaration());

      } else if (unaryExpression.getOperand() instanceof CFieldReference fieldReference) {
        CIdExpression fieldOwner = MPORUtil.recursivelyFindFieldOwner(fieldReference);
        return Optional.of(fieldOwner.getDeclaration());
      }

    } else if (pRightHandSide instanceof CFieldReference fieldReference) {
      CIdExpression fieldOwner = MPORUtil.recursivelyFindFieldOwner(fieldReference);
      return Optional.of(fieldOwner.getDeclaration());
    }
    // can e.g. occur with 'param = 4' i.e. literal integer expressions
    return Optional.empty();
  }

  // Pointer Field Member Assignments ==============================================================

  public static ImmutableTable<
          CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      mapFieldMemberAssignments(ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableTable.Builder<
            CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
        rAssignments = ImmutableTable.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rAssignments.putAll(substituteEdge.pointerFieldMemberAssignments);
    }
    return rAssignments.build();
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
