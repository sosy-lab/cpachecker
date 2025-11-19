// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error(void) { }

struct AllUnionPatterns {
  union {
    int simpleFirst;
    int simpleSecond;
  } simpleUnion;

  union {
    int scalarMember;
    int arrayMember[3];
  } unionWithArray;

  union {
    union {
      int anonMemberA;
    };
    union {
      int anonMemberB;
    };
  } unionWithAnonymous;

  union {
    int primitiveValue;
    struct {
      int nestedX;
      int nestedY;
    } structMember;
  } unionWithStruct;

  union {
    int outerValue;
    union {
      int innerValue;
    };
  } deeplyNested;

  union {
    int firstMember;
    int secondMember;
  } sequentialArray[3];

  struct {
    int beforeUnion;
    union {
      int unionFirstMember;
      int unionSecondMember;
    } embeddedUnion;
    int afterUnion;
  } structWithUnion;
};

int main(void) {
  struct AllUnionPatterns data = {
    .simpleUnion = {10},
    .unionWithArray.arrayMember = {20, 21, 22},
    .unionWithAnonymous.anonMemberA = 30,
    .unionWithStruct.structMember = {40, 41},
    .deeplyNested.innerValue = 50,
    .sequentialArray = {{.firstMember = 60}, {.secondMember = 70}, {80}},
    .structWithUnion = {90, 91, 92}
  };

  if (data.simpleUnion.simpleFirst != 10) { goto ERROR; }
  if (data.unionWithArray.arrayMember[0] != 20) { goto ERROR; }
  if (data.unionWithArray.arrayMember[1] != 21) { goto ERROR; }
  if (data.unionWithArray.arrayMember[2] != 22) { goto ERROR; }
  if (data.unionWithAnonymous.anonMemberA != 30) { goto ERROR; }
  if (data.unionWithStruct.structMember.nestedX != 40) { goto ERROR; }
  if (data.unionWithStruct.structMember.nestedY != 41) { goto ERROR; }
  if (data.deeplyNested.innerValue != 50) { goto ERROR; }
  if (data.sequentialArray[0].firstMember != 60) { goto ERROR; }
  if (data.sequentialArray[1].secondMember != 70) { goto ERROR; }
  if (data.sequentialArray[2].firstMember != 80) { goto ERROR; }
  if (data.structWithUnion.beforeUnion != 90) { goto ERROR; }
  if (data.structWithUnion.embeddedUnion.unionFirstMember != 91) { goto ERROR; }
  if (data.structWithUnion.afterUnion != 92) { goto ERROR; }

  return 0;

ERROR:
  reach_error();
  return 1;
}
