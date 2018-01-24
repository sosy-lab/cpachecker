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
package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public abstract class ConditionFolder {

  public enum FOLDER_TYPE {
    CFA,
    LOOP_ALWAYS,
    LOOP_BOUND,
    LOOP_SAME_CONTEXT
  }

  private interface MergeUpdateFunction {
    public void updateAfterMerging(ARGState merged, ARGState mergedInto);
  }

  private static class LoopInfo {

    private final CFA cfa;
    private final Map<CFANode, Loop> loopMap;

    public LoopInfo(final CFA pCfa) {
      cfa = pCfa;
      loopMap = buildLoopMap();
    }

    private Map<CFANode, Loop> buildLoopMap() {
      Map<CFANode, Loop> loopMapResult = Maps.newHashMapWithExpectedSize(cfa.getAllNodes().size());

      Deque<Pair<CFANode, List<Loop>>> toVisit = new ArrayDeque<>();
      toVisit.push(Pair.of(cfa.getMainFunction(), Collections.emptyList()));
      loopMapResult.put(cfa.getMainFunction(), null);
      List<Loop> loopStack, succLoopStack;
      CFANode node;
      Loop l, lsucc;

      while (!toVisit.isEmpty()) {
        node = toVisit.peek().getFirst();
        loopStack = toVisit.pop().getSecond();
        if (loopStack.isEmpty()) {
          l = null;
        } else {
          l = loopStack.get(loopStack.size() - 1);
        }

        for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
          if (loopMapResult.containsKey(edge.getSuccessor())) {
            continue;
          }

          succLoopStack = loopStack;
          lsucc = l;

          if (edge instanceof CFunctionReturnEdge) {
            continue; // successor treated by FunctionSummaryEdge
          }

          if (edge instanceof CFunctionCallEdge) {
            lsucc = null;
            succLoopStack = Collections.emptyList();
          }

          while (lsucc != null && lsucc.getOutgoingEdges().contains(edge)) {
            // leave edge
            succLoopStack = new ArrayList<>(succLoopStack);
            succLoopStack.remove(succLoopStack.size() - 1);
            if (succLoopStack.isEmpty()) {
              lsucc = null;
            } else {
              lsucc = succLoopStack.get(succLoopStack.size() - 1);
            }
          }

          if (cfa.getAllLoopHeads().get().contains(edge.getSuccessor())) {
            Set<Loop> loop = cfa.getLoopStructure().get().getLoopsForLoopHead(edge.getSuccessor());
            assert (loop.size() >= 1);
            lsucc = loop.iterator().next();

            if (succLoopStack == loopStack) {
              succLoopStack = new ArrayList<>(succLoopStack);
            }
            succLoopStack.add(lsucc);
          }

          loopMapResult.put(edge.getSuccessor(), lsucc);
          toVisit.push(Pair.of(edge.getSuccessor(), succLoopStack));
        }
      }
      return loopMapResult;
    }

    public boolean leaveLoop(final CFAEdge pEdge) {
      Loop l = loopMap.get(pEdge.getPredecessor());
      return l != null
          && !(pEdge instanceof CFunctionCallEdge)
          && !(pEdge instanceof CFunctionReturnEdge)
          && l != loopMap.get(pEdge.getSuccessor())
          && !l.getLoopNodes().contains(pEdge.getSuccessor());
    }

    public boolean startNewLoopIteation(final CFAEdge pEdge) {
      if (cfa.getAllLoopHeads().get().contains(pEdge.getSuccessor())) {
        if (loopMap.get(pEdge.getPredecessor()) == loopMap.get(pEdge.getSuccessor())) {
          return true;
        }
      }
      return false;
    }
  }

  @Options(prefix = "residualprogram")
  private static class FolderOptions {

    @Option(
      secure = true,
      description =
          "Define kind of folder to use when combining condition with folding approach in residual program generation"
    )
    private FOLDER_TYPE folderType = FOLDER_TYPE.CFA;
  }

  public static ConditionFolder createFolder(Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {
    FolderOptions opt = new FolderOptions();
    pConfig.inject(opt);

    switch (opt.folderType) {
      case CFA:
        return new CFAFolder();
      case LOOP_ALWAYS:
        return new LoopAlwaysFolder(pCfa);
      case LOOP_BOUND:
        return new BoundUnrollingLoopFolder(pCfa, pConfig);
      case LOOP_SAME_CONTEXT:
        return new ContextLoopFolder(pCfa);
      default:
        throw new AssertionError("Unknown condition folder.");
    }
  }

  public abstract ARGState foldARG(final ARGState pARGRoot);

  protected ARGState getUncoveredChild(ARGState pChild) {
    while (pChild.isCovered()) {
      pChild = pChild.getCoveringState();
    }
    return pChild;
  }

  protected void merge(
      final ARGState newState1, final ARGState newState2, final MergeUpdateFunction updateFun) {

    Map<ARGState, ARGState> mergedInto = new HashMap<>();
    Deque<Pair<ARGState, ARGState>> toMerge = new ArrayDeque<>();
    toMerge.push(Pair.of(newState1, newState2));
    ARGState merge, mergeInto;

    while (!toMerge.isEmpty()) {
      merge = toMerge.peek().getFirst();
      while (mergedInto.containsKey(merge)) {
        merge = mergedInto.get(merge);
      }
      mergeInto = toMerge.pop().getSecond();
      while (mergedInto.containsKey(mergeInto)) {
        mergeInto = mergedInto.get(mergeInto);
      }

      if (merge == mergeInto) {
        continue;
      }

      for (ARGState child : merge.getChildren()) {
        for (ARGState ch : mergeInto.getChildren()) {
          if (merge.getEdgeToChild(child) != null
              && merge.getEdgeToChild(child).equals(mergeInto.getEdgeToChild(ch))
              && ch != child) {
            toMerge.add(Pair.of(child, ch));
          }
        }
      }

      merge.replaceInARGWith(mergeInto);
      mergedInto.put(merge, mergeInto);
      updateFun.updateAfterMerging(merge, mergeInto);
    }
  }

  private static class CFAFolder extends ConditionFolder {

    @Override
    public ARGState foldARG(ARGState pARGRoot) {
      ARGState newRoot;
      Map<Pair<LocationState, CallstackStateEqualsWrapper>, ARGState> foldedNodesToNewARGNode =
          new HashMap<>();

      Set<ARGState> seen = new HashSet<>();
      Deque<Pair<ARGState, Pair<LocationState, CallstackStateEqualsWrapper>>> toProcess =
          new ArrayDeque<>();
      ARGState currentARGState, newChild;
      Pair<LocationState, CallstackStateEqualsWrapper> foldedNode, foldedChild;

      foldedNode =
          Pair.of(
              AbstractStates.extractStateByType(pARGRoot, LocationState.class),
              new CallstackStateEqualsWrapper(
                  AbstractStates.extractStateByType(pARGRoot, CallstackState.class)));
      seen.add(pARGRoot);
      toProcess.add(Pair.of(pARGRoot, foldedNode));

      newRoot = new ARGState(foldedNode.getFirst(), null);
      foldedNodesToNewARGNode.put(foldedNode, newRoot);

      while (!toProcess.isEmpty()) {
        currentARGState = toProcess.peek().getFirst();
        foldedNode = toProcess.pop().getSecond();

        for (ARGState child : currentARGState.getChildren()) {
          if (seen.add(child)) {
            foldedChild =
                Pair.of(
                    AbstractStates.extractStateByType(child, LocationState.class),
                    new CallstackStateEqualsWrapper(
                        AbstractStates.extractStateByType(child, CallstackState.class)));
            toProcess.add(Pair.of(child, foldedChild));

            newChild = foldedNodesToNewARGNode.get(foldedChild);

            if (newChild == null) {
              newChild =
                  new ARGState(foldedChild.getFirst(), foldedNodesToNewARGNode.get(foldedNode));
              foldedNodesToNewARGNode.put(foldedChild, newChild);
            } else {
              if (!foldedNodesToNewARGNode.get(foldedNode).getChildren().contains(newChild)) {
                newChild.addParent(foldedNodesToNewARGNode.get(foldedNode));
              }
            }
          }
        }
      }

      return newRoot;
    }
  }

  private abstract static class LoopFolder<T> extends ConditionFolder {
    protected final CFA cfa;
    protected final Set<CFANode> loopHeads;

    public LoopFolder(final CFA pCfa) {
      cfa = pCfa;
      Preconditions.checkState(cfa.getAllLoopHeads().isPresent());
      loopHeads = cfa.getAllLoopHeads().get();
    }

    protected abstract T getRootFoldId(final ARGState pRoot);

    protected abstract T adaptID(CFAEdge pEdge, T pFoldID, ARGState pChild);

    @Override
    public ARGState foldARG(final ARGState pRoot) {


      Map<ARGState, ARGState> oldARGToFoldedState = new HashMap<>();
      Map<ARGState, Set<ARGState>> newARGToFoldedStates = new HashMap<>();
      // takes function call context and taken branches into account
      Map<T, ARGState> loopHeadFoldIDToFoldedARGState = new HashMap<>();
      Map<ARGState, Set<T>> foldedARGStateToFoldIDs = new HashMap<>();

      MergeUpdateFunction update =
          (merge, mergeInto) -> {
            for (ARGState oldState : newARGToFoldedStates.remove(merge)) {
              oldARGToFoldedState.put(oldState, mergeInto);
              newARGToFoldedStates.get(mergeInto).add(oldState);
            }
            if (foldedARGStateToFoldIDs.containsKey(merge)) {
              for (T foldID : foldedARGStateToFoldIDs.remove(merge)) {
                loopHeadFoldIDToFoldedARGState.put(foldID, mergeInto);
                foldedARGStateToFoldIDs.get(mergeInto).add(foldID);
              }
            }
          };

      Deque<Pair<ARGState, T>> waitlist = new ArrayDeque<>();
      ARGState foldedNode;
      Set<ARGState> foldedStates;
      Set<T> loopContexts;
      ARGState oldState, newState, newChild;
      CFANode loc, locChild;
      CFAEdge edge;
      T foldID, foldIDChild;

      foldedNode = new ARGState(pRoot.getWrappedState(), null);
      oldARGToFoldedState.put(pRoot, foldedNode);
      foldedStates = new HashSet<>();
      foldedStates.add(pRoot);
      newARGToFoldedStates.put(foldedNode, foldedStates);
      loc = AbstractStates.extractLocation(pRoot);
      T id = getRootFoldId(pRoot);
      if (loopHeads.contains(loc)) {
        loopHeadFoldIDToFoldedARGState.put(id, foldedNode);
        loopContexts = new HashSet<>();
        loopContexts.add(id);
        foldedARGStateToFoldIDs.put(foldedNode, loopContexts);
      }
      waitlist.push(Pair.of(pRoot, id));

      while (!waitlist.isEmpty()) {
        oldState = waitlist.peek().getFirst();
        foldID = waitlist.pop().getSecond();
        loc = AbstractStates.extractLocation(oldState);

        for (ARGState child : oldState.getChildren()) {
          locChild = AbstractStates.extractLocation(child);
          edge = oldState.getEdgeToChild(child);
          child = getUncoveredChild(child);

          foldIDChild = adaptID(edge, foldID, child);

          if (!oldARGToFoldedState.containsKey(child)) {
            if (loopHeads.contains(locChild)
                && loopHeadFoldIDToFoldedARGState.containsKey(foldIDChild)) {
              foldedNode = loopHeadFoldIDToFoldedARGState.get(foldIDChild);
              assert (locChild == AbstractStates.extractLocation(foldedNode));
            } else {
              foldedNode = null;
              newState = oldARGToFoldedState.get(oldState);
              for (ARGState newARGChild : newState.getChildren()) {
                if (edge.equals(newState.getEdgeToChild(newARGChild))) {
                  foldedNode = newARGChild;
                  // there should be only one such child, thus break
                  break;
                }
              }

              if (foldedNode == null) {
                foldedNode = new ARGState(child.getWrappedState(), null);
                foldedStates = new HashSet<>();
                newARGToFoldedStates.put(foldedNode, foldedStates);
              }

              if (loopHeads.contains(locChild)) {
                loopHeadFoldIDToFoldedARGState.put(foldIDChild, foldedNode);
                if (!foldedARGStateToFoldIDs.containsKey(foldedNode)) {
                  foldedARGStateToFoldIDs.put(foldedNode, new HashSet<>());
                }
                foldedARGStateToFoldIDs.get(foldedNode).add(foldIDChild);
              }
            }

            oldARGToFoldedState.put(child, foldedNode);
            newARGToFoldedStates.get(foldedNode).add(child);
            waitlist.push(Pair.of(child, foldIDChild));

          } else {
            newState = oldARGToFoldedState.get(oldState);
            newChild = null;
            for (ARGState newARGChild : newState.getChildren()) {
              if (edge.equals(newState.getEdgeToChild(newARGChild))) {
                newChild = newARGChild;
                // there should be only one such child, thus break
                break;
              }
            }

            if (newChild != null && newChild != oldARGToFoldedState.get(child)) {
              merge(newChild, oldARGToFoldedState.get(child), update);
            }
          }

          newChild = oldARGToFoldedState.get(child);
          newChild.addParent(oldARGToFoldedState.get(oldState));
        }
      }

      return oldARGToFoldedState.get(pRoot);
    }
  }

  private static class LoopAlwaysFolder extends LoopFolder<Pair<CFANode, CallstackStateEqualsWrapper>> {

    public LoopAlwaysFolder(final CFA pCfa) {
      super(pCfa);
    }

    @Override
    protected @Nullable Pair<CFANode, CallstackStateEqualsWrapper> adaptID(
        final CFAEdge pEdge,
        final Pair<CFANode, CallstackStateEqualsWrapper> pFoldID,
        final ARGState child) {
      return getID(pEdge.getSuccessor(), child);
    }

    @Override
    protected @Nullable Pair<CFANode, CallstackStateEqualsWrapper> getRootFoldId(
        final ARGState pRoot) {
      return getID(AbstractStates.extractLocation(pRoot), pRoot);
    }

    private @Nullable Pair<CFANode, CallstackStateEqualsWrapper> getID(
        final CFANode node, final ARGState argNode) {
      if (loopHeads.contains(node)) {
        return Pair.of(
            node,
            new CallstackStateEqualsWrapper(
                AbstractStates.extractStateByType(argNode, CallstackState.class)));
      }

      return null;
    }
  }

  private static class ContextLoopFolder extends LoopFolder<String> {
    private final LoopInfo loopInfo;

    private ContextLoopFolder(final CFA pCfa) {
      super(pCfa);

      loopInfo = new LoopInfo(cfa);
    }

    private String extendLoopContext(final CFAEdge pEdge, final String pLoopContext) {
      String newLoopContext = pLoopContext;
      // leave loop
      if (loopInfo.leaveLoop(pEdge) && newLoopContext.contains("|")) {
        newLoopContext = newLoopContext.substring(0, newLoopContext.lastIndexOf("|"));
      }

      // next loop iteration
      if (loopInfo.startNewLoopIteation(pEdge) && newLoopContext.contains("|")) {
        newLoopContext = newLoopContext.substring(0, newLoopContext.lastIndexOf("|"));
      }

      if (pEdge instanceof FunctionReturnEdge && newLoopContext.contains("/")) {
        newLoopContext = newLoopContext.substring(0, newLoopContext.lastIndexOf("/"));
      }
      if (pEdge instanceof FunctionCallEdge) {
        newLoopContext =
            newLoopContext
                + "/"
                + "N"
                + ((FunctionCallEdge) pEdge).getPredecessor().getNodeNumber()
                + "N";
      }

      // enter loop or start next iteration
      if (cfa.getAllLoopHeads().get().contains(pEdge.getSuccessor())) {
        newLoopContext += "|L" + pEdge.getSuccessor().getNodeNumber() + "L";
      }

      if (pEdge instanceof AssumeEdge) {
        if (((AssumeEdge) pEdge).getTruthAssumption()) {
          newLoopContext += "1";
        } else {
          newLoopContext += "0";
        }
      }

      return newLoopContext;
    }

    @Override
    protected String getRootFoldId(final ARGState pRoot) {
      CFANode rootLoc = AbstractStates.extractLocation(pRoot);
      if (cfa.getAllLoopHeads().get().contains(rootLoc)) {
        return "|L" + rootLoc.getNodeNumber() + "L";
      }
      return "";
    }

    @Override
    protected String adaptID(
        final CFAEdge pEdge, final String pLoopContext, final ARGState pChild) {
      return extendLoopContext(pEdge, pLoopContext);
    }
  }

  @Options(prefix = "residualprogram")
  private static class BoundUnrollingLoopFolder extends LoopFolder<String> {

    private final LoopInfo loopInfo;

    @Option(
      secure = true,
      description = "How often may a loop be unrolled before it must be folded",
      name = "unrollBound"
    )
    @IntegerOption(min = 2)
    private int maxUnrolls = 2;

    private BoundUnrollingLoopFolder(final CFA pCfa, final Configuration pConfig)
        throws InvalidConfigurationException {
      super(pCfa);
      pConfig.inject(this);
      loopInfo = new LoopInfo(pCfa);
    }

    @Override
    protected String getRootFoldId(final ARGState pRoot) {
      CFANode rootLoc = AbstractStates.extractLocation(pRoot);
      if (cfa.getAllLoopHeads().get().contains(rootLoc)) {
        return "|" + rootLoc.getNodeNumber() + ":1";
      }
      return "";
    }

    @Override
    protected String adaptID(CFAEdge pEdge, String pFoldID, ARGState pChild) {
      String newLoopBoundID = pFoldID;
      int prevLoopIt = 0;

      // leave loop
      if (loopInfo.leaveLoop(pEdge) && newLoopBoundID.contains("|")) {
        newLoopBoundID = newLoopBoundID.substring(0, newLoopBoundID.lastIndexOf("|"));
      }

      // next loop iteration
      if (loopInfo.startNewLoopIteation(pEdge) && newLoopBoundID.contains("|")) {
        prevLoopIt =
            Integer.parseInt(
                newLoopBoundID.substring(
                    newLoopBoundID.lastIndexOf(":") + 1, newLoopBoundID.length()));
        newLoopBoundID = newLoopBoundID.substring(0, newLoopBoundID.lastIndexOf("|"));
      }

      if (pEdge instanceof FunctionReturnEdge && newLoopBoundID.contains("/")) {
        newLoopBoundID = newLoopBoundID.substring(0, newLoopBoundID.lastIndexOf("/"));
      }
      if (pEdge instanceof FunctionCallEdge) {
        newLoopBoundID =
            newLoopBoundID
                + "/"
                + "N"
                + ((FunctionCallEdge) pEdge).getPredecessor().getNodeNumber()
                + "N";
      }

      // enter loop or start next iteration
      if (cfa.getAllLoopHeads().get().contains(pEdge.getSuccessor())) {
        newLoopBoundID +=
            "|" + pEdge.getSuccessor().getNodeNumber() + ":" + Math.max(prevLoopIt + 1, maxUnrolls);
      }

      return newLoopBoundID;
    }

  }
}
