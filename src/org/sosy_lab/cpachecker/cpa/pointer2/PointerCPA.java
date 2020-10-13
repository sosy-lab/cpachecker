// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

/**
 * Instances of this class are configurable program analyses for analyzing a
 * program to gain information about pointer aliasing.
 */
public class PointerCPA extends AbstractCPA implements StatisticsProvider,
                                                       ConfigurableProgramAnalysisWithBAM {

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }

  @Options(prefix="cpa.pointer2")
  public static class PointerOptions {

    @Option(secure=true, values={"JOIN", "SEP"}, toUppercase=true,
        description="which merge operator to use for PointerCPA")
    private String merge = "JOIN";

    @Option(secure=true, name="precisionFile", description="name of a file containing "
      + "information on pointer relations")
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private Path path = Paths.get("TrackedPointers");

    @Option(secure=true, name="noOutput", description="whether the resulting Points-To map "
      + "needs to be printed into file")
    private boolean noOutput = true;

    @Option(secure=true, name="useFakeLocs", description="whether to use the fake locations "
        + "during analysis (more precise, but also slower)")
    private boolean useFakeLocs = false;

  }

  private final Statistics statistics;
  private final Reducer reducer;

  /**
   * Gets a factory for creating PointerCPAs.
   *
   * @return a factory for creating PointerCPAs.
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PointerCPA.class).withOptions(PointerOptions.class);
  }

  /**
   * Creates a PointerCPA.
   *
   * @param options the configured options.
   */
  public PointerCPA(PointerOptions options) {
    super(options.merge, "SEP", PointerDomain.INSTANCE, PointerTransferRelation.INSTANCE);
    reducer = new PointerReducer();
    statistics = new PointerStatistics(options.noOutput, options.path,
                                        PointerTransferRelation.INSTANCE, reducer);
    PointerTransferRelation.INSTANCE.setUseFakeLocs(options.useFakeLocs);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return PointerState.INITIAL_STATE;
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

}
