static char RCSid[] = "$Id: envmod.c,v 5.11 1999/11/19 08:56:06 uwe Exp $";
/* Copyright 1997, The Regents of the University of Colorado */

/* This file is part of the Eli Module Library.

The Eli Module Library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public License as
published by the Free Software Foundation; either version 2 of the
License, or (at your option) any later version.

The Eli Module Library is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with the Eli Module Library; see the file COPYING.LIB.
If not, write to the Free Software Foundation, Inc., 59 Temple Place -
Suite 330, Boston, MA 02111-1307, USA.  */

/* As a special exception, when this file is copied by Eli into the
   directory resulting from a :source derivation, you may use that
   created file as a part of that directory without restriction. */

#include <stdlib.h>
#include "err.h"
#include "obstack.h"
#include "envmod.h"
#ifndef NORESTORE
#include "obsave.h"
#endif
#ifdef MONITOR
#include "envmod_dapto.h"
#endif


#ifdef PROTO_OK
Binding DefinitionsOf (Environment env)
  /* the sequence of bindings of the environment env */
#else
Binding DefinitionsOf (env) Environment env;
#endif
{ return (env ? env->relate: NoBinding); }

#ifdef PROTO_OK
Binding NextDefinition (Binding b)
  /* the next binding within the sequence of an environment */
#else
Binding NextDefinition (b) Binding b;
#endif
{ return (b ? b->nxt : NoBinding); }

#ifdef PROTO_OK
int IdnOf (Binding b)
  /* the identifier of a binding */
#else
int IdnOf (b) Binding b;
#endif
{ return (b ? b->idn : 0); }

#ifdef PROTO_OK
DefTableKey KeyOf (Binding b)
  /* the key of a binding */
#else
DefTableKey KeyOf (b) Binding b;
#endif
{ return (b ? b->key : NoKey); }

#ifdef PROTO_OK
Environment EnvOf (Binding b)
  /* the environment of a binding */
#else
Environment EnvOf (b) Binding b;
#endif
{ return (b ? b->env : NoEnv); }

#ifdef PROTO_OK
Environment ParentOf (Environment env)
  /* the parent environment of env */
#else
Environment ParentOf (env) Environment env;
#endif
{ return (env ? env->parent : NoEnv); }

#ifdef PROTO_OK
DefTableKey KeyOfEnv (Environment env)
  /* the key of the environment env */
#else
DefTableKey KeyOfEnv (env) Environment env;
#endif
{ return (env ? env->key : NoKey); }

#ifdef PROTO_OK
DefTableKey SetKeyOfEnv (Environment env, DefTableKey k)
  /* the key of the environment env is set to k */
#else
DefTableKey SetKeyOfEnv (env, k) Environment env; DefTableKey k;
#endif
{ return (env ? env->key = k : NoKey); }

#ifdef PROTO_OK
int IsClass (Environment env)
  /* 0 iff the environment env does not participate in 
     an inheritance relation */
#else
int IsClass (env) Environment env;
#endif
{ return (env ? env->classdescr != (_Class)0 : 0); }

#ifdef PROTO_OK
InheritPtr  DirectInherits (Environment env)
  /* the sequence of environments which env directly inherits from */
#else
InheritPtr  DirectInherits (env) Environment env;
#endif
{ return (env && env->classdescr ?
          env->classdescr->inhlist : NoInherit);
}

#ifdef PROTO_OK
Environment EnvOfInherit (InheritPtr inh)
  /* the first environment of the inheritance sequence */
#else
Environment EnvOfInherit (inh) InheritPtr inh;
#endif
{ return (inh ? inh->fromcl->env : NoEnv); }

#ifdef PROTO_OK
InheritPtr  NextInherit (InheritPtr inh)
  /* the next element of the inheritance sequence */
#else
InheritPtr  NextInherit (inh) InheritPtr inh;
#endif
{ return (inh ? inh->nxt : NoInherit); }


#define NoClassNo -1
#define ClassVisited -2

static struct EnvmodData 
{
    StkPtr FreeStackElements;
    void *baseptr;
} state = {
    (StkPtr)0,
    (void *)0
};

static Obstack space;

#ifndef NORESTORE
/* Routine to register adresses of dynamically allocated obstacks */

static Obstack stack_adresses = /* Adresses of allocated obstacks, growing object */
   obstack_empty_chunk(4096, OBSTACK_PTR_ALIGN);

/* Obstacks must be allocated from outside of the 'space'-obstack, since normal
 * save/restore does not work for obstack-Pointers without memory leaks */

static Obstack stacks =         /* Dynamically allocated obstacks. */
   obstack_empty_chunk(4096, OBSTACK_PTR_ALIGN);

static ObstackP NEW_OBSTACK ELI_ARG((void))
{
    ObstackP obst = (ObstackP) obstack_alloc(&stacks, sizeof(Obstack));
    obstack_grow(&stack_adresses, &obst, sizeof(ObstackP));
    
    return obst;
}
#else
#define NEW_OBSTACK() (ObstackP)obstack_alloc(&space, sizeof(Obstack))
#endif 

#ifdef MONITOR
#define MON_BINDING(i,k,e) _dapto_binding_made (e, i, k)
#else
#define MON_BINDING(i,k,e) /* empty */
#endif

#define MAKE_BINDING(r,i,k,e) \
  { r = (Binding)obstack_alloc(&space, sizeof(struct _RelElt)); \
    r->nxt = e->relate; e->relate = r; r->env = e; r->key = k; r->idn = i; \
    MON_BINDING (i, k, e); }

#define IDN_STK(e,i) (((StkPtr *)obstack_base(((e)->access->IdnTbl)))[i])

#define IDN_ACCESSIBLE(i,e) \
  while ((i) >= (e)->access->MaxIdn) { \
    obstack_blank(((e)->access->IdnTbl), sizeof(StkPtr)); \
    IDN_STK((e), (e)->access->MaxIdn) = (StkPtr)0; \
    (e)->access->MaxIdn++; \
  }

#define PUSH_IDN_STK(i,e,r) \
  { StkPtr s = NewStkElt(); s->binding = (r); \
    s->out = IDN_STK((e), (i)); IDN_STK((e), (i)) = s; }

#define CLASS_IDN_STK(e,i) \
  (((StkPtr *)obstack_base((e)->access->ClassIdnTbl))[i])

#define CLASS_ACCESSIBLE(i,e) \
  while ((i) >= (e)->access->MaxClassIdn) { \
    obstack_blank((e)->access->ClassIdnTbl, sizeof(StkPtr)); \
    CLASS_IDN_STK((e), (e)->access->MaxClassIdn) = (StkPtr)0; \
    env->access->MaxClassIdn++; \
  }

#define INSERT_CLASS_IDN_STK(e,i,r) \
{ StkPtr s = NewStkElt(), after = (StkPtr)0, before; \
  int clno = (e)->classdescr->classno; \
  CLASS_ACCESSIBLE ((i), (e)) \
  before = CLASS_IDN_STK((e), (i)); \
  s->binding = r; \
  while (before && \
         before->binding->env->classdescr->classno > (clno)) { \
    after = before; before = before->out; \
  } \
  s->out = before; \
  if (after) after->out = s; \
  else CLASS_IDN_STK((e), (i)) = s; \
}

static void
#ifdef PROTO_OK
InitSpace(void)
#else
InitSpace()
#endif
{ obstack_init(&space);
  state.baseptr = obstack_alloc(&space, 0);
}

static StkPtr
#ifdef PROTO_OK
NewStkElt(void)
#else
NewStkElt()
#endif
/* Obtain a new definition stack element
 *   On exit-
 *     NewStkElt points to an element with undefined contents
 **/
{ if (state.FreeStackElements) {
    StkPtr tmp = state.FreeStackElements;
    state.FreeStackElements = state.FreeStackElements->out; 
    return tmp;
  }
  return (StkPtr)obstack_alloc(&space, sizeof(struct StkElt));
}

Environment
#ifdef PROTO_OK
NewEnv(void)
#else
NewEnv()
#endif
/* Establish a new environment
 *   On exit-
 *     NewEnv=new environment
 ***/
{ Environment e;

  if (!state.baseptr) InitSpace();
  e = (Environment)obstack_alloc(&space, sizeof(struct _EnvImpl));
  e->relate = NoBinding; e->parent = (Environment)0; e->key = NoKey;
  e->level = 0; e->classdescr = (_Class)0; e->nested = 1;
  e->haveusedbindings = 0;
  e->access = (_Access)obstack_alloc(&space, sizeof(struct _AccessMechanism)); 
  e->access->IdnTbl = NEW_OBSTACK(); obstack_init((e->access->IdnTbl)); 
  e->access->MaxIdn = 0; e->access->CurrEnv = e;
  e->access->ClassIdnTbl = (ObstackP)0; e->access->MaxClassIdn = 0;
  e->access->Classes = (_Class)0;
  e->access->NextClassNo = 0;

#ifdef MONITOR
  _dapto_env_created (e, NULL);
#endif

  return e;
}

static void
#ifdef PROTO_OK
EnterClasses(_Class cld)
#else
EnterClasses(cld) _Class cld;
#endif
/* Make the class access mechanism reflect cld and all its super classes
 *   On entry-
 *     the class cld is not yet entered in the class access mechanism
 *     all inheritances for cld have been established
 *     the inheritance relation is not cyclic
 */
{ InheritPtr inhlist; Binding r;
  Environment env = cld->env;

  for (inhlist = cld->inhlist; inhlist; inhlist = inhlist->nxt) {
    if (inhlist->fromcl->classno == NoClassNo) EnterClasses(inhlist->fromcl);
  }
  cld->classno = env->access->NextClassNo++;
  cld->inhset = AddElemToBitSet(cld->classno, NullBitSet);
  for (inhlist = cld->inhlist; inhlist; inhlist = inhlist->nxt) {
    (void)UnionToBitSet(cld->inhset, inhlist->fromcl->inhset);
  }

  for (r = cld->env->relate; r; r = r->nxt) {
    StkPtr s = NewStkElt(); s->binding = r;

    CLASS_ACCESSIBLE(r->idn, env)
    
    s->out = CLASS_IDN_STK(env, r->idn); CLASS_IDN_STK(env, r->idn) = s;
  }
}

static int doenterclasses = 0;

static void
#ifdef PROTO_OK
EnterEnv(Environment env)
#else
EnterEnv(env) Environment env;
#endif
/* Make the state of the array reflect env
 *   On entry-
 *     The access of the array reflects the parent of env
 **/
{ Binding r;

  for (r = env->relate; r;  r = r->nxt) {
    IDN_ACCESSIBLE(r->idn, env)
    PUSH_IDN_STK(r->idn, env, r)
  }
  env->nested = 1;
  env->access->CurrEnv = env;
  if (doenterclasses) env->haveusedbindings = 1;
  if (env->classdescr &&
      doenterclasses &&
      env->classdescr->classno == NoClassNo) {
    EnterClasses(env->classdescr);
  }
}
 
static void
#ifdef PROTO_OK
LeaveEnv(Environment env)
#else
LeaveEnv(env) Environment env;
#endif
/* Make the access of the array reflect the parent of env
 *   On entry-
 *     The access of the array reflects env
 **/
{ Binding r;

  for (r = env->relate; r; r = r->nxt) {
    StkPtr s = IDN_STK(env, r->idn);
    IDN_STK(env, r->idn) = s->out;
    s->out = state.FreeStackElements; state.FreeStackElements = s;
  }
  env->nested = 0;
  env->access->CurrEnv = env->parent;
}
 
static void
#ifdef PROTO_OK
SetEnv(Environment env)
#else
SetEnv(env) Environment env;
#endif
/* Make certain that the access of the array reflects env
 ***/
{
  if (env) {
    if (env->nested) {
      while (env->access->CurrEnv != env) LeaveEnv(env->access->CurrEnv);
    } else {
      SetEnv(env->parent); EnterEnv(env);
    }
  }
}
 
Environment
#ifdef PROTO_OK
NewScope(Environment env)
#else
NewScope(env) Environment env;
#endif
/* Establish a new scope within an environment
 *   On exit-
 *     NewScope=new environment that is a child of env
 ***/
{
  Environment e;
 
  if (!env) return NoEnv;
  e = (Environment)obstack_alloc(&space, sizeof(struct _EnvImpl));
  e->relate = NoBinding; e->parent = env; e->nested = 0;
  e->haveusedbindings = 0;
  e->access = env->access; e->key = NoKey;
  e->classdescr = (_Class)0;
  e->level = e->parent->level+1;

#ifdef MONITOR
  _dapto_env_created (e, env);
#endif

  return e;
}
 
static void
#ifdef PROTO_OK
MakeClass(Environment env)
#else
MakeClass(env) Environment env;
#endif
/* Prepare a scope to be used in inheritance
 *   On entry-
 *     env is the scope to be used in inheritance
 *   On exit-
 *     env may be used in inheritance
 *     The class descriptor is initialized
 *     The class access mechanism is initialized
 ***/
{ if (env == NoEnv || env->classdescr) return;

  env->classdescr =
    (_Class)obstack_alloc(&space, sizeof(struct _ClassDescriptor));
  env->classdescr->classno = NoClassNo;
  env->classdescr->inhlist = (InheritPtr)0;
  env->classdescr->env = env;

  /* append this class to list of all classes in access mechanism: */
  env->classdescr->nxt = env->access->Classes;
  env->access->Classes = env->classdescr;

  /* initialize class access structure: */
  if (!(env->access->ClassIdnTbl)) {
    env->access->ClassIdnTbl = NEW_OBSTACK();
    obstack_init(env->access->ClassIdnTbl);
  }
}

int
#ifdef PROTO_OK
Inheritsfrom(Environment tocl, Environment fromcl)
#else
Inheritsfrom(tocl, fromcl) Environment tocl, fromcl;
#endif
/* Checks the completed inheritance DAG
 * for existance of an inheritance path
 *   On entry-
 *     tocl and fromcl are Environments
 *   On exit-
 *     1 is returned if tocl == fromcl or if there is an
 *     inheritance path from fromcl to tocl;
 *     otherwise 0 is returned.
 *     No further inheritance may be established to tocl or fromcl
 *     after this call.
 ***/
{ 
  if (!tocl || !fromcl) return 0;
  MakeClass(tocl); MakeClass(fromcl);
  if (fromcl->classdescr->classno == NoClassNo) 
     EnterClasses (fromcl->classdescr);
  if (tocl->classdescr->classno == NoClassNo) 
     EnterClasses (tocl->classdescr);

  if (tocl == fromcl) return 1; /* shortcuts the following check */
  return
    ElemInBitSet (fromcl->classdescr->classno, tocl->classdescr->inhset);
}

static
int
#ifdef PROTO_OK
ChkInherit(Environment tocl, Environment fromcl)
#else
ChkInherit(tocl, fromcl) Environment tocl, fromcl;
#endif
/* Checks recursively the so far constructed inheritance DAG
 * for existance of an inheritance path
 * To be called only in InheritClass.
 *   On entry-
 *     tocl and fromcl are Environments
 *   On exit-
 *     1 is returned if tocl == fromcl or if there is an
 *     inheritance path from fromcl to tocl;
 *     otherwise 0 is returned
 ***/
{ InheritPtr inhlist;

  if (!tocl || !fromcl) return 0;
  if (tocl == fromcl) return 1;
  if (!(tocl->classdescr) || !(fromcl->classdescr)) return 0;

  for (inhlist = tocl->classdescr->inhlist; inhlist; inhlist = inhlist->nxt) {
    if (inhlist->fromcl->env == fromcl ||
        ChkInherit (inhlist->fromcl->env, fromcl)) return 1;
  }
  return 0;
}

int
#ifdef PROTO_OK
InheritClass(Environment tocl, Environment fromcl)
#else
InheritClass(tocl, fromcl) Environment tocl, fromcl;
#endif
/* Establish an inheritance from the class fromcl to tocl
 *   On entry-
 *     tocl and fromcl are Environments
 *     bindings must not yet have been sought in tocl
 *   On exit-
 *     the inheritance from fromcl to tocl is established and
 *     1 is returned if tocl and fromcl are both nested in
 *     the same Environment hierarchy and if
 *     tocl != fromcl and there is no inheritance path from
 *     tocl to fromcl and if
 *     tocl has not been searched before;
 *     otherwise no new inheritance is established and
 *     0 is returned.
 ***/
{ InheritPtr inhf;

  if (!tocl ||
      !fromcl ||
      tocl->access != fromcl->access ||
				/* Classes are in different name spaces */
      ChkInherit (fromcl, tocl) ||
				/* new inheritance would establish a cycle */ 
      tocl->haveusedbindings
				/* tocl has been searched before */
      ) return 0;

  MakeClass(tocl); MakeClass(fromcl);
  inhf = (InheritPtr)obstack_alloc(&space, sizeof(struct _Inheritance));
  inhf->fromcl = fromcl->classdescr;
  inhf->nxt = tocl->classdescr->inhlist; tocl->classdescr->inhlist = inhf;
  return 1;
}

Binding
#ifdef PROTO_OK
BindKey(Environment env, int idn, DefTableKey key)
#else
BindKey(env, idn, key) Environment env; int idn; DefTableKey key;
#endif
/* Bind an identifier to a given key in a scope, entering that scope
 *   If env is not the current environment then enter it
 *   If idn is bound in the innermost scope of env then on exit-
 *     BindKey=NoBinding
 *   Else on exit-
 *     BindKey=pointer to a new binding (idn,key)
 *       in the innermost scope of env
 ***/
{ Binding r;

  if (!env) return NoBinding;
  SetEnv(env);

  IDN_ACCESSIBLE(idn, env)

  if (IDN_STK(env, idn) ? EnvOf(IDN_STK(env, idn)->binding)==env : 0)
    return NoBinding;

  MAKE_BINDING(r, idn, key, env)
  PUSH_IDN_STK(r->idn, env, r)

  if (env->classdescr && env->classdescr->classno != NoClassNo) {
     /* class has been inserted into the class access;
	this new binding is added to the class access: */
    INSERT_CLASS_IDN_STK (env, idn, r)
  }
  return r;
}


Binding
#ifdef PROTO_OK
BindKeyInScope(Environment env, int idn, DefTableKey key)
#else
BindKeyInScope(env, idn, key) Environment env; int idn; DefTableKey key;
#endif
/* Bind an identifier to a given key in a scope without entering that scope
 *   If idn is bound in the innermost scope of env then on exit-
 *     BindKey=NoBinding
 *   Else on exit-
 *     BindKey=pointer to a new binding (idn,key)
 *       in the innermost scope of env
 ***/
{ Binding r;

  if (!env || BindingInScope(env, idn)) return NoBinding;
  else while (env->nested) LeaveEnv(env->access->CurrEnv);

  IDN_ACCESSIBLE(idn, env)

  MAKE_BINDING(r,idn,key,env)

  if (env->classdescr && env->classdescr->classno != NoClassNo) {
     /* class has been inserted into the class access;
	this new binding is added to the class access: */
    INSERT_CLASS_IDN_STK (env, idn, r)
  }
  return r;
}

Binding
#ifdef PROTO_OK
BindIdn(Environment env, int idn)
#else
BindIdn(env, idn) Environment env; int idn;
#endif
/* Bind an identifier in a scope, guaranteed to be the current scope
 *   If idn is bound in the innermost scope of env then on exit-
 *     BindIdn=pointer to the binding for idn in the innermost scope of env
 *   Else let n be a previously-unused definition table key
 *   Then on exit-
 *     BindIdn=pointer to a new binding (idn,n) in the innermost scope of env
 ***/
{ Binding r;

  if (!env) return NoBinding;
  SetEnv(env);

  IDN_ACCESSIBLE(idn, env)

  if (IDN_STK(env, idn) ? EnvOf(IDN_STK(env, idn)->binding)==env : 0)
    return IDN_STK(env, idn)->binding;

  MAKE_BINDING(r, idn, NewKey(), env)
  PUSH_IDN_STK(r->idn, env, r)

  if (env->classdescr && env->classdescr->classno != NoClassNo) {
     /* class has been inserted into the class access;
	this new binding is added to the class access: */
    INSERT_CLASS_IDN_STK (env, idn, r)
  }
  return r;
}

Binding
#ifdef PROTO_OK
BindInScope(Environment env, int idn)
#else
BindInScope(env, idn) Environment env; int idn;
#endif
/* Bind an identifier in a scope without entering that scope
 *   If idn is defined in the innermost scope of env then on exit-
 *     BindingInScope=pointer to the binding for idn
 *       in the innermost scope of env
 *   Else let n be a previously-unused definition table key
 *   Then on exit-
 *     BindingInScope=pointer to a new binding (idn,n)
 *       in the innermost scope of env
 ***/
{ Binding r;

  if (!env) return NoBinding;
  if ((r = BindingInScope(env, idn))) return r;

  while (env->nested) LeaveEnv(env->access->CurrEnv);

  IDN_ACCESSIBLE(idn, env)

  MAKE_BINDING(r,idn,NewKey(),env)

  if (env->classdescr && env->classdescr->classno != NoClassNo) {
     /* class has been inserted into the class access;
	this new binding is added to the class access: */
    INSERT_CLASS_IDN_STK (env, idn, r)
  }
  return r;
}

Binding
#ifdef PROTO_OK
BindingInEnv(Environment env, int idn)
#else
BindingInEnv(env, idn) Environment env; int idn;
#endif
/* Find the binding for an identifier in an environment
 *   If idn is bound in the innermost scope of env then on exit-
 *     BindingInEnv=pointer to the binding for idn in env
 *   Else if idn is bound in some ancestor of env then on exit-  
 *     BindingInEnv=BindingInEnv(Parent(env),idn)
 *   Else on exit-
 *     BindingInEnv=NoBinding
 *   Any class environment on the path from env to surrounding
 *     environments is checked for inherited definitions of idn
 ***/
{ StkPtr stk; Environment par;

  if (!env || idn >= env->access->MaxIdn) return NoBinding;
  doenterclasses = 1;
  SetEnv(env);
  doenterclasses = 0;

  stk = IDN_STK(env, idn);

  for (par = env; par; par = par->parent) {
    if (par->classdescr &&
        (!stk || (par->level > EnvOf(stk->binding)->level))) {
      Binding inhbind = BindingInScope(par, idn);
      if (inhbind) return inhbind;
    }
  }

  return (stk ? stk->binding : NoBinding);
}

Binding
#ifdef PROTO_OK
BindingInScope(Environment env, int idn)
#else
BindingInScope(env, idn) Environment env; int idn;
#endif
/* Find the binding for an identifier in a scope
 *   If idn is bound in the innermost scope of env then on exit-  
 *     BindingInScope=pointer to the binding for idn
 *       in the innermost scope of env
 *   Else on exit- 
 *     BindingInScope=NoBinding
 *   If env is a class environment inherited bindings are considered
 ***/
{ Binding r;

  if (!env) return NoBinding;

  env->haveusedbindings = 1;

  if (env->classdescr) {	/* use access structure for classes */
    StkPtr stk; int clno; BitSet inhset;

    if (env->classdescr->classno == NoClassNo)
		/* inheritances of this class have not been processed */
      EnterClasses(env->classdescr);

    if (idn >= env->access->MaxClassIdn) return NoBinding;

    clno = env->classdescr->classno;
    inhset = env->classdescr->inhset;

    stk = CLASS_IDN_STK(env, idn);

    /* skip all definitions in classes not inherited to this one */
    while (stk && clno < stk->binding->env->classdescr->classno)
      stk = stk->out;
    while (stk &&
           !ElemInBitSet(stk->binding->env->classdescr->classno, inhset))
      stk = stk->out;
    return (stk ? stk->binding : NoBinding);
  }

  /* search for a definition in an non class environment */
  for (r = env->relate; r; r = r->nxt) if (r->idn == idn) return r;

  return NoBinding;
}

static
StkPtr
#ifdef PROTO_OK
NextInhStkPtr (Environment env, StkPtr stk)
#else
NextInhStkPtr (env, stk) Environment env; StkPtr stk;
#endif
/* On entry:
 *   stk is an entry in the class access that refers to a binding
 *     of some identifier idn in an environment e which is
 *     inherited to an environment tocl that is env or is the
 *     next ancestor of env which inherits e.
 * On exit:
 *     NextInhStkPtr=nextstk represents the next binding to
 *     identifier idn in an environment ep which is also
 *     inherited to tocl but not to e,
 *     if any such binding exists;
 *     otherwise NextInhStkPtr=(StkPtr)0.
 ***/
{ Binding lastbinding = stk->binding;
  /* the class defining lastbinding: */
  int lastclno = EnvOf(lastbinding)->classdescr->classno;
  BitSet lastinhset = EnvOf(lastbinding)->classdescr->inhset;

  BitSet inhset; Environment par;

  stk = stk->out;

  /* find the first parent of env that is a class which
     inherits the class that defines lastbinding: */
  par = env;
  while (!(par->classdescr) ||
         !ElemInBitSet (lastclno, par->classdescr->inhset))
    par = par->parent;

  if (!par) return (StkPtr)0;

  /* classes that may contain definitions alternative to lastbinding: */
  inhset = par->classdescr->inhset;

  /* see if there are some which are not hidden by lastbinding: */
  while (stk &&
	 (!ElemInBitSet
	    (EnvOf(stk->binding)->classdescr->classno, inhset) ||
	     ElemInBitSet
	       (EnvOf(stk->binding)->classdescr->classno, lastinhset)))
    stk = stk->out;

  return stk;
}/* NextInhStkPtr */


DefTableKey
#ifdef PROTO_OK
NextInhKey(Environment env, int idn, DefTableKey lastkey)
#else
NextInhKey(env, idn, lastkey) Environment env; int idn; DefTableKey lastkey;
#endif
/* On entry:
 *   lastkey is a key bound to identifier idn in an
 *   environment e which is inherited to an environment tocl
 *   that is env or is the next ancestor of env which
 *   inherits e.
 * On exit:
 *     NextInhKey=key that represents the next definition bound to
 *     identifier idn in an environment ep which is also
 *     inherited to tocl but not to e,
 *     if any such definition exists;
 *     otherwise NextInhKey=NoKey
 ***/
{ StkPtr stk;

  if (lastkey == NoKey || 
      !env ||
      !(env->access->ClassIdnTbl) || 
      idn >= env->access->MaxClassIdn)
    return NoKey;

  stk = CLASS_IDN_STK(env, idn);

  /* skip all definitions until lastkey: */
  while (stk && (KeyOf(stk->binding) != lastkey)) stk = stk->out;

  if (!stk || !(stk->out)) return NoKey;

  stk = NextInhStkPtr (env, stk);
  return (stk ? KeyOf(stk->binding) : NoKey);
}/* NextInhKey */


Binding
#ifdef PROTO_OK
NextInhBinding (Environment env, Binding lastbinding)
#else
NextInhBinding (env, lastbinding)
  Environment env; Binding lastbinding;
#endif
/* On entry:
 *   lastbinding is a binding of an identifier idn in an
 *   environment e which is inherited to an environment tocl
 *   that is env or is the next ancestor of env which
 *   inherits e.
 * On exit:
 *     NextInhBinding=bdg is the next binding of
 *     identifier idn in an environment ep which is also
 *     inherited to tocl but not to e,
 *     if any such definition exists;
 *     otherwise NextInhBinding=NoBinding
 ***/
{ StkPtr stk;

  if (lastbinding == NoBinding || 
      !env ||
      !(env->access->ClassIdnTbl) || 
      !(lastbinding->env->classdescr) ||
      lastbinding->idn >= env->access->MaxClassIdn)
    return NoBinding;

  stk = CLASS_IDN_STK(env, lastbinding->idn);

  /* skip all definitions until lastbinding: */
  while (stk && (stk->binding != lastbinding)) stk = stk->out;

  if (!stk || !(stk->out)) return NoBinding;

  stk = NextInhStkPtr (env, stk);
  return (stk ? stk->binding : NoBinding);
}/* NextInhBinding */


Binding
#ifdef PROTO_OK
OverridesBinding (Binding bind)
#else
OverridesBinding (bind) Binding bind;
#endif
/* On entry:
 *   bind is a binding of an identifier idn in an
 *   environment env
 * On exit:
 *     OverridesBinding=bdg is the binding of
 *     identifier idn in an environment which is
 *     inherited to env,
 *     if any such definition exists;
 *     otherwise OverridesBinding=NoBinding
 ***/
{ StkPtr stk; BitSet inhset;

  if (bind == NoBinding ||
      !(bind->env->access->ClassIdnTbl) || 
      !(bind->env->classdescr))
    return NoBinding;

  if (bind->env->classdescr->classno == NoClassNo)
                /* inheritances of this class have not been processed */
     EnterClasses (bind->env->classdescr);
 
  if (bind->idn >= bind->env->access->MaxClassIdn) 
     return NoBinding;


  stk = CLASS_IDN_STK(bind->env, bind->idn);

  /* skip all definitions until bind: */
  while (stk && (stk->binding != bind)) stk = stk->out;
  if (!stk || !(stk->out)) return NoBinding;

  /* find the first overridden one: */
  stk = stk->out;
  /* skip all definitions in classes not inherited to this one */
  inhset = bind->env->classdescr->inhset;
  while (stk &&
         !ElemInBitSet(stk->binding->env->classdescr->classno, inhset))
    stk = stk->out;

  return (stk ? stk->binding : NoBinding);
}/* OverridesBinding */

#ifndef NORESTORE

static struct savedata_str
{
    void *space;
    void *saved_stacks;
    int stackscount;
    void *state;
    void *stacks_base;    
} savedata;

/* Store handles for saved obstacks somewhere. We need a dynamic growing array
 * to store them and must be able to Save/Restore the structure. So use an obstack: */
Obstack saved_stacks = obstack_empty_chunk(4096, OBSTACK_PTR_ALIGN);

void *SaveModuleEnvmod()
{
    void *base;
    ObstackP *the_stacks;
    
    /* envmod contains many growing obstacks; each contains one ever-growing object */
    /* The obstack 'stacks' contains pointers to all these object stacks */
    /* Strategy: Call SaveObstack for all these stacks and save the pointer
     * to another obstack. Then save the obstack containing the pointers */

    /* Make obstack 'saved_stacks' empty. */
    obstack_next_free(&saved_stacks) = (char *)obstack_base(&saved_stacks);
    
    /* Extract an array of pointers to dynamically allocated obstacks */
    the_stacks = (ObstackP *)obstack_base(&stack_adresses);
    
    if (the_stacks)
    {
	ObstackP *stacks_end;
	
	savedata.stackscount = obstack_object_size(&stack_adresses) / sizeof(ObstackP);
	stacks_end = the_stacks + savedata.stackscount;

	/* For each of the obstacks */
	while (the_stacks != stacks_end)
	{
	    void *savedstack = SaveObstack(*the_stacks, (void *)0);
	    obstack_grow(&saved_stacks, &savedstack, sizeof(savedstack));
	    the_stacks++;
	}

        /* Save the dynamic array yielding one single handle */
	savedata.saved_stacks = SaveObstack(&saved_stacks, (void *)0);
    }
    else /* No dynamic obstacks exist. Mark that fact with a special value */
	savedata.saved_stacks = (void*)0;

    /* Mark current allocation level for the real obstacks */
    savedata.stacks_base = obstack_alloc(&stacks, 0);
    
    /* Now save the rest of the data */
    base = obstack_alloc(&space, 0);
    savedata.space = SaveObstack(&space, base);

    /* and the static information */
    savedata.state = SaveData(&state, sizeof(state));
    
    return SaveData(&savedata, sizeof(savedata));
}
    
void
#ifdef PROTO_OK
RestoreModuleEnvmod(void *base)
#else
RestoreModuleEnvmod(base) 
void *base;
#endif     
{
    ObstackP *the_stacks;

    /* This restores the savedata-structure */
    RestoreData(base);

    /* New obstacks may have been allocated in the meantime */
    the_stacks = (ObstackP *) obstack_base(&stack_adresses);
    if (the_stacks)
    {
	int stackscount_now = obstack_object_size(&stack_adresses) / sizeof(ObstackP);
	ObstackP *stacks_end = the_stacks + stackscount_now;

	the_stacks += savedata.stackscount;
	if (savedata.stackscount > stackscount_now) 
	    abort();

	/* For every newly appeard obstack, obstack_free will be called */
	while (the_stacks != stacks_end)
	{
	    obstack_free(*the_stacks, (void *)0);  /* 0-Pointer frees everything! */
	    the_stacks++;
	}

	/* Shorten the stacks-obstack so that this will not be done again. */
	the_stacks = (ObstackP *) obstack_base(&stack_adresses);
       	obstack_next_free(&stack_adresses) = (char *)(the_stacks + savedata.stackscount);
    }

    /* Now restore the previously existing stacks */
    /* the_stacks points to beginning of obstack-adress-array */

    if (savedata.stackscount > 0)
    {
	void **saved_stack;
	ObstackP *stacks_end = the_stacks + savedata.stackscount;
	
	/* Restores Data in saved_stacks */
	RestoreObstack(&saved_stacks, savedata.saved_stacks);
	
	saved_stack = (void **)obstack_base(&saved_stacks);
	while (the_stacks != stacks_end)
	{
	    RestoreObstack(*the_stacks, *saved_stack);
	    the_stacks++;
	    saved_stack++;
	}
    }

    /* Free Memory of stack control structures */
    obstack_free(&stacks, savedata.stacks_base);

    /* Now the rest of the data */
    RestoreObstack(&space, savedata.space);
    RestoreData(savedata.state);
}

#endif

    
