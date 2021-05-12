// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.AssumeCase;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.RaceGraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.automaton.VerificationTaskMetaData;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

@Options(prefix="cpa.usage.export")
public class KleverErrorTracePrinter extends ErrorTracePrinter {

  @Option(
      secure = true,
      name = "witnessTemplate",
      description = "export counterexample core as text file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathFile = PathTemplate.ofFormatString("witness.%s.graphml");

  String defaultSourcefileName;

  private static class ThreadIterator implements Iterator<Integer> {
    private Set<Integer> usedThreadIds;
    private int currentThread;

    public ThreadIterator() {
      usedThreadIds = new HashSet<>();
      currentThread = 0;
    }

    @Override
    public boolean hasNext() {
      return true;
    }

    @Override
    public Integer next() {
      int nextThread = currentThread + 1;
      while (usedThreadIds.contains(nextThread)) {
        nextThread++;
      }
      currentThread = nextThread;
      usedThreadIds.add(Integer.valueOf(nextThread));
      return getCurrentThread();
    }

    public int getCurrentThread() {
      return currentThread;
    }

    public void setCurrentThread(int newVal) {
      currentThread = newVal;
    }
  }

  public KleverErrorTracePrinter(
      Configuration c,
      BAMMultipleCEXSubgraphComputer pT,
      CFA pCfa,
      LogManager pL,
      LockTransferRelation lT)
      throws InvalidConfigurationException {
    super(c, pT, pCfa, pL, lT);
    c.inject(this, KleverErrorTracePrinter.class);
  }

  private String getCurrentId() {
    return "A" + idCounter;
  }

  private String getNextId() {
    idCounter++;
    return getCurrentId();
  }

  private int idCounter = 0;
  private ThreadIterator threadIterator;
  private Element currentNode;

  @Override
  protected void printUnsafe(SingleIdentifier pId, Pair<UsageInfo, UsageInfo> pTmpPair, boolean refined) {
    UsageInfo firstUsage = pTmpPair.getFirst();
    UsageInfo secondUsage = pTmpPair.getSecond();

    List<CFAEdge> firstPath = getPath(firstUsage);
    List<CFAEdge> secondPath = getPath(secondUsage);

    if (firstPath.isEmpty()) {
      // Empty path is strange
      logger.log(Level.WARNING, "Path to " + firstUsage + "is empty");
      return;
    }

    if (secondPath.isEmpty()) {
      // Empty path is strange
      logger.log(Level.WARNING, "Path to " + secondUsage + "is empty");
      return;
    }

    Iterator<CFAEdge> firstIterator = firstPath.iterator();
    Iterator<CFAEdge> secondIterator = secondPath.listIterator();

    CFAEdge firstEdge = firstIterator.next();
    CFAEdge secondEdge = secondIterator.next();

    int forkThread = 0;

    defaultSourcefileName =
        firstEdge.getFileLocation().getFileName();

    String status;

    idCounter = 0;
    threadIterator = new ThreadIterator();

    if (firstUsage.isLooped() || secondUsage.isLooped()) {
      status = "Failed";
    } else if (refined) {
      status = "Confirmed";
    } else {
      status = "Unconfirmed";
    }

    try {

      GraphMlBuilder builder =
          new RaceGraphMlBuilder(
              WitnessType.VIOLATION_WITNESS,
              defaultSourcefileName,
              cfa,
              new VerificationTaskMetaData(config, Specification.alwaysSatisfied()),
              createUniqueName(pId),
              status);
      Element result = builder.createNodeElement(getCurrentId(), NodeType.ONPATH);
      builder.addDataElementChild(result, NodeFlag.ISENTRY.key, "true");

      int commonIndex = getCommonPrefix(firstPath, secondPath);
      if (commonIndex < 0) {
        logger
            .log(Level.WARNING, "No thread create found, likely, you need another WitnessPrinter");
        return;
      }
      for (int i = 0; i < commonIndex; i++) {
        printEdge(builder, firstEdge);

        firstEdge = firstIterator.next();
        secondEdge = secondIterator.next();
      }

      forkThread = threadIterator.getCurrentThread();
      printEdge(builder, firstEdge);
      printPath(firstUsage, firstIterator, builder);

      if (forkThread != threadIterator.currentThread || isThreadCreateFunction(secondEdge)) {
        threadIterator.setCurrentThread(forkThread);
        printEdge(builder, secondEdge);
      } else {
        // Means we split not on thread create, manually create thread
        Element edge = printEdge(builder, secondEdge);
        builder.addDataElementChild(
            edge,
            KeyDef.CREATETHREAD,
            Integer.toString(threadIterator.next()));
      }
      printPath(secondUsage, secondIterator, builder);
      builder.addDataElementChild(currentNode, NodeFlag.ISVIOLATION.key, "true");

      Path currentPath;
      String fileName = createUniqueName(pId).replace(" ", "_");
      currentPath = errorPathFile.getPath(fileName);
      int i = 0;

      while (Files.exists(currentPath)) {
        currentPath = errorPathFile.getPath(fileName.concat("__" + i++));
      }
      IO.writeFile(currentPath, Charset.defaultCharset(), (Appender) a -> builder.appendTo(a));
      printedUnsafes.inc();

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

  private int getCommonPrefix(List<CFAEdge> firstPath, List<CFAEdge> secondPath) {
    // Common prefix MUST be ended on thread create, thus it is i bit complicated
    Iterator<CFAEdge> firstIterator = firstPath.iterator();
    Iterator<CFAEdge> secondIterator = secondPath.iterator();
    int threadCreateIndex = -1;
    int index = 0;
    int mainIndex = -1;

    while (firstIterator.hasNext() && secondIterator.hasNext()) {
      CFAEdge firstEdge = firstIterator.next();
      CFAEdge secondEdge = secondIterator.next();

      if (isThreadCreateFunction(firstEdge) || isThreadCreateFunction(secondEdge)) {
        // Not indexof, just in case of multiple cases
        threadCreateIndex = index;
      }
      if (firstEdge != secondEdge) {
        // Note, after previous check, because devision on threadCreate is ok
        if (threadCreateIndex > 0) {
          return threadCreateIndex;
        } else {
          return mainIndex;
        }
      } else {
        if (firstEdge instanceof CFunctionCallEdge && mainIndex < 0) {
          // inter in main function will be considered as thread create if we do not found the
          // thread create
          mainIndex = index;
        }
      }
      index++;
    }
    if (threadCreateIndex > 0) {
      return threadCreateIndex;
    } else {
      return mainIndex;
    }
  }

  private void printPath(UsageInfo usage, Iterator<CFAEdge> iterator, GraphMlBuilder builder) {
    String pIdName = usage.getId().getName();
    List<Element> warnings = new ArrayList<>();

    while (iterator.hasNext()) {
      CFAEdge pEdge = iterator.next();

      Element edge = printEdge(builder, pEdge);

      if (Objects.equals(pEdge.getSuccessor(), usage.getCFANode())) {
        warnings.add(edge);
      }
    }

    if (warnings.isEmpty()) {
      logger.log(Level.WARNING, "Can not determine an unsafe edge for " + pIdName);
      potentialAliases.inc();
    } else {
      Element warningEdge = warnings.get(warnings.size() - 1);
      printWarningTo(builder, warningEdge, usage.toString());
    }
  }

  // Overrided in subclass
  protected void printWarningTo(GraphMlBuilder builder, Element element, String message) {
    builder.addDataElementChild(element, KeyDef.WARNING, message);
  }

  protected Element printEdge(GraphMlBuilder builder, CFAEdge edge) {

    if (handleAsEpsilonEdge0(edge)) {
      return null;
    }

    if (isThreadCreateFunction(edge)) {
      CFunctionSummaryEdge sEdge = ((CFunctionCallEdge) edge).getSummaryEdge();
      Element result = printEdge(builder, sEdge);
      builder.addDataElementChild(
          result, KeyDef.CREATETHREAD, Integer.toString(threadIterator.next()));
    }
    return printEdge(builder, edge, getCurrentId(), getNextId());
  }

  // Overrided in subclass
  protected String formatNote(String value) {
    return value;
  }

  private Element printEdge(GraphMlBuilder builder, CFAEdge edge, String currentId, String nextId) {
    Element result = builder.createEdgeElement(currentId, nextId);
    dumpCommonInfoForEdge(builder, result, edge);

    String note = getNoteFor(edge);
    if (note != null && !note.isEmpty()) {
      String formatted = formatNote(note);
      builder.addDataElementChild(result, KeyDef.NOTE, formatted);
    }
    currentNode = builder.createNodeElement(nextId, NodeType.ONPATH);
    return result;
  }

  private void dumpCommonInfoForEdge(GraphMlBuilder builder, Element result, CFAEdge pEdge) {
    CFANode succ = pEdge.getSuccessor();
    String functionName = null;
    if (succ instanceof FunctionEntryNode) {
      functionName = ((FunctionEntryNode) succ).getFunctionDefinition().getOrigName();
    } else if (AutomatonGraphmlCommon.isMainFunctionEntry(pEdge)) {
      functionName = succ.getFunctionName();
    }
    if (functionName != null) {
      builder.addDataElementChild(result, KeyDef.FUNCTIONENTRY, functionName);
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

    final Set<FileLocation> locations =
        AutomatonGraphmlCommon.getFileLocationsFromCfaEdge0(pEdge, cfa.getMainFunction());
    final Comparator<FileLocation> nodeOffsetComparator =
        Comparator.comparingInt(FileLocation::getNodeOffset);
    final FileLocation min =
        locations.isEmpty() ? null : Collections.min(locations, nodeOffsetComparator);
    final FileLocation max =
        locations.isEmpty() ? null : Collections.max(locations, nodeOffsetComparator);

    if (min != null) {
      builder.addDataElementChild(result, KeyDef.ORIGINFILE, min.getFileName());
      builder.addDataElementChild(
          result,
          KeyDef.STARTLINE,
          Integer.toString(min.getStartingLineInOrigin()));
    }
    if (max != null) {
      builder.addDataElementChild(
          result,
          KeyDef.ENDLINE,
          Integer.toString(max.getEndingLineInOrigin()));
    }

    if (min != null && min.isOffsetRelatedToOrigin()) {
      builder.addDataElementChild(result, KeyDef.OFFSET, Integer.toString(min.getNodeOffset()));
    }
    if (max != null && max.isOffsetRelatedToOrigin()) {
          builder.addDataElementChild(
              result,
              KeyDef.ENDOFFSET,
              Integer.toString(max.getNodeOffset() + max.getNodeLength() - 1));
    }

    if (!pEdge.getRawStatement().trim().isEmpty()) {
      builder.addDataElementChild(result, KeyDef.SOURCECODE, pEdge.getRawStatement());
    }

    builder.addDataElementChild(
        result, KeyDef.THREADID, Integer.toString(threadIterator.getCurrentThread()));
  }

  private boolean isThreadCreateFunction(CFAEdge pEdge) {
    return getThreadCreateStatementIfExists(pEdge) != null;
  }

  private CThreadCreateStatement getThreadCreateStatementIfExists(CFAEdge pEdge) {
    if (pEdge instanceof CFunctionCallEdge) {
      CFunctionSummaryEdge sEdge = ((CFunctionCallEdge) pEdge).getSummaryEdge();
      CFunctionCall fCall = sEdge.getExpression();
      if (fCall instanceof CThreadCreateStatement) {
        return (CThreadCreateStatement) fCall;
      }
    }
    return null;
  }

  private static boolean handleAsEpsilonEdge0(CFAEdge edge) {
    if (edge instanceof BlankEdge) {
      if (AutomatonGraphmlCommon.isMainFunctionEntry(edge)) {
        return false;
      }
      if (edge.getSuccessor() instanceof FunctionExitNode) {
        return AutomatonGraphmlCommon
            .isEmptyStub(((FunctionExitNode) edge.getSuccessor()).getEntryNode());
      }
      if (AutomatonGraphmlCommon.treatAsTrivialAssume(edge)) {
        return false;
      }
      if (AutomatonGraphmlCommon.treatAsWhileTrue(edge)) {
        return false;
      }
      return true;
    }
    return false;
  }
}
