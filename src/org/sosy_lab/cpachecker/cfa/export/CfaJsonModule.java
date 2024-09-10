// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/* This class is a Jackson module for serialization and deserialization. */
public class CfaJsonModule extends com.fasterxml.jackson.databind.module.SimpleModule {

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
        org.sosy_lab.cpachecker.cfa.model.CFAEdge.class, CFAEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.CfaMetadata.class, CfaMetadataMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.CFANode.class, CFANodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.class,
        CFunctionDeclarationMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.FileLocation.class, FileLocationMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.FunctionExitNode.class, FunctionExitNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.util.variableclassification.Partition.class, PartitionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.types.c.CFunctionType.class, CFunctionTypeMixin.class);
    pContext.setMixInAnnotations(org.sosy_lab.cpachecker.cfa.types.Type.class, TypeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames.class,
        CFunctionTypeWithNamesMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.types.c.CSimpleType.class, CSimpleTypeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.AAstNode.class, AAstNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration.class,
        CVariableDeclarationMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression.class,
        CInitializerExpressionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.class,
        CIntegerLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode.class,
        CFunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression.class, CIdExpressionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.CFALabelNode.class, CFALabelNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.BlankEdge.class, BlankEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge.class, CDeclarationEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge.class, CAssumeEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.class, CBinaryExpressionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge.class, CStatementEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement.class,
        CExpressionAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement.class,
        CExpressionStatementMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge.class,
        CReturnStatementEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement.class, CReturnStatementMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.util.LoopStructure.class, LoopStructureMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.util.variableclassification.VariableClassification.class,
        VariableClassificationMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.util.LoopStructure.Loop.class, LoopMixin.class);
  }

  /**
   * Represents an entry in a {@link com.google.common.collect.Table}.
   *
   * <p>This class encapsulates information about a CFAEdge, a column, and a Partition.
   */
  private record TableEntry(
      org.sosy_lab.cpachecker.cfa.model.CFAEdge edge,
      Integer column,
      org.sosy_lab.cpachecker.util.variableclassification.Partition partition) {}

  /**
   * A converter class that converts a {@link com.google.common.collect.Table} object to a list of
   * {@link TableEntry} objects.
   *
   * <p>The Table object represents a mapping between CFAEdges, Integers, and Partitions.
   */
  private static final class EtpTableToListConverter
      extends com.fasterxml.jackson.databind.util.StdConverter<
          com.google.common.collect.Table<
              org.sosy_lab.cpachecker.cfa.model.CFAEdge,
              Integer,
              org.sosy_lab.cpachecker.util.variableclassification.Partition>,
          java.util.List<TableEntry>> {
    @Override
    public java.util.List<TableEntry> convert(
        com.google.common.collect.Table<
                org.sosy_lab.cpachecker.cfa.model.CFAEdge,
                Integer,
                org.sosy_lab.cpachecker.util.variableclassification.Partition>
            pTable) {
      return pTable.cellSet().stream()
          .map(cell -> new TableEntry(cell.getRowKey(), cell.getColumnKey(), cell.getValue()))
          .collect(java.util.stream.Collectors.toList());
    }
  }

  /**
   * A converter class that converts a list of {@link TableEntry} objects to a {@link
   * com.google.common.collect.Table} object.
   *
   * <p>The Table object represents a mapping between CFAEdges, Integers, and Partitions.
   */
  private static final class ListToEtpTableConverter
      extends com.fasterxml.jackson.databind.util.StdConverter<
          java.util.List<TableEntry>,
          com.google.common.collect.Table<
              org.sosy_lab.cpachecker.cfa.model.CFAEdge,
              Integer,
              org.sosy_lab.cpachecker.util.variableclassification.Partition>> {
    @Override
    public com.google.common.collect.Table<
            org.sosy_lab.cpachecker.cfa.model.CFAEdge,
            Integer,
            org.sosy_lab.cpachecker.util.variableclassification.Partition>
        convert(java.util.List<TableEntry> pList) {
      return pList.stream()
          .collect(
              com.google.common.collect.HashBasedTable::create,
              (table, entry) -> table.put(entry.edge, entry.column, null),
              com.google.common.collect.HashBasedTable::putAll);
    }
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.util.LoopStructure.Loop}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class LoopMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    private LoopMixin(
        @JsonProperty("loopHeads")
            java.util.Set<org.sosy_lab.cpachecker.cfa.model.CFANode> pLoopHeads,
        @JsonProperty("nodes") java.util.Set<org.sosy_lab.cpachecker.cfa.model.CFANode> pNodes) {}
  }

  /**
   * This class is a mixin for {@link
   * org.sosy_lab.cpachecker.util.variableclassification.VariableClassification}.
   *
   * <p>It converts the edgeToPartitions field to a list of TableEntry objects during serialization
   * and back to a Table object during deserialization.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class VariableClassificationMixin {

    @JsonSerialize(converter = EtpTableToListConverter.class)
    @JsonDeserialize(converter = ListToEtpTableConverter.class)
    private com.google.common.collect.Table<
            org.sosy_lab.cpachecker.cfa.model.CFAEdge,
            Integer,
            org.sosy_lab.cpachecker.util.variableclassification.Partition>
        edgeToPartitions;

    @SuppressWarnings("unused")
    @JsonCreator
    VariableClassificationMixin(
        @JsonProperty("hasRelevantNonIntAddVars") boolean pHasRelevantNonIntAddVars,
        @JsonProperty("intBoolVars") java.util.Set<java.lang.String> pIntBoolVars,
        @JsonProperty("intEqualVars") java.util.Set<java.lang.String> pIntEqualVars,
        @JsonProperty("intAddVars") java.util.Set<java.lang.String> pIntAddVars,
        @JsonProperty("intOverflowVars") java.util.Set<java.lang.String> pIntOverflowVars,
        @JsonProperty("relevantVariables") java.util.Set<java.lang.String> pRelevantVariables,
        @JsonProperty("addressedVariables") java.util.Set<java.lang.String> pAddressedVariables,
        @JsonProperty("relevantFields")
            com.google.common.collect.Multimap<
                    org.sosy_lab.cpachecker.cfa.types.c.CCompositeType, java.lang.String>
                pRelevantFields,
        @JsonProperty("addressedFields")
            com.google.common.collect.Multimap<
                    org.sosy_lab.cpachecker.cfa.types.c.CCompositeType, java.lang.String>
                pAddressedFields,
        @JsonProperty("partitions")
            java.util.Collection<org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pPartitions,
        @JsonProperty("intBoolPartitions")
            java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pIntBoolPartitions,
        @JsonProperty("intEqualPartitions")
            java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pIntEqualPartitions,
        @JsonProperty("intAddPartitions")
            java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pIntAddPartitions,
        @JsonProperty("edgeToPartitions")
            com.google.common.collect.Table<
                    org.sosy_lab.cpachecker.cfa.model.CFAEdge,
                    java.lang.Integer,
                    org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pEdgeToPartitions,
        @JsonProperty("assumedVariables")
            com.google.common.collect.Multiset<java.lang.String> pAssumedVariables,
        @JsonProperty("assignedVariables")
            com.google.common.collect.Multiset<java.lang.String> pAssignedVariables) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.util.LoopStructure}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class LoopStructureMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    private LoopStructureMixin(
        @JsonProperty("loops")
            com.google.common.collect.ImmutableListMultimap<
                    java.lang.String, org.sosy_lab.cpachecker.util.LoopStructure.Loop>
                pLoops) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CReturnStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CReturnStatementMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("expression")
            java.util.Optional<org.sosy_lab.cpachecker.cfa.ast.c.CExpression> pExpression,
        @JsonProperty("assignment")
            java.util.Optional<org.sosy_lab.cpachecker.cfa.ast.c.CAssignment> pAssignment) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CReturnStatementEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CReturnStatementEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawStatement,
        @JsonProperty("returnStatement")
            org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement pReturnStatement,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.FunctionExitNode pSuccessor) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CExpressionStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CExpressionStatementMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("expression") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pExpression) {}
  }

  /**
   * This class is a mixin for {@link
   * org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CExpressionAssignmentStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CExpressionAssignmentStatementMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("leftHandSide") org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide pLeftHandSide,
        @JsonProperty("rightHandSide")
            org.sosy_lab.cpachecker.cfa.ast.c.CExpression pRightHandSide) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CStatementEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CStatementEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawStatement,
        @JsonProperty("statement") org.sosy_lab.cpachecker.cfa.ast.c.CStatement pStatement,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.CFANode pSuccessor) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CBinaryExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CBinaryExpressionMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CType pExpressionType,
        @JsonProperty("calculationType") org.sosy_lab.cpachecker.cfa.types.c.CType pCalculationType,
        @JsonProperty("operand1") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pOperand1,
        @JsonProperty("operand2") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pOperand2,
        @JsonProperty("operator")
            org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator pOperator) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CAssumeEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CAssumeEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawStatement,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.CFANode pSuccessor,
        @JsonProperty("expression") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pExpression,
        @JsonProperty("truthAssumption") boolean pTruthAssumption,
        @JsonProperty("swapped") boolean pSwapped,
        @JsonProperty("artificialIntermediate") boolean pArtificial) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CDeclarationEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CDeclarationEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawSignature,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.CFANode pSuccessor,
        @JsonProperty("declaration") org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration pDeclaration) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.BlankEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class BlankEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public BlankEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawStatement,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.CFANode pSuccessor,
        @JsonProperty("description") java.lang.String pDescription) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.CFALabelNode}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFALabelNodeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFALabelNodeMixin(
        @JsonProperty("function") org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration pFunction,
        @JsonProperty("label") java.lang.String pLabel) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CIdExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CIdExpressionMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CType pType,
        @JsonProperty("name") java.lang.String pName,
        @JsonProperty("declaration")
            org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration pDeclaration) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionEntryNodeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionEntryNodeMixin(
        @JsonProperty("location") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("functionDefinition")
            org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration pFunctionDefinition,
        @JsonProperty("exitNode") org.sosy_lab.cpachecker.cfa.model.FunctionExitNode pExitNode,
        @JsonProperty("returnVariable")
            java.util.Optional<org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration>
                pReturnVariable) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CIntegerLiteralExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CIntegerLiteralExpressionMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CType pType,
        @JsonProperty("value") java.math.BigInteger pValue) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CInitializerExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CInitializerExpressionMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("expression") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pExpression) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CVariableDeclarationMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CVariableDeclarationMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("isGlobal") boolean pIsGlobal,
        @JsonProperty("cStorageClass")
            org.sosy_lab.cpachecker.cfa.types.c.CStorageClass pCStorageClass,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CType pType,
        @JsonProperty("name") java.lang.String pName,
        @JsonProperty("origName") java.lang.String pOrigName,
        @JsonProperty("qualifiedName") java.lang.String pQualifiedName,
        @JsonProperty("initializer") org.sosy_lab.cpachecker.cfa.ast.c.CInitializer pInitializer) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.AAstNode}.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   */
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfAAstNode")
  private static final class AAstNodeMixin {}

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.types.c.CSimpleType}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CSimpleTypeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CSimpleTypeMixin(
        @JsonProperty("isConst") boolean pConst,
        @JsonProperty("isVolatile") boolean pVolatile,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CBasicType pType,
        @JsonProperty("isLong") boolean pIsLong,
        @JsonProperty("isShort") boolean pIsShort,
        @JsonProperty("isSigned") boolean pIsSigned,
        @JsonProperty("isUnsigned") boolean pIsUnsigned,
        @JsonProperty("isComplex") boolean pIsComplex,
        @JsonProperty("isImaginary") boolean pIsImaginary,
        @JsonProperty("isLongLong") boolean pIsLongLong) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionTypeWithNamesMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionTypeWithNamesMixin(
        @JsonProperty("returnType") org.sosy_lab.cpachecker.cfa.types.c.CType pReturnType,
        @JsonProperty("parameters")
            java.util.List<org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration> pParameters,
        @JsonProperty("takesVarArgs") boolean pTakesVarArgs) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.types.Type}.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   */
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfType")
  private static final class TypeMixin {}

  /**
   * This class is a mixin for {@link org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionTypeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionTypeMixin(
        @JsonProperty("returnType") org.sosy_lab.cpachecker.cfa.types.c.CType pReturnType,
        @JsonProperty("parameters")
            java.util.List<org.sosy_lab.cpachecker.cfa.types.c.CType> pParameters,
        @JsonProperty("takesVarArgs") boolean pTakesVarArgs) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.FileLocation}.
   *
   * <p>It forces the serialization of the {@link java.nio.file.Path} fileName field.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class FileLocationMixin {

    @JsonProperty private java.nio.file.Path fileName;

    @SuppressWarnings("unused")
    @JsonCreator
    public FileLocationMixin(
        @JsonProperty("fileName") java.nio.file.Path pFileName,
        @JsonProperty("niceFileName") java.lang.String pNiceFileName,
        @JsonProperty("offset") int pOffset,
        @JsonProperty("length") int pLength,
        @JsonProperty("startingLine") int pStartingLine,
        @JsonProperty("endingLine") int pEndingLine,
        @JsonProperty("startColumnInLine") int pStartColumnInLine,
        @JsonProperty("endColumnInLine") int pEndColumnInLine,
        @JsonProperty("startingLineInOrigin") int pStartingLineInOrigin,
        @JsonProperty("endingLineInOrigin") int pEndingLineInOrigin,
        @JsonProperty("offsetRelatedToOrigin") boolean pOffsetRelatedToOrigin) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.CFAEdge}.
   *
   * <p>Identity information is being serialized to prevent infinite recursion.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   */
  @JsonIdentityInfo(
      generator = ObjectIdGenerators.IntSequenceGenerator.class,
      property = "edgeNumber")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfCFAEdge")
  private static final class CFAEdgeMixin {}

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.CfaMetadata}.
   *
   * <p>It ensures that the {@link org.sosy_lab.cpachecker.util.ast.AstCfaRelation} is not being
   * serialized.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CfaMetadataMixin {

    @JsonIgnore private org.sosy_lab.cpachecker.util.ast.AstCfaRelation astCFARelation;

    @SuppressWarnings("unused")
    @JsonCreator
    private CfaMetadataMixin(
        @JsonProperty("machineModel") org.sosy_lab.cpachecker.cfa.types.MachineModel pMachineModel,
        @JsonProperty("language") org.sosy_lab.cpachecker.cfa.Language pLanguage,
        @JsonProperty("fileNames") java.util.List<java.nio.file.Path> pFileNames,
        @JsonProperty("mainFunctionEntry")
            org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode pMainFunctionEntry,
        @JsonProperty("connectedness") org.sosy_lab.cpachecker.cfa.CfaConnectedness pConnectedness,
        @JsonProperty("astCFARelation")
            org.sosy_lab.cpachecker.util.ast.AstCfaRelation pAstCfaRelation,
        @JsonProperty("loopStructure") org.sosy_lab.cpachecker.util.LoopStructure pLoopStructure,
        @JsonProperty("variableClassification")
            org.sosy_lab.cpachecker.util.variableclassification.VariableClassification
                pVariableClassification,
        @JsonProperty("liveVariables") org.sosy_lab.cpachecker.util.LiveVariables pLiveVariables) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.CFANode}.
   *
   * <p>Identity information is being serialized to prevent infinite recursion.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      scope = org.sosy_lab.cpachecker.cfa.model.CFANode.class,
      property = "nodeNumber")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfCFANode")
  private static final class CFANodeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFANodeMixin(
        @JsonProperty("function") org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration pFunction) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionDeclarationMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionDeclarationMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CFunctionType pType,
        @JsonProperty("name") java.lang.String pName,
        @JsonProperty("origName") java.lang.String pOrigName,
        @JsonProperty("parameters")
            java.util.List<org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration> parameters,
        @JsonProperty("attributes")
            com.google.common.collect.ImmutableSet<
                    org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute>
                pAttributes) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode}.
   *
   * <p>It serializes its {@link org.sosy_lab.cpachecker.cfa.model.FunctionExitNode} field as
   * number.
   */
  private static final class FunctionEntryNodeMixin {

    @JsonIdentityReference(alwaysAsId = true)
    private org.sosy_lab.cpachecker.cfa.model.FunctionExitNode exitNode;
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.FunctionExitNode}.
   *
   * <p>It serializes its {@link org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode} field as
   * number.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class FunctionExitNodeMixin {

    @JsonIdentityReference(alwaysAsId = true)
    private org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode entryNode;

    @SuppressWarnings("unused")
    @JsonCreator
    public FunctionExitNodeMixin(
        @JsonProperty("function") org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration pFunction) {}
  }

  /**
   * This class is a mixin for {@link
   * org.sosy_lab.cpachecker.util.variableclassification.Partition}.
   *
   * <p>It prevents cyclic references by serializing the {@link
   * org.sosy_lab.cpachecker.util.variableclassification.Partition} as index if the same object has
   * already been fully serialized once.
   *
   * <p>It converts the edgeToPartition field to a list of TableEntry objects during serialization
   * and back to a Table object during deserialization.
   */
  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      scope = org.sosy_lab.cpachecker.util.variableclassification.Partition.class,
      property = "index")
  private static final class PartitionMixin {

    @JsonSerialize(converter = EtpTableToListConverter.class)
    @JsonDeserialize(converter = ListToEtpTableConverter.class)
    private com.google.common.collect.Table<
            org.sosy_lab.cpachecker.cfa.model.CFAEdge,
            Integer,
            org.sosy_lab.cpachecker.util.variableclassification.Partition>
        edgeToPartition;
  }
}
