// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Contains classes and interfaces for implementing CFA transformers.
 *
 * <p>CFA transformers return for given CFAs (the original CFAs) new CFAs (the transformed CFAs)
 * that differ from the original CFA in some specific way (e.g, a CFA transformer may add conditions
 * to all array accesses to ensure that no out-of-bounds accesses occur). To make this easier, we
 * typically use {@link org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork FlexCfaNetwork}
 * implementations. These makes it straightforward to apply modifications as we don't have to keep
 * multiple elements (i.e., CFA nodes and edges) in sync (e.g., we don't have to create a CFA edge
 * with specific endpoints and also add the edge at those endpoints).
 *
 * <p>It's also possible to use CFA node/edge transformers that are applied to all nodes/edges
 * during CFA construction. Not only can complete nodes/edges be replaced with, e.g., nodes/edges of
 * a different type, but also modified copies of their AST nodes can be created during node/edge
 * transformation. There are transforming and substituting AST node visitors that make changing AST
 * nodes of a certain type a lot easier (e.g., changing all variable identifiers).
 *
 * <p>A typical CFA transformer looks like the following exemplary implementation:
 *
 * <pre>{@code
 * public final class SomeTransformer implements CfaTransformer {
 *
 *     {@literal @}Override
 *     public CFA transform(CfaNetwork pCfaNetwork,
 *                          CfaMetadata pCfaMetadata,
 *                          LogManager pLogger,
 *                          ShutdownNotifier pShutdownNotifier) {
 *
 *         // creates a mutable copy of the specified CFA
 *         FlexCfaNetwork flexCfaNetwork = FlexCfaNetwork.copy(pCfaNetwork);
 *
 *         // ---- start actual CFA transformation ----
 *
 *         // add, remove, replace nodes/edges
 *         flexCfaNetwork.removeNode(...);
 *         flexCfaNetwork.addEdge(...);
 *         flexCfaNetwork.replaceEdge(...);
 *
 *         // there are also more advanced operations
 *         // before: --- a ---> [node] --- b ---->
 *         // after: --- a ---> [node] --- newOutEdge ---> [newSuccessor] --- b ---->
 *         flexCfaNetwork.insertSuccessor(...);
 *
 *         // it's also possible to change the CFA metadata (e.g., change the function entry node)
 *         CfaMetadata newCfaMetadata = pCfaMetadata.withFunctionEntryNode(...);
 *
 *         // CFA node/edge transformers can be used that are applied to all nodes/edges during CFA
 *         // construction. Additionally, transforming AST node visitors can be used to transform
 *         // specific AST nodes contained in those CFA nodes/edges (e.g., changing all variable
 *         // identifiers).
 *         CfaEdgeTransformer edgeTransformer = ...
 *
 *         // ---- end actual CFA transformation ----
 *
 *         // create a new transformed CFA and return it
 *         CfaFactory cfaFactory =
 *             SomeCfaFactory
 *                .transformEdges(edgeTransformer)
 *                .executePostProcessor(...)
 *                .toSupergraph()
 *                .executePostProcessors(...);
 *
 *         return cfaFactory.createCfa(flexCfaNetwork, newCfaMetadata, pLogger, pShutdownNotifier);
 *     }
 * }
 * }</pre>
 *
 * There are six main interfaces:
 *
 * <ul>
 *   <li>{@link org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer CfaTransformer}: Interface
 *       all CFA transformers should implement.
 *   <li>{@link org.sosy_lab.cpachecker.cfa.transformer.CfaFactory CfaFactory}: Factories that
 *       create new {@link org.sosy_lab.cpachecker.cfa.CFA CFA} instances for CFAs represented by
 *       ({@link org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork}, {@link
 *       org.sosy_lab.cpachecker.cfa.CfaMetadata CfaMetadata}) pairs should implement this
 *       interface.
 *   <li>{@link org.sosy_lab.cpachecker.cfa.transformer.CfaNodeTransformer CfaNodeTransformer}:
 *       Interface all CFA node transformers should implement. CFA node transformers are for
 *       creating new transformed nodes for given nodes.
 *   <li>{@link org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeTransformer CfaEdgeTransformer}:
 *       Interface all CFA edge transformers should implement. CFA edge transformers are for
 *       creating new transformed edges for given edges.
 *   <li>{@link org.sosy_lab.cpachecker.cfa.transformer.CfaNodeProvider CfaNodeProvider}: The
 *       creation of some CFA nodes/edges depends on other nodes (e.g., to create a new CFA edge, we
 *       need its endpoints). {@link org.sosy_lab.cpachecker.cfa.transformer.CfaNodeProvider
 *       CfaNodeProvider} implementations resolve those dependencies. CFA node/edge transformers use
 *       {@link org.sosy_lab.cpachecker.cfa.transformer.CfaNodeProvider CfaNodeProvider} the
 *       following way: Given the nodes the original node/edge depends on, return the corresponding
 *       transformed nodes.
 *   <li>{@link org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeProvider CfaEdgeProvider}: The
 *       creation of some CFA edges depends on some other edges (e.g., to create a new function call
 *       edge, we needs its corresponding summary edge). {@link
 *       org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeProvider CfaEdgeProvider} implementations
 *       resolve those dependencies. CFA edge transformers use {@link
 *       org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeProvider CfaEdgeProvider} the following way:
 *       Given the edges the original edge depends on, return the corresponding transformed edges.
 * </ul>
 */
package org.sosy_lab.cpachecker.cfa.transformer;
