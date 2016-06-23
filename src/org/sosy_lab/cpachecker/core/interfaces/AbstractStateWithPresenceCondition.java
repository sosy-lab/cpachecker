/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;

/**
 * Sub-interface for {@link AbstractState}s that marks states
 *  with a presence condition (program configurations for which this state is relevant)
 */
public interface AbstractStateWithPresenceCondition extends AbstractState {

  /**
   * Get the presence condition of the abstract state.
   *
   * @return Optional.absent() if the CPA is not
   *    enabled for tracking the presence condition
   *    at the moment (but would support it).
   */
  public Optional<PresenceCondition> getPresenceCondition();

}