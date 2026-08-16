// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>

typedef int (*FuncDefFoo)(int, int);
typedef int (*FuncDefFooInFun)(FuncDefFoo, int, int);


struct fooStruct {
  int (*function)(int, int);
};


struct barStruct {
  struct fooStruct *bar;
};


struct localBarStruct {
  struct fooStruct bar;
};


struct funcDefFooStruct {
  FuncDefFoo localFoo;
  FuncDefFoo * funcDefFooPtrRef;
  struct barStruct barStructRef;
  struct localBarStruct localBarStructRef;
  struct barStruct * barStructPtrRef;
  struct localBarStruct * localBarStructPtrRef;
} combiStruct;


typedef struct funcDefFooStruct renamedFuncDefFooStruct;


int foo(int a, int b) {
  return a + b;
}

int glob = 11111;
int grob = 77777;
int bar(int (*functionPtr)(int, int)) {
  return (*functionPtr)(++glob, ++grob);
}


int barBar(int (*functionPtr)(int, int), int aa, int be) {
  return (*functionPtr)(aa, be);
}


struct barBarStruct {
    int (*(*barBarPtr))(int (*)(int, int), int, int);
    int (*(*nestedBarBarArray[2]))(int (*)(int, int), int, int);
} barBarBars;


FuncDefFoo functionFunction() {
    FuncDefFoo functionPtr = &foo;
    return functionPtr;
}

// This program tests some easy function pointer games with concrete values only
int main() {
  // Function pointers
  int (*functionNoPtr)(int,int);
  functionNoPtr = foo;
  int sum = functionNoPtr(1, 3);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 4);

  sum = bar(functionNoPtr);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 88890);

  sum = barBar(functionNoPtr, 2, 5);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 7);

  int (*functionPtr)(int,int);
  functionPtr = &foo;
  sum = functionPtr(3, 7);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 10);

  sum = bar(functionPtr);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 88892);

  sum = barBar(functionPtr, 4, 9);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 13);

  sum = (*functionPtr)(5, 11);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 16);

  functionPtr = functionFunction();
  sum = (*functionPtr)(6, 13);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 19);

  sum = bar(functionPtr);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 88894);

  sum = barBar(functionPtr, 7, 15);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 22);

  FuncDefFoo funcDefFoo = functionFunction();
  sum = (*funcDefFoo)(8, 17);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 25);

  sum = (funcDefFoo)(9, 19);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 28);

  sum = bar(funcDefFoo);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 88896);

  sum = barBar(funcDefFoo, 10, 21);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 31);


  // Structs w function pointers
  struct fooStruct * fooPtr = malloc(sizeof(struct fooStruct));
  if (fooPtr == 0) {
    return 0;
  }
  fooPtr->function = &foo;
  sum = fooPtr->function(11, 23);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 34);

  sum = barBar(fooPtr->function, 12, 25);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 37);

  struct fooStruct * fooPointa = malloc(sizeof(struct fooStruct));
  if (fooPointa == 0) {
    return 0;
  }
  fooPointa->function = foo;
  sum = fooPointa->function(13, 27);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 40);

  sum = barBar(fooPointa->function, 14, 29);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 43);

  struct barStruct * barPtr = malloc(sizeof(struct barStruct));
  if (barPtr == 0) {
    return 0;
  }
  barPtr->bar = fooPtr;
  sum = barPtr->bar->function(15, 31);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 46);

  sum = barBar(barPtr->bar->function, 16, 33);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 49);

  struct barStruct * barPointa = malloc(sizeof(struct barStruct));
  if (barPointa == 0) {
    return 0;
  }
  barPointa->bar = fooPointa;
  sum = barPointa->bar->function(17, 35);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 52);

  sum = barBar(barPointa->bar->function, 18, 37);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 55);

  struct barStruct localbarPtr1;
  localbarPtr1.bar = fooPtr;
  sum = localbarPtr1.bar->function(19, 39);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 58);

  sum = barBar(barPtr->bar->function, 20, 41);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 61);

  struct barStruct localBarPtr2;
  localBarPtr2.bar = fooPointa;
  sum = localBarPtr2.bar->function(21, 43);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 64);

  sum = barBar(localBarPtr2.bar->function, 23, 45);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 68);


  struct fooStruct localFooStruct1;
  localFooStruct1.function = &foo;
  sum = localFooStruct1.function(24, 47);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 71);

  sum = barBar(localFooStruct1.function, 25, 49);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 74);

  struct fooStruct localFooStruct2;
  localFooStruct2.function = foo;
  sum = localFooStruct2.function(26, 51);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 77);

  sum = barBar(localFooStruct2.function, 27, 53);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 80);

  struct localBarStruct localbar1;
  localbar1.bar = localFooStruct1;
  sum = localbar1.bar.function(28, 55);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 83);

  sum = barBar(localbar1.bar.function, 29, 57);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 86);

  struct localBarStruct localBar2;
  localBar2.bar = localFooStruct2;
  sum = localBar2.bar.function(30, 59);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 89);

  sum = barBar(localBar2.bar.function, 31, 61);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 92);


  struct funcDefFooStruct * funcDefFooPtr = malloc(sizeof(renamedFuncDefFooStruct));
  if (funcDefFooPtr == 0) {
    return 0;
  }
  funcDefFooPtr->localFoo = &foo;
  sum = funcDefFooPtr->localFoo(32, 63);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 95);

  sum = barBar(funcDefFooPtr->localFoo, 33, 65);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 98);

  
  funcDefFooPtr->funcDefFooPtrRef = &(funcDefFooPtr->localFoo);
  sum = (*(funcDefFooPtr->funcDefFooPtrRef))(34, 67);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 101);

  sum = barBar((*(funcDefFooPtr->funcDefFooPtrRef)), 35, 69);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 104);

  funcDefFooPtr->barStructRef = localbarPtr1;
  sum = funcDefFooPtr->barStructRef.bar->function(36, 71);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 107);

  sum = barBar(funcDefFooPtr->barStructRef.bar->function, 37, 73);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 110);

  funcDefFooPtr->localBarStructRef = localbar1;
  sum = funcDefFooPtr->localBarStructRef.bar.function(38, 75);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 113);

  sum = barBar(funcDefFooPtr->localBarStructRef.bar.function, 39, 77);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 116);

  funcDefFooPtr->barStructPtrRef = &localbarPtr1;
  funcDefFooPtr->localBarStructPtrRef = &localbar1;

  sum = funcDefFooPtr->barStructPtrRef->bar->function(40, 79);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 119);

  sum = barBar(funcDefFooPtr->barStructPtrRef->bar->function, 41, 81);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 122);

  sum = funcDefFooPtr->localBarStructPtrRef->bar.function(42, 83);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 125);

  sum = barBar(funcDefFooPtr->localBarStructPtrRef->bar.function, 53, 85);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 138);



  combiStruct.barStructRef = localBarPtr2;
  sum = combiStruct.barStructRef.bar->function(88, 188);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 276);

  sum = barBar(combiStruct.barStructRef.bar->function, 89, 191);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 280);

  FuncDefFoo bonusVar;
  bonusVar = foo;
  combiStruct.funcDefFooPtrRef = &bonusVar;
  sum = (*(combiStruct.funcDefFooPtrRef))(90, 194);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 284);

  sum = barBar((*(combiStruct.funcDefFooPtrRef)), 91, 197);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 288);

  combiStruct.localBarStructRef = localBar2;
  sum = combiStruct.localBarStructRef.bar.function(92, 200);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 292);

  sum = barBar(combiStruct.localBarStructRef.bar.function, 93, 203);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 296);

  combiStruct.localFoo = foo;
  sum = combiStruct.localFoo(94, 206);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 300);

  sum = barBar(combiStruct.localFoo, 95, 209);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 304);

  combiStruct.barStructPtrRef = &localBarPtr2;
  sum = combiStruct.barStructPtrRef->bar->function(96, 212);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 308);

  sum = barBar(combiStruct.barStructPtrRef->bar->function, 97, 215);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 312);

  combiStruct.localBarStructPtrRef = &localBar2;
  sum = combiStruct.localBarStructPtrRef->bar.function(98, 218);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 316);

  sum = barBar(combiStruct.localBarStructPtrRef->bar.function, 99, 221);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 320);


  // Start with something simple:
  // First get an array with function pointers that execute a function given via a function pointer that takes 2 int arguments and returns a int that receives 2 int arguments and returns an int
  int (*barBarArray[2])(int (*)(int, int), int, int) = {&barBar, barBar};

  sum = (*barBarArray[0])(combiStruct.localBarStructRef.bar.function, 100, 211232);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 211332);

  sum = (*barBarArray[0])(combiStruct.localBarStructPtrRef->bar.function, 101, 2231);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2332);

  sum = (*barBarArray[0])(combiStruct.barStructPtrRef->bar->function, 102, 21909);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 22011);

  sum = (*barBarArray[0])(combiStruct.localFoo, 103, 210099);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 210202);

  sum = (*barBarArray[0])(combiStruct.barStructRef.bar->function, 104, 214554);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 214658);

  sum = (*barBarArray[0])((*(combiStruct.funcDefFooPtrRef)), 105, 2198724);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2198829);

  sum = (*barBarArray[0])(funcDefFooPtr->localFoo, 106, 214562);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 214668);

  sum = (*barBarArray[0])((*(funcDefFooPtr->funcDefFooPtrRef)), 107, 211544);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 211651);

  sum = (*barBarArray[0])(funcDefFooPtr->barStructRef.bar->function, 108, 219536);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 219644);

  sum = (*barBarArray[0])(funcDefFooPtr->localBarStructRef.bar.function, 109, 21124);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21233);

  sum = (*barBarArray[0])(funcDefFooPtr->barStructPtrRef->bar->function, 110, 213656);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 213766);

  sum = (*barBarArray[0])(funcDefFooPtr->localBarStructPtrRef->bar.function, 111, 2154454);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2154565);


  sum = (*barBarArray[1])(combiStruct.localBarStructRef.bar.function, 112, 211312);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 211424);

  sum = (*barBarArray[1])(combiStruct.localBarStructPtrRef->bar.function, 113, 217999);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 218112);

  sum = (*barBarArray[1])(combiStruct.barStructPtrRef->bar->function, 114, 2178788);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2178902);

  sum = (*barBarArray[1])(combiStruct.localFoo, 115, 217878);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 217993);

  sum = (*barBarArray[1])(combiStruct.barStructRef.bar->function, 116, 2167677);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2167793);

  sum = (*barBarArray[1])((*(combiStruct.funcDefFooPtrRef)), 117, 216767);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 216884);

  sum = (*barBarArray[1])(funcDefFooPtr->localFoo, 118, 21565666);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21565784);

  sum = (*barBarArray[1])((*(funcDefFooPtr->funcDefFooPtrRef)), 119, 215656);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 215775);

  sum = (*barBarArray[1])(funcDefFooPtr->barStructRef.bar->function, 120, 21454555);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21454675);

  sum = (*barBarArray[1])(funcDefFooPtr->localBarStructRef.bar.function, 121, 214545);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 214666);

  sum = (*barBarArray[1])(funcDefFooPtr->barStructPtrRef->bar->function, 122, 212323333);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 212323455);

  sum = (*barBarArray[1])(funcDefFooPtr->localBarStructPtrRef->bar.function, 123, 2123233);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2123356);


  // Arrays with function pointers
  int (*funArray[3]) (int, int);

  funArray[0] = functionPtr;
  funArray[1] = funcDefFoo;
  funArray[2] = foo;

  sum = (*funArray[0]) (1109, 2200);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 3309);

  sum = (*funArray[1]) (1110, 2211);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 3321);

  sum = (*funArray[2]) (1111, 2222);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 3333);

  sum = barBar(funArray[0], 93, 203);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 296);

  sum = barBar(funArray[1], 94, 203);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 297);

  sum = barBar(funArray[2], 95, 203);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 298);

  sum = (*barBarArray[0])(funArray[0], 1067, 211);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 1278);

  sum = (*barBarArray[0])(funArray[1], 1056, 212);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 1268);

  sum = (*barBarArray[0])(funArray[2], 1045, 213);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 1258);


  sum = (*barBarArray[1])(funArray[0], 1034, 214);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 1248);

  sum = (*barBarArray[1])(funArray[1], 1023, 215);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 1238);

  sum = (*barBarArray[1])(funArray[2], 1012, 216);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 1228);


  // Array with pointer to structs that have function pointer etc.
  struct boah {
    struct funcDefFooStruct * ptrRef;
    struct funcDefFooStruct localRef;
    renamedFuncDefFooStruct * ptrRefRenamed;
    renamedFuncDefFooStruct localRefRenamed;
  } dasLetzteStruct = {funcDefFooPtr, combiStruct, (renamedFuncDefFooStruct *) funcDefFooPtr, (renamedFuncDefFooStruct) combiStruct};
  // Do we need this on the heap as well?

  struct boah * dasLetzteStructPtr = &dasLetzteStruct;

  struct boah * dasLetzteStructPtrArray[1];
  dasLetzteStructPtrArray[0] = dasLetzteStructPtr;

  // Do we need to test all 4 or are the 2 typedefs enough?
  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->localFoo(200, 15353535);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 15353735);

  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localFoo, 201, 21232323);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21232524);

  sum = (*((*dasLetzteStructPtrArray[0]).ptrRefRenamed->funcDefFooPtrRef))(202, 15121212);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 15121414);

  sum = barBar((*((*dasLetzteStructPtrArray[0]).ptrRefRenamed->funcDefFooPtrRef)), 203, 219999);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 220202);

  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructRef.bar->function(204, 158888);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 159092);

  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructRef.bar->function, 205, 217777);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 217982);

  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructRef.bar.function(206, 156666);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 156872);

  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructRef.bar.function, 207, 215555);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 215762);

  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructPtrRef->bar->function(208, 150000);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 150208);

  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructPtrRef->bar->function, 209, 211111);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 211320);

  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructPtrRef->bar.function(210, 152222);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 152432);

  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructPtrRef->bar.function, 211, 213333);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 213544);


  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.barStructRef.bar->function(212, 188);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 400);

  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructRef.bar->function, 213, 191);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 404);

  sum = (*((*dasLetzteStructPtrArray[0]).localRefRenamed.funcDefFooPtrRef))(90, 194);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 284);

  sum = barBar((*((*dasLetzteStructPtrArray[0]).localRefRenamed.funcDefFooPtrRef)), 91, 197);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 288);

  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructRef.bar.function(92, 200);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 292);

  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructRef.bar.function, 93, 203);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 296);

  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.localFoo(94, 206);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 300);

  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.localFoo, 95, 209);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 304);

  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.barStructPtrRef->bar->function(96, 212);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 308);

  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructPtrRef->bar->function, 97, 215);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 312);

  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructPtrRef->bar.function(98, 218);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 316);

  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructPtrRef->bar.function, 99, 221);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 320);


  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructRef.bar.function, 300, 211);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 511);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructPtrRef->bar.function, 301, 212);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 513);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructPtrRef->bar->function, 302, 213);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 515);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.localFoo, 303, 214);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 517);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructRef.bar->function, 304, 215);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 519);

  sum = (*barBarArray[0])((*((*dasLetzteStructPtrArray[0]).localRefRenamed.funcDefFooPtrRef)), 305, 216);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 521);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localFoo, 306, 217);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 523);

  sum = (*barBarArray[0])((*((*dasLetzteStructPtrArray[0]).ptrRefRenamed->funcDefFooPtrRef)), 307, 218);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 525);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructRef.bar->function, 308, 219);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 527);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructRef.bar.function, 309, 2100);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2409);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructPtrRef->bar->function, 310, 2111);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2421);

  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructPtrRef->bar.function, 311, 2122);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2433);


  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructRef.bar.function, 312, 2133);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2445);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructPtrRef->bar.function, 313, 2144);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2457);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructPtrRef->bar->function, 314, 2155);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2469);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.localFoo, 315, 2166);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2481);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructRef.bar->function, 316, 2177);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2493);

  sum = (*barBarArray[1])((*((*dasLetzteStructPtrArray[0]).localRefRenamed.funcDefFooPtrRef)), 317, 2188);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2505);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localFoo, 318, 2199);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 2517);

  sum = (*barBarArray[1])((*((*dasLetzteStructPtrArray[0]).ptrRefRenamed->funcDefFooPtrRef)), 319, 21111);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21430);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructRef.bar->function, 320, 21222);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21542);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructRef.bar.function, 321, 21333);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21654);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructPtrRef->bar->function, 322, 21444);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21766);

  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructPtrRef->bar.function, 323, 21555);
  // printf("%d\n", sum); // left for debugging
  assert(sum == 21878);

  return 0;
}
