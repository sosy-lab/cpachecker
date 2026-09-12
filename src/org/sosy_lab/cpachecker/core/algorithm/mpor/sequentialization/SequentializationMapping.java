// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Information that relates elements of a sequentialized program to the concurrent input program it
 * was created from.
 *
 * <p>The sequentialization is created as C code and parsed again, so only names survive that
 * transformation unchanged. Everything here is therefore keyed by a name of the output program and
 * holds the elements of the input program themselves, which is what keeps their source locations.
 *
 * @param originalDeclarationsBySubstituteName Maps the name of a variable of the sequentialization
 *     to the declaration of the input program variable it substitutes. Variables of the
 *     sequentialization that have no counterpart in the input program, e.g. program counters, are
 *     absent.
 * @param blockOriginsByLabel Maps the name of the label that precedes a block of statements in the
 *     sequentialization to the input program elements the block simulates.
 * @param threadIdByCreationEdge Maps the {@link CFAEdge} of the input program that creates a
 *     thread, e.g. a {@code pthread_create} call, to the ID of the created thread. The main thread
 *     is created by no edge and hence absent, and so is an edge that creates several threads,
 *     because which of them an execution of the edge creates cannot be told apart.
 */
public record SequentializationMapping(
    ImmutableMap<String, CSimpleDeclaration> originalDeclarationsBySubstituteName,
    ImmutableMap<String, BlockOrigin> blockOriginsByLabel,
    ImmutableMap<CFAEdge, Integer> threadIdByCreationEdge) {

  /**
   * The input program elements that one block of statements of the sequentialization simulates.
   *
   * @param threadId The ID of the thread that executes the block.
   * @param originalEdgeByStatement For each statement of the block, the {@link CFAEdge} of the
   *     input program it simulates. A block with two statements simulates the two assume edges of a
   *     branching.
   */
  public record BlockOrigin(int threadId, ImmutableList<CFAEdge> originalEdgeByStatement) {}
}
