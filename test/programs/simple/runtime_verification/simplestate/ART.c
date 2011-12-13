#include <stdio.h>
int __MONITOR_START_TRANSITION   = 0;
int __MONITOR_END_TRANSITION   = 0;
int k  ;
extern int ( /* missing proto */  nondet_int)() ;
int __BLAST_error  ;
void __initialize__(void) ;
extern int k ;
extern int ( /* missing proto */  anti_op)() ;
extern int ( /* missing proto */  nondet_int)() ;
int __return_2546;
int __return_2548;
int __return_2550;
int __return_2552;
int __return_2396;
int __return_2417;
int __return_2544;
int entry(void)
{
    int i ;
    int j ;
    int tmp ;
    k = 0;
    tmp = nondet_int();
    if (!(tmp))
    {
        j = 0;
        if (!(j < 100))
        {
             __return_2546 = 1;
            goto label_2544;
        }
        else if (j < 100)
        {
            k ++;
            j ++;
            if (!(j < 100))
            {
                 __return_2548 = 1;
                goto label_2544;
            }
            else if (j < 100)
            {
                k ++;
                j ++;
                if (!(j < 100))
                {
                     __return_2550 = 1;
                    goto label_2544;
                }
                else if (j < 100)
                {
                    k ++;
                    j ++;
                    label_2309:; 
                    if (!(j < 100))
                    {
                         __return_2552 = 1;
                        goto label_2544;
                    }
                    else if (j < 100)
                    {
                        k ++;
                        __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
                        {
                            int tmp ;
                            int tmp___0 ;
                            {
                                int result ;
                                int mybool ;
                                int tmp ;
                                result = k;
                                tmp = nondet_int();
                                mybool = tmp;
                                if (!(mybool))
                                {
                                    result -= 13;
                                    label_2332:; 
                                    if (!(! mybool))
                                    {
                                        result -= 13;
                                        label_2339:; 
                                        if (!(mybool))
                                        {
                                            result -= 13;
                                            label_2346:; 
                                            if (!(! mybool))
                                            {
                                                result -= 13;
                                                label_2353:; 
                                                if (!(mybool))
                                                {
                                                    result -= 13;
                                                    label_2360:; 
                                                    if (!(! mybool))
                                                    {
                                                        result -= 13;
                                                        label_2367:; 
                                                        if (!(mybool))
                                                        {
                                                            result -= 13;
                                                            label_2374:; 
                                                            if (!(! mybool))
                                                            {
                                                                result -= 13;
                                                                label_2381:; 
                                                                if (!(mybool))
                                                                {
                                                                    result -= 13;
                                                                    label_2388:; 
                                                                    if (!(! mybool))
                                                                    {
                                                                        result -= 13;
                                                                        label_2395:; 
                                                                         __return_2396 = result > 0;
                                                                    }
                                                                    else if (! mybool)
                                                                    {
                                                                        result += 26;
                                                                        goto label_2395;
                                                                    }
                                                                    tmp = __return_2396;
                                                                    if (!(tmp))
                                                                    {
                                                                        tmp___0 = 1;
                                                                        label_2416:; 
                                                                         __return_2417 = tmp___0;
                                                                    }
                                                                    else if (tmp)
                                                                    {
                                                                        if (!(k < 0))
                                                                        {
                                                                            if (!(k > 100))
                                                                            {
                                                                                tmp___0 = 1;
                                                                                label_2410:; 
                                                                                label_2412:; 
                                                                                goto label_2416;
                                                                            }
                                                                            else if (k > 100)
                                                                            {
                                                                                tmp___0 = 0;
                                                                                goto label_2410;
                                                                            }
                                                                        }
                                                                        else if (k < 0)
                                                                        {
                                                                            tmp___0 = 0;
                                                                            goto label_2412;
                                                                        }
                                                                    }
                                                                    tmp = __return_2417;
                                                                    if (!(! tmp))
                                                                    {
                                                                        __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                        j ++;
                                                                        goto label_2309;
                                                                    }
                                                                    else if (! tmp)
                                                                    {
                                                                        {
                                                                            assert(0); // target state
                                                                        }
                                                                    }
                                                                }
                                                                else if (mybool)
                                                                {
                                                                    result += 26;
                                                                    goto label_2388;
                                                                }
                                                            }
                                                            else if (! mybool)
                                                            {
                                                                result += 26;
                                                                goto label_2381;
                                                            }
                                                        }
                                                        else if (mybool)
                                                        {
                                                            result += 26;
                                                            goto label_2374;
                                                        }
                                                    }
                                                    else if (! mybool)
                                                    {
                                                        result += 26;
                                                        goto label_2367;
                                                    }
                                                }
                                                else if (mybool)
                                                {
                                                    result += 26;
                                                    goto label_2360;
                                                }
                                            }
                                            else if (! mybool)
                                            {
                                                result += 26;
                                                goto label_2353;
                                            }
                                        }
                                        else if (mybool)
                                        {
                                            result += 26;
                                            goto label_2346;
                                        }
                                    }
                                    else if (! mybool)
                                    {
                                        result += 26;
                                        goto label_2339;
                                    }
                                }
                                else if (mybool)
                                {
                                    result += 26;
                                    goto label_2332;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    else if (tmp)
    {
        i = 0;
        label_361:; 
        if (!(i < 1000000))
        {
             __return_2544 = 1;
            label_2544:; 
        }
        else if (i < 1000000)
        {
            anti_op();;
            i ++;
            goto label_361;
        }
    }
}
