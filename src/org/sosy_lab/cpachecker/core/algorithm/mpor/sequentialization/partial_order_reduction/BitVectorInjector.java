// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBinaryBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorInjector<T> {

  public static final int MIN_BIT_VECTOR_LENGTH = 8;
  public static final int MAX_BIT_VECTOR_LENGTH = 64;

  // TODO create option for binary / hex / scalar bit vector
  protected static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> inject(
      // ImmutableList.Builder<CIdExpression> pUpdatedVariables, // TODO needed?
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {
    return pCaseClauses; // TODO
  }

  // TODO maybe use lists, so that the same input always results in the same output (ordering)
  private static ImmutableSet<CVariableDeclaration> getAllGlobalVariables(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableSet.Builder<CVariableDeclaration> rVariables = ImmutableSet.builder();
    for (var entry : pCaseClauses.entrySet()) {
      for (SeqCaseClause caseClause : entry.getValue()) {
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
            for (CVariableDeclaration variable : substituteEdge.globalVariables) {
              assert variable.isGlobal();
              rVariables.add(variable);
            }
          }
        }
      }
    }
    return rVariables.build();
  }

  private static ImmutableMap<CVariableDeclaration, Integer> assignGlobalVariableIds(
      ImmutableSet<CVariableDeclaration> pGlobalVariables) {
    ImmutableMap.Builder<CVariableDeclaration, Integer> rIds = ImmutableMap.builder();
    int id = 0;
    for (CVariableDeclaration variable : pGlobalVariables) {
      assert variable.isGlobal();
      rIds.put(variable, id++);
    }
    return rIds.buildOrThrow();
  }

  private static <T> SeqBitVector createBitVector(
      @NonNull ImmutableMap<T, Integer> pIndices, @NonNull ImmutableSet<T> pVariables) {

    // TODO test - concurrent programs without global variables?
    checkArgument(!pIndices.isEmpty(), "no global variable found");

    final int length = getBitVectorLength(pIndices);
    final ImmutableSet<Integer> setBits =
        pIndices.entrySet().stream()
            .filter(entry -> pVariables.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(ImmutableSet.toImmutableSet());
    return new SeqBinaryBitVector(length, setBits);
  }

  private static <T> int getBitVectorLength(ImmutableMap<T, Integer> pIndices) {
    checkArgument(
        pIndices.size() <= MAX_BIT_VECTOR_LENGTH,
        "cannot have more than "
            + MAX_BIT_VECTOR_LENGTH
            + " global variables, please disable bit vectors for this program.");
    int rLength = MIN_BIT_VECTOR_LENGTH;
    while (rLength < pIndices.size()) {
      rLength *= 2;
    }
    return rLength;
  }
}
