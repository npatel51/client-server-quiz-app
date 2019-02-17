JCC = javac
JFLAGS = -g
default:
	$(JCC) -cp json.jar qserver.java DB.java
	$(JCC) $(JFLAGS) qclient.java
	jar cvmf servermanifest.txt qserver.jar qserver.class DB.class
	jar cvmf clientmanifest.txt qclient.jar qclient.class
clean:
	rm -f *.class


