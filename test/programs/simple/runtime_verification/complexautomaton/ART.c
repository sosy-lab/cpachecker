#include <stdio.h>
int __MONITOR_START_TRANSITION   = 0;
int __MONITOR_END_TRANSITION   = 0;
int __BLAST_error  ;
int __MONITOR_STATE_lockStatus   = 0;
void __initialize__(void) ;
extern int ( /* missing proto */  anti_op)() ;
int __return_157;
int __return_159;
int entry(void)
{
    int lastLock ;
    int i ;
    __MONITOR_STATE_lockStatus = 0;
    {
        anti_op();;
    }
    {
        anti_op();;
    }
    {
        anti_op();;
    }
    {
        anti_op();;
    }
    {
        anti_op();;
    }
    lastLock = 0;
    {
        anti_op();;
    }
    i = 1;
    label_85:; 
    if (!(i < 1000))
    {
         __return_157 = 1;
    }
    else if (i < 1000)
    {
        if (!(i - lastLock == 2))
        {
            {
                anti_op();;
            }
            label_120:; 
            i ++;
            if (!(i < 1000))
            {
                 __return_159 = 1;
            }
            else if (i < 1000)
            {
                if (!(i - lastLock == 2))
                {
                    assert(0); // target state
                }
                else if (i - lastLock == 2)
                {
                    if (!(i < 999))
                    {
                        assert(0); // target state
                    }
                    else if (i < 999)
                    {
                        lastLock += 2;
                        {
                            anti_op();;
                        }
                        i ++;
                        goto label_85;
                    }
                }
            }
        }
        else if (i - lastLock == 2)
        {
            {
                anti_op();;
            }
            goto label_120;
        }
    }
}
