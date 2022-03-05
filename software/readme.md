
```shell
cargo objdump --target riscv32i-unknown-none-elf --release -- -d software
cargo objcopy --release -- -O binary boot_sect_simple.bin
```


# install

```shell
rustup target add riscv32i-unknown-none-elf
cargo install cargo-binutils
rustup component add llvm-tools-preview
```