// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

public class DssMessageFormat {

  public static final String HEADER_KEY = "header";
  public static final String STATUS_KEY = "status";
  public static final String CONTENT_KEY = "content";

  public static final String SENDER_ID_KEY = "senderId";
  public static final String HEADER_TYPE_KEY = "messageType";
  public static final String HEADER_TIMESTAMP_KEY = "timestamp";
  public static final String HEADER_IDENTIFIER_KEY = "identifier";

  public static final String PRECISE_KEY = "precise";
  public static final String PROPERTY_KEY = "property";
  public static final String SOUND_KEY = "sound";
  public static final String UNREACHABLE_BLOCK_END_KEY = "unreachableBlockEnd";

  public static final String MULTIPLE_STATES_KEY = "states";
  public static final String STATE_KEY = "state";

  public static final String RESULT_KEY = "result";
  public static final String EXCEPTION_KEY = "exception";
  public static final String WITNESS_TYPE_KEY = "witnessType";
  public static final String VIOLATION_PATH_KEY = "violationPath";

  private DssMessageFormat() {}
}
