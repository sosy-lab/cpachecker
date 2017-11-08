/**************************************************************************/
/*                                                                        */
/*  This file is part of Frama-C.                                         */
/*                                                                        */
/*  Copyright (C) 2007-2016                                               */
/*    CEA (Commissariat à l'énergie atomique et aux énergies              */
/*         alternatives)                                                  */
/*                                                                        */
/*  you can redistribute it and/or modify it under the terms of the GNU   */
/*  Lesser General Public License as published by the Free Software       */
/*  Foundation, version 2.1.                                              */
/*                                                                        */
/*  It is distributed in the hope that it will be useful,                 */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of        */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         */
/*  GNU Lesser General Public License for more details.                   */
/*                                                                        */
/*  See the GNU Lesser General Public License version 2.1                 */
/*  for more details (enclosed in the file licenses/LGPLv2.1).            */
/*                                                                        */
/**************************************************************************/

// Functions used internally by the normalization phase.
// This file is systematically included by Frama-C's kernel. 
// FC's code normalization can use some of the functions declared here.
// If you add something here, be sure to use the FC_BUILTIN attribute, 
// that will ensure that the builtin is printed iff it is actually used
// in the normalized code.

/*@ requires \valid(dest+(0..n-1));
    assigns dest[0..n-1] \from \nothing;
    ensures \forall integer i; 0<= i < n ==> dest[i] == 0;
 */
void Frama_C_bzero(unsigned char* dest, unsigned long n)
      __attribute__((FC_BUILTIN)) ;

/*@ requires \valid(dest+(0..(size*n-1)));
    requires n >= 1;
    assigns dest[size..(size*n -1)] \from dest[0..size-1];
    ensures \forall integer i,j; 0<=i<size && 1<=j<n ==>
        dest[i+j*size] == dest[i];
*/
void Frama_C_copy_block(unsigned char* dest,
                          unsigned long size, unsigned long n)
  __attribute__((FC_BUILTIN)) ;
