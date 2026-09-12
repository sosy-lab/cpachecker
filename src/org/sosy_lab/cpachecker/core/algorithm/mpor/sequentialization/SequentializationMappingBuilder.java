// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table.Cell;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationMapping.BlockOrigin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.LocalVariableDeclarationSubstitute;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
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
  private static ImmutableMap<String, CSimpleDeclaration> buildOriginalDeclarations(
      SequentializationFields pFields) {

    Map<String, CSimpleDeclaration> rDeclarations = new LinkedHashMap<>();
    Set<String> ambiguous = new LinkedHashSet<>();
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
      Map<String, CSimpleDeclaration> pDeclarations,
      Set<String> pAmbiguous,
      CIdExpression pSubstitute,
      CSimpleDeclaration pOriginal) {

    CSimpleDeclaration substituteDeclaration = pSubstitute.getDeclaration();
    if (substituteDeclaration == null) {
      return;
    }
    String substituteName = substituteDeclaration.getName();
    CSimpleDeclaration existing = pDeclarations.get(substituteName);
    if (existing != null && !existing.getName().equals(pOriginal.getName())) {
      pAmbiguous.add(substituteName);
    }
    pDeclarations.put(substituteName, pOriginal);
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
        ImmutableList<SeqThreadStatement> statements = block.getStatements();
        if (statements.stream().allMatch(statement -> statement.originEdge().isPresent())) {
          rBlockOrigins.put(
              SeqNameUtil.buildThreadStatementBlockLabelName(threadId, block.getLabelNumber()),
              new BlockOrigin(
                  threadId,
                  transformedImmutableListCopy(
                      statements, statement -> statement.originEdge().orElseThrow())));
        }
      }
    }
    return rBlockOrigins.buildOrThrow();
  }

  /**
   * An edge that creates several threads, because it is executed in several call contexts, is left
   * out: which thread an execution of it creates cannot be told apart afterwards.
   */
  private static ImmutableMap<CFAEdge, Integer> buildThreadIdByCreationEdge(
      SequentializationFields pFields) {

    Map<CFAEdge, Integer> rCreationEdges = new LinkedHashMap<>();
    Set<CFAEdge> ambiguous = new LinkedHashSet<>();
    for (MPORThread thread : pFields.threads) {
      thread
          .startRoutineCall()
          .ifPresent(
              threadEdge -> {
                if (rCreationEdges.put(threadEdge.cfaEdge, thread.id()) != null) {
                  ambiguous.add(threadEdge.cfaEdge);
                }
              });
    }
    rCreationEdges.keySet().removeAll(ambiguous);
    return ImmutableMap.copyOf(rCreationEdges);
  }
}
