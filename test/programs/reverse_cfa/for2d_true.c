extern void reach_error();

int 
main()
{
    int a = 1;
    for (int i = 1; i <= 1; i++) {
        for (int j = 1; j <= 2; j++) {
            for (int k = 1; k <= 2; k++) {
                a *= 2;
            }
           
        }
           
    } 

    if (a != 16) { reach_error(); }
    
}