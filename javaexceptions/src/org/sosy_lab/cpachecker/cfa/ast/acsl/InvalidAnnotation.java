// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.List;

/**
 * Represents a malformed ACSL annotation. Used for communicating errors discovered by the scanner
 * to the parser.
 */
public final class InvalidAnnotation implements ACSLAnnotation {
  @Override
  public ACSLPredicate getPredicateRepresentation() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ACSLPredicate getCompletenessPredicate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Behavior> getDeclaredBehaviors() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Behavior> getReferencedBehaviors() {
    throw new UnsupportedOperationException();
  }
}
