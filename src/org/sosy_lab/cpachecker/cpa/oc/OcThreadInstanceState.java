// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * A state that knows which thread instance it belongs to. The ordering-consistency analysis does
 * not run a {@code ThreadingCPA}, so this is how consumers outside the analysis - the violation
 * witness export in particular - learn which thread executed a step.
 */
public interface OcThreadInstanceState extends AbstractState {

  /** The thread instance this state belongs to; {@link ThreadInstance#MAIN_INSTANCE_ID} is main. */
  int getThreadInstanceId();

  /**
   * The thread instance created by the step that led to this state, or {@link
   * MemoryEvent#NO_INSTANCE} if that step created no thread.
   */
  default int getCreatedThreadInstanceId() {
    return MemoryEvent.NO_INSTANCE;
  }
}
