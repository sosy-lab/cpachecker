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

public class CollectorTransferRelation implements TransferRelation {

  private final TransferRelation transferRelation;
  private final LogManager logger;
  private myARGState mytransferARG;
  private CollectorState successorElem;


  public CollectorTransferRelation(TransferRelation tr, LogManager trLogger) {
    transferRelation = tr;
    logger = trLogger;
  }


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision)
      throws CPATransferException, InterruptedException {

  assert pElement instanceof CollectorState;

    AbstractState state = pElement;
    //logger.log(Level.INFO, "sonja pElement:\n" + pElement);
    ARGState wrappedState = (ARGState) ((CollectorState) state).getWrappedState();

    Collection<? extends AbstractState> successors;
    try {
      assert transferRelation instanceof ARGTransferRelation : "Transfer relation no ARG transfer"
          + " relation, but " + transferRelation.getClass().getSimpleName();

      successors = transferRelation.getAbstractSuccessors(wrappedState, pPrecision);
      //logger.log(Level.INFO, "sonja successors:\n" + successors);

      Collection<AbstractState> wrappedSuccessors = new ArrayList<>();
      for (AbstractState absElement : successors) {
        ARGState succARG = (ARGState) absElement;
        mytransferARG = new myARGState(succARG,wrappedState,null, logger);
        successorElem = new CollectorState(absElement, null, mytransferARG, false,null,null,logger);
        wrappedSuccessors.add(successorElem);
      }

      //logger.log(Level.INFO, "sonja wrappedSuccesors:\n" + wrappedSuccessors);
      return wrappedSuccessors;

    } catch (UnsupportedCodeException e) {
      throw e;
    }
  }

  // same as in ARGTransferRelation
  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "ARGCPA needs to be used as the outer-most CPA,"
            + " thus it does not support returning successors for a single edge.");
  }
}
