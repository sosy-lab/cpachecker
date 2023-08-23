
extern void reach_error();

int 
main()
{
    int a = 1;
    for (int i = 1; i <= 10; i++) {
        a *= 2; 
    } 

    if (a != 1024) { reach_error(); }
    
}