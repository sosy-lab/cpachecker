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
package org.sosy_lab.cpachecker.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * Exception for cases when a class implements an interface like
 * {@link ConfigurableProgramAnalysis}, but fails to conform to some additional
 * semantic condition.
 */
public class InvalidComponentException extends CPAException {

  private static final long serialVersionUID = 3018467878727210858L;

  public InvalidComponentException(Class<?> cls, String componentType, String msg) {
    super(cls.getCanonicalName() + " is not a valid " + checkNotNull(componentType) + ": " + checkNotNull(msg));
  }

  public InvalidComponentException(Class<?> cpa, String componentType, Throwable cause) {
    super(cpa.getCanonicalName() + " is not a valid " + checkNotNull(componentType) + ": "
        + (cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName()), cause);
  }
}