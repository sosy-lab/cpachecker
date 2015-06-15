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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class allows to obtain interpolants statically from a given ARGPath.
 */
public class UseDefBasedInterpolator {

  /**
   * the logger in use
   */
  private final LogManager logger;

  /**
   * the use-def relation of the final, failing (assume) edge
   */
  private final UseDefRelation useDefRelation;

  /**
   * the sliced infeasible prefix for which to compute the interpolants
   */
  private final ARGPath slicedPrefix;

  /**
   * the machine model in use
   */
  private final MachineModel machineModel;

  /**
   * This class allows the creation of (fake) interpolants by using the use-def-relation.
   * This interpolation approach only works if the given path is a sliced prefix,
   * obtained via {@link PrefixSelector#obtainSlicedPrefix}.
   *
   * @param pSlicedPrefix
   * @param pUseDefRelation
   * @param pMachineModel
   */
  public UseDefBasedInterpolator(LogManager pLogger, ARGPath pSlicedPrefix, UseDefRelation pUseDefRelation, MachineModel pMachineModel) {
    logger         = pLogger;
    slicedPrefix   = pSlicedPrefix;
    useDefRelation = pUseDefRelation;
    machineModel   = pMachineModel;
  }

  /**
   * This method obtains the interpolation sequence as pairs of {@link ARGState}s
   * and their respective {@link ValueAnalysisInterpolant}s.
   *
   * @return the (ordered) list of {@link ARGState}s and their respective {@link ValueAnalysisInterpolant}s
   */
  public List<Pair<ARGState, ValueAnalysisInterpolant>> obtainInterpolants() {
    Map<ARGState, Collection<ASimpleDeclaration>> useDefSequence = useDefRelation.getExpandedUses(slicedPrefix);
    ValueAnalysisInterpolant trivialItp = ValueAnalysisInterpolant.FALSE;

    LinkedList<Pair<ARGState, ValueAnalysisInterpolant>> interpolants = new LinkedList<>();
    PathIterator iterator = slicedPrefix.reversePathIterator();
    while (iterator.hasNext()) {
      ARGState state = iterator.getAbstractState();

      Collection<ASimpleDeclaration> uses = useDefSequence.get(state);

      ValueAnalysisInterpolant interpolant = uses.isEmpty()
          ? trivialItp
          : createInterpolant(uses);

      interpolants.addFirst(Pair.of(state, interpolant));

      // as the traversal goes backwards, once the interpolant was non-trivial once,
      // the next time it is trivial, it has to be TRUE, and no longer FALSE
      if (interpolant != trivialItp) {
        trivialItp = ValueAnalysisInterpolant.TRUE;
      }

      iterator.advance();
    }

    return interpolants;
  }

  /**
   * This method obtains the interpolation sequence as mapping from {@link ARGState}s
   * to their respective {@link ValueAnalysisInterpolant}s.
   *
   * @return the (ordered) mapping from {@link ARGState}s to their respective {@link ValueAnalysisInterpolant}s
   */
  public Map<ARGState, ValueAnalysisInterpolant> obtainInterpolantsAsMap() {

    Map<ARGState, ValueAnalysisInterpolant> interpolants = new LinkedHashMap<>();
    for(Pair<ARGState, ValueAnalysisInterpolant> itp : obtainInterpolants()) {
      interpolants.put(itp.getFirst(), itp.getSecond());
    }

    return interpolants;
  }

  private ValueAnalysisInterpolant createInterpolant(Collection<ASimpleDeclaration> uses) {
    HashMap<MemoryLocation, Value> useDefInterpolant = new HashMap<>();

    for (ASimpleDeclaration use : uses) {

      Set<MemoryLocation> memoryLocations = (use.getType() instanceof CArrayType)
        ? memoryLocations = createMemoryLocationsForArray(use)
        : Collections.singleton(MemoryLocation.valueOf(use.getQualifiedName()));

      for (MemoryLocation memoryLocation : memoryLocations) {
        useDefInterpolant.put(memoryLocation, UnknownValue.getInstance());
      }
    }

    return new ValueAnalysisInterpolant(useDefInterpolant, Collections.<MemoryLocation, Type>emptyMap());
  }

  /**
   * This method returns a set of memory locations for an array.
   *
   * As this interpolation is static, memory locations for the whole array are added.
   * If the size of the array is not known statically, than a fixed number of memory locations
   * are created.
   */
  private Set<MemoryLocation> createMemoryLocationsForArray(ASimpleDeclaration arrayDeclaration) {
    CArrayType arrayType = (CArrayType)arrayDeclaration.getType();

    int length = 20; // magic
    if (arrayType.getLength() instanceof CLiteralExpression) {
      try {
        length = NumberFormat.getInstance().parse(arrayType.getLength().toString()).intValue();
      } catch (ParseException e) {
        logger.log(Level.INFO, e, "Could not parse array length expression", arrayType.getLength().toString(), "to an integer representation.");
      }
    }

    Set<MemoryLocation> arrayMemoryLocations = new HashSet<>();
    int size = machineModel.getSizeof(arrayType.getType());
    for (int i = 0; i < length; i++) {
      arrayMemoryLocations.add(MemoryLocation.valueOf(arrayDeclaration.getQualifiedName(), i * size));
    }

    return arrayMemoryLocations;
  }
}
