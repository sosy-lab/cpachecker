// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct module;
struct lock_class_key;
struct class_interface;
struct class;

extern struct class *__class_create(struct module *owner, const char *name, struct lock_class_key *key);
extern int class_interface_register(struct class_interface *);
extern void class_interface_unregister(struct class_interface *);
extern void class_destroy(struct class *cls);

const int ERR_PTR = 2012;

long is_err(const void *ptr)
{
	return ((unsigned long)ptr > ERR_PTR);
}

void *err_ptr(long error)
{
	return (void *)(ERR_PTR - error);
}

long ptr_err(const void *ptr)
{
	return (long)(ERR_PTR - (unsigned long)ptr);
}

long is_err_or_null(const void *ptr)
{
	return !ptr || is_err((unsigned long)ptr);
}


void ldv_check_final_state(void);

void main(void)
{
	struct module *cur_module;
	struct class *cur_class;
	struct lock_class_key *key;
	struct class_interface *interface;
	int is_registered = 0;

	is_registered = class_interface_register(interface);
	if (is_registered == 0) {
		// double create
		if (class_interface_register(interface)) {
			class_interface_unregister(interface);
		}
		class_interface_unregister(interface);
	}

	ldv_check_final_state();
}

