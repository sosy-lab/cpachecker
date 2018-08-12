/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.floatingpoint;

import java.util.Arrays;
import java.util.HashMap;

public class CFloatUtil {

  private static final HashMap<Long, byte[]> BIT_TO_DEC_MAP = new HashMap<>();

  static {
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0});
    BIT_TO_DEC_MAP.put(
        0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0});
    BIT_TO_DEC_MAP.put(
        0b01000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {5});
    BIT_TO_DEC_MAP.put(
        0b00100000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {2, 5});
    BIT_TO_DEC_MAP.put(
        0b00010000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00001000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000100_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000010_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 1, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000001_00000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 7, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_10000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 3, 9, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_01000000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 1, 9, 5, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00100000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 9, 7, 6, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 4, 8, 8, 2, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00001000_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 2, 4, 4, 1, 4, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000100_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 1, 2, 2, 0, 7, 0, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000010_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 6, 1, 0, 3, 5, 1, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000001_00000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 3, 0, 5, 1, 7, 5, 7, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_10000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 1, 5, 2, 5, 8, 7, 8, 9, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_01000000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 7, 6, 2, 9, 3, 9, 4, 5, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00100000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 3, 8, 1, 4, 6, 9, 7, 2, 6, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00010000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 1, 9, 0, 7, 3, 4, 8, 6, 3, 2, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00001000_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 9, 5, 3, 6, 7, 4, 3, 1, 6, 4, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000100_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 4, 7, 6, 8, 3, 7, 1, 5, 8, 2, 0, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000010_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 2, 3, 8, 4, 1, 8, 5, 7, 9, 1, 0, 1, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000001_00000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 1, 1, 9, 2, 0, 9, 2, 8, 9, 5, 5, 0, 7, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_10000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 5, 9, 6, 0, 4, 6, 4, 4, 7, 7, 5, 3, 9, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_01000000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 2, 9, 8, 0, 2, 3, 2, 2, 3, 8, 7, 6, 9, 5, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00100000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 1, 4, 9, 0, 1, 1, 6, 1, 1, 9, 3, 8, 4, 7, 6, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00010000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 7, 4, 5, 0, 5, 8, 0, 5, 9, 6, 9, 2, 3, 8, 2, 8, 1, 2,
            5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00001000_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 3, 7, 2, 5, 2, 9, 0, 2, 9, 8, 4, 6, 1, 9, 1, 4, 0, 6, 2,
            5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 1, 8, 6, 2, 6, 4, 5, 1, 4, 9, 2, 3, 0, 9, 5, 7, 0, 3, 1,
            2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000010_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 3, 1, 3, 2, 2, 5, 7, 4, 6, 1, 5, 4, 7, 8, 5, 1, 5,
            6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000001_00000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 6, 5, 6, 6, 1, 2, 8, 7, 3, 0, 7, 7, 3, 9, 2, 5, 7,
            8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_10000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 2, 8, 3, 0, 6, 4, 3, 6, 5, 3, 8, 6, 9, 6, 2, 8,
            9, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_01000000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 6, 4, 1, 5, 3, 2, 1, 8, 2, 6, 9, 3, 4, 8, 1, 4,
            4, 5, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00100000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 8, 2, 0, 7, 6, 6, 0, 9, 1, 3, 4, 6, 7, 4, 0, 7,
            2, 2, 6, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00010000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 9, 1, 0, 3, 8, 3, 0, 4, 5, 6, 7, 3, 3, 7, 0, 3,
            6, 1, 3, 2, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00001000_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 5, 5, 1, 9, 1, 5, 2, 2, 8, 3, 6, 6, 8, 5, 1,
            8, 0, 6, 6, 4, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000100_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 2, 7, 5, 9, 5, 7, 6, 1, 4, 1, 8, 3, 4, 2, 5,
            9, 0, 3, 3, 2, 0, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000010_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 6, 3, 7, 9, 7, 8, 8, 0, 7, 0, 9, 1, 7, 1, 2,
            9, 5, 1, 6, 6, 0, 1, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000001_00000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 8, 1, 8, 9, 8, 9, 4, 0, 3, 5, 4, 5, 8, 5, 6,
            4, 7, 5, 8, 3, 0, 0, 7, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_10000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 9, 4, 9, 4, 7, 0, 1, 7, 7, 2, 9, 2, 8,
            2, 3, 7, 9, 1, 5, 0, 3, 9, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_01000000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 5, 4, 7, 4, 7, 3, 5, 0, 8, 8, 6, 4, 6, 4,
            1, 1, 8, 9, 5, 7, 5, 1, 9, 5, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00100000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 7, 3, 7, 3, 6, 7, 5, 4, 4, 3, 2, 3, 2,
            0, 5, 9, 4, 7, 8, 7, 5, 9, 7, 6, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00010000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 3, 6, 8, 6, 8, 3, 7, 7, 2, 1, 6, 1, 6,
            0, 2, 9, 7, 3, 9, 3, 7, 9, 8, 8, 2, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 6, 8, 4, 3, 4, 1, 8, 8, 6, 0, 8, 0, 8,
            0, 1, 4, 8, 6, 9, 6, 8, 9, 9, 4, 1, 4, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000100_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 8, 4, 2, 1, 7, 0, 9, 4, 3, 0, 4, 0, 4,
            0, 0, 7, 4, 3, 4, 8, 4, 4, 9, 7, 0, 7, 0, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000010_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 2, 1, 0, 8, 5, 4, 7, 1, 5, 2, 0, 2,
            0, 0, 3, 7, 1, 7, 4, 2, 2, 4, 8, 5, 3, 5, 1, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000001_00000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 1, 0, 5, 4, 2, 7, 3, 5, 7, 6, 0, 1,
            0, 0, 1, 8, 5, 8, 7, 1, 1, 2, 4, 2, 5, 7, 5, 7, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_10000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 5, 5, 2, 7, 1, 3, 6, 7, 8, 8, 0, 0,
            5, 0, 0, 9, 2, 9, 3, 5, 5, 6, 2, 1, 3, 3, 7, 8, 9, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_01000000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 7, 7, 6, 3, 5, 6, 8, 3, 9, 4, 0, 0,
            2, 5, 0, 4, 6, 4, 6, 7, 7, 8, 1, 0, 6, 6, 8, 9, 4, 5, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00100000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 8, 1, 7, 8, 4, 1, 9, 7, 0, 0,
            1, 2, 5, 2, 3, 2, 3, 3, 8, 9, 0, 5, 3, 3, 4, 4, 7, 2, 6, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00010000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 4, 0, 8, 9, 2, 0, 9, 8, 5, 0,
            0, 6, 2, 6, 1, 6, 1, 6, 9, 4, 5, 2, 6, 6, 7, 2, 3, 6, 3, 2, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00001000_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 0, 4, 4, 6, 0, 4, 9, 2, 5,
            0, 3, 1, 3, 0, 8, 0, 8, 4, 7, 2, 6, 3, 3, 3, 6, 1, 8, 1, 6, 4, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000100_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 3, 0, 2, 4, 6, 2,
            5, 1, 5, 6, 5, 4, 0, 4, 2, 3, 6, 3, 1, 6, 6, 8, 0, 9, 0, 8, 2, 0, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000010_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 5, 5, 1, 1, 1, 5, 1, 2, 3, 1,
            2, 5, 7, 8, 2, 7, 0, 2, 1, 1, 8, 1, 5, 8, 3, 4, 0, 4, 5, 4, 1, 0, 1, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000001_00000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 7, 7, 5, 5, 5, 7, 5, 6, 1, 5,
            6, 2, 8, 9, 1, 3, 5, 1, 0, 5, 9, 0, 7, 9, 1, 7, 0, 2, 2, 7, 0, 5, 0, 7, 8, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_10000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 8, 7, 7, 7, 8, 7, 8, 0, 7,
            8, 1, 4, 4, 5, 6, 7, 5, 5, 2, 9, 5, 3, 9, 5, 8, 5, 1, 1, 3, 5, 2, 5, 3, 9, 0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01000000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 9, 3, 8, 8, 9, 3, 9, 0, 3,
            9, 0, 7, 2, 2, 8, 3, 7, 7, 6, 4, 7, 6, 9, 7, 9, 2, 5, 5, 6, 7, 6, 2, 6, 9, 5, 3, 1, 2,
            5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00100000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 4, 6, 9, 4, 4, 6, 9, 5, 1,
            9, 5, 3, 6, 1, 4, 1, 8, 8, 8, 2, 3, 8, 4, 8, 9, 6, 2, 7, 8, 3, 8, 1, 3, 4, 7, 6, 5, 6,
            2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00010000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 7, 3, 4, 7, 2, 3, 4, 7, 5,
            9, 7, 6, 8, 0, 7, 0, 9, 4, 4, 1, 1, 9, 2, 4, 4, 8, 1, 3, 9, 1, 9, 0, 6, 7, 3, 8, 2, 8,
            1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00001000L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 6, 7, 3, 6, 1, 7, 3, 7,
            9, 8, 8, 4, 0, 3, 5, 4, 7, 2, 0, 5, 9, 6, 2, 2, 4, 0, 6, 9, 5, 9, 5, 3, 3, 6, 9, 1, 4,
            0, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000100L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 3, 3, 6, 8, 0, 8, 6, 8,
            9, 9, 4, 2, 0, 1, 7, 7, 3, 6, 0, 2, 9, 8, 1, 1, 2, 0, 3, 4, 7, 9, 7, 6, 6, 8, 4, 5, 7,
            0, 3, 1, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 6, 8, 4, 0, 4, 3, 4,
            4, 9, 7, 1, 0, 0, 8, 8, 6, 8, 0, 1, 4, 9, 0, 5, 6, 0, 1, 7, 3, 9, 8, 8, 3, 4, 2, 2, 8,
            5, 1, 5, 6, 2, 5});
    BIT_TO_DEC_MAP.put(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L,
        new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 8, 4, 2, 0, 2, 1, 7,
            2, 4, 8, 5, 5, 0, 4, 4, 3, 4, 0, 0, 7, 4, 5, 2, 8, 0, 0, 8, 6, 9, 9, 4, 1, 7, 1, 1, 4,
            2, 5, 7, 8, 1, 2, 5});
  }

  private CFloatUtil() {
    // do not instantiate
  }

  public static long getSignBitMask(int pType) {
    long signBit = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000001_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_00001000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_10000000_00000000L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return signBit;
  }

  public static long getExponentMask(int pType) {
    long exp = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_01111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return exp;
  }

  public static long getMantissaMask(int pType) {
    long man = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        man = 0b00000000_00000000_00000000_00000000_00000000_01111111_11111111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        man = 0b00000000_00001111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        man = 0b01111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return man;
  }

  public static long getNormalizationMask(int pType) {
    long oneBit = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        oneBit = 0b00000000_00000000_00000000_00000000_00000000_10000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        oneBit = 0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        oneBit = 0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return oneBit;
  }

  public static long getNormalizedMantissaMask(int pType) {
    long man = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        return getMantissaMask(pType);
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        man = 0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return man;
  }

  public static long getBias(int pType) {
    long bias = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        bias = getExponentMask(pType) / 2;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return bias;
  }

  public static int getMantissaLength(int pType) {
    int res = -1;

    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        res = 23;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        res = 52;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        res = 64;
        break;
      default:
        throw new IllegalArgumentException("Unimplemented floating point type: " + pType);
    }

    return res;
  }

  public static int getExponentLength(int pType) {
    int res = -1;

    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        res = 8;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        res = 11;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        res = 15;
        break;
      default:
        throw new IllegalArgumentException("Unimplemented floating point type: " + pType);
    }

    return res;
  }

  public static CFloatWrapper round(CFloatWrapper pWrapper, long pOverflow, int pType) {
    // TODO: currently only rounding mode NEAREST_TIE_TO_EVEN; implement others
    if (pOverflow != 0) {
      long man = pWrapper.getMantissa();
      long exp = pWrapper.getExponent();

      boolean isNormalized =
          (pType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
              && (man & getNormalizationMask(pType)) != 0);

      if ((getHighestOrderOverflowBitMask() & pOverflow) != 0) {
        if (((getLowerOrderOverflowBitsMask(pType) & pOverflow) != 0) || ((1 & man) != 0)) {
          long nMan = (man + 1) & getNormalizedMantissaMask(pType);

          if ((pType != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
              && (nMan & getNormalizationMask(pType)) != 0)
              || (pType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
                  && (nMan & getNormalizationMask(pType)) == 0
                  && isNormalized)) {
            nMan >>>= 1;
            nMan ^= getNormalizationMask(pType);
            nMan &= getNormalizedMantissaMask(pType);
            exp--;
          }

          pWrapper.setExponent(exp);
          pWrapper.setMantissa(nMan);
        }
      }
    }

    return pWrapper;
  }

  public static long getLowerOrderOverflowBitsMask(int pType) {
    long bits = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        bits = 0b01111111_11111111_11111110_00000000_00000000_00000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        bits = 0b01111111_11111111_11111111_11111111_11111111_11111111_11110000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        bits = 0b01111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return bits;
  }

  public static long getHighestOrderOverflowBitMask() {
    return 0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  }

  public static long getOverflowHighBitsMask(int pType) {
    long bits = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        bits = 0b11111111_11111111_11111110_00000000_00000000_00000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        bits = 0b11111111_11111111_11111111_11111111_11111111_11111111_11110000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        bits = 0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return bits;
  }

  public static int getNormalizedMantissaLength(int pType) {
    int length = 0;

    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        length = 24;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        length = 53;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        length = 64;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return length;
  }

  /**
   * Split a {@link CFloat} object into its fields and put their bit representation as well as their
   * human readable base-10 format into a {@link String}.
   * <p>
   * This method is meant mostly for any kind of debugging purpose, where one could need some clear
   * visualization of the components of some {@link CFloat} object to evaluate potential misbehavior
   * in some implemented floating point operation or maybe to assess why some edge cases behave the
   * way they do.
   *
   * @param pFloat the {@link CFloat} object to decompose and represent as a {@link String}
   * @return the {@link String} built from <code>pFloat</code>
   */
  public static String getFieldsAsComprehensiveStringRepresentation(CFloat pFloat) {
    StringBuilder builder = new StringBuilder();
    CFloatWrapper wrapper = pFloat.copyWrapper();
    int type = pFloat.getType();

    builder.append("\n\tSign:                 ");
    builder.append(pFloat.isNegative() ? "-" : "+");

    builder.append("\n\tExponent:             ");
    long exp = wrapper.getExponent();
    traverseSingleField(builder, type, exp, false);

    builder.append("\n\tMantissa/Significant: ");
    long man = wrapper.getMantissa();
    traverseSingleField(builder, type, man, true);

    builder.append("\n");
    builder.append("\n\tExponent (readable):  ").append(exp);
    builder.append("\n\tMantissa (readable):  ").append(man);

    builder.append("\n");

    return builder.toString();
  }

  private static void
      traverseSingleField(StringBuilder pBuilder, int pType, long pMask, boolean mantissa) {
    int length = (mantissa ? getMantissaLength(pType) : getExponentLength(pType)) - 1;
    for (int i = length; i >= 0; i--) {
      pBuilder.append(((pMask & 1L << i) == 0) ? "0" : "1");
      if (i > 0 && i % 8 == 0) {
        pBuilder.append(" ");
      }
    }
  }

  public static byte[] getDecimalArray(int pType, long pSignificand) {
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        pSignificand = pSignificand << 40;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        pSignificand = pSignificand << 11;
        break;
    }
    byte[] res = BIT_TO_DEC_MAP.get(pSignificand);
    return Arrays.copyOf(res, res.length);
  }
}
