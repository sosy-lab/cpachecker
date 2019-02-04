/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.collector;

import static java.util.logging.Level.WARNING;

import com.google.common.base.Charsets;
import com.google.common.base.Predicates;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.eclipse.cdt.core.index.IPDOMASTProcessor.Abstract;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.arg.ARGTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class CollectorTransferRelation implements TransferRelation, Graphable {

  private final TransferRelation transferRelation;
  private final LogManager logger;
  Map<Integer, Collection<? extends AbstractState>>
      successorsHashmap = new HashMap<Integer, Collection<? extends AbstractState>>();
  Collection<AbstractState> NEWstoredstates = new ArrayList<AbstractState>();
  Collection<ARGState> rootStatestest = new ArrayList<ARGState>();



  public CollectorTransferRelation(TransferRelation tr, LogManager trLogger) {
    transferRelation = tr;
    logger = trLogger;
  }


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision)
      throws CPATransferException, InterruptedException {

  assert pElement instanceof CollectorState;

    AbstractState wrappedState = pElement;
    ARGState wrappedState2 = (ARGState) ((CollectorState) wrappedState).getWrappedState();
    AbstractState wrappedState3 = wrappedState2;

    Collection<? extends AbstractState> successors;
    try {
      assert transferRelation instanceof ARGTransferRelation : "Transfer relation no ARG transfer"
          + " relation, but " + transferRelation.getClass().getSimpleName();

      successors = transferRelation.getAbstractSuccessors(wrappedState2, pPrecision);

      Collection<AbstractState> wrappedSuccessors = new ArrayList<>();
      for (AbstractState absElement : successors) {
       CollectorState successorElem = new CollectorState(absElement, null,logger) {
       };
        wrappedSuccessors.add(successorElem);
      }
      //CollectorState test2 = new CollectorState(element,wrappedSuccessors, logger);
      CollectorState test2 = new CollectorState(wrappedState3,wrappedSuccessors, logger);
      NEWstoredstates.add(test2);
      //logger.log(Level.INFO, "sonja NEW TESTinTR:\n" + NEWstoredstates);
      //makeFile2(NEWstoredstates);


      rootStatestest.add(wrappedState2);
      //writeArg(wrappedState2);

      return wrappedSuccessors;

    } catch (UnsupportedCodeException e) {
      throw e;
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "ARGCPA needs to be used as the outer-most CPA,"
            + " thus it does not support returning successors for a single edge.");
  }


  @Override
  public String toDOTLabel() {
    if (successorsHashmap instanceof Graphable) {
      return ((Graphable)successorsHashmap).toDOTLabel();
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }


  private void makeFile2(Collection<AbstractState> pNEWstoredstates) {
    try{
      Writer writer = new FileWriter("./output/SonjasFileA.txt", true);
      //Writer writer = IO.openOutputFile(Paths.get("./output/SonjasFile2.txt"), Charsets.UTF_8);
      BufferedWriter bw = new BufferedWriter(writer);
      String line = "CollectorTransferRelation Data";
      bw.write(line + "\n");
      bw.write(String.valueOf(pNEWstoredstates) + "\n");
      bw.close();

    }catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create Sonjas file.");
    }
  }

  private void writeArg(// muss wieder raus, achtung dazugehöriges writeArg weiteroben nicht
                        // vergessen
      //final Path file,
      //final Multimap<ARGState, ARGState> connections,
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
