JAVAC = javac
JFLAGS = -encoding UTF-8

JAR_PKG = LL1Parser.jar
SOURCE_FILES = \
structs/Symbol.java\
structs/MBNFParser.java\
parser/LL1Parser.java

ENTRY_POINT = parser.LL1Parser

vpath %.class bin
vpath parser/LL1Parser.class bin
vpath structs/MBNFParser.class bin
vpath structs/Symbol.class bin
vpath %.java src
vpath parser/LL1Parser.java src
vpath structs/MBNFParser.java src
vpath structs/Symbol.java src

all : mkdir LL1Parser build jar

mkdir :
	mkdir -p bin

LL1Parser : 
	cp src/llgen llgen

build : $(SOURCE_FILES:.java=.class)

%.class : %.java
	$(JAVAC) -cp bin -d bin $(JFLAGS) $<
	
jar :
	jar cvfe $(JAR_PKG) $(ENTRY_POINT) -C bin .
	
clean :
	rm -fv llgen
	rm -fv LL1Parser.jar
	rm -frv bin/*



