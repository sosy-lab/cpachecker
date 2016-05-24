
// Copyright (c) 2015 Michael Tautschnig <michael.tautschnig@qmul.ac.uk>
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

typedef unsigned int size_t;
extern void *malloc (size_t __size) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) ;
extern int __VERIFIER_nondet_int();

int flag=0;

int is_relaxed_prefix(
  int pat[], unsigned long pat_length,
  int a[], unsigned long a_length)
{
  if(pat_length>a_length+1)
    return 0;

  int shift=0;

  for(int i=0; i<pat_length && i<a_length; i++)
  {
    flag = i;
    flag = i-shift;
    if(pat[i]!=a[i-shift])
    {
      if(shift==0)
        shift=1;
      else
        return 0;
    }
  }

  if(pat_length>a_length && shift==1)
    return 0;
  else
    return 1;
}

int main()
{
  int SIZE = 6;
  unsigned long pat_len = SIZE, a_len = SIZE;

  int *pat=malloc(sizeof(int)*pat_len);
  int *a=malloc(sizeof(int)*a_len);

  is_relaxed_prefix(pat, pat_len, a, a_len);

  return 0;
}

