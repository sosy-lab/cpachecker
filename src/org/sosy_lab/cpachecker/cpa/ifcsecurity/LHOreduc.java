/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.interval.Interval;

/**
 * L
 */
@Options(prefix = "pcc.arg.ifc")
public class LHOreduc implements Statistics, Serializable {

  private static final long serialVersionUID = -4736274363488699658L;

  @Option(
    secure = true,
    name = "pcc.lhoWriteFile",
    description = "file in which lho-Order will be stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  protected transient Path lhoOrderFile = Paths.get("lho.obj");

  private Map<CFANode, CFANode> dom = new TreeMap<>();
  private ArrayList<CFANode> lhoorderlist = new ArrayList<>();
  private Map<CFANode, List<CFANode>> rdom = new TreeMap<>();

  private CFANode entry;
  private CFANode exit;
  private int mode;

  private transient LogManager logger;

  // Temporary
  private CFA cfa;

  @SuppressWarnings("unused")
  public LHOreduc(
      LogManager pLogger,
      Configuration pConfig,
      CFA pCfa,
      int pMode,
      Map<CFANode, CFANode> dom)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    this.cfa = pCfa;
    this.logger = pLogger;
    this.entry = pCfa.getMainFunction();
    this.exit = ((FunctionEntryNode) entry).getExitNode();
    this.mode = pMode;
    this.dom = dom;
    /*
     * this.topOrder=new ArrayList<>(pCfa.getAllNodes().size()); for (int i = 0; i <
     * pCfa.getAllNodes().size() + 1; i++) { topOrder.add(null); }
     */
    for (Entry<CFANode, CFANode> entryPair : dom.entrySet()) {
      CFANode child = entryPair.getKey();
      CFANode parent = entryPair.getValue();

      if (parent != null) {
        if (!(rdom.containsKey(parent))) {
          rdom.put(parent, new ArrayList<CFANode>());
        }
        List<CFANode> childList = rdom.get(parent);
        childList.add(child);
      }
    }
  }

  /**
   * Execute the specified LHO-Order computation.
   */
  public void execute() {
    if (mode == 0) {
      lhoorderlist.add(entry);
      step(entry);
    } else {
      lhoorderlist.add(exit);
      step(exit);
    }
  }

  private void step(CFANode pNode) {
    logger.log(Level.FINE, pNode);
    List<CFANode> newN = new ArrayList<>();
    int pos = lhoorderlist.indexOf(pNode);
    //
    List<CFANode> sucessors = sucessors(pNode);
    List<CFANode> childList = new ArrayList<>();
    if (rdom.containsKey(pNode)) {
      childList = rdom.get(pNode);
    }
    for (CFANode succ : sucessors) {
      if (childList.contains(succ)) {
        if (!(lhoorderlist.contains(succ))) {
          /* (t(v),v) */
          lhoorderlist.add(pos + 1, succ);
          newN.add(succ);
        }
      } else {
        /* (u,v),(w,v) u<v<w */
        if (!(lhoorderlist.contains(succ))) {
          List<CFANode> siblings = predecessors(succ);
          if (siblings.size() >= 2) {
            CFANode sibling0 = siblings.get(0);
            CFANode sibling1 = siblings.get(1);
            if (lhoorderlist.indexOf(sibling0) < lhoorderlist.indexOf(sibling1)) {
              int pos2 = lhoorderlist.indexOf(sibling1);
              lhoorderlist.add(pos2 - 1, succ);
              newN.add(succ);
            } else {
              int pos2 = lhoorderlist.indexOf(sibling0);
              lhoorderlist.add(pos2 - 1, succ);
              newN.add(succ);
            }
          }
        }
      }
    }

    for (CFANode node : newN) {
      step(node);
    }
  }

  private List<CFANode> predecessors(CFANode first) {
    List<CFANode> result = new ArrayList<>();
    int m;
    if (mode == 1) {
      m = first.getNumLeavingEdges();
    } else {
      m = first.getNumEnteringEdges();
    }
    FunctionSummaryEdge e;
    CFANode w;
    if (mode == 1) {
      e = first.getLeavingSummaryEdge();
    } else {
      e = first.getEnteringSummaryEdge();
    }

    if (e != null) {
      if (mode == 1) {
        w = e.getSuccessor();
      } else {
        w = e.getPredecessor();
      }
    }
    // else{
    for (int i = 0; i < m; i++) {
      if (mode == 1) {
        w = first.getLeavingEdge(i).getSuccessor();
      } else {
        w = first.getEnteringEdge(i).getPredecessor();
      }
      if (!(result.contains(w))) {
        result.add(w);
      }
    }
    return result;
  }

  private List<CFANode> sucessors(CFANode first) {
    List<CFANode> result = new ArrayList<>();
    int m;
    if (mode == 0) {
      m = first.getNumLeavingEdges();
    } else {
      m = first.getNumEnteringEdges();
    }
    FunctionSummaryEdge e;
    CFANode w;
    if (mode == 0) {
      e = first.getLeavingSummaryEdge();
    } else {
      e = first.getEnteringSummaryEdge();
    }

    if (e != null) {
      if (mode == 0) {
        w = e.getSuccessor();
      } else {
        w = e.getPredecessor();
      }
    }
    // else{
    for (int i = 0; i < m; i++) {
      if (mode == 0) {
        w = first.getLeavingEdge(i).getSuccessor();
      } else {
        w = first.getEnteringEdge(i).getPredecessor();
      }
      result.add(w);
    }
    return result;
  }

  public boolean existsEdge(CFANode first, CFANode second) {
    int m;
    if (mode == 0) {
      m = first.getNumLeavingEdges();
    } else {
      m = first.getNumEnteringEdges();
    }
    FunctionSummaryEdge e;
    CFANode w;
    if (mode == 0) {
      e = first.getLeavingSummaryEdge();
    } else {
      e = first.getEnteringSummaryEdge();
    }

    if (e != null) {
      if (mode == 0) {
        w = e.getSuccessor();
      } else {
        w = e.getPredecessor();
      }
    }
    // else{
    for (int i = 0; i < m; i++) {
      if (mode == 0) {
        w = first.getLeavingEdge(i).getSuccessor();
      } else {
        w = first.getEnteringEdge(i).getPredecessor();
      }
      logger.log(Level.FINE, first + "," + second + "," + w);
      if (w.equals(second)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the Low-High-Order
   *
   * @return the computed map of Dominators
   */
  public ArrayList<CFANode> getLHO() {
    return lhoorderlist;
  }

  public Map<CFANode, CFANode> getDom() {
    return dom;
  }

  public Map<CFANode, List<CFANode>> getRDom() {
    return rdom;
  }

  public CFA getCFA() {
    return cfa;
  }

  static class NodeInfo {
    // CFANode node;
    public CFANode parent;
    public CFANode ancestor;
    public CFANode label;
    public Integer semi;
    public Collection<CFANode> pred;
    public Collection<CFANode> bucket;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    // TODO Auto-generated method stub
  }

  @Override
  public @Nullable String getName() {
    return "LHO-Order";
  }

  @SuppressFBWarnings(
    value = "OS_OPEN_STREAM",
    justification = "Do not close stream o because it wraps stream zos/fos which need to remain open and would be closed if o.close() is called.")
  public void writeLHOOrder() {

    Path dir = lhoOrderFile.getParent();

    try {
      if (dir != null) {
        Files.createDirectories(dir);
      }

      try (OutputStream o = Files.newOutputStream(lhoOrderFile);
          ObjectOutputStream out = new ObjectOutputStream(o);) {
        out.writeObject(this);
        out.flush();
        o.close();
      }

    } catch (NotSerializableException eS) {
      logger.log(
          Level.SEVERE,
          "LHOOrder cannot be written. Class "
              + eS.getMessage()
              + " does not implement Serializable interface");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static LHOreduc readLHOOrder(Path lhoOrderreadFile, LogManager pLogger) {
    Path dir = lhoOrderreadFile.getParent();

    try {
      if (dir != null) {
        Files.createDirectories(dir);
      }

      try (InputStream fis = Files.newInputStream(lhoOrderreadFile);
          ObjectInputStream in = new ObjectInputStream(fis);) {
        LHOreduc result = (LHOreduc) in.readObject();
        fis.close();
        return result;
      }

    } catch (NotSerializableException eS) {
      pLogger.log(
          Level.SEVERE,
          "LHOOrder cannot be written. Class "
              + eS.getMessage()
              + " does not implement Serializable interface");
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      pLogger.log(Level.FINE, e);
    }
    return null;
  }

  public boolean checkForTreeandCycle(CFA pCfa) {
    Set<CFANode> visited = new TreeSet<>();
    Queue<CFANode> queue = new LinkedList<>();
    // CFANode nExit = pCfa.getMainFunction().getExitNode();
    int nNodes = pCfa.getAllNodes().size();
    queue.add(exit);
    while (queue.size() != 0) {
      CFANode node = queue.poll();
      if (visited.contains(node)) {
        return false;
      }
      visited.add(node);
      if (rdom.containsKey(node)) {
        for (CFANode succ : rdom.get(node)) {

          queue.add(succ);
        }
      }
    }
    // Contains all nodes
    if (!(visited.size() == nNodes)) {
      return false;
    }

    return true;

  }

  private Map<CFANode, Interval> intervals;

  public boolean doDFS(CFA pCfa) {
    intervals = new TreeMap<>();
    Set<CFANode> visited = new TreeSet<>();
    Deque<CFANode> stack = new ArrayDeque<>();
    CFANode nExit = pCfa.getMainFunction().getExitNode();
    int nNodes = pCfa.getAllNodes().size();
    Interval inv = new Interval((long) 0, (long) lhoorderlist.size());
    intervals.put(nExit, inv);
    stack.add(exit);
    // int i = 0;
    while (stack.size() != 0) {

      CFANode node = stack.pop();
      inv = intervals.get(node);
      // long min = inv.getLow();
      long max = inv.getHigh();
      if (visited.contains(node)) {
        return false;
      }
      visited.add(node);
      if (rdom.containsKey(node)) {
        TreeSet<Integer> positions = new TreeSet<>();

        for (CFANode succ : rdom.get(node)) {
          positions.add(lhoorderlist.indexOf(succ));
        }
        List<Integer> positions2 = new ArrayList<>(positions);
        long r;
        long l = max; //
        for (Integer j = positions2.size() - 1; j >= 0; j--) {
          Integer lhoPos = positions2.get(j);
          stack.push(lhoorderlist.get(lhoPos));
          r = l;
          l = lhoPos;
          inv = new Interval(l, r);
          intervals.put(lhoorderlist.get(lhoPos), inv);
        }
      }
    }
    // Contains all nodes
    if (!(visited.size() == nNodes)) {
      return false;
    }

    return true;

  }

  public Map<CFANode, Interval> getAncestors() {
    return intervals;
  }

}
