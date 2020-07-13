// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import com.google.common.collect.Lists;
import java.util.List;

public class TaEncodingOptions {
  public static enum TAEncodingExtension {
    SHALLOW_SYNC,
    INVARIANTS,
    ACTION_SYNC
  }

  public List<TAEncodingExtension> encodingExtensions =
      Lists.newArrayList(TAEncodingExtension.INVARIANTS);

  public static enum DiscreteEncodingType {
    LOCAL_ID,
    GLOBAL_ID,
    BOOLEAN_VAR
  }

  public DiscreteEncodingType locationEncoding = DiscreteEncodingType.LOCAL_ID;
  public DiscreteEncodingType actionEncoding = DiscreteEncodingType.GLOBAL_ID;

  public static enum TimeEncodingType {
    GLOBAL_IMPLICIT,
    GLOBAL_EXPLICIT,
    LOCAL_EXPLICIT,
  }

  public TimeEncodingType timeEncoding = TimeEncodingType.GLOBAL_IMPLICIT;

  public static enum AutomatonEncodingType {
    TRANSITION_UNROLLING,
    LOCATION_UNROLLING,
    CONSTRAINT_UNROLLING
  }

  public AutomatonEncodingType automatonEncodingType = AutomatonEncodingType.CONSTRAINT_UNROLLING;

  public static enum SpecialActionType {
    LOCAL,
    GLOBAL,
    NONE
  }

  public SpecialActionType idleAction = SpecialActionType.NONE;
  public SpecialActionType delayAction = SpecialActionType.NONE;
}
