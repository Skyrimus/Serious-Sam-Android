#include <Engine/StdH.h>
#include <Engine/Base/Console.h>
#include "DeterministicFP.h"
#include <stdint.h>

#if defined(__aarch64__)
static inline uint64_t read_fpcr() {
  uint64_t r; __asm__ volatile("mrs %0, fpcr" : "=r"(r)); return r;
}
static inline void write_fpcr(uint64_t v) {
  __asm__ volatile("msr fpcr, %0" :: "r"(v));
  __asm__ volatile("isb");
}
#elif defined(__arm__) && !defined(__aarch64__)
static inline uint32_t read_fpscr() {
  uint32_t r; __asm__ volatile("vmrs %0, fpscr" : "=r"(r)); return r;
}
static inline void write_fpscr(uint32_t v) {
  __asm__ volatile("vmsr fpscr, %0" :: "r"(v));
  __asm__ volatile("isb");
}
#endif

void SetDeterministicFP() {
#if defined(__aarch64__)
    uint64_t fp = read_fpcr();
  fp |= (1ull << 24);
  fp |= (1ull << 25);
  fp &= ~(3ull << 22);
  write_fpcr(fp);
  CPrintF("[DeterministicFP] AArch64 FPCR=0x%llx (FZ=1, DN=1, RN)\n", (unsigned long long)fp);
#elif defined(__arm__) && !defined(__aarch64__)
    uint32_t fp = read_fpscr();
  fp |= (1u << 24);
  fp |= (1u << 25);
  fp &= ~(3u << 22);
  write_fpscr(fp);
  CPrintF("[DeterministicFP] ARMv7 FPSCR=0x%08x (FZ=1, DN=1, RN)\n", fp);
#else
    CPrintF("[DeterministicFP] Non-ARM platform: using toolchain flags only\n");
#endif
}
