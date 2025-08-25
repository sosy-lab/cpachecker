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
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryModelBuilder {

  public static Optional<MemoryModel> tryBuildMemoryModel(
      MPOROptions pOptions,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    if (pOptions.linkReduction) {
      return Optional.of(
          new MemoryModel(
              assignMemoryLocationIds(pAllMemoryLocations),
              mapAllPointerAssignments(pSubstituteEdges),
              mapPointerParameterAssignments(pSubstituteEdges)));
    } else {
      return Optional.empty();
    }
  }

  private static ImmutableMap<MemoryLocation, Integer> assignMemoryLocationIds(
      ImmutableSet<MemoryLocation> pAllMemoryLocations) {

    ImmutableMap.Builder<MemoryLocation, Integer> rVariables = ImmutableMap.builder();
    int id = BitVectorUtil.RIGHT_INDEX;
    for (MemoryLocation memoryLocation : pAllMemoryLocations) {
      rVariables.put(memoryLocation, id++);
    }
    return rVariables.buildOrThrow();
  }

  // Pointer Assignments ===========================================================================

  private static ImmutableSetMultimap<CVariableDeclaration, MemoryLocation>
      mapAllPointerAssignments(ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSetMultimap.Builder<CVariableDeclaration, MemoryLocation> rAllAssignments =
        ImmutableSetMultimap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rAllAssignments.putAll(substituteEdge.pointerAssignments.asMultimap());
    }
    return rAllAssignments.build();
  }

  // Pointer Parameter Assignments =================================================================

  private static ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
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

  private static MemoryLocation extractMemoryLocation(CExpression pRightHandSide) {
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
    throw new IllegalArgumentException("pFieldReference owner type must be CTypedefType");
  }
}
