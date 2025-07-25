// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
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

  public enum FolderType {
    CFA,
    FOLD_EXCEPT_LOOPS,
    LOOP_ALWAYS,
    LOOP_BOUND,
    LOOP_BOUND_SAME_CONTEXT,
    LOOP_SAME_CONTEXT
  }

  @FunctionalInterface
  private interface MergeUpdateFunction {
    void updateAfterMerging(ARGState merged, ARGState mergedInto);
  }

  private static class LoopInfo {

    private final CFA cfa;
    private final Map<CFANode, Loop> loopMap;

    LoopInfo(final CFA pCfa) {
      cfa = pCfa;
      loopMap = buildLoopMap();
    }

    private Map<CFANode, Loop> buildLoopMap() {
      Map<CFANode, Loop> loopMapResult = Maps.newHashMapWithExpectedSize(cfa.nodes().size());

      Deque<Pair<CFANode, List<Loop>>> toVisit = new ArrayDeque<>();
      toVisit.push(Pair.of(cfa.getMainFunction(), ImmutableList.of()));
      loopMapResult.put(cfa.getMainFunction(), null);

      while (!toVisit.isEmpty()) {
        CFANode node = toVisit.peek().getFirst();
        List<Loop> loopStack = toVisit.pop().getSecond();
        Loop l;
        if (loopStack.isEmpty()) {
          l = null;
        } else {
          l = loopStack.get(loopStack.size() - 1);
        }

        for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
          if (loopMapResult.containsKey(edge.getSuccessor())) {
            continue;
          }

          List<Loop> succLoopStack = loopStack;
          Loop lsucc = l;

          if (edge instanceof CFunctionReturnEdge) {
            continue; // successor treated by FunctionSummaryEdge
          }

          if (edge instanceof CFunctionCallEdge) {
            lsucc = null;
            succLoopStack = ImmutableList.of();
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

          if (cfa.getAllLoopHeads().orElseThrow().contains(edge.getSuccessor())) {
            Set<Loop> loop =
                cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(edge.getSuccessor());
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

    boolean leaveLoop(final CFAEdge pEdge) {
      Loop l = loopMap.get(pEdge.getPredecessor());
      return l != null
          && !(pEdge instanceof CFunctionCallEdge)
          && !(pEdge instanceof CFunctionReturnEdge)
          && l != loopMap.get(pEdge.getSuccessor())
          && !l.getLoopNodes().contains(pEdge.getSuccessor());
    }

    boolean startNewLoopIteation(final CFAEdge pEdge) {
      if (cfa.getAllLoopHeads().orElseThrow().contains(pEdge.getSuccessor())) {
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
            "Define kind of folder to use when combining condition with folding approach in"
                + " residual program generation")
    private FolderType folderType = FolderType.CFA;
  }

  public static ConditionFolder createFolder(Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {
    FolderOptions opt = new FolderOptions();
    pConfig.inject(opt);

    return switch (opt.folderType) {
      case CFA -> new CFAFolder();
      case FOLD_EXCEPT_LOOPS -> new ExceptLoopFolder(pCfa);
      case LOOP_ALWAYS -> new LoopAlwaysFolder(pCfa);
      case LOOP_BOUND -> new BoundUnrollingLoopFolder(pCfa, pConfig);
      case LOOP_BOUND_SAME_CONTEXT -> new BoundUnrollingContextLoopFolder(pCfa, pConfig);
      case LOOP_SAME_CONTEXT -> new ContextLoopFolder(pCfa);
    };
  }

  private final FolderType type;

  protected ConditionFolder(final FolderType pType) {
    type = pType;
  }

  public abstract ARGState foldARG(final ARGState pARGRoot);

  public FolderType getType() {
    return type;
  }

  protected ARGState getUncoveredChild(ARGState pChild) {
    while (pChild.isCovered()) {
      pChild = pChild.getCoveringState();
    }
    return pChild;
  }

  @SuppressWarnings("checkstyle:PublicReferenceToPrivateType")
  protected void merge(
      final ARGState newState1, final ARGState newState2, final MergeUpdateFunction updateFun) {

    Map<ARGState, ARGState> mergedInto = new HashMap<>();
    Deque<Pair<ARGState, ARGState>> toMerge = new ArrayDeque<>();
    toMerge.push(Pair.of(newState1, newState2));

    while (!toMerge.isEmpty()) {
      ARGState merge = toMerge.peek().getFirst();
      while (mergedInto.containsKey(merge)) {
        merge = mergedInto.get(merge);
      }
      ARGState mergeInto = toMerge.pop().getSecond();
      while (mergedInto.containsKey(mergeInto)) {
        mergeInto = mergedInto.get(mergeInto);
      }

      if (merge.equals(mergeInto)) {
        continue;
      }

      for (ARGState child : merge.getChildren()) {
        for (ARGState ch : mergeInto.getChildren()) {
          if (merge.getEdgeToChild(child) != null
              && merge.getEdgeToChild(child).equals(mergeInto.getEdgeToChild(ch))
              && !Objects.equals(ch, child)) {
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

    CFAFolder() {
      super(FolderType.CFA);
    }

    @Override
    public ARGState foldARG(ARGState pARGRoot) {
      Map<Pair<LocationState, CallstackStateEqualsWrapper>, ARGState> foldedNodesToNewARGNode =
          new HashMap<>();

      Set<ARGState> seen = new HashSet<>();
      Deque<Pair<ARGState, Pair<LocationState, CallstackStateEqualsWrapper>>> toProcess =
          new ArrayDeque<>();

      Pair<LocationState, CallstackStateEqualsWrapper> foldedNode =
          Pair.of(
              AbstractStates.extractStateByType(pARGRoot, LocationState.class),
              new CallstackStateEqualsWrapper(
                  AbstractStates.extractStateByType(pARGRoot, CallstackState.class)));
      seen.add(pARGRoot);
      toProcess.add(Pair.of(pARGRoot, foldedNode));

      ARGState newRoot = new ARGState(foldedNode.getFirst(), null);
      foldedNodesToNewARGNode.put(foldedNode, newRoot);

      while (!toProcess.isEmpty()) {
        ARGState currentARGState = toProcess.peek().getFirst();
        foldedNode = toProcess.pop().getSecond();

        for (ARGState child : currentARGState.getChildren()) {
          if (seen.add(child)) {
            Pair<LocationState, CallstackStateEqualsWrapper> foldedChild =
                Pair.of(
                    AbstractStates.extractStateByType(child, LocationState.class),
                    new CallstackStateEqualsWrapper(
                        AbstractStates.extractStateByType(child, CallstackState.class)));
            toProcess.add(Pair.of(child, foldedChild));

            ARGState newChild = foldedNodesToNewARGNode.get(foldedChild);

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

  private abstract static class StructureFolder<T> extends ConditionFolder {
    final CFA cfa;
    final Set<CFANode> loopHeads;

    StructureFolder(final CFA pCfa, final FolderType type) {
      super(type);
      cfa = pCfa;
      Preconditions.checkState(cfa.getAllLoopHeads().isPresent());
      loopHeads = cfa.getAllLoopHeads().orElseThrow();
    }

    abstract T getRootFoldId(final ARGState pRoot);

    abstract T adaptID(CFAEdge pEdge, T pFoldID, ARGState pChild);

    abstract boolean shouldFold(CFANode loc);

    @Override
    public ARGState foldARG(final ARGState pRoot) {

      Map<ARGState, ARGState> oldARGToFoldedState = new HashMap<>();
      Map<ARGState, Set<ARGState>> newARGToFoldedStates = new HashMap<>();

      // folderStates, states folded when shouldFold returned true (often loop heads)
      Map<T, ARGState> folderStatesFoldIDToFoldedARGState = new HashMap<>();
      Map<ARGState, Set<T>> foldedARGStateToFoldIDs = new HashMap<>();

      MergeUpdateFunction update =
          (merge, mergeInto) -> {
            for (ARGState oldState : newARGToFoldedStates.remove(merge)) {
              oldARGToFoldedState.put(oldState, mergeInto);
              newARGToFoldedStates.get(mergeInto).add(oldState);
            }
            if (foldedARGStateToFoldIDs.containsKey(merge)) {
              for (T foldID : foldedARGStateToFoldIDs.remove(merge)) {
                folderStatesFoldIDToFoldedARGState.put(foldID, mergeInto);
                foldedARGStateToFoldIDs.get(mergeInto).add(foldID);
              }
            }
          };

      Deque<Pair<ARGState, T>> waitlist = new ArrayDeque<>();
      ARGState foldedNode = new ARGState(pRoot.getWrappedState(), null);
      oldARGToFoldedState.put(pRoot, foldedNode);
      Set<ARGState> foldedStates = new HashSet<>();
      foldedStates.add(pRoot);
      newARGToFoldedStates.put(foldedNode, foldedStates);
      CFANode loc = AbstractStates.extractLocation(pRoot);
      T id = getRootFoldId(pRoot);
      if (shouldFold(loc)) {
        folderStatesFoldIDToFoldedARGState.put(id, foldedNode);
        Set<T> loopContexts = new HashSet<>();
        loopContexts.add(id);
        foldedARGStateToFoldIDs.put(foldedNode, loopContexts);
      }
      waitlist.push(Pair.of(pRoot, id));

      while (!waitlist.isEmpty()) {
        ARGState oldState = waitlist.peek().getFirst();
        T foldID = waitlist.pop().getSecond();
        loc = AbstractStates.extractLocation(oldState);

        for (ARGState child : oldState.getChildren()) {
          CFANode locChild = AbstractStates.extractLocation(child);
          CFAEdge edge = oldState.getEdgeToChild(child);
          child = getUncoveredChild(child);

          T foldIDChild = adaptID(edge, foldID, child);

          if (!oldARGToFoldedState.containsKey(child)) {
            if (shouldFold(locChild)
                && folderStatesFoldIDToFoldedARGState.containsKey(foldIDChild)) {
              foldedNode = folderStatesFoldIDToFoldedARGState.get(foldIDChild);
              assert Objects.equals(locChild, AbstractStates.extractLocation(foldedNode));
            } else {
              foldedNode = null;
              ARGState newState = oldARGToFoldedState.get(oldState);
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

              if (shouldFold(locChild)) {
                folderStatesFoldIDToFoldedARGState.put(foldIDChild, foldedNode);
                if (!foldedARGStateToFoldIDs.containsKey(foldedNode)) {
                  foldedARGStateToFoldIDs.put(foldedNode, new HashSet<>());
                }
                foldedARGStateToFoldIDs.get(foldedNode).add(foldIDChild);
              }
            }

            oldARGToFoldedState.put(child, foldedNode);
            newARGToFoldedStates.get(foldedNode).add(child);
            waitlist.push(Pair.of(child, foldIDChild));
          }

          ARGState newState = oldARGToFoldedState.get(oldState);
          ARGState newChild = null;
          for (ARGState newARGChild : newState.getChildren()) {
            if (edge.equals(newState.getEdgeToChild(newARGChild))) {
              newChild = newARGChild;
              // there should be only one such child, thus break
              break;
            }
          }

          if (newChild != null && !newChild.equals(oldARGToFoldedState.get(child))) {
            merge(newChild, oldARGToFoldedState.get(child), update);
          }

          newChild = oldARGToFoldedState.get(child);
          newChild.addParent(oldARGToFoldedState.get(oldState));
        }
      }

      return oldARGToFoldedState.get(pRoot);
    }
  }

  private static class LoopAlwaysFolder
      extends StructureFolder<Pair<CFANode, CallstackStateEqualsWrapper>> {

    LoopAlwaysFolder(final CFA pCfa) {
      super(pCfa, FolderType.LOOP_ALWAYS);
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
      if (shouldFold(node)) {
        return Pair.of(
            node,
            new CallstackStateEqualsWrapper(
                AbstractStates.extractStateByType(argNode, CallstackState.class)));
      }

      return null;
    }

    @Override
    protected boolean shouldFold(CFANode pLoc) {
      return loopHeads.contains(pLoc);
    }
  }

  private static class ContextLoopFolder extends StructureFolder<String> {
    // takes function call context and taken branches into account

    private final LoopInfo loopInfo;

    private ContextLoopFolder(final CFA pCfa) {
      super(pCfa, FolderType.LOOP_SAME_CONTEXT);

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
        newLoopContext = newLoopContext + "/N" + pEdge.getPredecessor().getNodeNumber() + "N";
      }

      // enter loop or start next iteration
      if (cfa.getAllLoopHeads().orElseThrow().contains(pEdge.getSuccessor())) {
        newLoopContext += "|L" + pEdge.getSuccessor().getNodeNumber() + "L";
      }

      if (pEdge instanceof AssumeEdge assumeEdge) {
        if (assumeEdge.getTruthAssumption()) {
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
      if (shouldFold(rootLoc)) {
        return "|L" + rootLoc.getNodeNumber() + "L";
      }
      return "";
    }

    @Override
    protected String adaptID(
        final CFAEdge pEdge, final String pLoopContext, final ARGState pChild) {
      return extendLoopContext(pEdge, pLoopContext);
    }

    @Override
    protected boolean shouldFold(CFANode pLoc) {
      return loopHeads.contains(pLoc);
    }
  }

  @Options(prefix = "residualprogram")
  private static class BoundUnrollingLoopFolder extends StructureFolder<String> {

    private final LoopInfo loopInfo;

    @Option(
        secure = true,
        description = "How often may a loop be unrolled before it must be folded",
        name = "unrollBound")
    @IntegerOption(min = 2)
    private int maxUnrolls = 2;

    private BoundUnrollingLoopFolder(final CFA pCfa, final Configuration pConfig)
        throws InvalidConfigurationException {
      super(pCfa, FolderType.LOOP_BOUND);
      pConfig.inject(this);
      loopInfo = new LoopInfo(pCfa);
    }

    @Override
    protected String getRootFoldId(final ARGState pRoot) {
      CFANode rootLoc = AbstractStates.extractLocation(pRoot);
      if (shouldFold(rootLoc)) {
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
            Integer.parseInt(newLoopBoundID.substring(newLoopBoundID.lastIndexOf(":") + 1));
        newLoopBoundID = newLoopBoundID.substring(0, newLoopBoundID.lastIndexOf("|"));
      }

      if (pEdge instanceof FunctionReturnEdge && newLoopBoundID.contains("/")) {
        newLoopBoundID = newLoopBoundID.substring(0, newLoopBoundID.lastIndexOf("/"));
      }
      if (pEdge instanceof FunctionCallEdge) {
        newLoopBoundID = newLoopBoundID + "/N" + pEdge.getPredecessor().getNodeNumber() + "N";
      }

      // enter loop or start next iteration
      if (cfa.getAllLoopHeads().orElseThrow().contains(pEdge.getSuccessor())) {
        newLoopBoundID +=
            "|" + pEdge.getSuccessor().getNodeNumber() + ":" + Math.min(prevLoopIt + 1, maxUnrolls);
      }

      return newLoopBoundID;
    }

    @Override
    protected boolean shouldFold(CFANode pLoc) {
      return loopHeads.contains(pLoc);
    }
  }

  @Options(prefix = "residualprogram")
  private static class BoundUnrollingContextLoopFolder extends StructureFolder<String> {

    private final LoopInfo loopInfo;

    @Option(
        secure = true,
        description = "How often may a loop be unrolled before it must be folded",
        name = "unrollBound")
    @IntegerOption(min = 2)
    private int maxUnrolls = 2;

    private BoundUnrollingContextLoopFolder(final CFA pCfa, final Configuration pConfig)
        throws InvalidConfigurationException {
      super(pCfa, FolderType.LOOP_BOUND_SAME_CONTEXT);
      pConfig.inject(this);
      loopInfo = new LoopInfo(pCfa);
    }

    @Override
    protected String getRootFoldId(final ARGState pRoot) {
      CFANode rootLoc = AbstractStates.extractLocation(pRoot);
      if (shouldFold(rootLoc)) {
        return "|L" + rootLoc.getNodeNumber() + ":1L";
      }
      return "";
    }

    @Override
    protected String adaptID(CFAEdge pEdge, String pFoldID, ARGState pChild) {
      String newLoopBoundContextID = pFoldID;
      int prevLoopIt = 0;
      int indexCol;

      // leave loop
      if (loopInfo.leaveLoop(pEdge) && newLoopBoundContextID.contains("|")) {
        newLoopBoundContextID =
            newLoopBoundContextID.substring(0, newLoopBoundContextID.lastIndexOf("|"));
      }

      // next loop iteration
      if (loopInfo.startNewLoopIteation(pEdge) && newLoopBoundContextID.contains("|")) {
        indexCol = newLoopBoundContextID.lastIndexOf(":");
        prevLoopIt =
            Integer.parseInt(
                newLoopBoundContextID.substring(
                    indexCol + 1, newLoopBoundContextID.indexOf("L", indexCol)));
        newLoopBoundContextID =
            newLoopBoundContextID.substring(0, newLoopBoundContextID.lastIndexOf("|"));
      }

      if (pEdge instanceof FunctionReturnEdge && newLoopBoundContextID.contains("/")) {
        newLoopBoundContextID =
            newLoopBoundContextID.substring(0, newLoopBoundContextID.lastIndexOf("/"));
      }
      if (pEdge instanceof FunctionCallEdge) {
        newLoopBoundContextID =
            newLoopBoundContextID + "/N" + pEdge.getPredecessor().getNodeNumber() + "N";
      }

      // enter loop or start next iteration
      if (cfa.getAllLoopHeads().orElseThrow().contains(pEdge.getSuccessor())) {
        newLoopBoundContextID +=
            "|L"
                + pEdge.getSuccessor().getNodeNumber()
                + ":"
                + Math.min(prevLoopIt + 1, maxUnrolls)
                + "L";
      }

      if (pEdge instanceof AssumeEdge assumeEdge) {
        if (assumeEdge.getTruthAssumption()) {
          newLoopBoundContextID += "1";
        } else {
          newLoopBoundContextID += "0";
        }
      }

      return newLoopBoundContextID;
    }

    @Override
    protected boolean shouldFold(CFANode pLoc) {
      return loopHeads.contains(pLoc);
    }
  }

  private static class ExceptLoopFolder
      extends StructureFolder<Pair<String, CallstackStateEqualsWrapper>> {

    private final LoopInfo loopInfo;

    private ExceptLoopFolder(final CFA pCfa) {
      super(pCfa, FolderType.FOLD_EXCEPT_LOOPS);
      loopInfo = new LoopInfo(pCfa);
    }

    @Override
    protected Pair<String, CallstackStateEqualsWrapper> getRootFoldId(final ARGState pRoot) {
      CFANode rootLoc = AbstractStates.extractLocation(pRoot);
      String loopPart = ":";
      if (!shouldFold(rootLoc)) {
        loopPart = ":|" + pRoot.getStateId();
      }
      return Pair.of(
          rootLoc + loopPart,
          new CallstackStateEqualsWrapper(
              AbstractStates.extractStateByType(pRoot, CallstackState.class)));
    }

    @Override
    protected Pair<String, CallstackStateEqualsWrapper> adaptID(
        final CFAEdge pEdge,
        final Pair<String, CallstackStateEqualsWrapper> pFoldID,
        final ARGState pChild) {
      String newLoopNesting = pFoldID.getFirstNotNull();
      newLoopNesting = newLoopNesting.substring(newLoopNesting.indexOf(":"));

      // leave loop or start new iteration
      if ((loopInfo.leaveLoop(pEdge) || loopInfo.startNewLoopIteation(pEdge))
          && newLoopNesting.contains("|")) {
        newLoopNesting = newLoopNesting.substring(0, newLoopNesting.lastIndexOf("|"));
      }

      // enter loop or start next iteration
      if (cfa.getAllLoopHeads().orElseThrow().contains(pEdge.getSuccessor())) {
        newLoopNesting += "|" + pChild.getStateId();
      }

      return Pair.of(
          pEdge.getSuccessor().getNodeNumber() + newLoopNesting,
          new CallstackStateEqualsWrapper(
              AbstractStates.extractStateByType(pChild, CallstackState.class)));
    }

    @Override
    protected boolean shouldFold(CFANode pLoc) {
      return !loopHeads.contains(pLoc);
    }
  }
}
