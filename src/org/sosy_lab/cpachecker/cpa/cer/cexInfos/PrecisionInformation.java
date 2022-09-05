package org.sosy_lab.cpachecker.cpa.cer.cexInfos;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class PrecisionInformation implements CounterexampleInformation {
    private final Set<MemoryLocation> valuePrecison;

    public PrecisionInformation(Set<MemoryLocation> pValuePrecision) {
        valuePrecison = pValuePrecision;
    }

    public Set<MemoryLocation> getValuePrecison() {
        return valuePrecison;
    }

    public static PrecisionInformation
            merge(PrecisionInformation prec1, PrecisionInformation prec2) {
        Set<MemoryLocation> valueResultPrec = new HashSet<>();
        if (prec1 != null) {
            valueResultPrec.addAll(prec1.getValuePrecison());
        }
        if (prec2 != null) {
            valueResultPrec.addAll(prec2.getValuePrecison());
        }
        return new PrecisionInformation(valueResultPrec);
    }

    @Override
    public boolean equals(Object pObj) {
        if (!(pObj instanceof PrecisionInformation)) {
            return false;
        }
        PrecisionInformation p = (PrecisionInformation) pObj;
        if (this.valuePrecison.containsAll(p.getValuePrecison())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Used by precision information in cer states in the cer transfer relation.
     */
    public static PrecisionInformation step(PrecisionInformation oldPrecInfo, CFAEdge pCfaEdge) {
        if (oldPrecInfo == null) {
            oldPrecInfo = new PrecisionInformation(ImmutableSet.of());
        }
        Set<MemoryLocation> oldValuePrec = oldPrecInfo.getValuePrecison();
        Set<MemoryLocation> newValuePrec = computeValuePrecision(oldValuePrec, pCfaEdge);
        if (newValuePrec.containsAll(oldValuePrec)) {
            return oldPrecInfo;
        } else {
            return new PrecisionInformation(newValuePrec);
        }
    }

    private static Set<MemoryLocation>
            computeValuePrecision(Set<MemoryLocation> oldValuePrecision, CFAEdge pCfaEdge) {

        Preconditions.checkArgument(pCfaEdge != null, "Edge is null");

        Set<MemoryLocation> resultPrecision = new HashSet<>(oldValuePrecision);
        // Drop variable if the edge is an assignment and the store does not contain infos about it.
        handleEdge(resultPrecision, pCfaEdge);
        return resultPrecision;
    }

    private static void handleEdge(Set<MemoryLocation> modifiablePrecision, CFAEdge edge) {
        if (edge instanceof AStatementEdge || edge instanceof FunctionReturnEdge) {
            MemoryLocation var = CERUtils.getAssignedMemoryLocation(edge);
            if (var != null) {
                modifiablePrecision.remove(var);
            }
        } else if (edge instanceof AReturnStatementEdge) {
            AReturnStatementEdge retEdge = (AReturnStatementEdge) edge;
            FunctionExitNode retNode = retEdge.getSuccessor();
            for (ASimpleDeclaration decl : retNode.getOutOfScopeVariables()) {
                MemoryLocation var = MemoryLocation.fromQualifiedName(decl.getQualifiedName());
                if (modifiablePrecision.contains(var)) {
                    modifiablePrecision.remove(var);
                }
            }
        }
    }
}
