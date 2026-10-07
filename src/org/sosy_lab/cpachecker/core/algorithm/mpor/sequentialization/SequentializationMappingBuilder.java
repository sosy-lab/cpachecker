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
import com.google.common.collect.Table.Cell;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationMapping.BlockOrigin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.LocalVariableDeclarationSubstitute;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.SeqCallContext;

/** Builds the {@link SequentializationMapping} back to the input program of a sequentialization. */
public class SequentializationMappingBuilder {

  public static SequentializationMapping buildMapping(SequentializationFields pFields) {

    return new SequentializationMapping(
        buildOriginalDeclarations(pFields),
        buildBlockOrigins(pFields),
        buildThreadIdByCreationEdge(pFields));
  }

  /** Substitutes that stand for input program variables with different names are left out. */
  private static ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> buildOriginalDeclarations(
      SequentializationFields pFields) {

    Map<CSimpleDeclaration, CSimpleDeclaration> rDeclarations = new LinkedHashMap<>();
    Set<CSimpleDeclaration> ambiguous = new LinkedHashSet<>();
    for (MPORSubstitution substitution : pFields.substitutions) {
      for (Entry<CVariableDeclaration, CIdExpression> entry :
          substitution.getGlobalVariableSubstitutes()) {
        put(rDeclarations, ambiguous, entry.getValue(), entry.getKey());
      }
      for (Cell<SeqCallContext, CVariableDeclaration, LocalVariableDeclarationSubstitute> cell :
          substitution.getLocalVariableSubstituteTable().cellSet()) {
        put(rDeclarations, ambiguous, cell.getValue().idExpression(), cell.getColumnKey());
      }
      for (Cell<SeqCallContext, CParameterDeclaration, ImmutableList<CIdExpression>> cell :
          substitution.getParameterSubstitutes().cellSet()) {
        for (CIdExpression idExpression : cell.getValue()) {
          put(rDeclarations, ambiguous, idExpression, cell.getColumnKey());
        }
      }
      for (Entry<CParameterDeclaration, CIdExpression> entry :
          substitution.getMainFunctionArgSubstitutes().entrySet()) {
        put(rDeclarations, ambiguous, entry.getValue(), entry.getKey());
      }
      for (Cell<SeqCallContext, CParameterDeclaration, CIdExpression> cell :
          substitution.getStartRoutineArgSubstitutes().cellSet()) {
        put(rDeclarations, ambiguous, cell.getValue(), cell.getColumnKey());
      }
    }
    rDeclarations.keySet().removeAll(ambiguous);
    return ImmutableMap.copyOf(rDeclarations);
  }

  /**
   * Records that {@code pSubstitute} stands for {@code pOriginal}. A variable of the input program
   * can have several substitutes, e.g. one per call context, and several declarations, e.g. {@code
   * int x; int x = 1;}, which all agree on its name. Substitutes that would disagree are collected
   * in {@code pAmbiguous} instead.
   */
  private static void put(
      Map<CSimpleDeclaration, CSimpleDeclaration> pDeclarations,
      Set<CSimpleDeclaration> pAmbiguous,
      CIdExpression pSubstitute,
      CSimpleDeclaration pOriginal) {

    CSimpleDeclaration substituteDeclaration = Objects.requireNonNull(pSubstitute.getDeclaration());
    CSimpleDeclaration existing = pDeclarations.get(substituteDeclaration);
    if (existing != null && !existing.getName().equals(pOriginal.getName())) {
      pAmbiguous.add(substituteDeclaration);
    }
    pDeclarations.put(substituteDeclaration, pOriginal);
  }

  /**
   * Maps the label of every block of statements in the output program to the input program elements
   * that the block simulates. Blocks that simulate no input program edge are absent.
   */
  private static ImmutableMap<String, BlockOrigin> buildBlockOrigins(
      SequentializationFields pFields) {

    ImmutableMap.Builder<String, BlockOrigin> rBlockOrigins = ImmutableMap.builder();
    for (Entry<MPORThread, SeqThreadStatementClause> entry : pFields.clauses.entries()) {
      int threadId = entry.getKey().id();
      for (SeqThreadStatementBlock block : entry.getValue().getBlocks()) {
        ImmutableList.Builder<CFAEdgeForThread> originalEdges = ImmutableList.builder();
        for (SeqThreadStatement statement : block.getStatements()) {
          // Use the first CFAEdgeForThread of the input program that this statement simulates.
          // A statement that merges several edges is represented by the first one. It is possible
          // that there is no edge at all (declarations of ghost variables), hence optional.
          Optional<CFAEdgeForThread> firstEdge =
              statement.data().getSubstituteEdges().stream()
                  .map(SubstituteEdge::getThreadEdge)
                  .filter(edge -> edge.cfaEdge.getFileLocation().isRealLocation())
                  .findFirst();
          if (firstEdge.isPresent()) {
            originalEdges.add(firstEdge.orElseThrow());
          }
        }
        rBlockOrigins.put(
            SeqNameUtil.buildThreadStatementBlockLabelName(threadId, block.getLabelNumber()),
            new BlockOrigin(threadId, originalEdges.build()));
      }
    }
    return rBlockOrigins.buildOrThrow();
  }

  /**
   * Maps {@link CFAEdgeForThread} to the unique thread id they create. Note that {@link
   * CFAEdgeForThread} considers the call context in which the thread is created.
   */
  private static ImmutableMap<CFAEdgeForThread, Integer> buildThreadIdByCreationEdge(
      SequentializationFields pFields) {

    ImmutableMap.Builder<CFAEdgeForThread, Integer> rCreationEdges = ImmutableMap.builder();
    for (MPORThread thread : pFields.threads) {
      thread
          .startRoutineCall()
          .ifPresent(
              threadEdge -> {
                rCreationEdges.put(threadEdge, thread.id());
              });
    }
    return rCreationEdges.buildOrThrow();
  }
}
