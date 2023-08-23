// FALSE

extern void reach_error(); 


int main()
{ 
	int a = 3;
	a = a + 1;
	if ( a == 4 ) { reach_error(); } 
}
