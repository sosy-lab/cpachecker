/*extern int __VERIFIER_nondet_int();

#define TRUE 0
#define FALSE 1

int isSorted(int a[], int len){
	for(int i = 0; i < len-1; i++) {
		if(a[i] > a[i+1]) {
			return FALSE;
		}	
	}
	return TRUE;
}

int main(){
	
	int a[] = {6,4,2};
	int len = 3;
	int i = 0;
	while(!isSorted(a,len)) {
		int buff = a[i];
		a[i] = a[i+1];
		a[i+1] = buff;
		i++;
		if (i == len-1) {
			i = 0;		
		}	
	}

	if (a[0] <= a[1] && a[1] <= a[2]) {
		goto EXIT;	
	} else {
		goto ERROR;	
	}


EXIT: return 0;
ERROR: return 1;
}*/

int main(){
	
	int a = 0;
	int b = 4;
	int c = 2;
	//char* message = "sample text";

	b = a+2;
	c = 1+b+a+c;
	c = b / (a + 6);
	
	if(c == a)
		goto ERROR;


EXIT: return 0;
ERROR: return 1;
}
