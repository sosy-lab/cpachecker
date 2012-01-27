
#ifndef _DefTableKeyLIST_H
#define _DefTableKeyLIST_H

/* include header file defining DefTableKey if deftbl is set: */
#define EMPTYdeftblHDR
#ifndef EMPTYHDR
#include "deftbl.h"
#endif
#undef EMPTYdeftblHDR

#include "VoidPtrList.h"

typedef VoidPtrList     DefTableKeyList;
typedef VoidPtrList    *DefTableKeyListPtr;
typedef VoidPtrCmpFctType     DefTableKeyCmpFctType;

#define NULLDefTableKeyList             ((DefTableKeyList)0)
#define NullDefTableKeyList()   ((DefTableKeyList)0)
#define SingleDefTableKeyList(e)        (ConsDefTableKeyList((e),NULLDefTableKeyList))

#define FinlDefTableKeyList             FinlVoidPtrList

#define ConsDefTableKeyList(e,l)        ConsVoidPtrList ((VoidPtr)(e),(l))

#define HeadDefTableKeyList(l)  ((DefTableKey)(HeadVoidPtrList (l)))

#define TailDefTableKeyList(l)  TailVoidPtrList (l)

#define LengthDefTableKeyList(l)        LengthVoidPtrList (l)

#define IthElemDefTableKeyList(l,i)     ((DefTableKey)(IthElemVoidPtrList (l, i)))

#define CopyDefTableKeyList(l,cp)       CopyVoidPtrList (l, cp)

#define AppDefTableKeyList(l1,l2)       AppVoidPtrList (l1, l2)

#define AppElDefTableKeyList(l,e)       AppElVoidPtrList (l, (VoidPtr)e)

#define InsertAfterDefTableKeyList(l,e) \
        InsertAfterVoidPtrList (l, (VoidPtr)e)

#define OrderedInsertDefTableKeyList(l,e,fcmp) \
        OrderedInsertVoidPtrList (l, (VoidPtr)e, (VoidPtrCmpFctType)fcmp)

#define RefEndConsDefTableKeyList(addr,e) \
        RefEndConsVoidPtrList (addr, (VoidPtr)e)

#define RefEndAppDefTableKeyList(addr,l) \
        RefEndAppVoidPtrList (addr, l)

#define ElemInDefTableKeyList(e,l,cmpfct) \
        ElemInVoidPtrList ((VoidPtr)e, l, (VoidPtrCmpFctType)cmpfct)

#define AddToSetDefTableKeyList(e,l,cmpfct) \
        AddToSetVoidPtrList ((VoidPtr)e, l, (VoidPtrCmpFctType)cmpfct)

#define AddToOrderedSetDefTableKeyList(e,l,cmpfct) \
        AddToOrderedSetVoidPtrList ((VoidPtr)e, l, (VoidPtrCmpFctType)cmpfct)

#define MapDefTableKeyList(l,f) \
        MapVoidPtrList (l,(VoidPtrMapFct)f)

#define CompDefTableKeyList(l1,l2,f) \
        CompVoidPtrList (l1, l2, (VoidPtrCmpFctType)f)

#define SumDefTableKeyList(l,f,a) \
        SumVoidPtrList (l, (VoidPtrSumFct)f, (VoidPtr)a)

#ifdef MONITOR
#define DAPTO_RESULTDefTableKeyList(l) DAPTO_RESULT_PTR(l)
#define DAPTO_ARGDefTableKeyList(l)    DAPTO_ARG_PTR (l, DefTableKeyList)
#endif

#endif
