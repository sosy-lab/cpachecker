/*
 ============================================================================
 *  Author      : Vadim Mutilin
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 ============================================================================
*/
#include "header.h"

//pass a field to register(store) the whole object
//field is stored as global variable

void __VERIFIER_error(void);

void ldv_assert(int b) {
	if(!b) //__VERIFIER_error();
		ERROR: goto ERROR;
}

struct A20 {
	void *private_data;
	struct ldv_device dev;
};

struct ldv_device *saved_device_20;

void ldv_register_device(struct ldv_device *dev) {
	saved_device_20 = dev;
}

struct ldv_device *ldv_get_device() {
	return saved_device_20;
}

void ldv_deregister_device(void) {
	saved_device_20 = 0;
}

void alloc_20(void) {
	struct A20 *p = (struct A20*)ldv_zalloc(sizeof(struct A20));
	if(p) {
		ldv_register_device(&p->dev);
	}
}

void free_20(void) {
	struct ldv_device *dev = ldv_get_device();
	if(dev) {
		struct A20 *p = ldv_container_of(dev, struct A20, dev);
		ldv_assert(p->private_data!=0);
		free(p);
	}
}

void entry_point(void) {
	alloc_20();
	free_20();
	saved_device_20 = 0;
}

void main(void) {
     entry_point();
}
