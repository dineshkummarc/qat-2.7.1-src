ROOT=$(shell pwd)
WORKSHOP=/opt/SUNWspro
CXX=$(WORKSHOP)/bin/CC
CCC=$(WORKSHOP)/bin/CC
CC=$(WORKSHOP)/bin/cc

UNZIP_VERSION=unzip542
AGENT_COMPILE_FLAGS=-c -DMULTI_THREADED -D_REENTRANT -D_POSIX_PTHREAD_SEMANTICS -DDLL -I$(ROOT) -I$(ROOT)/$(UNZIP_VERSION)
AGENT_LINK_FLAGS=-L. -lstdc++ -lsocket -lnsl -lpthread -lc
# FOR G++ : AGENT_COMPILE_FLAGS=-Wall -c -DMULTI_THREADED -D_REENTRANT -D_POSIX_PTHREAD_SEMANTICS -DDLL -I$(ROOT) -I$(ROOT)/$(UNZIP_VERSION)
# FOR G++ : AGENT_LINK_FLAGS=-L. -lstdc++ -lsocket -lnsl -lpthread -lc

CXXFLAGS +=$(AGENT_COMPILE_FLAGS)
CFLAGS   +=$(AGENT_COMPILE_FLAGS)
LDFLAGS  +=$(AGENT_LINK_FLAGS)

AGENT_OBJS =  \
	Utils.o \
	Common.o \
	ConsoleServer.o \
	DiscoveryResponder.o \
	TestObject.o \
	ExecProcess.o \
	AgentThread.o \
	agent.o

ZIP_OBJS = \
	$(UNZIP_VERSION)/unzip.o \
	$(UNZIP_VERSION)/crc32.o \
	$(UNZIP_VERSION)/crctab.o \
	$(UNZIP_VERSION)/crypt.o \
	$(UNZIP_VERSION)/envargs.o \
	$(UNZIP_VERSION)/explode.o \
	$(UNZIP_VERSION)/extract.o \
	$(UNZIP_VERSION)/fileio.o \
	$(UNZIP_VERSION)/globals.o \
	$(UNZIP_VERSION)/inflate.o \
	$(UNZIP_VERSION)/list.o \
	$(UNZIP_VERSION)/match.o \
	$(UNZIP_VERSION)/process.o \
	$(UNZIP_VERSION)/ttyio.o \
	$(UNZIP_VERSION)/unreduce.o \
	$(UNZIP_VERSION)/unshrink.o \
	$(UNZIP_VERSION)/zipinfo.o \
	$(UNZIP_VERSION)/unix/unix.o \
	$(UNZIP_VERSION)/api.o

all: zip agent

agent:	$(AGENT_OBJS)
	$(CXX) $(AGENT_OBJS) $(ZIP_OBJS) $(LDFLAGS) -o $@

agentclean:
	rm -f *.o *~;\
	rm -f agent

run: agent
	agent 9000 9


zip: $(ZIP_OBJS)

zipclean:
	rm -rf $(ZIP_OBJS)

clean: zipclean agentclean

help:
	@echo ""
	@echo "make all - for Solaris Unix systems"
	@echo "make clean - cleans all temporary files"
	@echo ""

