

- block all csr instructions when one is in the pipeline
  - use `isCsrAccess`

- decode should produce alufun and memaccesswidth again since alufun should always be add for mem stuff
- alufun has to be zero for jal (jalr has funct3 0 and should be fine)
- auipc needs alufun 0
- lui needs passthrough -> we have got pc+4 in decode (need channel from fetch) -> pass it


# assembly

```shell
riscv64-unknown-elf-as blinkTest.s -o blinkTest -march=rv32i -mabi=ilp32
riscv64-unknown-elf-objcopy -O binary blinkTest blinkTest.bin
```

# CSR

1. 0xF11 ro `mvendorid` vendor id: 0
2. 0xF12 ro `marchid` architecture id: 0
3. 0xF13 ro `mimpid` implementation id: 0
4. 0xF14 ro `mhartid` hardware thread id: 0 (if multicore set to constant)
5. 0xF15 ro `mconfigptr` pointer to configuration data structure: 0
6. 0x300 rw `mstatus` machine status register:
  - bit 3 `MIE` machine interrupt enable (global)
7. 0x301 rw `misa` ISA and extenstions:
8. 0x304 rw `mie` machine interrupt-enable register:
9.  0x305 rw `mtvec` machine trap-handler base address:
10. 0x306 rw `mcounteren` machine counter enable:
11. 0x310 rw `mstatush` additional machine status register
12. 0x340 rw `mscratch` scratch register for machine trap handlers
13. 0x341 rw `mepc` machine exception program counter:
14. 0x342 rw `mcause` machine trap cause
15. 0x343 rw `mtval` machine bad address or instruction:
16. 0x344 rw `mip` machine interrupt pending
17. 0x34A rw `mtinst` machine trap instruction (transformed)??