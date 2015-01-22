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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import java.io.Serializable;

import com.google.common.annotations.VisibleForTesting;

/** Class for retrieving fresh indices for an old value from an SSAMap, based on some increment. */
public interface FreshValueProvider {

  /** get a new unused value based on the given one. */
  public int getFreshValue(String variable, int value);

  /** get a new provider, that is based on the current one and the given one. */
  public FreshValueProvider merge(FreshValueProvider other);


  /** The default implementation returns always the old index plus default increment. */
  public static class DefaultFreshValueProvider implements FreshValueProvider, Serializable {

    private static final long serialVersionUID = 8349867460915893626L;
    // Default difference for two SSA-indizes of the same name.
    @VisibleForTesting
    public static final int DEFAULT_INCREMENT = 1;

    @Override
    public int getFreshValue(String variable, int value) {
      return value + DEFAULT_INCREMENT;
    }

    /** returns the current FreshValueProvider without a change. */
    @Override
    public FreshValueProvider merge(FreshValueProvider other) {
      if (other instanceof DefaultFreshValueProvider) {
        return this;
      } else {
        return other.merge(this); // endless recursion!!
      }
    }

  }
}
