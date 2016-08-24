extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>

typedef struct {
    int *lo;
    int *hi;
} TData;

static void alloc_data(TData *pdata)
{
    pdata->lo = (int *) malloc(sizeof(int));
    pdata->hi = (int *) malloc(sizeof(int));
    
    *(pdata->lo) = 4;
    *(pdata->hi) = 8;
}

static void free_data(TData *data)
{
    int *lo = data->lo;
    int *hi = data->hi;

    if (*lo >= *hi) {
        free(lo);
        free(hi);
    }

    data->lo = NULL;
    data->hi = NULL;
}

int main() {
    TData data;
    alloc_data(&data);
    free_data(&data);
    return 0;
}
