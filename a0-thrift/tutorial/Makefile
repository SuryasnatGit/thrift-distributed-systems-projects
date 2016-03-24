# Makefile adapted from: https://www.devin.com/cruft/javamakefile.html
JAVAC=javac
JAVA=java
LIBS= .:/usr/local/Thrift/*
OUTPUT= ./bin/
FLAGS= -g -cp $(LIBS):$(OUTPUT) -d $(OUTPUT)

# Begin define sources, this approach is used so we only compile changed .java files
# The JVM is slow to start up and feeding one file each time is inefficient
# However this can only be used if each class file is also found in the 
# same folder, which Thrift creates a ton of compiled class files.
sources = $(wildcard *.java)
classes = $(sources:.java=.class)

%.class : %.java
	$(JAVAC) $(FLAGS) $<

# End define sources

# Begin define targets
# Recomplies all .java files in every run even when .class hasn't changed 
# no way to avoid this since java files depend on each other in thrift 
all:
	@mkdir -p $(OUTPUT)
	$(JAVAC) $(FLAGS) $(wildcard gen-java/*.java) $(sources)

clean :
	rm -rf $(OUTPUT)*.class

run-rpc-server:
	cd bin/
	$(JAVA) -cp $(LIBS):$(OUTPUT) MulServer

run-rpc-client:
	$(JAVA) -cp $(LIBS):$(OUTPUT) MulClient
