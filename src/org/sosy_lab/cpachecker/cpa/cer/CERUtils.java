package org.sosy_lab.cpachecker.cpa.cer;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.ConvertingTags;
import org.sosy_lab.cpachecker.cpa.cer.reducer.CERConvertingTags;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.fasterxml.jackson.databind.json.JsonMapper;

public class CERUtils {

    public static JsonMapper mapperInstance = null;

    public static JsonMapper getJSONMapperInstance() {
        if (mapperInstance == null) {
            mapperInstance = new JsonMapper();
        }
        return mapperInstance;
    }

    public static String CFAEdgeToString(CFAEdge pEdge) {
        if (pEdge instanceof BlankEdge) {
            return pEdge.getDescription();
        } else if (pEdge instanceof FunctionReturnEdge) {
            FunctionReturnEdge edge = (FunctionReturnEdge) pEdge;
            return edge.getSummaryEdge().getRawStatement()
                    + "@"
                    + pEdge.getSuccessor().getFunction().getQualifiedName();
        } else {
            return pEdge.getRawStatement();
        }
    }

    public static MemoryLocation getAssignedMemoryLocation(CFAEdge edge) {
        if (edge instanceof AStatementEdge) {
            return getAssignedMemoryLocation(((AStatementEdge) edge).getStatement());
        } else if (edge instanceof ADeclarationEdge) {
            ADeclaration decl = ((ADeclarationEdge) edge).getDeclaration();
            if (decl instanceof AVariableDeclaration) {
                return MemoryLocation.fromQualifiedName(decl.getQualifiedName());
            }
        } else if (edge instanceof FunctionReturnEdge) {
            FunctionReturnEdge returnEdge = (FunctionReturnEdge) edge;
            AFunctionCall expression = returnEdge.getSummaryEdge().getExpression();
            return getAssignedMemoryLocation(expression);
        }
        return null;
    }

    public static Set<MemoryLocation> getParameterMemoryLocations(CFAEdge pEdge) {
        if (pEdge instanceof FunctionCallEdge) {
            FunctionCallEdge edge = (FunctionCallEdge) pEdge;
            List<? extends AParameterDeclaration> params =
                    edge.getSummaryEdge().getFunctionEntry().getFunctionParameters();
            if (params.size() == 0) {
                return Collections.emptySet();
            } else {
                Set<MemoryLocation> result = new HashSet<>(edge.getArguments().size());
                for (AParameterDeclaration param : params) {
                    result.add(MemoryLocation.fromQualifiedName(getVarName(param)));
                }
                return result;
            }
        }
        return null;
    }

    public static MemoryLocation getAssignedMemoryLocation(AStatement statement) {
        if (statement instanceof AFunctionCallAssignmentStatement) {
            AFunctionCallAssignmentStatement assignment =
                    (AFunctionCallAssignmentStatement) statement;
            return MemoryLocation.fromQualifiedName(getVarName(assignment.getLeftHandSide()));
        } else if (statement instanceof AAssignment) {
            AAssignment assignment = (AAssignment) statement;
            return MemoryLocation.fromQualifiedName(getVarName(assignment.getLeftHandSide()));
        } else {
            return null;
        }
    }

    private static String getVarName(AParameterDeclaration exp) {
        return exp.getQualifiedName();
    }

    private static String getVarName(AExpression exp) {
        if (exp instanceof AIdExpression) {
            return ((AIdExpression) exp).getDeclaration().getQualifiedName();
        } else {
            return exp.toASTString();
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<MemoryLocation> extractPrecisionInfo(CFAEdgeWithAdditionalInfo edgeWithInfo) {
        for (Entry<ConvertingTags, Object> e : edgeWithInfo.getInfos()) {
            if (e.getKey().equals(CERConvertingTags.PRECISION)) {
                try {
                    return (Set<MemoryLocation>) e.getValue();
                } catch (Exception exc) {
                    return Collections.emptySet();
                }
            }
        }
        return Collections.emptySet();
    }
}
