/** Test writing float data to klb with compression */
#include <stdio.h>
#include <klb_Cwrapper.h>

#define FILENAME ("write_float_bz2.klb")
#define countof(e) (sizeof(e)/sizeof(*(e)))
#define CHECK(expr) do{int eee=(expr); if(eee!=0){printf("\nGot error code: %d\n\t%s\n",eee,#expr); goto Error;}}while(0)
static float frand();

static int write(void *data) {
    const uint32_t xyzct[KLB_DATA_DIMS]={4,4,4,4,1};
    const float32_t pixelSize[KLB_DATA_DIMS]={1.0f,1.0f,1.0f,1.0f,1.0f};
	const uint32_t blockSize[KLB_DATA_DIMS]={4,4,4,1,1};
	//return writeKLBstack(data,FILENAME,xyzct,FLOAT32_TYPE,-1,pixelSize, blockSize,NONE,0);
	return writeKLBstack(data,FILENAME,xyzct,FLOAT32_TYPE,-1,pixelSize, blockSize,ZLIB,0);
    //return writeKLBstack(data,FILENAME,xyzct,FLOAT32_TYPE,-1,pixelSize, blockSize,BZIP2,0);
}

int main(int argc,char* argv[]) {
    float src[4*4*4*4]={0},
          dst[countof(src)];
    for(int i=0;i<countof(src);++i)
        src[i]=frand();
    CHECK(write(src));
    {
        enum KLB_DATA_TYPE type;
        CHECK(readKLBstackInPlace(FILENAME,dst,&type,1));
    }
    for(int i=0;i<countof(src);++i) {
        float e=dst[i]-src[i];
        if(e*e>1e-3) {
            printf("Written value did not match read\n\tread: %f\n\twritten: %f\n\n",dst[i],src[i]);
            goto Error;
        }
    }
    return 0;
Error:
    return 1;
}


/* Rand */

static uint64_t myrand48_a = 0x5deece66dull;
static uint64_t myrand48_c = 0xbull;
static uint64_t myrand_state = 0x1234abcd330eull;

static double erand48(uint64_t *state)
{ uint64_t  temp;
  *state = ((*state) * myrand48_a + myrand48_c) & 0xffffffffffffull;
  temp = 0x3ff0000000000000ull | (*state << 4);
  return (*(double*)&temp - 1.0);
}
static float frand() {return (float) erand48(&myrand_state);}
