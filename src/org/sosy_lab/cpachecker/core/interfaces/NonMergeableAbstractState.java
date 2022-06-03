// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

/**
 * This is a marker interface that tells other CPAs that this state should not be merged with other
 * states. Other CPAs may or may not reflect this.
 *
 * <p>It is primarily used to tell the merge operator of CompositeCPA to not merge an abstract
 * state.
 */
public interface NonMergeableAbstractState extends AbstractState {}
