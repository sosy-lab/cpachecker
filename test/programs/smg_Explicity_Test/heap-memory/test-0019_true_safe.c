#include <stdlib.h>

typedef struct {
    int *lo;
    int *hi;
} TData;

static void alloc_data(TData *pdata)
{
    pdata->lo = (int *)malloc(sizeof(int));
    pdata->hi = (int *)malloc(sizeof(int));
	
    __VERIFIER_BUILTIN_PLOT("C1");	
    
    *(pdata->lo) = 4;
    *(pdata->hi) = 8;    

    __VERIFIER_BUILTIN_PLOT("C2");


}

static void free_data(TData data)
{
    int *lo = data.lo;
    int *hi = data.hi;

    __VERIFIER_BUILTIN_PLOT("C3");


    if (*lo > *hi)
        return;

    free(lo);
    free(hi);
}

int main() {     
    TData data;
    alloc_data(&data);
    __VERIFIER_BUILTIN_PLOT("C4");

    free_data(data);
    return 0;
}
