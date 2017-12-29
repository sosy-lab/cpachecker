/* There is a bug with not ordering assignement/assumbption in the same line;
 * The read access in 11th line is not reachable and during refinement the analysis tried to remove it twice
 * 
 * Important! There is no unsafe if we do not consider the single write usage in 15th line as an unsafe
 */
int unsafe;

int f(b) 
{ 
    if (b == 0) {
      b = unsafe == 0 ? unsafe + 1 : unsafe - 1;
    }
}
int ldv_main() {
    unsafe = 0;
    f(1);
}

