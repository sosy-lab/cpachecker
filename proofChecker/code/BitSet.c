
#include <stdlib.h>
#include <stdio.h>

#include "obstack.h"
#include "err.h"
#include "BitSet.h"

#define LEFT1   ((unsigned)1<<(_INTBITS-1))
#define ALLNULL ((unsigned)0)
#define ALLONE  ((unsigned)(~ ALLNULL))

#define MAXCARD (_MAXSET*_INTBITS)

#if defined(__STDC__) || defined(__cplusplus)
static void nullvektor(_cset v)
#else
static void nullvektor(v) 
_cset v;
#endif
{
        register int i;
        for (i = 0; i < _MAXSET; i++) v[i] = ALLNULL;
}

typedef struct {
  Obstack space;
  void *baseptr;
} Dyn, *DynP;

static DynP spacedescr = (DynP)0;

static BitSet heap = NullBitSet;

#if defined(__STDC__) || defined(__cplusplus)
void FreeBitSet(BitSet s)
#else
void FreeBitSet(s) 
BitSet s;
#endif
/* deallocates the set s
*/
{
        BitSet sptr;

        sptr = s;
        if (sptr == NullBitSet) return;
        while (sptr->next != NullBitSet) 
        {       if (sptr == heap)
                        message(DEADLY, "FreeBitSet: cyclic heap pointer",
                                0, (POSITION *)0);
                sptr = sptr->next;
        }
        sptr->next = heap;
        heap = s;
}

void FreeMemBitSet()
/* deallocates all memory allocated for sets
*/
{
  if ( spacedescr != (DynP)0 )
  {
     obstack_free(&(spacedescr->space), spacedescr->baseptr);
     spacedescr->baseptr = obstack_alloc(&(spacedescr->space), 0);
     heap = NullBitSet;
  }
}

BitSet NewBitSet()
/* allocates an empty set
*/
{
        BitSet s;

        if (heap == NullBitSet)
        {
                if (!spacedescr) {
                        spacedescr = (DynP) malloc (sizeof(Dyn));
                        if (!spacedescr) {
                                message(DEADLY, "NewBitSet: malloc failed",
                                        0, (POSITION *)0);
                                return NullBitSet;
                        }
                        obstack_init(&(spacedescr->space));
                        spacedescr->baseptr = 
                                obstack_alloc(&(spacedescr->space), 0);
                }
                s = (BitSet)obstack_alloc(&(spacedescr->space),
                                sizeof(struct _set_struct));
        }
        else
        {
                s = heap;
                heap = heap->next;
        }
        nullvektor(s->vektor);
        s->next = NullBitSet;
        return s;
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet AddElemToBitSet(int el, BitSet s)
#else
BitSet AddElemToBitSet(el, s) int el; BitSet s;
#endif
/* imperative: add element el to set s
*/
{
        unsigned int pattern = LEFT1;
        int pos;
        BitSet res;

        if (el < 0) {
                message(DEADLY, "AddElemToBitSet: wrong element",
                                0, (POSITION *)0);
                return s;
        }

        if (s == NullBitSet) s = NewBitSet();
        res = s;

        pos = el / MAXCARD;
        el %= MAXCARD;
        while (pos > 0)
        {
                if (s->next == NullBitSet) s->next = NewBitSet();
                s = s->next;
                pos--;
        }
        pattern >>= el % _INTBITS;
        s->vektor[el / _INTBITS] |= pattern;
        return res;
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet AddRangeToBitSet(int el1, int el2, BitSet s)
#else
BitSet AddRangeToBitSet(el1, el2, s) int el1, el2; BitSet s;
#endif
/* imperative: all elements in the range from el1 to el2 are added to the set s
*/
{
        unsigned int pattern = LEFT1;
        int pos1,pos2,pos,i;
        BitSet res;

        if (el2 < el1) return s;
        if (el1 < 0 || el2 < 0) {
                message(DEADLY, "AddRangeToBitSet: wrong range",
                                0, (POSITION *)0);
                return s;
        }
        if (s == NullBitSet) s = NewBitSet();
        res = s;
        pos1 = el1 / MAXCARD;
        pos2 = el2 / MAXCARD;
        el1 %= MAXCARD;
        el2 %= MAXCARD;
        pos = 0;
        while (pos < pos1) 
        {
                if (s->next == NullBitSet) s->next = NewBitSet();
                s = s->next;
                pos++;
        }

        if (pos1 == pos2)
        {       
                for(i=el1; i<=el2; i++)
                        s->vektor[i / _INTBITS] |= pattern >> i%_INTBITS;
        }
        else
        {
                for(i=el1; i<MAXCARD; i++)
                        s->vektor[i / _INTBITS] |= pattern >> i%_INTBITS;

                if (s->next == NullBitSet) s->next = NewBitSet();
                s = s->next;
                pos++;
                while(pos < pos2 )
                {       for (i=0; i<MAXCARD/_INTBITS; i++)
                                s->vektor[i] = ALLONE;
                        if (s->next == NullBitSet) s->next = NewBitSet();
                        s = s->next;
                        pos++;
                }

                for(i=0; i<=el2; i++)
                        s->vektor[i / _INTBITS] |= pattern >> i%_INTBITS;
        }
        return res;
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet SubElemFromBitSet(int el, BitSet s)
#else
BitSet SubElemFromBitSet(el, s) int el; BitSet s;
#endif
/* imperative: subtract element el from set s
*/
{
        unsigned int pattern = LEFT1;
        int pos;
        BitSet res;

        if (el < 0) {
                message(DEADLY, "SubElemFromBitSet: wrong element",
                                0, (POSITION *)0);
                return s;
        }
        if (s == NullBitSet) s = NewBitSet();
        res = s;
        pos = el / MAXCARD;
        el %= MAXCARD;
        while (pos > 0)
        {
                if (s->next == NullBitSet) break; /* Set hat <= pos Vektoren  => fertig */
                s = s->next;
                pos--;
        }
        if (pos <= 0)
        {
                pattern >>= el % _INTBITS;
                s->vektor[el / _INTBITS] &= ~pattern;
        }
        return res;
}

#if defined(__STDC__) || defined(__cplusplus)
int EqualBitSet(BitSet s1, BitSet s2)
#else
int EqualBitSet(s1,s2) BitSet s1, s2;
#endif
/* yields 1 if s1 and s2 contain the same elements; otherwise 0
*/
{
        register int i;

        while (s1 != NullBitSet && s2 != NullBitSet)
        {
                for (i = 0;i < _MAXSET; i++)
                        if (s1->vektor[i] != s2->vektor[i]) return 0;
                s1 = s1->next;
                s2 = s2->next;
        }
        if (s1 != NullBitSet) return(EmptyBitSet(s1)); /* s1 enthaelt noch Elemente */
        if (s2 != NullBitSet) return(EmptyBitSet(s2)); /* s2 enthaelt noch Elemente */
        return 1;
}

#if defined(__STDC__) || defined(__cplusplus)
int EmptyBitSet(BitSet s)
#else
int EmptyBitSet(s) BitSet s;
#endif
/* yields 1 if s is empty; otherwise 0
*/
{
        register int i;

        while (s != NullBitSet)
        {
                for (i = 0; i < _MAXSET; i++)
                        if (s->vektor[i] != ALLNULL) return 0;
                s = s->next;
        }
        return 1;
}

#if defined(__STDC__) || defined(__cplusplus)
int EmptyIntersectBitSet(BitSet s1, BitSet s2)
#else
int EmptyIntersectBitSet(s1,s2) BitSet s1, s2;
#endif
/* yields 1 if the intersection of s1 and s2 is empty; otherwise 0
*/
{
        register int i;

        while (s1 != NullBitSet && s2 != NullBitSet)
        {
           for (i = 0;i < _MAXSET; i++)
              if ((s1->vektor[i] & s2->vektor[i]) != ALLNULL) return 0;
           s1 = s1->next;
           s2 = s2->next;
        }
        return 1;
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet ComplBitSet(int upb, BitSet s)
#else
BitSet ComplBitSet(upb, s) int upb; BitSet s;
#endif
/* functional: yields the complement of s with respect to the
   range 0 .. upb; 
   no assumption can be made on elements larger than upb in the result
*/

{
        if (upb < 0) {
                message(DEADLY, "ComplBitSet: wrong range", 
                        0, (POSITION *)0);
                return NullBitSet;
        }

        return SubtractFromBitSet (AddRangeToBitSet (0, upb, NullBitSet), s);
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet ComplToBitSet(int upb, BitSet s) 
#else
BitSet ComplToBitSet(upb, s) int upb; BitSet s;
#endif
/* imperative: s is complemented with respect to the
   range 0 .. upb;
   no assumption can be made on elements larger than upb in s
*/

{
        register int i; int upbinset;
        BitSet scomp;

        if (upb < 0) {
                message(DEADLY, "ComplToBitSet: wrong range", 
                        0, (POSITION *)0);
                return NullBitSet;
        }

        scomp = s;
        upbinset = ElemInBitSet (upb, scomp);
        if (!upbinset) scomp = AddElemToBitSet (upb, scomp);

        for (;;)
        {
                for (i = 0; i < _MAXSET; i++) s->vektor[i] = ~(s->vektor[i]);
                s = s->next;
                if (s == NullBitSet)
                    break;      /* Menge enthaelt keine Elemente mehr */
        }
        if (!upbinset) s = AddElemToBitSet (upb, scomp);
        return scomp;
}

#if defined(__STDC__) || defined(__cplusplus)
int ElemInBitSet(int el, BitSet s)
#else
int ElemInBitSet(el, s) int el; BitSet s;
#endif
/* yields 1 if el is element of s; otherwise 0         
*/
{
        unsigned int mask = LEFT1;
        int pos;
        if (el < 0) {
                message(DEADLY, "ElemInBitSet: wrong element", 
                        0, (POSITION *)0);
                return 0;
        }
        if (s == NullBitSet) return 0;

        pos = el / MAXCARD;
        el %= MAXCARD;
        while (pos > 0)
        {
                if (s->next == NullBitSet) return 0;
                  /* Set hat <= pos Vektoren => Element ist nicht in der Menge */
                s = s->next;
                pos--;
        }
        
        /* pos<= 0 */
        mask >>= el % _INTBITS;
        return  (int) (s->vektor[el / _INTBITS] & mask);
}

#if defined(__STDC__) || defined(__cplusplus)
int CardOfBitSet(BitSet s)
#else
int CardOfBitSet(s) BitSet s;
#endif
/* yields the number of elements in s
*/
{
        register int i, j;
        int c = 0;
        unsigned int mask;

        while (s != NullBitSet)
        {
                for (i = 0; i < _MAXSET; i++)
                {
                        mask = s->vektor[i];
                        for (j = 0; j < _INTBITS; j ++)
                        {
                                c +=  mask & 1;
                                mask >>= 1;
                        }
                }
                s = s->next;
        }
        return(c);
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet UniteBitSet(BitSet s1, BitSet s2)
#else
BitSet UniteBitSet(s1, s2) BitSet s1, s2;
#endif
/* functional: yields the union of s1 and s2
*/
{
        register int i;
        BitSet sr, sptr;

        sr = NewBitSet();
        if (s1 == NullBitSet && s2 == NullBitSet) return(sr);
        sptr = sr;
        while (s1 != NullBitSet && s2 != NullBitSet)
        {
                for (i = 0; i < _MAXSET; i++)
                        sptr->vektor[i] = s1->vektor[i] | s2->vektor[i];
                s1 = s1->next;
                s2 = s2->next;
                if (s1 == NullBitSet && s2 == NullBitSet) return sr;
                sptr->next = NewBitSet();
                sptr = sptr->next;
        }
        if (s1 != NullBitSet)   /* s1 enthaelt noch Elemente */
                for (;;)
                {
                        for (i = 0; i < _MAXSET; i++)
                                sptr->vektor[i] = s1->vektor[i];
                        s1 = s1->next;
                        if (s1 == NullBitSet) return sr;
                        sptr->next = NewBitSet();
                        sptr = sptr->next;
                }
        if (s2 != NullBitSet)   /* s2 enthaelt noch Elemente */
                for (;;)
                {
                        for (i = 0; i < _MAXSET; i++)
                                sptr->vektor[i] = s2->vektor[i];
                        s2 = s2->next;
                        if (s2 == NullBitSet) return sr;
                        sptr->next = NewBitSet();
                        sptr = sptr->next;
                }

        /* This shouldn't happen */
        return(sr);
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet UnionToBitSet(BitSet s1, BitSet s2)
#else
BitSet UnionToBitSet(s1, s2) BitSet s1, s2;
#endif
/* imperative: s1 is set to the union of s1 and s2
*/
{
        register int i;
        BitSet sptr;

        if (s2 == NullBitSet) return s1;
        if (s1 == NullBitSet) s1 = NewBitSet();
        sptr = s1;
        for (;;)
        {
                for (i = 0; i < _MAXSET; i++)
                        s1->vektor[i] = s1->vektor[i] | s2->vektor[i];
                s2 = s2->next;
                if (s2 == NullBitSet) return sptr;
                if (s1->next == NullBitSet)  s1->next = NewBitSet(); 
                s1 = s1->next;
        }
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet IntersectBitSet(BitSet s1, BitSet s2)
#else
BitSet IntersectBitSet(s1, s2) BitSet s1, s2;
#endif
/* functional: yields the intersection of s1 and s2
*/
{
        register int i;
        BitSet sr, sptr;

        if (s1 == NullBitSet || s2 == NullBitSet) return NullBitSet;
        sr = NewBitSet();
        sptr = sr;
        for (;;)
        {
                for (i = 0; i < _MAXSET; i++)
                        sptr->vektor[i] = s1->vektor[i] & s2->vektor[i];
                s1 = s1->next;
                s2 = s2->next;
                if (s1 != NullBitSet && s2 != NullBitSet)
                {
                        sptr->next = NewBitSet();
                        sptr = sptr->next;
                }
                else return sr;         /* maximal eine Menge enthaelt */
        }                               /* noch Elemente => fertig     */
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet IntersectToBitSet(BitSet s1, BitSet s2)
#else
BitSet IntersectToBitSet(s1, s2) BitSet s1, s2;
#endif
/* imperative: s1 is set to the intersection of s1 and s2
*/
{
        register int i;
        BitSet sptr;

        if (s1 == NullBitSet) return s1;
        if (s2 == NullBitSet) s2 = NewBitSet();
        sptr = s1;
        for (;;)
        {
                for (i = 0; i < _MAXSET; i++)
                        s1->vektor[i] = s1->vektor[i] & s2->vektor[i];
                s2 = s2->next;
                if (s2 == NullBitSet)
                {       if (s1->next != NullBitSet)
                        {     FreeBitSet(s1->next);
                              s1->next = NullBitSet;
                         }
                }
                s1 = s1->next;
                if (s1 == NullBitSet) return sptr;
        }       /* maximal eine Menge enthaelt */
                /* noch Elemente => fertig     */
}
                        

#if defined(__STDC__) || defined(__cplusplus)
BitSet SubtractBitSet(BitSet s1, BitSet s2)
#else
BitSet SubtractBitSet(s1, s2) BitSet s1, s2;
#endif
/* functional: yields s1 minus s2
*/
{
        register int i;
        BitSet sr, sptr;

        if (s1 == NullBitSet) return NullBitSet;
        sr = NewBitSet();
        sptr = sr;
        if (s2 != NullBitSet)
                for (;;)
                {
                        for (i = 0; i < _MAXSET; i++)
                                sptr->vektor[i] = s1->vektor[i] & ~(s2->vektor[i]);
                        s1 = s1->next;
                        s2 = s2->next;
                        if (s1 != NullBitSet && s2 != NullBitSet)
                        {
                                sptr->next = NewBitSet();
                                sptr = sptr->next;
                        }
                        else break;     /* maximal eine Menge enthaelt noch Elemente */
                }
        else    /* Elemente aus erstem Vektor von Menge s1 uebernehmen */
        {
                for (i = 0; i < _MAXSET; i++) sptr->vektor[i] = s1->vektor[i];
                s1 = s1->next;
        }
        while (s1 != NullBitSet)        /* s1 enthaelt noch Elemente */
        {
                sptr->next = NewBitSet();
                sptr = sptr->next;
                for (i = 0; i < _MAXSET; i++) sptr->vektor[i] = s1->vektor[i];
                s1 = s1->next;
        }
        return sr;
}

#if defined(__STDC__) || defined(__cplusplus)
BitSet SubtractFromBitSet(BitSet s1, BitSet s2)
#else
BitSet SubtractFromBitSet(s1, s2) BitSet s1, s2;
#endif
/* imperative: s2 is subtracted from s1
*/
{
        register int i;
        BitSet sr;

        if (s1 == NullBitSet) return s1;
        if (s2 == NullBitSet) return s1;
        sr = s1;
        for (;;)
        {
                for (i = 0; i < _MAXSET; i++)
                        s1->vektor[i] = s1->vektor[i] & ~(s2->vektor[i]);
                s1 = s1->next;
                s2 = s2->next;
                if (s1 == NullBitSet || s2 == NullBitSet)
                        return(sr);
                /* maximal eine Menge enthaelt noch Elemente */
        }
}

#if defined(__STDC__) || defined(__cplusplus)
void PrintBitSet(BitSet s)
#else
void PrintBitSet(s) BitSet s;
#endif
/* prints s as a string of 0 and 1 to stdout
*/
{
        register int i, j;
        unsigned int pattern;

        while (s != NullBitSet)
        {
                for (i = 0; i < _MAXSET; i++)
                {
                        pattern = s->vektor[i];
                        for (j = 0; j < _INTBITS; j ++)
                        {
                                if (pattern & LEFT1) putchar('1');
                                else putchar('0');
                                pattern <<= 1;
                        }
                        putchar('\n');
                }
                putchar('\n');
                s = s->next;
        }
}

#if defined(__STDC__) || defined(__cplusplus)
void PrintElemsBitSet(BitSet s)
#else
void PrintElemsBitSet(s) BitSet s;
#endif
/* prints s as a comma separated sequence of its elements
*/
{       register int i;
        int offset = 0;
        int lauf = 0;
        int anfang;
        int komma = 0;

        while (s != NullBitSet)
        {
                for (i = 0; i < MAXCARD; i++)
                        if (lauf) 
                        {       if (!ElemInBitSet(i, s))
                                {       if (komma) putchar(',');
                                        if (i + offset - anfang < 2)
                                                printf("%d",anfang);
                                        else
                                        if (i + offset - anfang < 3) 
                                                printf("%d,%d",anfang,anfang+1);
                                        else printf("%d..%d",
                                                anfang,i + offset - 1);
                                        lauf = 0; komma = 1;
                                }
                        }       
                        else
                        {       if (ElemInBitSet(i, s))
                                {       anfang = i + offset;
                                        lauf = 1;
                                }
                        }
                s = s->next;
                offset += MAXCARD;
        }
        if (lauf)
        {       if (komma) putchar(',');
                if (offset - anfang < 2)
                        printf("%d",anfang);
                else 
                if (offset - anfang < 3) 
                        printf("%d,%d", anfang, anfang+1);
                else printf("%d..%d",anfang,offset - 1);
        }
        putchar('\n');
}

#if defined(__STDC__) || defined(__cplusplus)
int NextElemInBitSet(int elem, BitSet s)
#else
int NextElemInBitSet(elem, s) int elem; BitSet s;
#endif
/* yields the smallest element of s that is larger than elem, if any;
   -1 otherwise
*/
{       unsigned int mask = LEFT1;
        int pos, bit;

        if (s == NullBitSet) return -1;
        pos = (++elem) / MAXCARD;
        while (pos > 0)
        {
                if (s->next == NullBitSet) return -1;
                s = s->next;
                pos--;
        }
        bit = elem % MAXCARD;
        mask >>= bit % _INTBITS;
        while (!(s->vektor[bit / _INTBITS] & mask))
        {
                bit++;
                elem++;
                mask >>= 1;
                if (mask == 0) mask = LEFT1;
                if (bit == MAXCARD)
                {
                        bit = 0;
                        s = s->next;
                        if (s == NullBitSet) return -1;
                }
        }
        return elem;
}

#if defined(__STDC__) || defined(__cplusplus)
void ApplyToBitSet(BitSet s, void (*func)(int))
#else
void ApplyToBitSet(s, func) BitSet s; void (*func)();
#endif
/* apply function func to each element of s
*/
{
        register int i, j;
        int k=0;
        unsigned int pattern;

        while (s != NullBitSet)
        {
                for (i = 0; i < _MAXSET; i++)
                {
                        pattern = s->vektor[i];
                        if (pattern != 0)
                                for (j = 0; j < _INTBITS; j ++)
                                {
                                        if (pattern & LEFT1)
                                                func(k+j);
                                        pattern <<= 1;
                                };
                        k += _INTBITS;
                }
                s = s->next;
        }
        
}

