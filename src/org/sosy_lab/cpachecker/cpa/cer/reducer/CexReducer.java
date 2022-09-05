package org.sosy_lab.cpachecker.cpa.cer.reducer;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class CexReducer {

    boolean reduceFunctionCalls = true;
    boolean sliceAutomaton = true;
    boolean reduceStatementTransitions = true;

    public ImmutableList<CFAEdgeWithAdditionalInfo>
            reduce(Deque<CFAEdgeWithAdditionalInfo> pathWithInfos) {

        Deque<CFAEdgeWithAdditionalInfo> reducedPath = new ArrayDeque<>(pathWithInfos);

        if (sliceAutomaton) {
            // find cut-off
            CFAEdgeWithAdditionalInfo cutOff = findCutOff(reducedPath);
            if (cutOff == null) {
                cutOff = reducedPath.getLast();
            }

            while (reducedPath.peekLast() != null && reducedPath.peekLast() != cutOff) {
                reducedPath.removeLast();
            }
        }

        if (reduceFunctionCalls) {
            reduceFunctionCall(reducedPath);
        }

        if (sliceAutomaton) {
            CFAEdgeWithAdditionalInfo root = findRoot(reducedPath);
            if (root == null) {
                root = reducedPath.getFirst();
            }

            while (reducedPath.peekFirst() != null && reducedPath.peekFirst() != root) {
                reducedPath.removeFirst();
            }
        }

        if (reduceStatementTransitions) {
            Iterator<CFAEdgeWithAdditionalInfo> iter = reducedPath.iterator();
            while (iter.hasNext()) {
                CFAEdgeWithAdditionalInfo edge = iter.next();
                if (!isRelevant(edge)) {
                    iter.remove();
                }
            }
        }

        return ImmutableList.copyOf(reducedPath);
    }

    /**
     * If an function call {some edges} Function return structure contains no counterexample
     * information then we can remove it
     */
    private static void reduceFunctionCall(Deque<CFAEdgeWithAdditionalInfo> input) {

        Set<FunctionEntryNode> relevanceSet = new HashSet<>();

        // mark irrelevant of function call - return pairs
        Iterator<CFAEdgeWithAdditionalInfo> iter = input.iterator();
        Stack<FunctionEntryNode> functionStack = new Stack<>();
        while (iter.hasNext()) {
            CFAEdgeWithAdditionalInfo edgeWithInfo = iter.next();
            if (edgeWithInfo.getCFAEdge().getPredecessor() instanceof FunctionEntryNode) {
                FunctionEntryNode fcEntry =
                        (FunctionEntryNode) edgeWithInfo.getCFAEdge().getPredecessor();
                functionStack.add(fcEntry);
            } else if (edgeWithInfo.getCFAEdge() instanceof FunctionReturnEdge) {
                FunctionReturnEdge frEdge = (FunctionReturnEdge) edgeWithInfo.getCFAEdge();
                AFunctionDeclaration function = frEdge.getFunctionEntry().getFunction();
                if (!functionStack.isEmpty()
                        && functionStack.peek()
                                .getFunction()
                                .getQualifiedName()
                                .equals(function.getQualifiedName())) {
                    functionStack.pop();
                }
            }

            if (!edgeWithInfo.getInfos().isEmpty()) {
                for (FunctionEntryNode function : functionStack) {
                    relevanceSet.add(function);
                }
            }
        }

        // add all unclosed function calls
        for (FunctionEntryNode function : functionStack) {
            relevanceSet.add(function);
        }

        // remove irrelevant function call - return pairs
        iter = input.iterator();
        functionStack.clear(); // will contain a stack of irrelevant functions
        while (iter.hasNext()) {
            CFAEdgeWithAdditionalInfo edgeWithInfo = iter.next();

            if (edgeWithInfo.getCFAEdge().getPredecessor() instanceof FunctionEntryNode) {
                FunctionEntryNode fcEntry =
                        (FunctionEntryNode) edgeWithInfo.getCFAEdge().getPredecessor();
                if (!relevanceSet.contains(fcEntry)) {
                    functionStack.add(fcEntry);
                }
                if (!functionStack.isEmpty()) {
                    iter.remove();
                }
            } else if (edgeWithInfo.getCFAEdge() instanceof FunctionReturnEdge) {
                if (!functionStack.isEmpty()) {
                    iter.remove();
                }
                FunctionReturnEdge frEdge = (FunctionReturnEdge) edgeWithInfo.getCFAEdge();
                AFunctionDeclaration function = frEdge.getFunctionEntry().getFunction();
                if (!functionStack.isEmpty()
                        && functionStack.peek()
                                .getFunction()
                                .getQualifiedName()
                                .equals(function.getQualifiedName())) {
                    functionStack.pop();
                }
            } else {
                if (!functionStack.isEmpty()) {
                    iter.remove();
                }
            }
        }
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

    private static CFAEdgeWithAdditionalInfo findCutOff(Deque<CFAEdgeWithAdditionalInfo> path) {
        Iterator<CFAEdgeWithAdditionalInfo> iter = path.descendingIterator();
        while (iter.hasNext()) {
            CFAEdgeWithAdditionalInfo edge = iter.next();
            if (!edge.getInfos().isEmpty()) {
                return edge;
            }
        }
        return null;
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
            return true;
        }

        return false;
    }

    public static Deque<CFAEdgeWithAdditionalInfo> getPathWithPrecisionInfos(
            ARGPath path,
            Collection<ARGState> cutOffs,
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
            } else if (edge.getPredecessor() instanceof FunctionEntryNode) {
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
                if (cutOffs.contains(currState)) {
                    // contain the slicing edge in the resulting cex with an empty precision
                    CFAEdgeWithAdditionalInfo sliceEdge = CFAEdgeWithAdditionalInfo.of(edge);
                    sliceEdge.addInfo(CERConvertingTags.PRECISION, ImmutableSet.of());
                    result.addLast(sliceEdge);
                } else {
                    result.addLast(createEdgeWithInfo(edge, ImmutableSet.of()));
                }
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
