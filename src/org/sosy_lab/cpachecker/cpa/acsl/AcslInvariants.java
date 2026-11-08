// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Collection;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssumes;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslBehavior;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslEnsures;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslRequires;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.acsl.AcslMetadata;

/** The predicates that the ACSL annotations of a program state for the nodes of its CFA. */
public final class AcslInvariants {

  private AcslInvariants() {}

  /**
   * Returns the predicates that hold at each node: assertions and loop invariants at their node,
   * the pre-state of a contract at the start of its function or statement, and its post-state at
   * the end. A behavior only needs to hold if its assumes clauses hold in the pre-state.
   */
  public static ImmutableSetMultimap<CFANode, AcslPredicate> of(CFA pCfa) {
    AcslMetadata metadata = pCfa.getAcslMetadata();
    ImmutableSetMultimap.Builder<CFANode, AcslPredicate> invariants =
        ImmutableSetMultimap.builder();
    if (metadata == null) {
      return invariants.build();
    }

    metadata
        .assertions()
        .forEach((node, assertion) -> invariants.put(node, assertion.getPredicate()));
    metadata
        .loopAnnotations()
        .forEach(
            (node, annotation) ->
                invariants.putAll(
                    node,
                    FluentIterable.from(annotation.getLoopInvariants())
                        .transform(AcslLoopInvariant::getPredicate)));

    for (Entry<CFANode, AcslFunctionContract> entry : metadata.functionContracts().entries()) {
      FunctionEntryNode functionEntry = (FunctionEntryNode) entry.getKey();
      // As before the removal of the old ACSL parser, the pre-state is after the first edge
      for (CFAEdge edge : functionEntry.getLeavingEdges()) {
        invariants.putAll(edge.getSuccessor(), preState(entry.getValue()));
      }
      functionEntry
          .getExitNode()
          .ifPresent(exit -> invariants.putAll(exit, postState(entry.getValue())));
    }

    // Statement contracts for behaviors of enclosing contracts cannot be expressed as invariants
    metadata
        .statementContracts()
        .forEach(
            (node, contract) -> {
              if (contract.getEnclosingBehaviors().isEmpty()) {
                invariants.putAll(node, preState(contract.getContract()));
              }
            });
    metadata
        .statementContractEnds()
        .forEach(
            (node, contract) -> {
              if (contract.getEnclosingBehaviors().isEmpty()) {
                invariants.putAll(node, postState(contract.getContract()));
              }
            });

    return invariants.build();
  }

  private static ImmutableList<AcslPredicate> preState(AcslFunctionContract pContract) {
    ImmutableList.Builder<AcslPredicate> predicates = ImmutableList.builder();
    predicates.addAll(
        FluentIterable.from(pContract.getRequiresClauses())
            .transform(AcslRequires::getPredicate)
            .toList());
    for (AcslBehavior behavior : pContract.getBehaviors()) {
      if (!behavior.getRequiresClauses().isEmpty()) {
        predicates.add(
            or(
                conjunction(
                    FluentIterable.from(behavior.getRequiresClauses())
                        .transform(AcslRequires::getPredicate)
                        .toList()),
                not(assumption(behavior))));
      }
    }
    return predicates.build();
  }

  private static ImmutableList<AcslPredicate> postState(AcslFunctionContract pContract) {
    ImmutableList.Builder<AcslPredicate> predicates = ImmutableList.builder();
    predicates.addAll(
        FluentIterable.from(pContract.getEnsuresClauses())
            .transform(AcslEnsures::getPredicate)
            .toList());
    for (AcslBehavior behavior : pContract.getBehaviors()) {
      if (!behavior.getEnsuresClauses().isEmpty()) {
        predicates.add(
            or(
                conjunction(
                    FluentIterable.from(behavior.getEnsuresClauses())
                        .transform(AcslEnsures::getPredicate)
                        .toList()),
                not(new AcslOldPredicate(FileLocation.DUMMY, assumption(behavior)))));
      }
    }
    return predicates.build();
  }

  private static AcslPredicate assumption(AcslBehavior pBehavior) {
    return conjunction(
        FluentIterable.from(pBehavior.getAssumesClauses())
            .transform(AcslAssumes::getPredicate)
            .toList());
  }

  private static AcslPredicate conjunction(Collection<AcslPredicate> pPredicates) {
    AcslPredicate result = null;
    for (AcslPredicate predicate : pPredicates) {
      result =
          result == null
              ? predicate
              : new AcslBinaryPredicate(
                  FileLocation.DUMMY, result, predicate, AcslBinaryPredicateOperator.AND);
    }
    return result == null ? new AcslBooleanLiteralPredicate(FileLocation.DUMMY, true) : result;
  }

  private static AcslPredicate or(AcslPredicate pLeft, AcslPredicate pRight) {
    return new AcslBinaryPredicate(
        FileLocation.DUMMY, pLeft, pRight, AcslBinaryPredicateOperator.OR);
  }

  private static AcslPredicate not(AcslPredicate pPredicate) {
    return new AcslUnaryPredicate(
        FileLocation.DUMMY, pPredicate, AcslUnaryPredicateOperator.NEGATION);
  }
}
