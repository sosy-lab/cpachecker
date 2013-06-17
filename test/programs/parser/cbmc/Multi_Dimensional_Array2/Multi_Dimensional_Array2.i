# 1 "Multi_Dimensional_Array2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Multi_Dimensional_Array2/main.c"

unsigned int nondet_uint();
int main(){
    int array[3][3]={{0,0,0},{1,1,1},{2,2,2}};

    unsigned int a, b;
    a = nondet_uint();
    b = nondet_uint();
    __CPROVER_assume (a < 3 && b < 3);
    array[a][a] = array[b][b];
    array[a][a] = array[b][b];
    assert(array[a][a] >= 0);
}
