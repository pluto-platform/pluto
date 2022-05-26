

SRCS := $(wildcard src/main/**/*.scala)

listen-DE2-115:
	screen /dev/ttyUSB0 115200,cs8,-ixon,-ixoff,-istrip

listen-Basys3:
	screen /dev/ttyUSB1 115200,cs8,-ixon,-ixoff,-istrip

test-nix-pipeline:
	$(MAKE) -C tests
	sbt "testOnly cores.nix.TestPrograms"

TopCached.v: $(SRCS)
	sbt "runMain TopCachedEmitter"

quartus: TopCached.v
	$(MAKE) -C synthesis/quartus program