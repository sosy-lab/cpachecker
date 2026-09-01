// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

public final class DssMessageKeys {

  public static final String HEADER = "header";
  public static final String STATUS = "status";
  public static final String STATES = "states";
  public static final String CONTENT = "content";

  public static final String SENDER_ID = "senderId";
  public static final String MESSAGE_TYPE = "messageType";
  public static final String TIMESTAMP = "timestamp";
  public static final String IDENTIFIER = "identifier";

  public static final String PRECISE = "precise";
  public static final String PROPERTY = "property";
  public static final String SOUND = "sound";
  public static final String UNREACHABLE_BLOCK_END = "unreachableBlockEnd";

  public static final String MULTIPLE_STATES = "states";
  public static final String STATE = "state";

  public static final String RESULT = "result";
  public static final String EXCEPTION = "exception";
  public static final String WITNESS_TYPE = "witnessType";
  public static final String VIOLATION_PATH = "violationPath";

  private DssMessageKeys() {}
}
