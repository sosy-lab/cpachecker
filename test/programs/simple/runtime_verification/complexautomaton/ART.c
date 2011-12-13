#include <stdio.h>
int __MONITOR_START_TRANSITION   = 0;
int __MONITOR_END_TRANSITION   = 0;
int __BLAST_error  ;
int __MONITOR_STATE_lockStatus   = 0;
void __initialize__(void) ;
extern void init() ;
extern void lock() ;
extern void unlock() ;
int __return_285;
int __return_287;
int __return_289;
int __return_291;
int entry(void)
{
    int lastLock ;
    int i ;
    __MONITOR_STATE_lockStatus = 0;
    init();;
    lock();;
    unlock();;
    lock();;
    unlock();;
    lastLock = 0;
    lock();;
    i = 1;
    if (!(i < 1000))
    {
         __return_285 = 1;
        label_285:; 
    }
    else if (i < 1000)
    {
        if (!(i - lastLock == 2))
        {
            unlock();;
            label_197:; 
            i ++;
            if (!(i < 1000))
            {
                 __return_287 = 1;
                label_287:; 
            }
            else if (i < 1000)
            {
                lastLock += 2;
                lock();;
                i ++;
                label_226:; 
                if (!(i < 1000))
                {
                     __return_289 = 1;
                    goto label_285;
                }
                else if (i < 1000)
                {
                    if (!(i - lastLock == 2))
                    {
                        unlock();;
                        label_253:; 
                        i ++;
                        if (!(i < 1000))
                        {
                             __return_291 = 1;
                            goto label_287;
                        }
                        else if (i < 1000)
                        {
                            if (!(i < 999))
                            {
                                HALT275: goto HALT275; // target state
                            }
                            else if (i < 999)
                            {
                                lastLock += 2;
                                lock();;
                                i ++;
                                goto label_226;
                            }
                        }
                    }
                    else if (i - lastLock == 2)
                    {
                        unlock();;
                        goto label_253;
                    }
                }
            }
        }
        else if (i - lastLock == 2)
        {
            unlock();;
            goto label_197;
        }
    }
}
