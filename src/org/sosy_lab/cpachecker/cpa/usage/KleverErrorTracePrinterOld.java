/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.AssumeCase;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.automaton.VerificationTaskMetaData;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public class KleverErrorTracePrinterOld extends ErrorTracePrinter {

  public KleverErrorTracePrinterOld(
      Configuration c,
      BAMMultipleCEXSubgraphComputer pT,
      CFA pCfa,
      LogManager pL,
      LockTransferRelation lT)
      throws InvalidConfigurationException {
    super(c, pT, pCfa, pL, lT);
  }

  int idCounter = 0;

  private String getId() {
    return "A" + idCounter++;
  }

  @Override
  protected void printUnsafe(SingleIdentifier pId, Pair<UsageInfo, UsageInfo> pTmpPair) {
    UsageInfo firstUsage = pTmpPair.getFirst();
    UsageInfo secondUsage = pTmpPair.getSecond();
    List<CFAEdge> firstPath, secondPath;

    firstPath = getPath(firstUsage);
    secondPath = getPath(secondUsage);

    if (firstPath == null || secondPath == null) {
      return;
    }
    try {
      File name = new File("output/witness." + createUniqueName(pId) + ".graphml");
      String defaultSourcefileName =
          from(firstPath)
              .filter(FILTER_EMPTY_FILE_LOCATIONS)
              .get(0)
              .getFileLocation()
              .getFileName();

      GraphMlBuilder builder =
          new GraphMlBuilder(
              WitnessType.VIOLATION_WITNESS,
              defaultSourcefileName,
              cfa,
              new VerificationTaskMetaData(config, Specification.alwaysSatisfied()));

      idCounter = 0;
      Element result = builder.createNodeElement("A0", NodeType.ONPATH);
      builder.addDataElementChild(result, NodeFlag.ISENTRY.key, "true");
      printPath(firstUsage, 0, builder);
      result = printPath(secondUsage, 2, builder);

      builder.addDataElementChild(result, NodeFlag.ISVIOLATION.key, "true");

      // builder.appendTo(w);
      IO.writeFile(
          Paths.get(name.getAbsolutePath()),
          Charset.defaultCharset(),
          (Appender) a -> builder.appendTo(a));
      // w.close();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Exception during printing unsafe " + pId + ": " + e.getMessage());
    } catch (ParserConfigurationException e) {
      logger.log(Level.SEVERE, "Exception during printing unsafe " + pId + ": " + e.getMessage());
    } catch (DOMException e1) {
      logger.log(Level.SEVERE, "Exception during printing unsafe " + pId + ": " + e1.getMessage());
    } catch (InvalidConfigurationException e1) {
      logger.log(Level.SEVERE, "Exception during printing unsafe " + pId + ": " + e1.getMessage());
    }
  }

  private Element printPath(UsageInfo usage, int threadId, GraphMlBuilder builder) {
    String currentId = getId(), nextId = currentId;
    SingleIdentifier pId = usage.getId();
    List<CFAEdge> path = usage.getPath();

    Iterator<CFAEdge> iterator = from(path).filter(FILTER_EMPTY_FILE_LOCATIONS).iterator();

    Optional<CFAEdge> warningEdge =
        from(path)
            .filter(
                e ->
                Objects.equals(e.getSuccessor(), usage.getCFANode())
                        && e.toString().contains(pId.getName()))
            .last();

    CFAEdge warning;

    if (warningEdge.isPresent()) {
      warning = warningEdge.get();
    } else {
      logger.log(Level.WARNING, "Can not determine an unsafe edge");
      warning = null;
    }
    Element result = null;
    Element lastWarningElement = null;

    boolean printEdge = false;
    boolean globalDeclaration = true;

    if (threadId == 0) {
      printEdge = true;
    }

    while (iterator.hasNext()) {
      CFAEdge pEdge = iterator.next();

      if (!printEdge
          && pEdge.getEdgeType() != CFAEdgeType.DeclarationEdge
          && pEdge.getEdgeType() != CFAEdgeType.BlankEdge) {
        assert globalDeclaration;
        printEdge = true;
      } else if (!printEdge) {
        continue;
      }

      currentId = nextId;
      nextId = getId();

      result = builder.createEdgeElement(currentId, nextId);
      dumpCommonInfoForEdge(builder, result, pEdge);

      String note = getNoteFor(pEdge);
      if (!note.isEmpty()) {
        builder.addDataElementChild(result, KeyDef.NOTE, note);
      }

      if (globalDeclaration
          && pEdge.getEdgeType() != CFAEdgeType.DeclarationEdge
          && pEdge.getEdgeType() != CFAEdgeType.BlankEdge) {
        globalDeclaration = false;
        if (threadId == 0) {
          threadId++;
        }
        builder.addDataElementChild(result, KeyDef.FUNCTIONENTRY, "main");
      }

      builder.addDataElementChild(result, KeyDef.THREADID, Integer.toString(threadId));

      if (pEdge == warning) {
        lastWarningElement = result;
      }
      result = builder.createNodeElement(nextId, NodeType.ONPATH);
    }
    if (lastWarningElement != null) {
      builder.addDataElementChild(lastWarningElement, KeyDef.WARNING, usage.toString());
    }

    // Special hack to connect two traces
    idCounter--;
    return result;
  }

  private void dumpCommonInfoForEdge(GraphMlBuilder builder, Element result, CFAEdge pEdge) {

    if (pEdge.getSuccessor() instanceof FunctionEntryNode) {
      FunctionEntryNode in = (FunctionEntryNode) pEdge.getSuccessor();
      builder.addDataElementChild(result, KeyDef.FUNCTIONENTRY, in.getFunctionName());
    }
    if (pEdge.getSuccessor() instanceof FunctionExitNode) {
      FunctionExitNode out = (FunctionExitNode) pEdge.getSuccessor();
      builder.addDataElementChild(result, KeyDef.FUNCTIONEXIT, out.getFunctionName());
    }

    if (pEdge instanceof AssumeEdge) {
      AssumeEdge a = (AssumeEdge) pEdge;
      AssumeCase assumeCase = a.getTruthAssumption() ? AssumeCase.THEN : AssumeCase.ELSE;
      builder.addDataElementChild(result, KeyDef.CONTROLCASE, assumeCase.toString());
    }

    FileLocation location = pEdge.getFileLocation();
    assert (location != null) : "should be filtered";
    builder.addDataElementChild(result, KeyDef.ORIGINFILE, location.getFileName());
    builder.addDataElementChild(
        result, KeyDef.STARTLINE, Integer.toString(location.getStartingLineInOrigin()));
    builder.addDataElementChild(result, KeyDef.OFFSET, Integer.toString(location.getNodeOffset()));

    if (!pEdge.getRawStatement().trim().isEmpty()) {
      builder.addDataElementChild(result, KeyDef.SOURCECODE, pEdge.getRawStatement());
    }
  }
}
