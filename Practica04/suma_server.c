/*
 * This is sample code generated by rpcgen.
 * These are only templates and you can use them
 * as a guideline for developing your own functions.
 */

#include "suma.h"

int *
suma_1_svc(dupla_int *argp, struct svc_req *rqstp)
{
	static int  result;

	result=argp->a+argp->b;
	printf("Se ha invocado remotamente el procedimiento\n");
	printf("Primer numero: %d\nSegundo numero: %d\n\n",argp->a,argp->b);
	printf("El resultado de la suma es: %d\n\n",result);

	return &result;
}