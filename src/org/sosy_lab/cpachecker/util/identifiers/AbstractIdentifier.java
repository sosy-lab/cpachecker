// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import java.util.Collection;

public interface AbstractIdentifier extends Comparable<AbstractIdentifier> {
  @Override
  boolean equals(Object other);

  @Override
  int hashCode();

  @Override
  String toString();

  boolean isGlobal();

  AbstractIdentifier cloneWithDereference(int dereference);

  int getDereference();

  boolean isPointer();

  boolean isDereferenced();

  Collection<AbstractIdentifier> getComposedIdentifiers();
}
