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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static org.junit.Assert.*;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.MapMerger.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableMap;


public class MapMergerTest {

  private static final PersistentSortedMap<String, String> EMPTY_MAP =
      PathCopyingPersistentTreeMap.<String, String>of();

  private static final PersistentSortedMap<String, String> HALF1_MAP =
      PathCopyingPersistentTreeMap.copyOf(ImmutableMap.of("a", "1",
                                                          "c", "3"));

  private static final PersistentSortedMap<String, String> HALF2_MAP =
      PathCopyingPersistentTreeMap.copyOf(ImmutableMap.of("b", "2",
                                                          "d", "4"));

  private static final PersistentSortedMap<String, String> FULL_MAP =
      PathCopyingPersistentTreeMap.copyOf(ImmutableMap.of("a", "1",
                                                          "b", "2",
                                                          "c", "3",
                                                          "d", "4"));

  @Test
  public void testMerge_Equal() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(FULL_MAP,
              FULL_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);
    assertTrue(differences.isEmpty());
  }

  @Test
  public void testMerge_map1Empty() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(EMPTY_MAP,
              FULL_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);

    @SuppressWarnings("unchecked")
    Triple<String, String, String>[] expectedDifferences = new Triple[] {
      Triple.of("a", null, "1"),
      Triple.of("b", null, "2"),
      Triple.of("c", null, "3"),
      Triple.of("d", null, "4")
    };

    assertEquals(Arrays.asList(expectedDifferences), differences);
  }

  @Test
  public void testMerge_map2Empty() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(FULL_MAP,
              EMPTY_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);

    @SuppressWarnings("unchecked")
    Triple<String, String, String>[] expectedDifferences = new Triple[] {
      Triple.of("a", "1", null),
      Triple.of("b", "2", null),
      Triple.of("c", "3", null),
      Triple.of("d", "4", null)
    };

    assertEquals(Arrays.asList(expectedDifferences), differences);
  }

  @Test
  public void testMerge_map1Half1() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(HALF1_MAP,
              FULL_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);

    @SuppressWarnings("unchecked")
    Triple<String, String, String>[] expectedDifferences = new Triple[] {
      Triple.of("b", null, "2"),
      Triple.of("d", null, "4")
    };

    assertEquals(Arrays.asList(expectedDifferences), differences);
  }

  @Test
  public void testMerge_map1Half2() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(HALF2_MAP,
              FULL_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);

    @SuppressWarnings("unchecked")
    Triple<String, String, String>[] expectedDifferences = new Triple[] {
      Triple.of("a", null, "1"),
      Triple.of("c", null, "3")
    };

    assertEquals(Arrays.asList(expectedDifferences), differences);
  }

  @Test
  public void testMerge_map2Half1() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(FULL_MAP,
              HALF1_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);

    @SuppressWarnings("unchecked")
    Triple<String, String, String>[] expectedDifferences = new Triple[] {
      Triple.of("b", "2", null),
      Triple.of("d", "4", null)
    };

    assertEquals(Arrays.asList(expectedDifferences), differences);
  }

  @Test
  public void testMerge_map2Half2() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(FULL_MAP,
              HALF2_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);

    @SuppressWarnings("unchecked")
    Triple<String, String, String>[] expectedDifferences = new Triple[] {
      Triple.of("a", "1", null),
      Triple.of("c", "3", null)
    };

    assertEquals(Arrays.asList(expectedDifferences), differences);
  }

  @Test
  public void testMerge_map1Half1_map2Half2() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(HALF1_MAP,
              HALF2_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);

    @SuppressWarnings("unchecked")
    Triple<String, String, String>[] expectedDifferences = new Triple[] {
      Triple.of("a", "1", null),
      Triple.of("b", null, "2"),
      Triple.of("c", "3", null),
      Triple.of("d", null, "4")
    };

    assertEquals(Arrays.asList(expectedDifferences), differences);
  }

  @Test
  public void testMerge_map1Half2_map2Half1() {

    List<Triple<String, String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(HALF2_MAP,
              HALF1_MAP,
              Equivalence.equals(),
              MapMerger.<String, String>getExceptionOnConflictHandler(),
              differences);

    assertEquals(FULL_MAP, result);

    @SuppressWarnings("unchecked")
    Triple<String, String, String>[] expectedDifferences = new Triple[] {
      Triple.of("a", null, "1"),
      Triple.of("b", "2", null),
      Triple.of("c", null, "3"),
      Triple.of("d", "4", null)
    };

    assertEquals(Arrays.asList(expectedDifferences), differences);
  }


  @Test
  public void testMergeSortedSets_Equal() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(FULL_MAP,
                        FULL_MAP,
                        MapMerger.<String, String>getExceptionOnConflictHandler());

    assertEquals(FULL_MAP, result.getThird());
    assertTrue(result.getFirst().isEmpty());
    assertTrue(result.getSecond().isEmpty());
  }

  @Test
  public void testMergeSortedSets_map1Empty() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(EMPTY_MAP,
                        FULL_MAP,
                        MapMerger.<String, String>getExceptionOnConflictHandler());

    assertTrue(result.getFirst().isEmpty());
    assertEquals(FULL_MAP, result.getSecond());
    assertEquals(FULL_MAP, result.getThird());
  }

  @Test
  public void testMergeSortedSets_map2Empty() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(FULL_MAP,
                        EMPTY_MAP,
                        MapMerger.<String, String>getExceptionOnConflictHandler());

    assertEquals(FULL_MAP, result.getFirst());
    assertTrue(result.getSecond().isEmpty());
    assertEquals(FULL_MAP, result.getThird());
  }

  @Test
  public void testMergeSortedSets_map1Half1() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(HALF1_MAP,
                        FULL_MAP,
                        MapMerger.<String, String>getExceptionOnConflictHandler());

    assertEquals(EMPTY_MAP, result.getFirst());
    assertEquals(HALF2_MAP, result.getSecond());
    assertEquals(FULL_MAP, result.getThird());
  }

  @Test
  public void testMergeSortedSets_map1Half2() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(HALF2_MAP,
              FULL_MAP,
              MapMerger.<String, String>getExceptionOnConflictHandler());

    assertEquals(EMPTY_MAP, result.getFirst());
    assertEquals(HALF1_MAP, result.getSecond());
    assertEquals(FULL_MAP, result.getThird());
  }

  @Test
  public void testMergeSortedSets_map2Half1() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(FULL_MAP,
              HALF1_MAP,
              MapMerger.<String, String>getExceptionOnConflictHandler());

    assertEquals(HALF2_MAP, result.getFirst());
    assertEquals(EMPTY_MAP, result.getSecond());
    assertEquals(FULL_MAP, result.getThird());
  }

  @Test
  public void testMergeSortedSets_map2Half2() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(FULL_MAP,
              HALF2_MAP,
              MapMerger.<String, String>getExceptionOnConflictHandler());

    assertEquals(HALF1_MAP, result.getFirst());
    assertEquals(EMPTY_MAP, result.getSecond());
    assertEquals(FULL_MAP, result.getThird());
  }

  @Test
  public void testMergeSortedSets_map1Half1_map2Half2() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(HALF1_MAP,
              HALF2_MAP,
              MapMerger.<String, String>getExceptionOnConflictHandler());

    assertEquals(HALF1_MAP, result.getFirst());
    assertEquals(HALF2_MAP, result.getSecond());
    assertEquals(FULL_MAP, result.getThird());
  }

  @Test
  public void testMergeSortedSets_map1Half2_map2Half1() {

    Triple<PersistentSortedMap<String, String>, PersistentSortedMap<String, String>, PersistentSortedMap<String, String>> result =
        mergeSortedSets(HALF2_MAP,
              HALF1_MAP,
              MapMerger.<String, String>getExceptionOnConflictHandler());

    assertEquals(HALF2_MAP, result.getFirst());
    assertEquals(HALF1_MAP, result.getSecond());
    assertEquals(FULL_MAP, result.getThird());
  }
}
