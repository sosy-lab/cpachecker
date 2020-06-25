/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public interface Splitable {

  /**
   * Returns a version of this where the passed states are used as replacements. The returned state
   * is allowed to be identical to this in case there are no changes.
   *
   * @param pReplacementStates states that shall be used in the forked as replacement for states in
   *     the old state
   */
  AbstractState forkWithReplacements(Collection<AbstractState> pReplacementStates);

}