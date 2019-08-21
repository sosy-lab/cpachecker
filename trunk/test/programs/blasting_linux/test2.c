/* simplified version of pci.c for the I2O User-After-Free Error */
#include "test2.h"

i2o_controller *i2o_iop_alloc(void) {
  i2o_controller *c;
  c = malloc(sizeof(struct i2o_controller));
  return (c);
}

void i2o_iop_free(i2o_controller *c) {
  free(c);
}

void put_device(int i) { }

int main(void) {
  i2o_controller *c;
  c = i2o_iop_alloc();
  i2o_iop_free(c);
  put_device(c->device.parent);
  return (0);
}
