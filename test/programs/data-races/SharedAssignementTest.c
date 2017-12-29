int global; 

int g() {
	int **p;
    int *s;
    int t;
    
    s = &global;
    p = &s;
    
    //unsafe
    **p = 1;
    
    //Not an unsafe
    *p = &t;
    
    //Not an unsafe
    **p = 1;
}

int ldv_main() {
	g();
}

