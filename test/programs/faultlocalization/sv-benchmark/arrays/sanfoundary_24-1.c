extern void abort(void); 
void reach_error(){}
extern int __VERIFIER_nondet_int(void);
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }

/*
 * Adapted from http://www.sanfoundry.com/c-programming-examples-arrays/
 * C Program to Print the Number of Odd & Even Numbers in an Array
 */
#define SIZE 100000

void printEven( int i ) {
  __VERIFIER_assert(  ( i % 2 ) == 0  );
  // printf( "%d" , i );
}

void printOdd( int i ) {
  __VERIFIER_assert(  ( i % 2 ) != 0  );
  // printf( "%d" , i );
}

int main()
{
    int array[SIZE];
    int i;
    int num = __VERIFIER_nondet_int();

		for(i = 0; i < num; i++) 
		{
		  array[i] = __VERIFIER_nondet_int();
		}
		
    //printf("Even numbers in the array are - ");
    for (i = 0; i < num; i++)
    {
        if (array[i] % 2 == 1)// error in conditional expression : array[i] % 2 == 1 instead of array[i] % 2 == 0
        {
            printEven( array[i] );
        }
    }
    //printf("\n Odd numbers in the array are -");
    for (i = 0; i < num; i++)
    {
        if (array[i] % 2 != 0)
        {
            printOdd( array[i] );
        }
    }
  return 0;
}