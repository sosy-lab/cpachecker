/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;

/**
 * A view on a CLangSMG, where no modifications are allowed.
 *
 * <p>All returned Collections are unmodifiable.
 */
public interface UnmodifiableCLangSMG extends UnmodifiableSMG {

  @Override
  CLangSMG copyOf();

  boolean containsInvalidElement(Object elem);

  String getNoteMessageOnElement(Object elem);

  boolean isStackEmpty();

  SMGRegion getObjectForVisibleVariable(String pVariableName);

  PersistentStack<CLangStackFrame> getStackFrames();

  Set<SMGObject> getHeapObjects();

  boolean isHeapObject(SMGObject object);

  PersistentMap<String, SMGRegion> getGlobalObjects();

  boolean isGlobal(SMGObject object);

  SMGObject getFunctionReturnObject();

  IDExpression createIDExpression(SMGObject pObject);

  Optional<SMGEdgeHasValue> getHVEdgeFromMemoryLocation(SMGMemoryPath pLocation);

  Set<SMGMemoryPath> getMemoryPaths();

  Map<SMGObject, SMGMemoryPath> getHeapObjectMemoryPaths();
}
