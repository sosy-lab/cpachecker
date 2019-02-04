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
package org.sosy_lab.cpachecker.cpa.collector;

import static com.google.common.collect.FluentIterable.from;
import static java.util.logging.Level.WARNING;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options
public class CollectorMergeJoin implements MergeOperator {

  private final MergeOperator wrappedMergeCol;
  private final LogManager logger;
  Collection<AbstractState> NEWstoredstatesAbstract = new ArrayList<AbstractState>();

  public CollectorMergeJoin(MergeOperator pWrappedMerge, AbstractDomain pWrappedDomain,
                            Configuration config, LogManager mjLogger)
      throws InvalidConfigurationException {

    wrappedMergeCol = pWrappedMerge;
    config.inject(this);
    logger = mjLogger;

  }

  @Override
  public AbstractState merge(AbstractState pElement1,
                             AbstractState pElement2, Precision pPrecision) throws CPAException, InterruptedException {

    AbstractState abs1 = pElement1;
    AbstractState abs2 = pElement2;
    ARGState wrappedState1 = (ARGState) ((CollectorState) abs1).getWrappedState();
    ARGState wrappedState2 = (ARGState) ((CollectorState) abs2).getWrappedState();
    //logger.log(Level.INFO, "sonja StopwrappedState:\n" + wrappedState1);
    //logger.log(Level.INFO, "sonja StopwrappedState2:\n" + wrappedState2);
    AbstractState wrappedStateabs1 = wrappedState1;
    AbstractState wrappedStateabs2 = wrappedState2;


    CollectorState merge1 = new CollectorState(wrappedStateabs2, null,logger);
    CollectorState merge2 = new CollectorState(wrappedStateabs1, null, logger);

    Collection<AbstractState> wrappedAbstract = new ArrayList<>();
    wrappedAbstract.add(merge1);
    wrappedAbstract.add(merge2);


    ARGState mergedElement = (ARGState) wrappedMergeCol.merge(wrappedState1,wrappedState2,
        pPrecision);

    AbstractState merged = mergedElement;
    CollectorState merge3 = new CollectorState(merged, null, logger);

    wrappedAbstract.add(merge3);

    CollectorState mergedElementplusStorage = new CollectorState(merged,wrappedAbstract, logger);
    NEWstoredstatesAbstract.add(mergedElementplusStorage);
    //logger.log(Level.INFO, "sonja NEW TESTinMERGE:\n" + NEWstoredstatesAbstract);
    //makeFile4(NEWstoredstatesAbstract);
    AbstractState mergedElementplusStorageAbstract = mergedElementplusStorage;


    return mergedElementplusStorageAbstract;
  }
  private void makeFile4(Collection<AbstractState> pNEWstoredstates) {
    try{
      Writer writer = new FileWriter("./output/SonjasFileA.txt", true);
      //Writer writer = IO.openOutputFile(Paths.get("./output/SonjasFile2.txt"), Charsets.UTF_8);
      BufferedWriter bw = new BufferedWriter(writer);
      String line = "CollectorMergeJoin Data";
      bw.write(line + "\n");
      bw.write(String.valueOf(pNEWstoredstates) + "\n");
      bw.close();

    }catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create Sonjas file.");
    }
  }
  public Collection<AbstractState> getStorage() {
    return NEWstoredstatesAbstract;
  }

  private void writeArg( // muss wieder raus
      final ARGState rootStates)
  {
    try (
        Writer writer = new FileWriter("./output/SonjasDot.dot", true);
        //Writer writer = IO.openOutputFile(Paths.get("./output/SonjasFile2.txt"), Charsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(writer);
        //Writer w = IO.openOutputFile(Paths.get("./output/SonjasARGDOTfile.txt"),Charset.defaultCharset())
    ) {
      ARGToDotWriter.write(
          bw,
          rootStates,
          ARGState::getChildren,
          Predicates.alwaysTrue(),
          Predicates.alwaysFalse());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, String.format("Could not write ARG to file: %s"));
    }
  }

}
