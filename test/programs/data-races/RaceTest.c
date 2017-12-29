int false_unsafe, global;
int true_unsafe;
int unsafe, false_unsafe2;

 __inline static int tryLock(int id___0) 
{ 
  int idx ;
  if (id___0 == 0) {
    return (0);
  }
  kernDispatchDisable();
  if (id___0 == idx) {
    return (1);
  } 
  kernDispatchEnable();
  return 0;
}

__inline static int get(int mutex ) 
{ 
  int rt, mtx, tmp___1 ;

  if (mutex == 0) {
    return (0);
  }
  mtx = tryLock(rt);
  if (mtx != 0) {
    return (mtx);
  } 
  tmp___1 = init(mutex);
  return (tmp___1);
}
 
__inline static int check(int code ) 
{ 
  int tmp;
  if (code == 27) {
    tmp = tryLock(tmp);
    if (tmp == 0) {
      return (28);
    }
  }
  return code;
}

__inline static int init(int mutex ) 
{ 
  int mtx, rt ;

  if (mutex == (unsigned int )(138 << 24)) {
    if (rt) {
      return (0);
    }
    kernDispatchDisable();
    if (mtx != 0) {
      return (mtx);
    } 
    kernDispatchEnable();
  }
  return (0);
}

int difficult_function() {
	int ret, param, mutex;
    ret = get(mutex);
    if (ret == 0) {
      return 28;
    }
restart: 
    false_unsafe = 0;
    true_unsafe = 0;
  
  kernDispatchEnable();
  ret = check(param); 
  if (ret == 27) {
    goto restart;
  }
  unsafe = 1;
  true_unsafe = 0;
}


int f(int i) {
	if (i >= 0) {
      intLock();
      false_unsafe2 = 1;
      intUnlock();
    } else {
      false_unsafe2 = 1;
    }
}

int g() {
	global = 1;
}

int main(int i) {
	difficult_function();
	g();
	f(i);
}

int ldv_main() {
	main(0);
}
