// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange;

import com.google.common.collect.ImmutableMap;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;

/**
 * One invariant of a correctness witness, together with its already parsed formula.
 *
 * <p>Everything except the formula and the variables holding previous values is delegated to the
 * entry, so that this can never disagree with the witness it was read from.
 *
 * @param entry the entry this invariant was parsed from
 * @param formula the parsed value of the entry
 * @param previousValueVariables for transition invariants, a mapping from the fresh variables
 *     holding the previous value of a variable to that variable, empty otherwise
 */
public record ParsedInvariant(
    InvariantEntry entry,
    ExpressionTree<AExpression> formula,
    ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> previousValueVariables) {

  public InvariantRecordType type() {
    return InvariantRecordType.fromKeyword(entry.getType());
  }

  public int line() {
    return entry.getLocation().getLine();
  }

  public OptionalInt column() {
    return entry.getLocation().getColumn();
  }

  public String function() {
    return entry.getLocation().getFunction();
  }

  /** The unparsed value of the entry, for use in messages. */
  public String value() {
    return entry.getValue();
  }

  public boolean isLoopInvariant() {
    return type() == InvariantRecordType.LOOP_INVARIANT
        || type() == InvariantRecordType.TRANSITION_LOOP_INVARIANT;
  }

  /**
   * Whether this invariant may refer to previous values of variables, which are encoded as fresh
   * variables. Currently only transition invariants over loops do.
   */
  public boolean hasPreviousValueVariables() {
    return type() == InvariantRecordType.TRANSITION_LOOP_INVARIANT;
  }
}
