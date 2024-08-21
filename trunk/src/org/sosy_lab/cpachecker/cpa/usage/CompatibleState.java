// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public interface CompatibleState extends Comparable<CompatibleState> {

  default boolean isCompatibleWith(@SuppressWarnings("unused") CompatibleState state) {
    return true;
  }

  default CompatibleState prepareToStore() {
    return this;
  }

  CompatibleNode getCompatibleNode();

  default boolean isRelevantFor(@SuppressWarnings("unused") SingleIdentifier id) {
    return true;
  }
}
