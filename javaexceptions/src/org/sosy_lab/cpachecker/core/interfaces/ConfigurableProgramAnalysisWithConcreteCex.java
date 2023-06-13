// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/** This Cpa can generate a concrete counterexample from an {@link ARGPath} path. */
public interface ConfigurableProgramAnalysisWithConcreteCex {

  /**
   * Creates a {@link ConcreteStatePath} path, that contain the concrete values of the given
   * variables along the given {@link ARGPath}. The {@link ConcreteStatePath} path is used to
   * calculate the concrete values of the variables along the generated counterexample path.
   *
   * @param path An {@link ARGPath} counterexample path, generated from the {@link ARGCPA}. The
   *     concrete values of variables along this path should be calculated.
   * @return A {@link ConcreteStatePath} path along the {@link CFAEdge} edges of the {@link ARGPath}
   *     path that contain concrete values for the variables along the path.
   */
  ConcreteStatePath createConcreteStatePath(ARGPath path);
}
