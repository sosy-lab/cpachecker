#include <stdio.h>
#include <stdlib.h>

typedef struct device
{
 int parent;
} device;

typedef struct i2o_controller
{
 struct device device;
} i2o_controller;

i2o_controller *i2o_iop_alloc(void);
void i2o_iop_free(i2o_controller *c);
void put_device(int i);
