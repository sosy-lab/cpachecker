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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Checks if an abstract state or a set of abstract states adheres to the property which should be checked by the
 * specific implementation of PropertyChecker.
 */
public interface PropertyChecker {

  /**
   * Checks if a single abstract state satisfies the represented property. If the property cannot be checked for each
   * abstract state individually, an UnsupportedOperationException should be thrown.
   *
   * An UnsupportedOperationException should be thrown for every abstract element of a domain D or for none of the
   * abstract elements of domain D.
   *
   * @param elemToCheck - abstract state for which property satisfaction will be checked
   * @return true if property is successfully checked on abstract state elemToCheck, false otherwise
   * @throws UnsupportedOperationException
   */
  public boolean satisfiesProperty(AbstractState elemToCheck) throws UnsupportedOperationException;

  /**
   * Checks if a set of abstract states satisfies the represented property.
   *
   * @param certificate - set of abstract states for which property satisfaction will be checked
   * @return true if property holds for set of abstract states certificate, false otherwise
   */
  public boolean satisfiesProperty(Collection<AbstractState> certificate);

}
