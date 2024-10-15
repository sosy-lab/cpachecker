// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.deserialization;

import java.io.IOException;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class CfaEdgeTypeIdResolver extends TypeIdResolverBase {
  private JavaType superType;
  private static BiMap<String, Class<?>> typeMap;

  @Override
  public void init(JavaType baseType) {
      superType = baseType;

    typeMap = HashBiMap.create();
    typeMap.putAll(Map.ofEntries(
    Map.entry("AStatement", AStatementEdge.class),
    Map.entry("CStatement", CStatementEdge.class),
    Map.entry("AReturnStatement", AReturnStatementEdge.class),
    Map.entry("CAssume", CAssumeEdge.class),
    Map.entry("ADeclaration", ADeclarationEdge.class),
    Map.entry("CReturnStatement", CReturnStatementEdge.class),
    Map.entry("CFunctionReturn", CFunctionReturnEdge.class),
    Map.entry("FunctionCall", FunctionCallEdge.class),
    Map.entry("CFunctionSummary", CFunctionSummaryEdge.class),
    Map.entry("Dummy", DummyCFAEdge.class),
    Map.entry("FunctionSummary", FunctionSummaryEdge.class),
    Map.entry("CDeclaration", CDeclarationEdge.class),
    Map.entry("Blankk", BlankEdge.class),
    Map.entry("CFunctionCall", CFunctionCallEdge.class),
    Map.entry("FunctionReturn", FunctionReturnEdge.class),
    Map.entry("CFunctionSummaryStatement", CFunctionSummaryStatementEdge.class),
    Map.entry("Assume", AssumeEdge.class)
  ));
  }

  @Override
  public String idFromValue(Object value) {
      return typeMap.inverse().get(value.getClass());
  }

  @Override
  public JavaType typeFromId(DatabindContext context, String id) throws IOException {
      Class<?> subType = typeMap.get(id);

      if (subType == null) {
          throw new IOException("Unknown type id " + id);
      }

      return context.constructSpecializedType(superType, subType);
  }

  @Override
  public Id getMechanism() {
    throw new UnsupportedOperationException("Unimplemented method 'getMechanism'");
  }

  @Override
  public String idFromValueAndType(Object pArg0, Class<?> pArg1) {
    throw new UnsupportedOperationException("Unimplemented method 'idFromValueAndType'");
  }
}
