#include <stdlib.h>
#include <assert.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;

int globalState = 0;
ssize_t l_read(int,char*,size_t);
int l_open(char*,int);

int
main(int argc, char* argv[]) {
	int file = l_open("unknown",O_RDONLY);
	void* cbuf = (void*) malloc(sizeof(char)*100);
	int a = l_read(file,cbuf,99);
	return 0;
}

ssize_t l_read(int fd, char* cbuf, size_t count) {
	assert(globalState == 1);
	return read(fd,cbuf,count);
}

int l_open(char* file, int flags) {
	int fd = open(file,flags);
	if(fd>0) globalState = 1;
	return fd;
}

