/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import java.util.Iterator;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;

import com.google.common.base.Function;

/**
 * Helper functions to work with CPAs.
 */
public class CPAs {

  private CPAs() { }

  /**
   * Retrieve a specific CPA out of a structure of wrapper and composite CPAs.
   *
   * @param cpa The root of the tree of CPAs where to search.
   * @param cls The type to search for.
   * @return The found CPA, or null if none was found.
   */
  public <T extends ConfigurableProgramAnalysis> T retrieveCPA(ConfigurableProgramAnalysis cpa, Class<T> cls) {
    if (cls.isInstance(cpa)) {
      return cls.cast(cpa);
    } else if (cpa instanceof WrapperCPA) {
      return ((WrapperCPA)cpa).retrieveWrappedCpa(cls);
    } else {
      return null;
    }
  }

  /**
   * Creates an iterable that enumerates all the CPAs contained in
   * a single CPA, including the root CPA itself.
   * The tree of elements is traversed in pre-order.
   */
  public static Iterable<ConfigurableProgramAnalysis> asIterable(final ConfigurableProgramAnalysis pCpa) {

    return new TreeIterable<ConfigurableProgramAnalysis>(pCpa, CPA_CHILDREN_FUNCTION);
  }

  private static final Function<ConfigurableProgramAnalysis, Iterator<? extends ConfigurableProgramAnalysis>> CPA_CHILDREN_FUNCTION =
    new Function<ConfigurableProgramAnalysis, Iterator<? extends ConfigurableProgramAnalysis>>() {
      @Override
      public Iterator<? extends ConfigurableProgramAnalysis> apply(ConfigurableProgramAnalysis cpa) {
        return (cpa instanceof WrapperCPA)
             ? ((WrapperCPA)cpa).getWrappedCPAs().iterator()
             : null;
      }
    };
}
