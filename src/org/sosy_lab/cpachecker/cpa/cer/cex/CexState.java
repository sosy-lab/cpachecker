package org.sosy_lab.cpachecker.cpa.cer.cex;

import java.util.Optional;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.CounterexampleInformation;

import com.google.common.collect.ImmutableSet;

public class CexState {

    private CFANode mappedNode;
    private CexTransition leavingTransition;
    private Set<CounterexampleInformation> cexInfos;

    public CexState() {
        mappedNode = null;
        leavingTransition = null;
        cexInfos = ImmutableSet.of();
    }

    public CexState(
            CexTransition pLeavingTransition,
            Set<CounterexampleInformation> pInfos,
            CFANode pMappedNode) {
        leavingTransition = pLeavingTransition;
        cexInfos = pInfos;
        mappedNode = pMappedNode;
    }

    public int getId() {
        return this.hashCode();
    }

    public void setLeavingTransition(CexTransition pLeavingTransition) {
        leavingTransition = pLeavingTransition;
    }

    public Optional<CexTransition> getLeavingTransition() {
        return Optional.ofNullable(leavingTransition);
    }

    public void setCexInfos(Set<CounterexampleInformation> pCexInfos) {
        cexInfos = pCexInfos;
    }

    public Set<CounterexampleInformation> getCexInfos() {
        return cexInfos;
    }

    public void setMappedNode(CFANode pMappedNode) {
        mappedNode = pMappedNode;
    }

    public Optional<CFANode> getMappedNode() {
        return Optional.ofNullable(mappedNode);
    }
}
