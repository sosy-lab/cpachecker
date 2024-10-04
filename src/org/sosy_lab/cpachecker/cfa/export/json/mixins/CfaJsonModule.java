// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
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
    pContext.setMixInAnnotations(AAstNode.class, AAstNodeMixin.class);
    pContext.setMixInAnnotations(AbstractCFAEdge.class, AbstractCFAEdgeMixin.class);
    pContext.setMixInAnnotations(BlankEdge.class, BlankEdgeMixin.class);
    pContext.setMixInAnnotations(
        CArraySubscriptExpression.class, CArraySubscriptExpressionMixin.class);
    pContext.setMixInAnnotations(CArrayType.class, CArrayTypeMixin.class);
    pContext.setMixInAnnotations(CAssumeEdge.class, CAssumeEdgeMixin.class);
    pContext.setMixInAnnotations(CBinaryExpression.class, CBinaryExpressionMixin.class);
    pContext.setMixInAnnotations(CDeclarationEdge.class, CDeclarationEdgeMixin.class);
    pContext.setMixInAnnotations(
        CExpressionAssignmentStatement.class, CExpressionAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(CExpressionStatement.class, CExpressionStatementMixin.class);
    pContext.setMixInAnnotations(CFAEdge.class, CFAEdgeMixin.class);
    pContext.setMixInAnnotations(CFALabelNode.class, CFALabelNodeMixin.class);
    pContext.setMixInAnnotations(CfaMetadata.class, CfaMetadataMixin.class);
    pContext.setMixInAnnotations(CFANode.class, CFANodeMixin.class);
    pContext.setMixInAnnotations(CFATerminationNode.class, CFATerminationNodeMixin.class);
    pContext.setMixInAnnotations(
        CFunctionCallAssignmentStatement.class, CFunctionCallAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(CFunctionCallEdge.class, CFunctionCallEdgeMixin.class);
    pContext.setMixInAnnotations(CFunctionCallExpression.class, CFunctionCallExpressionMixin.class);
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
    pContext.setMixInAnnotations(
        CIntegerLiteralExpression.class, CIntegerLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(CParameterDeclaration.class, CParameterDeclarationMixin.class);
    pContext.setMixInAnnotations(CPointerType.class, CPointerTypeMixin.class);
    pContext.setMixInAnnotations(CProblemType.class, CProblemTypeMixin.class);
    pContext.setMixInAnnotations(CReturnStatement.class, CReturnStatementMixin.class);
    pContext.setMixInAnnotations(CReturnStatementEdge.class, CReturnStatementEdgeMixin.class);
    pContext.setMixInAnnotations(CSimpleType.class, CSimpleTypeMixin.class);
    pContext.setMixInAnnotations(CStatementEdge.class, CStatementEdgeMixin.class);
    pContext.setMixInAnnotations(
        CStringLiteralExpression.class, CStringLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(CVariableDeclaration.class, CVariableDeclarationMixin.class);
    pContext.setMixInAnnotations(CVoidType.class, CVoidTypeMixin.class);
    pContext.setMixInAnnotations(FileLocation.class, FileLocationMixin.class);
    pContext.setMixInAnnotations(FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(FunctionExitNode.class, FunctionExitNodeMixin.class);
    pContext.setMixInAnnotations(Loop.class, LoopMixin.class);
    pContext.setMixInAnnotations(LoopStructure.class, LoopStructureMixin.class);
    pContext.setMixInAnnotations(Partition.class, PartitionMixin.class);
    pContext.setMixInAnnotations(Type.class, TypeMixin.class);
    pContext.setMixInAnnotations(VariableClassification.class, VariableClassificationMixin.class);
  }
}
