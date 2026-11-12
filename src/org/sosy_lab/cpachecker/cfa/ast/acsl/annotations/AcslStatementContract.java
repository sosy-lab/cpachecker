// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * The contract of a statement, which consists of the same clauses as a function contract. If it
 * names behaviors of the enclosing contracts with "for", it only applies to these behaviors.
 */
public final class AcslStatementContract extends AAcslAnnotation {

  private final ImmutableList<String> enclosingBehaviors;
  private final AcslFunctionContract contract;

  public AcslStatementContract(
      FileLocation pFileLocation,
      ImmutableList<String> pEnclosingBehaviors,
      AcslFunctionContract pContract) {
    super(pFileLocation);
    enclosingBehaviors = checkNotNull(pEnclosingBehaviors);
    contract = checkNotNull(pContract);
  }

  /** The behaviors of the enclosing contracts that this contract is for, if any. */
  public ImmutableList<String> getEnclosingBehaviors() {
    return enclosingBehaviors;
  }

  /** The clauses of this contract. */
  public AcslFunctionContract getContract() {
    return contract;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslStatementContract other
        && enclosingBehaviors.equals(other.enclosingBehaviors)
        && contract.equals(other.contract);
  }

  @Override
  public int hashCode() {
    return 31 * enclosingBehaviors.hashCode() + contract.hashCode();
  }

  @Override
  public String toAstString() {
    return (enclosingBehaviors.isEmpty()
            ? ""
            : "for " + Joiner.on(", ").join(enclosingBehaviors) + ": ")
        + contract.toAstString();
  }
}
