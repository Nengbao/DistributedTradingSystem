JC = javac
JFLAGS = -g
CLASSPATH = -classpath ../../src/:../../lib/jgroups-3.6.9.Final.jar:../../lib/slf4j-api-1.7.21.jar:../../lib/slf4j-simple-1.7.21.jar
FLAGS = -Djava.net.preferIPv4Stack=true

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $(CLASSPATH) $*.java

SRC = $(wildcard ../../src/*.java)

BIN: $(SRC:.java=.class)

all: BIN

run_sentinel_has_stock1:
	java $(CLASSPATH) $(FLAGS) edu.uchicago.cs.SentinelServer --exchangeClusterName=north_america --isRestart=false

run_sentinel_has_stock2:
	java $(CLASSPATH) $(FLAGS) edu.uchicago.cs.SentinelServer --exchangeClusterName=north_america --isRestart=false

run_sentinel_has_no_stock1:
	java $(CLASSPATH) $(FLAGS) edu.uchicago.cs.SentinelServer --exchangeClusterName=asia --isRestart=false

run_sentinel_has_no_stock2:
	java $(CLASSPATH) $(FLAGS) edu.uchicago.cs.SentinelServer --exchangeClusterName=asia --isRestart=false

run_exchange_has_stock:
	java $(CLASSPATH) $(FLAGS) edu.uchicago.cs.ExchangeServer --exchangeName=new_york --clusterName=north_america --stockFile=../qty_stocks.csv --priceFile=../price_stocks.csv --isRestart=false --portNum=10000

run_exchange_has_no_stock:
	java $(CLASSPATH) $(FLAGS) edu.uchicago.cs.ExchangeServer --exchangeName=shanghai --clusterName=asia --stockFile=null --priceFile=null --isRestart=false --portNum=10001

run_client1:
	java $(CLASSPATH) $(FLAGS) edu.uchicago.cs.Client --exchangeIp=127.0.0.1 --exchangePortNum=10001 --file=client_1_action_list.csv

clean:
	$(RM) ../../src/*.class

.PHONY: kill
kill:
	kill -9 `ps -aef | grep 'java -classpath .:jgroups-3.6.9.Final.jar' | grep -v grep | awk '{print $$2}'`