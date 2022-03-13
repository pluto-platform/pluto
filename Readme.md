


# assembly

```shell
riscv64-unknown-elf-as blinkTest.s -o blinkTest -march=rv32i -mabi=ilp32
riscv64-unknown-elf-objcopy -O binary blinkTest blinkTest.bin
```

# performance counters
- `minstret`: send incr signal from wb
- `mcycle`: always plus one
- `mhpmcounterx`: how many? others are ro 0

# Interrupts
- order: MEI > MSI > MTI

# CSR

1. 0xF11 ro `mvendorid` vendor id: 0
2. 0xF12 ro `marchid` architecture id: 0
3. 0xF13 ro `mimpid` implementation id: 0
4. 0xF14 ro `mhartid` hardware thread id: 0 (if multicore set to constant)
5. 0xF15 ro `mconfigptr` pointer to configuration data structure: 0
6. 0x300 rw `mstatus` machine status register:
  - [3] `MIE` machine interrupt enable (global)
7. 0x301 rw `misa` ISA and extenstions:
8. 0x304 rw `mie` machine interrupt-enable register:
  - [15:0] standard interrupt enable
    - [3] `MSIE` machine software interrupt
    - [7] `MTIE` machine timer interrupt
    - [11] `MEIE` machine external interrupt
  - [31:16] custom interrupt enable
9.  0x305 rw `mtvec` machine trap-handler base address:
  - [1:0] rw `MODE` 0=direct 1=vectored (base+4*cause)
  - [31:2] rw `BASE` is [31:2] handler address
  - note: coarser alignment for base allows for base+4*cause to be (base | 4*cause)
10. 0x306 rw `mcounteren` machine counter enable:
  - [0] cycle counter
  - [1] timer
  - [2] retired instructions
  - [31:3] hpm
11. 0x??? rw `mcountinhibit` stop counting:
  - [0] cycle counter
  - [1] timer
  - [2] retired instructions
  - [31:3] hpm
11. 0x310 rw `mstatush` additional machine status register
12. 0x340 rw `mscratch` scratch register for machine trap handlers
13. 0x341 rw `mepc` machine exception program counter:
  - can drop lower two bits
14. 0x342 rw `mcause` machine trap cause:
  - [30:0] exception code
  - [31] interrupt?
15. 0x343 rw `mtval` machine bad address or instruction:
16. 0x344 rw `mip` machine interrupt pending
  - [15:0] standard interrupt pending
    - [3] `MSIP` machine software interrupt
    - [7] `MTIP` machine timer interrupt
    - [11] `MEIP` machine external interrupt
  - [31:16] custom interrupt pending
17. 0x34A rw `mtinst` machine trap instruction (transformed)??