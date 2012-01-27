
#ifndef _VoidPtrLIST_H
#define _VoidPtrLIST_H

/* include header file defining VoidPtr if VoidPtr is set: */
#define EMPTYVoidPtrHDR
#ifndef EMPTYHDR
#include "VoidPtr.h"
#endif
#undef EMPTYVoidPtrHDR

#include "VoidPtrFunctionTypes.h"

typedef struct _VoidPtrLE *VoidPtrList;
typedef VoidPtrList    *VoidPtrListPtr;

struct _VoidPtrLE {
VoidPtr         head;
VoidPtrList     tail;
};

#define NULLVoidPtrList         ((VoidPtrList)0)
#define NullVoidPtrList()       ((VoidPtrList)0)
#define SingleVoidPtrList(e)    (ConsVoidPtrList((e),NULLVoidPtrList))

#if defined(__STDC__) || defined(__cplusplus)

extern void FinlVoidPtrList(void);
   /* Deallocates all VoidPtrLists. */
   /* Dynamic storage is allocated on first need */

extern VoidPtrList ConsVoidPtrList (VoidPtr e, VoidPtrList l);
   /* Constructs a VoidPtrList of an element e and a given tail l.
    * e is the first element of the list. */

extern VoidPtr HeadVoidPtrList (VoidPtrList l);
   /* Returns the first element of the list l.
    * The list l must not be empty. */

extern VoidPtrList TailVoidPtrList (VoidPtrList l);
   /* Returns the tail of the list l.
    * If l is empty, an empty list is returned. */

extern int LengthVoidPtrList (VoidPtrList l);
   /* Returns the number of elements in the list l. */

extern VoidPtr IthElemVoidPtrList (VoidPtrList l, int i);
   /* Returns the i-th element of the List l. The head of l
    * is referred to as 1. If the value of
    * i is greater than the length of the list, an error
    * is reported and the program exits.
    */

extern VoidPtrList CopyVoidPtrList (VoidPtrList l, VoidPtrMapFct cp);
   /* Copies the list l. Elements are copied by calls of cp. */

extern VoidPtrList AppVoidPtrList (VoidPtrList l1, VoidPtrList l2);
   /* Concatenates two lists l1 and l2. The resulting list contains l2 at
    * the end of a copy of list l1. */

extern VoidPtrList AppElVoidPtrList (VoidPtrList l, VoidPtr e);
   /* Appends an element e to the list l.
    * The list l is not copied, it is modified as a side-effect
    * of this function. */

extern void InsertAfterVoidPtrList (VoidPtrList l, VoidPtr e);
   /* This function requires a non-empty list l. The element e is inserted
    * just after the first element of l. 
    * The list l is modified as a side-effect of this function. */

extern VoidPtrList OrderedInsertVoidPtrList (VoidPtrList l, VoidPtr e, VoidPtrCmpFctType fcmp);
   /* Inserts the element e into the list l maintaining l in
    * ascending order with respect to the compare fcmp. 
    * The list l is modified as a side-effect of this function. */

extern VoidPtrListPtr RefEndConsVoidPtrList (VoidPtrListPtr addr, VoidPtr e);
   /* Appends an element e to the end of a list given by its address addr.
    * The address where the next element may be appended is returned.
    * The list is modified as a side-effect of this function. */

extern VoidPtrListPtr RefEndAppVoidPtrList (VoidPtrListPtr addr, VoidPtrList l);
   /* Appends a list l to the end of a list given by its address addr.
    * The address where the next element may be appended is returned.
    * The list is modified as a side-effect of this function. */

extern int ElemInVoidPtrList (VoidPtr e, VoidPtrList l, VoidPtrCmpFctType cmpfct);
   /* This function returns true (1) iff the element e is in the List l.
    * List elements are compared by the function cmpfct. */

extern VoidPtrList AddToSetVoidPtrList (VoidPtr e, VoidPtrList l, VoidPtrCmpFctType cmpfct);
   /* A list is returned that has e as an element.
    * l is checked whether it already contains e using the compare
    * function cmpfct. */

extern VoidPtrList AddToOrderedSetVoidPtrList 
       (VoidPtr e, VoidPtrList l, VoidPtrCmpFctType cmpfct);
   /* A list is returned that has e as an element.
    * l is checked whether it already contains e using the compare
    * function cmpfct. l is assumed to be ordered increasingly in the sense of
    * cmpfct. */

extern VoidPtrList MapVoidPtrList (VoidPtrList l, VoidPtrMapFct f);
   /* Returns a new VoidPtrList obtained by applying f to each element of l. */

extern int CompVoidPtrList (VoidPtrList l1, VoidPtrList l2, VoidPtrCmpFctType f);
   /* Compares the lists l1 and l2 lexicographically by applying f
    * to the corresponding elements. */

extern VoidPtr SumVoidPtrList (VoidPtrList l, VoidPtrSumFct f, VoidPtr a);
   /* Applies the binary function f to the elements of the List:
    *   f( f(... f(a, e1), e2, ...), en)
    * If l is empty a is returned. */

#else

extern void FinlVoidPtrList ();
extern VoidPtrList ConsVoidPtrList ();
extern VoidPtr HeadVoidPtrList ();
extern VoidPtrList TailVoidPtrList ();
extern int LengthVoidPtrList ();
extern VoidPtr IthElemVoidPtrList ();
extern VoidPtrList CopyVoidPtrList ();
extern VoidPtrList AppVoidPtrList ();
extern VoidPtrList AppElVoidPtrList ();
extern void InsertAfterVoidPtrList ();
extern VoidPtrList OrderedInsertVoidPtrList ();
extern VoidPtrListPtr RefEndConsVoidPtrList ();
extern VoidPtrListPtr RefEndAppVoidPtrList ();
extern int ElemInVoidPtrList ();
extern VoidPtrList AddToSetVoidPtrList ();
extern VoidPtrList MapVoidPtrList ();
extern int CompVoidPtrList ();
extern VoidPtr SumVoidPtrList ();

#endif

#ifdef MONITOR
#define DAPTO_RESULTVoidPtrList(l) DAPTO_RESULT_PTR(l)
#define DAPTO_ARGVoidPtrList(l)    DAPTO_ARG_PTR (l, VoidPtrList)
#endif

#endif
