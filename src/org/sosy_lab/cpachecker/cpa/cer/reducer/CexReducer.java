package org.sosy_lab.cpachecker.cpa.cer.reducer;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class CexReducer {

    // TODO make option
    boolean reduceFunctionCalls = true;

    private enum Status {
        SlicePostfix,
        ReduceCex,
        Done
    }

    public ImmutableList<CFAEdgeWithAdditionalInfo> reduce(
            ARGPath fullPath,
            Optional<Multimap<CFANode, MemoryLocation>> precisionInc,
            Optional<InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant>> interpolTree,
            Collection<ARGState> cutOffs) {

        Preconditions.checkArgument(
                cutOffs.size() <= 1,
                "Counterexample has more than one cut-off, which is not supported by cer");
        ARGState cutOff = cutOffs.isEmpty() ? fullPath.getLastState() : cutOffs.iterator().next();

        // TODO rework
        Deque<CFAEdgeWithAdditionalInfo> pathWithInfos;
        if (interpolTree.isPresent()) {
            pathWithInfos = getAdditionalPrecisionInfos(fullPath, interpolTree.get());
        } else if (precisionInc.isPresent()) {
            pathWithInfos = getAdditionalPrecisionInfos(fullPath.getFullPath(), precisionInc.get());
        } else {
            pathWithInfos = new ArrayDeque<>();
            for (CFAEdge edge : fullPath.getFullPath()) {
                pathWithInfos.addLast(createEdgeWithInfo(edge, ImmutableSet.of()));
            }
        }

        CFAEdgeWithAdditionalInfo root = findRoot(pathWithInfos);
        if (root == null) {
            root = pathWithInfos.getFirst();
        }

        Deque<CFAEdgeWithAdditionalInfo> reducedPath = new ArrayDeque<>();
        Status status = Status.SlicePostfix;
        PathIterator iter = fullPath.reverseFullPathIterator();

        while (iter.advanceIfPossible()) {
            CFAEdgeWithAdditionalInfo edgeWithInfo = pathWithInfos.pollLast();

            if (edgeWithInfo == null || !edgeWithInfo.getCFAEdge().equals(iter.getOutgoingEdge())) {
                // mismatch
                return null;
            }

            switch (status) {

                case SlicePostfix:
                    // iter.getAbstractState() has no state in sometimes -> bug?
                    ARGState state;
                    try {
                        state = iter.getAbstractState();
                    } catch (IllegalStateException e) {
                        continue;
                    }

                    if (state == cutOff) {
                        status = Status.ReduceCex;
                        // the next edgeWithInfo is the cutoff edge.
                        // Since cutoffs are assumeEdges it will be added to the result path
                        continue;
                    }
                    break;
                case ReduceCex:
                    if (isRelevant(edgeWithInfo)) {
                        reducedPath.addFirst(edgeWithInfo);
                    }

                    if (reduceFunctionCalls
                            && edgeWithInfo.getCFAEdge() instanceof FunctionCallEdge) {
                        reduceFunctionCall(
                                reducedPath,
                                (FunctionCallEdge) edgeWithInfo.getCFAEdge());
                    }

                    if (edgeWithInfo == root) {
                        status = Status.Done;
                    }
                    // TODO loop reduction, function call-return reduction
                    break;
                default:
                    break;
            }

            if (status == Status.Done) {
                break;
            }
        }

        return ImmutableList.copyOf(reducedPath);
    }

    /**
     * If an function call {some edges} Function return structure contains no counterexample
     * information, then we can reduce it to its summary edge
     */
    private static void
            reduceFunctionCall(Deque<CFAEdgeWithAdditionalInfo> input, FunctionCallEdge fcEdge) {

        // reduce only if there is a summary edge
        if (fcEdge.getSummaryEdge() == null) {
            return;
        }

        Iterator<CFAEdgeWithAdditionalInfo> iter = input.iterator();
        CFAEdgeWithAdditionalInfo edgeWithInfo = iter.next();
        FunctionEntryNode entryNode;
        if (edgeWithInfo.getCFAEdge().getPredecessor() instanceof FunctionEntryNode) {
            entryNode = (FunctionEntryNode) edgeWithInfo.getCFAEdge().getPredecessor();
        } else {
            return;
        }

        boolean relevant = false;
        boolean returnFound = false;
        while (iter.hasNext()) {
            edgeWithInfo = iter.next();
            if (!edgeWithInfo.getInfos().isEmpty()) {
                relevant = true;
                break;
            }
            if (edgeWithInfo.getCFAEdge() instanceof FunctionReturnEdge) {
                FunctionReturnEdge e = (FunctionReturnEdge) edgeWithInfo.getCFAEdge();
                if (e.getFunctionEntry().equals(entryNode)) {
                    returnFound = true;
                    break;
                }
            }
        }

        if (relevant || !returnFound) {
            return;
        }

        // else reduce to a statement
        iter = input.iterator();
        while (iter.hasNext()) {
            edgeWithInfo = iter.next();
            iter.remove();
            if (edgeWithInfo.getCFAEdge() instanceof FunctionReturnEdge) {
                FunctionReturnEdge e = (FunctionReturnEdge) edgeWithInfo.getCFAEdge();
                if (e.getFunctionEntry().equals(entryNode)) {
                    break;
                }
            }
        }

        input.addFirst(createEdgeWithInfo(fcEdge.getSummaryEdge(), ImmutableSet.of()));
        return;
    }

    private static CFAEdgeWithAdditionalInfo findRoot(Deque<CFAEdgeWithAdditionalInfo> path) {
        CFAEdgeWithAdditionalInfo result = null;
        for (CFAEdgeWithAdditionalInfo edge : path) {
            if (edge.getCFAEdge().getPredecessor() instanceof FunctionEntryNode) {
                result = edge;
                continue;
            }
            if (!edge.getInfos().isEmpty()) {
                return result;
            }
        }
        return result;
    }

    private boolean isRelevant(CFAEdgeWithAdditionalInfo edgeWithInfo) {
        // Edges with infos
        if (edgeWithInfo.getInfos().size() > 0) {
            return true;
        }

        // Edges required for identification
        CFAEdge edge = edgeWithInfo.getCFAEdge();
        if (edge instanceof BlankEdge) {
            if (edge.getPredecessor() instanceof FunctionEntryNode) {
                return true;
            }
        } else if (edge instanceof AssumeEdge) {
            return true;
        } else if (edge instanceof FunctionReturnEdge) {
            if (edge.getPredecessor().getNumLeavingEdges() > 1) {
                return true;
            }
        }

        return false;
    }

    /**
     * Iterate backwards through the edges. If the edge has a precision increment store the relevant
     * variable and continue search until the corresponding variable assignment was reached. If
     * possible use the version with the InterpolationTree since it is more accurate.
     */
    private static Deque<CFAEdgeWithAdditionalInfo> getAdditionalPrecisionInfos(
            List<CFAEdge> path,
            Multimap<CFANode, MemoryLocation> precisionInc) {
        Deque<CFAEdgeWithAdditionalInfo> result = new ArrayDeque<>(path.size());

        Set<MemoryLocation> currRelevantVars = new HashSet<>();

        for (int i = path.size() - 1; i >= 0; --i) {
            CFAEdge edge = path.get(i);

            if (precisionInc.keySet().contains(edge.getSuccessor())) {
                currRelevantVars.addAll(precisionInc.get(edge.getSuccessor()));
            }

            MemoryLocation var = CERUtils.getAssignedMemoryLocation(edge);
            if (var != null && currRelevantVars.contains(var)) {
                result.addFirst(createEdgeWithInfo(edge, new HashSet<>(currRelevantVars)));
                currRelevantVars.remove(var);
            } else if (edge instanceof FunctionCallEdge) {
                result.addFirst(createEdgeWithInfo(edge, new HashSet<>(currRelevantVars)));
                for (MemoryLocation param : CERUtils.getParameterMemoryLocations(edge)) {
                    if (currRelevantVars.contains(param)) {
                        currRelevantVars.remove(param);
                    }
                }
            } else {
                result.addFirst(createEdgeWithInfo(edge, ImmutableSet.of()));
            }
        }
        return result;
    }

    private static Deque<CFAEdgeWithAdditionalInfo> getAdditionalPrecisionInfos(
            ARGPath path,
            InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> interpolTree) {
        Deque<CFAEdgeWithAdditionalInfo> result = new ArrayDeque<>(path.size());

        PathIterator iter = path.fullPathIterator();
        ARGState currState = iter.getNextAbstractState();
        while (iter.advanceIfPossible()) {
            CFAEdge edge = iter.getIncomingEdge();
            MemoryLocation var = CERUtils.getAssignedMemoryLocation(edge);
            if (var != null) {
                ValueAnalysisInterpolant interpolant =
                        interpolTree.getInterpolantForState(currState);
                if (interpolant.getMemoryLocations().contains(var)) {
                    result.addLast(createEdgeWithInfo(edge, interpolant.getMemoryLocations()));
                } else {
                    result.addLast(createEdgeWithInfo(edge, ImmutableSet.of()));
                }
            } else if (edge instanceof FunctionCallEdge) {
                ValueAnalysisInterpolant interpolant =
                        interpolTree.getInterpolantForState(currState);
                Set<MemoryLocation> prec = new HashSet<>();
                for (MemoryLocation param : CERUtils.getParameterMemoryLocations(edge)) {
                    if (interpolant.getMemoryLocations().contains(param)) {
                        prec.add(param);
                    }
                }
                result.addLast(createEdgeWithInfo(edge, prec));
            } else {
                result.addLast(createEdgeWithInfo(edge, ImmutableSet.of()));
            }

            CFANode node = AbstractStates.extractLocation(currState);
            if (edge.getSuccessor().equals(node)) {
                try {
                    currState = iter.getNextAbstractState();
                } catch (IllegalStateException e) {
                    break;
                }
            }
        }
        return result;
    }

    private static CFAEdgeWithAdditionalInfo
            createEdgeWithInfo(CFAEdge pEdge, Set<MemoryLocation> prec) {
        CFAEdgeWithAdditionalInfo result = CFAEdgeWithAdditionalInfo.of(pEdge);
        if (!prec.isEmpty()) {
            result.addInfo(CERConvertingTags.PRECISION, prec);
        }
        return result;
    }
}
