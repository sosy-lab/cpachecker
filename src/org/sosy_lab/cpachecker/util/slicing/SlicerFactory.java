/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;

/**
 * Factory class for creating {@link Slicer} objects. The concrete <code>Slicer</code> that is
 * created by this classes {@link #create(LogManager, ShutdownNotifier, Configuration, CFA) create}
 * method depends on the given configuration.
 */
public class SlicerFactory {

  private enum ExtractorType {
    ALL,
    REDUCER,
    SYNTAX;
  }

  private enum SlicingType {
    /**
     * Use static program slicing based on dependence graph of CFA
     *
     * @see org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph
     * @see StaticSlicer
     */
    STATIC,
    /**
     * Use identity function as slicer.
     *
     * @see IdentitySlicer
     */
    IDENTITY
  }

  @Options(prefix = "slicing")
  public static class SlicerOptions {
    @Option(
        name = "extractor",
        secure = true,
        description = "which type of extractor for slicing criteria to use")
    private ExtractorType extractorType = ExtractorType.ALL;

    @Option(secure = true, name = "type", description = "what kind of slicing to use")
    private SlicingType slicingType = SlicingType.STATIC;

    public SlicerOptions(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public ExtractorType getExtractorType() {
      return extractorType;
    }

    public SlicingType getSlicingType() {
      return slicingType;
    }
  }

  /**
   * Creates a new {@link Slicer} object according to the given {@link Configuration}. All other
   * arguments of this method are passed to the created slicer and its components, if required by
   * them.
   */
  public Slicer create(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {
    SlicerOptions options = new SlicerOptions(pConfig);

    final SlicingCriteriaExtractor extractor;
    final ExtractorType extractorType = options.getExtractorType();
    switch (extractorType) {
      case ALL:
        extractor = new AllTargetsExtractor();
        break;
      case REDUCER:
        extractor = new ReducerExtractor(pConfig);
        break;
      case SYNTAX:
        extractor = new SyntaxExtractor(pConfig, pCfa, pLogger, pShutdownNotifier);
        break;
      default:
        throw new AssertionError("Unhandled criterion extractor type " + extractorType);
    }

    final SlicingType slicingType = options.getSlicingType();
    switch (slicingType) {
      case STATIC:
        return new StaticSlicer(extractor, pLogger, pShutdownNotifier, pConfig, pCfa);
      case IDENTITY:
        return new IdentitySlicer(extractor, pLogger, pShutdownNotifier, pConfig);
      default:
        throw new AssertionError("Unhandled slicing type " + slicingType);
    }
  }
}
