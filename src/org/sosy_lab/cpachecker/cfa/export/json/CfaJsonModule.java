// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.AbstractInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AbstractLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AbstractReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AbstractRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.*;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.cfa.types.AbstractFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * This class is a Jackson module for serialization and deserialization.
 *
 * <p>Important: The {@link AstCfaRelation} in {@link CfaMetadata} is not serialized or
 * deserialized.
 *
 * @see CfaJsonIO
 */
public class CfaJsonModule extends SimpleModule {

  private static final long serialVersionUID = 1945912240762984485L;

  /**
   * Sets up the module by registering all mixins.
   *
   * @param pContext The setup context.
   */
  @Override
  public void setupModule(SetupContext pContext) {
    super.setupModule(pContext);

    /* Register all mixins. */
    pContext.setMixInAnnotations(
        AArraySubscriptExpression.class, AArraySubscriptExpressionMixin.class);
    pContext.setMixInAnnotations(AArrayType.class, AArrayTypeMixin.class);
    pContext.setMixInAnnotations(AAssignment.class, AAssignmentMixin.class);
    pContext.setMixInAnnotations(AAstNode.class, AAstNodeMixin.class);
    pContext.setMixInAnnotations(ABinaryExpression.class, ABinaryExpressionMixin.class);
    pContext.setMixInAnnotations(ACastExpression.class, ACastExpressionMixin.class);
    pContext.setMixInAnnotations(ACharLiteralExpression.class, ACharLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(ADeclarationEdge.class, ADeclarationEdgeMixin.class);
    pContext.setMixInAnnotations(ADeclaration.class, ADeclarationMixin.class);
    pContext.setMixInAnnotations(
        AExpressionAssignmentStatement.class, AExpressionAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(AExpression.class, AExpressionMixin.class);
    pContext.setMixInAnnotations(AExpressionStatement.class, AExpressionStatementMixin.class);
    pContext.setMixInAnnotations(AFloatLiteralExpression.class, AFloatLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(
        AFunctionCallAssignmentStatement.class, AFunctionCallAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(AFunctionCallExpression.class, AFunctionCallExpressionMixin.class);
    pContext.setMixInAnnotations(AFunctionCall.class, AFunctionCallMixin.class);
    pContext.setMixInAnnotations(AFunctionCallStatement.class, AFunctionCallStatementMixin.class);
    pContext.setMixInAnnotations(AFunctionDeclaration.class, AFunctionDeclarationMixin.class);
    pContext.setMixInAnnotations(AFunctionType.class, AFunctionTypeMixin.class);
    pContext.setMixInAnnotations(AIdExpression.class, AIdExpressionMixin.class);
    pContext.setMixInAnnotations(AInitializerExpression.class, AInitializerExpressionMixin.class);
    pContext.setMixInAnnotations(AInitializer.class, AInitializerMixin.class);
    pContext.setMixInAnnotations(
        AIntegerLiteralExpression.class, AIntegerLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(ALeftHandSide.class, ALeftHandSideMixin.class);
    pContext.setMixInAnnotations(ALiteralExpression.class, ALiteralExpressionMixin.class);
    pContext.setMixInAnnotations(AParameterDeclaration.class, AParameterDeclarationMixin.class);
    pContext.setMixInAnnotations(APointerExpression.class, APointerExpressionMixin.class);
    pContext.setMixInAnnotations(AReturnStatementEdge.class, AReturnStatementEdgeMixin.class);
    pContext.setMixInAnnotations(AReturnStatement.class, AReturnStatementMixin.class);
    pContext.setMixInAnnotations(ARightHandSide.class, ARightHandSideMixin.class);
    pContext.setMixInAnnotations(ASimpleDeclaration.class, ASimpleDeclarationMixin.class);
    pContext.setMixInAnnotations(AStatementEdge.class, AStatementEdgeMixin.class);
    pContext.setMixInAnnotations(AStatement.class, AStatementMixin.class);
    pContext.setMixInAnnotations(
        AStringLiteralExpression.class, AStringLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(AUnaryExpression.class, AUnaryExpressionMixin.class);
    pContext.setMixInAnnotations(AVariableDeclaration.class, AVariableDeclarationMixin.class);
    pContext.setMixInAnnotations(AbstractAstNode.class, AbstractAstNodeMixin.class);
    pContext.setMixInAnnotations(AbstractCFAEdge.class, AbstractCFAEdgeMixin.class);
    pContext.setMixInAnnotations(AbstractDeclaration.class, AbstractDeclarationMixin.class);
    pContext.setMixInAnnotations(AbstractExpression.class, AbstractExpressionMixin.class);
    pContext.setMixInAnnotations(AbstractFunctionType.class, AbstractFunctionTypeMixin.class);
    pContext.setMixInAnnotations(AbstractInitializer.class, AbstractInitializerMixin.class);
    pContext.setMixInAnnotations(AbstractLeftHandSide.class, AbstractLeftHandSideMixin.class);
    pContext.setMixInAnnotations(AbstractReturnStatement.class, AbstractReturnStatementMixin.class);
    pContext.setMixInAnnotations(AbstractRightHandSide.class, AbstractRightHandSideMixin.class);
    pContext.setMixInAnnotations(
        AbstractSimpleDeclaration.class, AbstractSimpleDeclarationMixin.class);
    pContext.setMixInAnnotations(AbstractStatement.class, AbstractStatementMixin.class);
    pContext.setMixInAnnotations(AssumeEdge.class, AssumeEdgeMixin.class);
    pContext.setMixInAnnotations(BlankEdge.class, BlankEdgeMixin.class);
    pContext.setMixInAnnotations(
        CArraySubscriptExpression.class, CArraySubscriptExpressionMixin.class);
    pContext.setMixInAnnotations(CArrayType.class, CArrayTypeMixin.class);
    pContext.setMixInAnnotations(CAssignment.class, CAssignmentMixin.class);
    pContext.setMixInAnnotations(CAssumeEdge.class, CAssumeEdgeMixin.class);
    pContext.setMixInAnnotations(CAstNode.class, CAstNodeMixin.class);
    pContext.setMixInAnnotations(CBinaryExpression.class, CBinaryExpressionMixin.class);
    pContext.setMixInAnnotations(CComplexType.class, CComplexTypeMixin.class);
    pContext.setMixInAnnotations(CDeclarationEdge.class, CDeclarationEdgeMixin.class);
    pContext.setMixInAnnotations(CDeclaration.class, CDeclarationMixin.class);
    pContext.setMixInAnnotations(CDesignator.class, CDesignatorMixin.class);
    pContext.setMixInAnnotations(
        CExpressionAssignmentStatement.class, CExpressionAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(CExpression.class, CExpressionMixin.class);
    pContext.setMixInAnnotations(CExpressionStatement.class, CExpressionStatementMixin.class);
    pContext.setMixInAnnotations(CFAEdge.class, CFAEdgeMixin.class);
    pContext.setMixInAnnotations(CFALabelNode.class, CFALabelNodeMixin.class);
    pContext.setMixInAnnotations(CFANode.class, CFANodeMixin.class);
    pContext.setMixInAnnotations(CFATerminationNode.class, CFATerminationNodeMixin.class);
    pContext.setMixInAnnotations(
        CFunctionCallAssignmentStatement.class, CFunctionCallAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(CFunctionCallEdge.class, CFunctionCallEdgeMixin.class);
    pContext.setMixInAnnotations(CFunctionCallExpression.class, CFunctionCallExpressionMixin.class);
    pContext.setMixInAnnotations(CFunctionCall.class, CFunctionCallMixin.class);
    pContext.setMixInAnnotations(CFunctionCallStatement.class, CFunctionCallStatementMixin.class);
    pContext.setMixInAnnotations(CFunctionDeclaration.class, CFunctionDeclarationMixin.class);
    pContext.setMixInAnnotations(CFunctionEntryNode.class, CFunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(CFunctionReturnEdge.class, CFunctionReturnEdgeMixin.class);
    pContext.setMixInAnnotations(CFunctionSummaryEdge.class, CFunctionSummaryEdgeMixin.class);
    pContext.setMixInAnnotations(CFunctionType.class, CFunctionTypeMixin.class);
    pContext.setMixInAnnotations(CFunctionTypeWithNames.class, CFunctionTypeWithNamesMixin.class);
    pContext.setMixInAnnotations(CIdExpression.class, CIdExpressionMixin.class);
    pContext.setMixInAnnotations(CInitializerExpression.class, CInitializerExpressionMixin.class);
    pContext.setMixInAnnotations(CInitializerList.class, CInitializerListMixin.class);
    pContext.setMixInAnnotations(CInitializer.class, CInitializerMixin.class);
    pContext.setMixInAnnotations(
        CIntegerLiteralExpression.class, CIntegerLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(CLeftHandSide.class, CLeftHandSideMixin.class);
    pContext.setMixInAnnotations(CLiteralExpression.class, CLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(CParameterDeclaration.class, CParameterDeclarationMixin.class);
    pContext.setMixInAnnotations(CPointerType.class, CPointerTypeMixin.class);
    pContext.setMixInAnnotations(CProblemType.class, CProblemTypeMixin.class);
    pContext.setMixInAnnotations(CReturnStatementEdge.class, CReturnStatementEdgeMixin.class);
    pContext.setMixInAnnotations(CReturnStatement.class, CReturnStatementMixin.class);
    pContext.setMixInAnnotations(CRightHandSide.class, CRightHandSideMixin.class);
    pContext.setMixInAnnotations(CSimpleDeclaration.class, CSimpleDeclarationMixin.class);
    pContext.setMixInAnnotations(CSimpleType.class, CSimpleTypeMixin.class);
    pContext.setMixInAnnotations(CStatementEdge.class, CStatementEdgeMixin.class);
    pContext.setMixInAnnotations(CStatement.class, CStatementMixin.class);
    pContext.setMixInAnnotations(
        CStringLiteralExpression.class, CStringLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(
        CThreadOperationStatement.class, CThreadOperationStatementMixin.class);
    pContext.setMixInAnnotations(CTypeDeclaration.class, CTypeDeclarationMixin.class);
    pContext.setMixInAnnotations(CType.class, CTypeMixin.class);
    pContext.setMixInAnnotations(CVariableDeclaration.class, CVariableDeclarationMixin.class);
    pContext.setMixInAnnotations(CVoidType.class, CVoidTypeMixin.class);
    pContext.setMixInAnnotations(CfaJsonModule.class, CfaJsonModule.class);
    pContext.setMixInAnnotations(CfaMetadata.class, CfaMetadataMixin.class);
    pContext.setMixInAnnotations(FileLocation.class, FileLocationMixin.class);
    pContext.setMixInAnnotations(FunctionCallEdge.class, FunctionCallEdgeMixin.class);
    pContext.setMixInAnnotations(FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(FunctionExitNode.class, FunctionExitNodeMixin.class);
    pContext.setMixInAnnotations(FunctionReturnEdge.class, FunctionReturnEdgeMixin.class);
    pContext.setMixInAnnotations(FunctionSummaryEdge.class, FunctionSummaryEdgeMixin.class);
    pContext.setMixInAnnotations(Loop.class, LoopMixin.class);
    pContext.setMixInAnnotations(LoopStructure.class, LoopStructureMixin.class);
    pContext.setMixInAnnotations(Partition.class, PartitionMixin.class);
    pContext.setMixInAnnotations(Type.class, TypeMixin.class);
    pContext.setMixInAnnotations(VariableClassification.class, VariableClassificationMixin.class);
  }
}
