

SRCS := $(wildcard *.s)
OBJ := $(SRCS:%.s=%.out)
BINS := $(SRCS:%.s=%.bin)

all: ${BINS}

%.out: %.s
	riscv64-unknown-elf-as $< -o $@ -march=rv32i -mabi=ilp32
	riscv64-unknown-elf-objdump $@ -d

%.bin: %.out
	riscv64-unknown-elf-objcopy -O binary $< $@