extern int __VERIFIER_nondet_int();

void bubblesort(int *array, int length)
{
   int i, j,tmp;

   for (i = 1; i < length ; i++) 
   {
      for (j = 0; j < length - i ; j++) 
      {
          if (array[j] > array[j+1]) 
          {
              tmp = array[j];
              array[j] = array[j+1];
              array[j+1] = tmp;
          }
      }
   }
}

int isSorted(int a[], int len){
	for(int i = 0; i < len-1; i++;){
		if (a[i] > a[i+1]) {
			return 0;		
		}
	}	
	return 1;
}

int main(){
	int a[] = {__VERIFIER_nondet_int(), __VERIFIER_nondet_int(), __VERIFIER_nondet_int()};
	bubblesort(a, 3);
	if(!isSorted(a,3)){
		goto ERROR;
	}
EXIT: return 0;
ERROR: return 1;
}
