// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.residualprogram.slicing;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.slicing.AbstractSlicer;
import org.sosy_lab.cpachecker.util.slicing.Slicer;
import org.sosy_lab.cpachecker.util.slicing.SlicerFactory;
import org.sosy_lab.cpachecker.util.slicing.StaticSlicer;

/**
 * Algorithm that creates a program slice. Whether and how the created program slice ends up in an
 * output file depends on the configuration of {@link StaticSlicer} and {@link AbstractSlicer}. The
 * slicing criterion used is determined by the specification given to this algorithm and the
 * configuration of <code>StaticSlicer</code> and <code>AbstractSlicer</code>
 */
public class SlicingAlgorithm implements Algorithm {

  private final Slicer slicer;
  private final Specification spec;
  private final CFA cfa;

  public SlicingAlgorithm(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification)
      throws CPAException, InvalidConfigurationException, InterruptedException {

    slicer = new SlicerFactory().create(pLogger, pShutdownNotifier, pConfig, pCfa);
    cfa = pCfa;
    spec = pSpecification;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // at the moment, we don't do anything with the computed slice here,
    // but expect the slicer itself to output it in some file.
    slicer.getSlice(cfa, spec);
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }
}
