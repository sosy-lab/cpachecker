package org.sosy_lab.cpachecker.cpa.cer.refiner;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.collect.Multimap;

public class CERRefinerReport {
    private final ARGPath fullPath;
    private boolean isFeasible;
    private Collection<ARGState> refinementRoots;
    private Collection<ARGState> cutOffRoots;

    private Multimap<CFANode, MemoryLocation> precisionInc;
    private InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> interpolationTree;

    public CERRefinerReport(ARGPath pPath) {
        fullPath = pPath;
    }

    public boolean isFeasible() {
        return isFeasible;
    }

    public void makeFeasible() {
        isFeasible = true;
    }

    public void makeSpurious() {
        isFeasible = false;
    }

    public ARGPath getErrorPath() {
        return fullPath;
    }

    public Collection<ARGState> getCutOffRoots() {
        return cutOffRoots;
    }

    public void setCutOffRoots(Collection<ARGState> pCutOffRoots) {
        cutOffRoots = pCutOffRoots;
    }

    public Collection<ARGState> getRefinementRoots() {
        return refinementRoots;
    }

    public void setRefinementRoots(Collection<ARGState> pRefinementRoots) {
        refinementRoots = pRefinementRoots;
    }

    public void setPrecisionInc(Multimap<CFANode, MemoryLocation> pPrecisionInc) {
        precisionInc = pPrecisionInc;
    }

    public Multimap<CFANode, MemoryLocation> getPrecisionInc() {
        return precisionInc;
    }

    public void setInterpolationTree(
            InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> pInterpolationTree) {
        interpolationTree = pInterpolationTree;
    }

    public InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> getInterpolationTree() {
        return interpolationTree;
    }
}
