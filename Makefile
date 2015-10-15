all: InstantCloud.class

InstantCloud.class: InstantCloud.java InstantCloudClient.class
	javac -classpath '.:json-simple-1.1.1.jar' $<

InstantCloudClient.class: InstantCloudClient.java InstantCloudLicense.class InstantCloudMachine.class
	javac -classpath '.:json-simple-1.1.1.jar' $<

InstantCloudMachine.class: InstantCloudMachine.java
	javac $<

InstantCloudLicense.class: InstantCloudLicense.java
	javac $<

clean:
	rm *.class
