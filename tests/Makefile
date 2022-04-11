

ASRCS := $(wildcard *.s)
CSRCS := $(wildcard *.c)
AOBJ := $(ASRCS:%.s=%.out)
COBJ := $(CSRCS:%.c=%.out)
BINS := $(COBJ:%.out=%.bin) $(AOBJ:%.out=%.bin)
RES := $(BINS:%.bin=%.res)

SIMULATOR ?= ~/devel/simson/target/release/simson

all: ${AOBJ} ${COBJ} ${BINS} ${RES}

hello:
	@echo ${ASRCS}
	@echo ${CSRCS}
	@echo ${AOBJ}
	@echo ${COBJ}
	@echo ${BINS}
	@echo ${RES}

obj: ${OBJ}

bin: ${BINS}

res: ${RES}

%.out: %.c
	riscv64-unknown-elf-gcc -ffreestanding -nostartfiles -nostdlib -nodefaultlibs -Wl,-T,linker.ld $< -o $@ -march=rv32i -mabi=ilp32
	riscv64-unknown-elf-objdump $@ -d

%.out: %.s
	riscv64-unknown-elf-as $< -o $@ -march=rv32i -mabi=ilp32
	riscv64-unknown-elf-objdump $@ -d

%.bin: %.out
	riscv64-unknown-elf-objcopy -O binary $< $@

%.res: %.bin
	$(SIMULATOR) -d $@ $<

clean:
	@rm *.bin *.res *.out

#riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 -ffreestanding -nostartfiles -nostdlib -nodefaultlibs -Wl,-T,linker.ld recursive.c -o test.out