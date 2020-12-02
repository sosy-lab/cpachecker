// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct mutex;

extern void mutex_lock(struct mutex *lock);
extern void mutex_unlock(struct mutex *lock);
void check_final_state(void);

void main(void)
{
	struct mutex *mutex_1;

	mutex_lock(&mutex_1);
	mutex_lock(&mutex_1); // error - double lock of the same mutex
	mutex_unlock(&mutex_1);
	mutex_unlock(&mutex_1);

	check_final_state();
}

