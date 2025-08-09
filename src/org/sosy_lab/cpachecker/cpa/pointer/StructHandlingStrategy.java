// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.pointer.location.FieldScopeStructLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InstanceScopeStructLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.TypeScopeStructLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;

public enum StructHandlingStrategy {
  JUST_STRUCT {
    @Override
    public LocationSet getUnionLocation(CType structType, String instanceName, CFAEdge edge) {
      return LocationSetFactory.withPointerLocation(
          new TypeScopeStructLocation(edge.getPredecessor().getFunctionName(), structType));
    }

    @Override
    public LocationSet getStructLocation(
        CType structType, String instanceName, String fieldName, CFAEdge edge) {
      return LocationSetFactory.withPointerLocation(
          new TypeScopeStructLocation(edge.getPredecessor().getFunctionName(), structType));
    }
  },
  STRUCT_INSTANCE {
    @Override
    public LocationSet getUnionLocation(CType structType, String instanceName, CFAEdge edge) {
      return LocationSetFactory.withPointerLocation(
          new InstanceScopeStructLocation(
              edge.getPredecessor().getFunctionName(), structType, instanceName));
    }

    @Override
    public LocationSet getStructLocation(
        CType structType, String instanceName, String fieldName, CFAEdge edge) {
      return LocationSetFactory.withPointerLocation(
          new InstanceScopeStructLocation(
              edge.getPredecessor().getFunctionName(), structType, instanceName));
    }
  },
  ALL_FIELDS {
    @Override
    public LocationSet getUnionLocation(CType structType, String instanceName, CFAEdge edge) {
      return LocationSetFactory.withPointerLocation(
          new InstanceScopeStructLocation(
              edge.getPredecessor().getFunctionName(), structType, instanceName));
    }

    @Override
    public LocationSet getStructLocation(
        CType structType, String instanceName, String fieldName, CFAEdge edge) {
      return LocationSetFactory.withPointerLocation(
          new FieldScopeStructLocation(
              edge.getPredecessor().getFunctionName(), structType, instanceName, fieldName));
    }
  };

  public abstract LocationSet getUnionLocation(CType structType, String instanceName, CFAEdge edge);

  public abstract LocationSet getStructLocation(
      CType structType, String instanceName, String fieldName, CFAEdge edge);
}
