/* $Id: attrpredef.h,v 4.7 1997/09/18 12:23:49 mjung Exp $ */
/* (C) Copyright 1997 University of Paderborn */

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

#define ADD(lop,rop) ( (lop) + (rop) )
#define SUB(lop,rop) ( (lop) - (rop) )
#define MUL(lop,rop) ( (lop) * (rop) )
#define DIV(lop,rop) ( (lop) / (rop) )
#define MOD(lop,rop) ( (lop) % (rop) )

#define NEG(op) (-op)
#define NOT(op) (!op)

#define AND(lop,rop) ( (lop) && (rop) )
#define OR(lop,rop) ( (lop) || (rop) )

#define BITAND(lop,rop) ( (lop) & (rop) )
#define BITOR(lop,rop) ( (lop) | (rop) )
#define BITXOR(lop,rop) ( (lop) ^ (rop) )

#define GT(lop,rop) ( (lop) > (rop) )
#define LT(lop,rop) ( (lop) < (rop) )
#define EQ(lop,rop) ( (lop) == (rop) )
#define NE(lop,rop) ( (lop) != (rop) )
#define GE(lop,rop) ( (lop) >= (rop) )
#define LE(lop,rop) ( (lop) <= (rop) )

#define CAST(tp,ex) ((tp) (ex))
#define SELECT(str,fld) ((str).fld)
#define PTRSELECT(str,fld) ((str)->fld)
#define INDEX(arr,indx) ((arr)[indx])

#define ZERO() 0
#define ONE()  1
#define ARGTOONE(x) 1
 
#define DEP(x,y)	(x)
#define VOIDEN(a)	((void)a)
#define IDENTICAL(x)	(x)


