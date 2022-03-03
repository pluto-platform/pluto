
```shell
cargo objdump --target riscv32i-unknown-none-elf -- -d software
```


# install

```shell
rustup target add riscv32i-unknown-none-elf
cargo install cargo-binutils
rustup component add llvm-tools-preview
```