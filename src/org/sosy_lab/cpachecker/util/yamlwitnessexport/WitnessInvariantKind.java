// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import java.util.Optional;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;

/**
 * The kinds of information which the invariant set of a correctness witness can contain.
 *
 * <p>This is a selector used to decide which information is exported into a witness and which
 * information a witness of a given {@link YAMLWitnessVersion} may contain. The keywords used in the
 * witness itself are not part of this enum, they belong to {@link InvariantRecordType} and {@link
 * FunctionContractEntry}.
 */
public enum WitnessInvariantKind {
  LOOP_INVARIANT,
  LOCATION_INVARIANT,
  LOOP_TRANSITION_INVARIANT,
  LOCATION_TRANSITION_INVARIANT,
  FUNCTION_CONTRACT;

  /**
   * The kind of the given invariant type, as it is used inside an {@link InvariantEntry}.
   *
   * @param pType the type of an invariant entry
   * @return the matching kind, or an empty Optional if the type is not known
   */
  public static Optional<WitnessInvariantKind> of(InvariantRecordType pType) {
    return switch (pType) {
      case LOOP_INVARIANT -> Optional.of(LOOP_INVARIANT);
      case LOCATION_INVARIANT -> Optional.of(LOCATION_INVARIANT);
      case TRANSITION_LOOP_INVARIANT -> Optional.of(LOOP_TRANSITION_INVARIANT);
      case TRANSITION_LOCATION_INVARIANT -> Optional.of(LOCATION_TRANSITION_INVARIANT);
      case UNKNOWN -> Optional.empty();
    };
  }
}
