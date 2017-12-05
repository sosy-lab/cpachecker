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
import org.sosy_lab.common.configuration.Configuration;
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
    LOOP_SAME_CONTEXT
  }

  private interface MergeUpdateFunction {
    public void updateAfterMerging(ARGState merged, ARGState mergedInto);
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

  private static class LoopAlwaysFolder extends ConditionFolder {

    private final CFA cfa;

    public LoopAlwaysFolder(final CFA pCfa) {
      cfa = pCfa;
    }

    @Override
    public ARGState foldARG(final ARGState pRoot) {
      Preconditions.checkState(cfa.getAllLoopHeads().isPresent());
      Set<CFANode> loopHeads = cfa.getAllLoopHeads().get();

      Map<ARGState, ARGState> oldARGToFoldedState = new HashMap<>();
      Map<ARGState, Set<ARGState>> newARGToFoldedStates = new HashMap<>();
      Map<Pair<CFANode, CallstackStateEqualsWrapper>, ARGState> loopHeadToFoldedARGState =
          new HashMap<>();

      MergeUpdateFunction update = (merge, mergeInto) -> {
        for (ARGState oldState : newARGToFoldedStates.remove(merge)) {
          oldARGToFoldedState.put(oldState, mergeInto);
          newARGToFoldedStates.get(mergeInto).add(oldState);
        }
      };

      Deque<ARGState> waitlist = new ArrayDeque<>();
      ARGState foldedNode;
      Set<ARGState> foldedStates;
      ARGState oldState, newState, newChild;
      CFANode loc;
      CFAEdge edge;
      Pair<CFANode, CallstackStateEqualsWrapper> inlinedLoc;

      foldedNode = new ARGState(pRoot.getWrappedState(), null);
      oldARGToFoldedState.put(pRoot, foldedNode);
      foldedStates = new HashSet<>();
      foldedStates.add(pRoot);
      newARGToFoldedStates.put(foldedNode, foldedStates);
      loc = AbstractStates.extractLocation(pRoot);
      if (loopHeads.contains(loc)) {
        loopHeadToFoldedARGState.put(
            Pair.of(
                loc,
                new CallstackStateEqualsWrapper(
                    AbstractStates.extractStateByType(pRoot, CallstackState.class))),
            foldedNode);
      }
      waitlist.push(pRoot);

      while (!waitlist.isEmpty()) {
        oldState = waitlist.pop();

        for (ARGState child : oldState.getChildren()) {
          loc = AbstractStates.extractLocation(child);
          edge = oldState.getEdgeToChild(child);
          child = getUncoveredChild(child);

          if (!oldARGToFoldedState.containsKey(child)) {
            if (loopHeads.contains(loc)) {
              inlinedLoc =
                  Pair.of(
                      loc,
                      new CallstackStateEqualsWrapper(
                          AbstractStates.extractStateByType(child, CallstackState.class)));
              foldedNode = loopHeadToFoldedARGState.get(inlinedLoc);
              if (foldedNode == null) {
                foldedNode = new ARGState(child.getWrappedState(), null);
                foldedStates = new HashSet<>();
                newARGToFoldedStates.put(foldedNode, foldedStates);
                loopHeadToFoldedARGState.put(inlinedLoc, foldedNode);
              }

            } else {
              foldedNode = null;
              newState = oldARGToFoldedState.get(oldState);
              for (ARGState newARGChild : newState.getChildren()) {
                if (edge != null && edge.equals(newState.getEdgeToChild(newARGChild))) {
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
            }

            oldARGToFoldedState.put(child, foldedNode);
            newARGToFoldedStates.get(foldedNode).add(child);
            waitlist.push(child);

          } else {
            newState = oldARGToFoldedState.get(oldState);
            newChild = null;
            for (ARGState newARGChild : newState.getChildren()) {
              if (edge != null && edge.equals(newState.getEdgeToChild(newARGChild))) {
                newChild = newARGChild;
                // there should be only one such child, thus break
                break;
              }
            }

            if (newChild != null && newChild != oldARGToFoldedState.get(child)) {
              merge(
                  newChild,
                  oldARGToFoldedState.get(child),
                  update);
            }
          }

          newChild = oldARGToFoldedState.get(child);
          newChild.addParent(oldARGToFoldedState.get(oldState));
        }
      }

      return oldARGToFoldedState.get(pRoot);
    }
  }

  private static class ContextLoopFolder extends ConditionFolder {
    private final Map<CFANode, Loop> loopMap;

    private final CFA cfa;

    private ContextLoopFolder(final CFA pCfa) {
      cfa = pCfa;

      loopMap = buildLoopMap();
    }

    private Map<CFANode, Loop> buildLoopMap() {
      Map<CFANode, Loop> loopMap = Maps.newHashMapWithExpectedSize(cfa.getAllNodes().size());

      Deque<Pair<CFANode, List<Loop>>> toVisit = new ArrayDeque<>();
      toVisit.push(Pair.of(cfa.getMainFunction(), Collections.emptyList()));
      loopMap.put(cfa.getMainFunction(), null);
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
          if (loopMap.containsKey(edge.getSuccessor())) {
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

          loopMap.put(edge.getSuccessor(), lsucc);
          toVisit.push(Pair.of(edge.getSuccessor(), succLoopStack));
        }
      }
      return loopMap;
    }

    @Override
    public ARGState foldARG(final ARGState pRoot) {
      Preconditions.checkState(cfa.getAllLoopHeads().isPresent());
      Set<CFANode> loopHeads = cfa.getAllLoopHeads().get();

      Map<ARGState, ARGState> oldARGToFoldedState = new HashMap<>();
      Map<ARGState, Set<ARGState>> newARGToFoldedStates = new HashMap<>();
      // takes function call context and taken branches into account
      Map<String, ARGState> loopContextToFoldedARGState = new HashMap<>();
      Map<ARGState, Set<String>> foldedARGStateToLoopContexts = new HashMap<>();

      MergeUpdateFunction update =
          (merge, mergeInto) -> {
            for (ARGState oldState : newARGToFoldedStates.remove(merge)) {
              oldARGToFoldedState.put(oldState, mergeInto);
              newARGToFoldedStates.get(mergeInto).add(oldState);
            }
            if (foldedARGStateToLoopContexts.containsKey(merge)) {
              for (String loopContext : foldedARGStateToLoopContexts.remove(merge)) {
                loopContextToFoldedARGState.put(loopContext, mergeInto);
                foldedARGStateToLoopContexts.get(mergeInto).add(loopContext);
              }
            }
          };

      Deque<Pair<ARGState, String>> waitlist = new ArrayDeque<>();
      ARGState foldedNode;
      Set<ARGState> foldedStates;
      Set<String> loopContexts;
      ARGState oldState, newState, newChild;
      CFANode loc, locChild;
      CFAEdge edge;
      String loopContext, loopContextChild;

      foldedNode = new ARGState(pRoot.getWrappedState(), null);
      oldARGToFoldedState.put(pRoot, foldedNode);
      foldedStates = new HashSet<>();
      foldedStates.add(pRoot);
      newARGToFoldedStates.put(foldedNode, foldedStates);
      loc = AbstractStates.extractLocation(pRoot);
      if (loopHeads.contains(loc)) {
        loopContextToFoldedARGState.put("", foldedNode);
        loopContexts = new HashSet<>();
        loopContexts.add("");
        foldedARGStateToLoopContexts.put(foldedNode, loopContexts);
      }
      waitlist.push(Pair.of(pRoot, ""));

      while (!waitlist.isEmpty()) {
        oldState = waitlist.peek().getFirst();
        loopContext = waitlist.pop().getSecond();
        loc = AbstractStates.extractLocation(oldState);

        for (ARGState child : oldState.getChildren()) {
          locChild = AbstractStates.extractLocation(child);
          edge = oldState.getEdgeToChild(child);
          child = getUncoveredChild(child);

          loopContextChild = extendLoopContext(edge, loopContext);

          if (!oldARGToFoldedState.containsKey(child)) {
            if (loopHeads.contains(locChild)
                && loopContextToFoldedARGState.containsKey(loopContextChild)) {
              foldedNode = loopContextToFoldedARGState.get(loopContextChild);
              assert (locChild == AbstractStates.extractLocation(foldedNode));
            } else {
              foldedNode = null;
              newState = oldARGToFoldedState.get(oldState);
              for (ARGState newARGChild : newState.getChildren()) {
                if (edge != null && edge.equals(newState.getEdgeToChild(newARGChild))) {
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
                loopContextToFoldedARGState.put(loopContextChild, foldedNode);
                if (!foldedARGStateToLoopContexts.containsKey(foldedNode)) {
                  foldedARGStateToLoopContexts.put(foldedNode, new HashSet<>());
                }
                foldedARGStateToLoopContexts.get(foldedNode).add(loopContextChild);
              }
            }

            oldARGToFoldedState.put(child, foldedNode);
            newARGToFoldedStates.get(foldedNode).add(child);
            waitlist.push(Pair.of(child, loopContextChild));

          } else {
            newState = oldARGToFoldedState.get(oldState);
            newChild = null;
            for (ARGState newARGChild : newState.getChildren()) {
              if (edge != null && edge.equals(newState.getEdgeToChild(newARGChild))) {
                newChild = newARGChild;
                // there should be only one such child, thus break
                break;
              }
            }

            if (newChild != null && newChild != oldARGToFoldedState.get(child)) {
              merge(
                  newChild,
                  oldARGToFoldedState.get(child),
                 update);
            }
          }

          newChild = oldARGToFoldedState.get(child);
          newChild.addParent(oldARGToFoldedState.get(oldState));
        }
      }

      return oldARGToFoldedState.get(pRoot);
    }

    private String extendLoopContext(final CFAEdge pEdge, final String pLoopContext) {
      String newLoopContext = pLoopContext;
      // leave loop
      if (leaveLoop(pEdge) && newLoopContext.contains("|")) {
        newLoopContext = newLoopContext.substring(0, newLoopContext.lastIndexOf("|"));
      }

      // next loop iteration
      if (startNewLoopIteation(pEdge) && newLoopContext.contains("|")) {
        newLoopContext = newLoopContext.substring(0, newLoopContext.lastIndexOf("|"));
      }

      if (pEdge instanceof FunctionReturnEdge && newLoopContext.contains("/")) {
        newLoopContext =
            newLoopContext.substring(newLoopContext.indexOf("/") + 1, newLoopContext.length());
      }
      if (pEdge instanceof FunctionCallEdge) {
        newLoopContext =
            ((FunctionCallEdge) pEdge).getSuccessor().getFunctionName() + "/" + newLoopContext;
      }

      // enter loop or start next iteration
      if (cfa.getAllLoopHeads().get().contains(pEdge.getSuccessor())) {
        newLoopContext += "|";
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

    private boolean leaveLoop(final CFAEdge pEdge) {
      Loop l = loopMap.get(pEdge.getPredecessor());
      return l != null
          && !(pEdge instanceof CFunctionCallEdge)
          && !(pEdge instanceof CFunctionReturnEdge)
          && l != loopMap.get(pEdge.getSuccessor())
          && !l.getLoopNodes().contains(pEdge.getSuccessor());
    }

    private boolean startNewLoopIteation(final CFAEdge pEdge) {
      if (cfa.getAllLoopHeads().get().contains(pEdge.getSuccessor())) {
        if (loopMap.get(pEdge.getPredecessor()) == loopMap.get(pEdge.getSuccessor())) {
          return true;
        }
      }
      return false;
    }
  }
}
