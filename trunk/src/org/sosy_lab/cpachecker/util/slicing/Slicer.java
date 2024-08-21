// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.specification.Specification;

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
