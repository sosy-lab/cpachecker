//SPDX-FileCopyrightText: Schindar Ali
//SPDX-License-Identifier: Apache-2.0
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();

void __VERIFIER_assert(int cond)
{
  if (!(cond))
  {
    ERROR: __VERIFIER_error();
  }
  return;
}
/*function declaration */
int worngMid(int x, int y, int z);
int correctMid(int x, int y, int z);
int main()
{

  /*local variable definition */
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();
  int z = __VERIFIER_nondet_int();
  int worngResult;
  int correctResult;

  /*calling a function to get mid value */
  worngResult = worngMid(x,y,z);
  correctResult = correctMid(x,y,z);
  __VERIFIER_assert(worngResult == correctResult);

  return 0;
}

/*function returning the mid between three numbers */
int worngMid(int x, int y, int z)
{

  int m;
  m = z;
  if (y < z)
  {
    if (x > y)  // bug this should be x < y
    {
      m = y;
    }
    else if (x < z)
    {
      m = x; 
    }
  }
  else
  {
    if (x > y)
    {
      m = y;
    }
    else if (x > z)
    {
      m = x;
    }
  }
  return m;
}


// check for the correctniss


int correctMid(int x, int y, int z)
{
int m;
  m = z;
  if (y < z)
  {
    if (x < y)
    {
      m = y;
    }
    else if (x < z)
    {
      m = x;  
    }
  }
  else
  {
    if (x > y)
    {
      m = y;
    }
    else if (x > z)
    {
      m = x;
    }
  }
  return m;
}







