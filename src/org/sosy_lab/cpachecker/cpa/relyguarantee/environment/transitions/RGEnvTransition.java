/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;

import com.google.common.collect.ImmutableCollection;

/**
 * Interface for rely-guarantee environmental transitions.
 */
public interface RGEnvTransition {

  RGEnvTransitionType getRGType();

  /**
   * The point where the element was abstracted.
   * @return
   */
  ARTElement getAbstractionElement();


  /**
   * Source thread's id.
   * @return
   */
  int getTid();

  /**
   * Returns ART elements that generated this transitions.
   * It contains  {@link #getSourceARTElement getSourceARTElement()} and
   * {@link #getTargetARTElement getTargetARTElement()}.
   * @return
   */
  ImmutableCollection<ARTElement> getGeneratingARTElements();

  /**
   * ART element where the concrete operation was applied.
   * @return
   */
  ARTElement getSourceARTElement();

  /**
   * ART element created by the concreate operation.
   * @return
   */
  ARTElement getTargetARTElement();



}




