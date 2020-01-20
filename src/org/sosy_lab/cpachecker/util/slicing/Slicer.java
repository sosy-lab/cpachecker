/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.slicing;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.Specification;

/**
 * Interface for program slicers.
 *
 * <p>Classes implementing this interface provide means to get the {@link CFAEdge CFA edges} of a
 * {@link CFA} that are relevant regarding a {@link Specification} or a set of slicing criteria.
 * Slicing criteria are given as CFA edges.
 *
 * @see SlicerFactory
 */
public interface Slicer {

  /** Returns the {@link Slice} of the given CFA that is relevant for the given specification. */
  Slice getSlice(CFA pCfa, Specification pSpecification) throws InterruptedException;

  /** Returns the {@link Slice} in the given CFA that is relevant for the given specification. */
  Slice getSlice(CFA pCfa, Collection<CFAEdge> pSlicingCriteria) throws InterruptedException;
}
