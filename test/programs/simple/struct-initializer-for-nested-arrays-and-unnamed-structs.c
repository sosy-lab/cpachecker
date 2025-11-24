// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error(void) { }

struct AllPatterns {
    struct {
        int nestedPointx;
        int nestedPointy;
    };
    struct {
        int nestedCoord1[2];
    };
    struct {
        int nestedCoord2[3];
    };
    struct {
        int x;
        struct {
          int nestedy;
        };
    } pointArray[5][2];

    struct {
      struct {
        int a[2];
      };
      struct {
        int b[2];
      };
    } multipleArrays[2];

    struct {
      int g;
      struct {
        int h;
      };
    } otherStructArray[5];

    // Test Bug 1: Array designator in middle of field chain
    struct {
      struct {
        int value;
      } items[3];
      int trailing;
    } arrayInMiddle;

    // Test Bug 2: Sequential initialization with nested struct
    struct {
      int a;
      struct {
        int x;
        int y;
      } nested;
      int c;
    } sequentialNested;
};

int main(void) {
    struct AllPatterns data1 = {
        {.nestedPointx = 20, .nestedPointy = 21},
        {.nestedCoord1 = {25, 26}},
        {30, 31, 32},
        .pointArray[0][0].x = 35,
        .pointArray[0][0].nestedy = 36,
        .pointArray[0][1] = {40, 41},
        .pointArray[1] = {{.x = 45, .nestedy = 46}, {50, 51}},
        .pointArray[2] = {{.x = 55, 56}, {60, {61}}},
        .pointArray[3] = {{.x = 65, {66}}, {70, {.nestedy = 71}}},
        .pointArray[4] = {{.x = 75, { .nestedy = 76 }}, {.x = 80, .nestedy = 81}},
        .multipleArrays = {{{100, 101}, {102, 103}}, {.a = {115, 116}, {{117, 118}}}},
        .otherStructArray = { [0 ... 2] = {.g = 130, {.h = 131}}, [3 ... 4] = {.g = 135, .h = 136}},

        // Bug 1: Designator with array subscript in the middle
        // After processing .arrayInMiddle.items[1].value, cursor should advance to .trailing
        .arrayInMiddle.items[1].value = 200,
        .arrayInMiddle.trailing = 201,

        // Bug 2: Sequential initialization should descend into nested struct
        // 300 initializes .a, then 301 should initialize .nested.x (not .nested itself)
        .sequentialNested = {300, 301, 302, 303}
    };

    if (data1.nestedPointx != 20) { goto ERROR; }
    if (data1.nestedPointy != 21) { goto ERROR; }
    if (data1.nestedCoord1[0] != 25) { goto ERROR; }
    if (data1.nestedCoord1[1] != 26) { goto ERROR; }
    if (data1.nestedCoord2[0] != 30) { goto ERROR; }
    if (data1.nestedCoord2[1] != 31) { goto ERROR; }
    if (data1.nestedCoord2[2] != 32) { goto ERROR; }
    if (data1.pointArray[0][0].x != 35) { goto ERROR; }
    if (data1.pointArray[0][0].nestedy != 36) { goto ERROR; }
    if (data1.pointArray[0][1].x != 40) { goto ERROR; }
    if (data1.pointArray[0][1].nestedy != 41) { goto ERROR; }
    if (data1.pointArray[1][0].x != 45) { goto ERROR; }
    if (data1.pointArray[1][0].nestedy != 46) { goto ERROR; }
    if (data1.pointArray[1][1].x != 50) { goto ERROR; }
    if (data1.pointArray[1][1].nestedy != 51) { goto ERROR; }
    if (data1.pointArray[2][0].x != 55) { goto ERROR; }
    if (data1.pointArray[2][0].nestedy != 56) { goto ERROR; }
    if (data1.pointArray[2][1].x != 60) { goto ERROR; }
    if (data1.pointArray[2][1].nestedy != 61) { goto ERROR; }
    if (data1.pointArray[3][0].x != 65) { goto ERROR; }
    if (data1.pointArray[3][0].nestedy != 66) { goto ERROR; }
    if (data1.pointArray[3][1].x != 70) { goto ERROR; }
    if (data1.pointArray[3][1].nestedy != 71) { goto ERROR; }
    if (data1.pointArray[4][0].x != 75) { goto ERROR; }
    if (data1.pointArray[4][0].nestedy != 76) { goto ERROR; }
    if (data1.pointArray[4][1].x != 80) { goto ERROR; }
    if (data1.pointArray[4][1].nestedy != 81) { goto ERROR; }
    if (data1.multipleArrays[0].a[0] != 100) { goto ERROR; }
    if (data1.multipleArrays[0].a[1] != 101) { goto ERROR; }
    if (data1.multipleArrays[0].b[0] != 102) { goto ERROR; }
    if (data1.multipleArrays[0].b[1] != 103) { goto ERROR; }
    if (data1.multipleArrays[1].a[0] != 115) { goto ERROR; }
    if (data1.multipleArrays[1].a[1] != 116) { goto ERROR; }
    if (data1.multipleArrays[1].b[0] != 117) { goto ERROR; }
    if (data1.multipleArrays[1].b[1] != 118) { goto ERROR; }
    if (data1.otherStructArray[0].g != 130) { goto ERROR; }
    if (data1.otherStructArray[0].h != 131) { goto ERROR; }
    if (data1.otherStructArray[1].g != 130) { goto ERROR; }
    if (data1.otherStructArray[1].h != 131) { goto ERROR; }
    if (data1.otherStructArray[2].g != 130) { goto ERROR; }
    if (data1.otherStructArray[2].h != 131) { goto ERROR; }
    if (data1.otherStructArray[3].g != 135) { goto ERROR; }
    if (data1.otherStructArray[3].h != 136) { goto ERROR; }
    if (data1.otherStructArray[4].g != 135) { goto ERROR; }
    if (data1.otherStructArray[4].h != 136) { goto ERROR; }

    // Bug 1: Array designator in middle - verify items[1] was set and trailing was set
    if (data1.arrayInMiddle.items[1].value != 200) { goto ERROR; }
    if (data1.arrayInMiddle.trailing != 201) { goto ERROR; }

    // Bug 2: Sequential initialization with nested struct
    if (data1.sequentialNested.a != 300) { goto ERROR; }
    if (data1.sequentialNested.nested.x != 301) { goto ERROR; }
    if (data1.sequentialNested.nested.y != 302) { goto ERROR; }
    if (data1.sequentialNested.c != 303) { goto ERROR; }

    return 0;

ERROR:
    reach_error();
    return 1;
}
