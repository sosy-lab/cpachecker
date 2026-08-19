// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

public final class DssMessageFormat {

  private DssMessageFormat() {
  }

  public static final String HEADER = "header";
  public static final String CONTENT = "content";

  public static final String STATUS = "status";
  public static final String STATUS_SOUND = "sound";
  public static final String STATUS_PRECISE = "precise";
  public static final String STATUS_PROPERTY = "property";

  public static final String STATES = "states";
  public static final String STATE_PREFIX = "state";

  public static final String LOCATION_STATE = "location";
  public static final String PREDICATE_STATE = "predicate";
  public static final String BLOCK_STATE = "block";

}
