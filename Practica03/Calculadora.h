/*
 * Please do not edit this file.
 * It was generated using rpcgen.
 */

#ifndef _CALCULADORA_H_RPCGEN
#define _CALCULADORA_H_RPCGEN

#include <rpc/rpc.h>


#ifdef __cplusplus
extern "C" {
#endif


struct valores {
	float a;
	float b;
	float opcion;
};
typedef struct valores valores;

#define CALCULADORA_PROG 0x31111111
#define CALCULADORA_VERS 1

#if defined(__STDC__) || defined(__cplusplus)
#define calculadora 1
extern  float * calculadora_1(valores *, CLIENT *);
extern  float * calculadora_1_svc(valores *, struct svc_req *);
extern int calculadora_prog_1_freeresult (SVCXPRT *, xdrproc_t, caddr_t);

#else /* K&R C */
#define calculadora 1
extern  float * calculadora_1();
extern  float * calculadora_1_svc();
extern int calculadora_prog_1_freeresult ();
#endif /* K&R C */

/* the xdr functions */

#if defined(__STDC__) || defined(__cplusplus)
extern  bool_t xdr_valores (XDR *, valores*);

#else /* K&R C */
extern bool_t xdr_valores ();

#endif /* K&R C */

#ifdef __cplusplus
}
#endif

#endif /* !_CALCULADORA_H_RPCGEN */
