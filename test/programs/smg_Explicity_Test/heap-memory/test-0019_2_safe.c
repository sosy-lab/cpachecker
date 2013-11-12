#include <stdlib.h>

typedef struct {
    int *lo;
    int *hi;
} TData;

typedef struct {
    int *lo;
    int *hi;
    TData data;
} TData2;


static void alloc_data(TData2 *pdata)
{
    pdata->lo = (int *)malloc(sizeof(int));
    pdata->hi = (int *)malloc(sizeof(int));

    pdata->data.lo = (int *)malloc(sizeof(int));
    pdata->data.hi = (int *)malloc(sizeof(int));
	

	
    __VERIFIER_BUILTIN_PLOT("C1");	
    
    *(pdata->lo) = 4;
    *(pdata->hi) = 8;    

    *(pdata->data.lo) = 5;
    *(pdata->data.hi) = 10;
    __VERIFIER_BUILTIN_PLOT("C2");


}

static void free_data(TData2 data)
{
    int *lo = data.lo;
    int *hi = data.hi;

    __VERIFIER_BUILTIN_PLOT("C5");


    if (*lo > *hi)
        return;

    free(lo);
    free(hi);
}

static void free_data2(TData data)
{
    int *lo = data.lo;
    int *hi = data.hi;

    __VERIFIER_BUILTIN_PLOT("C6");


    if (*lo > *hi)
        return;

    free(lo);
    free(hi);
}


int main() {     
    TData2 data;
    alloc_data(&data);
    __VERIFIER_BUILTIN_PLOT("C4");

    free_data(data);
    free_data2(data.data);
    return 0;
}
