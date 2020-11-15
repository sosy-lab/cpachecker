// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned gfp_t;
typedef unsigned int size_t;
struct usb_device;
struct usb_interface;

void free(void *);
static void *kmalloc(size_t size, gfp_t flags);
void *vmalloc(unsigned long size);
void kfree(const void *objp) {
	free(objp);
}

extern void usb_lock_device(struct usb_device *udev);
extern void usb_unlock_device(struct usb_device *udev);
int usb_trylock_device(struct usb_device *udev) {
	int res;
	return res;
}
extern int usb_lock_device_for_reset(struct usb_device *udev, const struct usb_interface *iface);

static void memory_allocation(gfp_t flags)
{
	size_t size;
	void *mem = kmalloc(size, flags);
	kfree(mem);
}

static void memory_allocation_nonatomic(void)
{
	int size;
	void *mem = vmalloc(size);
	kfree(mem);
}

void main(void)
{
	struct usb_device *udev;
	if (usb_trylock_device(udev)) {
		memory_allocation_nonatomic();
		usb_unlock_device(udev);
	}
}

