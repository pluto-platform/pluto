

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