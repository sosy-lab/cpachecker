
#ifndef BITSET_H
#define BITSET_H

#include <limits.h>

#define _INTBITS (CHAR_BIT * (int)sizeof(unsigned int))

#define _MAXSET ((int)(128/_INTBITS))

typedef unsigned int _cset[_MAXSET];

typedef struct _set_struct *BitSet;
struct _set_struct {
        _cset vektor;
        BitSet next;
};

#undef ARGS
#if defined (__cplusplus) || defined(__STDC__)
#define ARGS(args)      args
#else
#define ARGS(args)      ()
#endif

#define NullBitSet ((BitSet) 0)

extern BitSet NewBitSet ARGS((void));
/* allocates an empty set
*/

extern void FreeBitSet ARGS((BitSet s));
/* deallocates the set s
*/

extern void FreeMemBitSet ARGS((void));
/* deallocates all memory allocated for sets
*/

extern int EqualBitSet ARGS((BitSet s1, BitSet s2));
/* yields 1 if s1 and s2 contain the same elements; otherwise 0
*/

extern int EmptyBitSet ARGS((BitSet s));
/* yields 1 if s is empty; otherwise 0
*/

extern int EmptyIntersectBitSet ARGS((BitSet s1, BitSet s2));
/* yields 1 if the intersection of s1 and s2 is empty; otherwise 0
*/

extern int ElemInBitSet ARGS((int el, BitSet s));
/* yields 1 if el is element of s; otherwise 0
*/

extern int CardOfBitSet ARGS((BitSet s));
/* yields the number of elements in s
*/

extern BitSet AddElemToBitSet ARGS((int el, BitSet s));
/* imperative: add element el to set s
*/

#define ElemToBitSet(el) AddElemToBitSet(el, NullBitSet)
/* construct BitSet consisting of element el 
*/

extern BitSet AddRangeToBitSet ARGS((int el1, int el2, BitSet s));
/* imperative: all elements in the range from el1 to el2 are added to the set s
*/

extern BitSet SubElemFromBitSet ARGS((int el, BitSet s));
/* imperative: subtract element el from set s
*/

extern BitSet UnionToBitSet ARGS((BitSet s1, BitSet s2));
/* imperative: s1 is set to the union of s1 and s2
*/

extern BitSet IntersectToBitSet ARGS((BitSet s1, BitSet s2));
/* imperative: s1 is set to the intersection of s1 and s2
*/

extern BitSet SubtractFromBitSet ARGS((BitSet s1, BitSet s2));
/* imperative: s2 is subtracted from s1
*/

extern BitSet ComplToBitSet ARGS((int upb, BitSet s));
/* imperative: s is complemented with respect to the
   range 0 .. upb;
   no assumption can be made on elements larger than upb in s
*/

extern BitSet UniteBitSet ARGS((BitSet s1, BitSet s2));
/* functional: yields the union of s1 and s2
*/

extern BitSet IntersectBitSet ARGS((BitSet s1, BitSet s2));
/* functional: yields the intersection of s1 and s2
*/

extern BitSet SubtractBitSet ARGS((BitSet s1, BitSet s2));
/* functional: yields s1 minus s2
*/

extern BitSet ComplBitSet ARGS((int upb, BitSet s));
/* functional: yields the complement of s with respect to the
   range 0 .. upb;
   no assumption can be made on elements larger than upb in the result
*/

extern int NextElemInBitSet ARGS((int elem, BitSet s));
/* yields the smallest element of s that is larger than elem, if any;
   -1 otherwise
*/

extern void ApplyToBitSet ARGS((BitSet s, void func(int)));
/* apply function func to each element of s
*/

extern void PrintBitSet ARGS((BitSet s));
/* prints s as a string of 0 and 1 to stdout
*/

extern void PrintElemsBitSet ARGS((BitSet s));
/* prints s as a comma separated sequence of its elements
*/

#endif

