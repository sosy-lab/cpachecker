// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

import java.util.*;

public class TimeSelectionStrategy implements CFATraversal.CFAVisitor {

    private final CFA cfa;

    private final Set<String> functionNames = new HashSet<>();
    private final Set<String> arrayVariables = new HashSet<>();
    private final Set<String> floatVariables = new HashSet<>();

    public TimeSelectionStrategy(CFA cfa) {
        this.cfa = cfa;
    }

    @Override
    public CFATraversal.TraversalProcess visitEdge(CFAEdge pEdge) {
        switch (pEdge.getEdgeType()) {
            case StatementEdge: {
                final AStatementEdge edge = (AStatementEdge) pEdge;
                if (edge.getStatement() instanceof AFunctionCall) {
                    final AFunctionCall call = (AFunctionCall) edge.getStatement();
                    final AExpression exp = call.getFunctionCallExpression().getFunctionNameExpression();
                    if (exp instanceof AIdExpression id) {
                        functionNames.add(id.getName());
                    }
                }
                break;
            }
            case DeclarationEdge: {
                final ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
                ADeclaration declaration = declarationEdge.getDeclaration();
                Type declType = declaration.getType();
                Queue<Type> types = new ArrayDeque<>();
                Set<Type> visitedTypes = new HashSet<>();
                types.add(declType);
                while (!types.isEmpty()) {
                    Type type = types.poll();
                    if (type instanceof CType) {
                        type = ((CType) type).getCanonicalType();
                    }
                    if (visitedTypes.add(type)) {
                        if (type instanceof CCompositeType compositeType) {
                            for (CCompositeType.CCompositeTypeMemberDeclaration member : compositeType.getMembers()) {
                                types.offer(member.getType());
                            }
                        }
                        if (type instanceof CArrayType || type instanceof JArrayType) {
                            arrayVariables.add(declaration.getQualifiedName());
                        } else if (type instanceof CSimpleType simpleType) {
                            if (simpleType.getType().isFloatingPointType()) {
                                floatVariables.add(declaration.getQualifiedName());
                            }
                        } else if ((type instanceof JSimpleType simpleType)
                                && simpleType.getType().isFloatingPointType()) {
                            floatVariables.add(declaration.getQualifiedName());
                        }
                    }
                }
                break;
            }
            case FunctionCallEdge:
            case FunctionReturnEdge:
            case CallToReturnEdge:
            default:
        }
        return CFATraversal.TraversalProcess.CONTINUE;
    }

    @Override
    public CFATraversal.TraversalProcess visitNode(CFANode pNode) {
        return CFATraversal.TraversalProcess.CONTINUE;
    }

    public AlgSelectionBooleanVector extractStatisticsFromCfa() {
        CFANode startingNode = cfa.getMainFunction();
        CFATraversal.dfs().traverseOnce(startingNode, this);

        Optional<LoopStructure> loopStructure = cfa.getLoopStructure();
        VariableClassification variableClassification = cfa.getVarClassification().orElseThrow();

        final boolean hasAlias =
                !variableClassification.getAddressedVariables().isEmpty()
                        || !variableClassification.getAddressedFields().isEmpty();

        final boolean hasLoop =
                !loopStructure.isPresent() || !loopStructure.orElseThrow().getAllLoops().isEmpty();

        final boolean hasCompositeType = !variableClassification.getRelevantFields().isEmpty();

        final boolean hasArray =
                !Collections.disjoint(variableClassification.getRelevantVariables(), this.arrayVariables)
                        || !Collections.disjoint(
                        variableClassification.getAddressedFields().values(), this.arrayVariables);

        final boolean hasFloat =
                !Collections.disjoint(variableClassification.getRelevantVariables(), this.floatVariables)
                        || !Collections.disjoint(
                        variableClassification.getAddressedFields().values(), this.floatVariables);

        final boolean hasSingleLoop =
                loopStructure.isPresent() && loopStructure.orElseThrow().getAllLoops().size() == 1;

        return AlgSelectionBooleanVector.init(hasAlias, hasArray, hasCompositeType, hasFloat, hasLoop, hasSingleLoop);
    }
}
