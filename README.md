# Instant Cloud API - Java client

This repository contains sample Java code for making API requests to the Gurobi Instant Cloud.

## Obtaining this repository

If you have git installed, you can obtain this repository, by cloning it, with the following command:

```
git clone https://github.com/Gurobi/instantcloud-java.git
```

If you don't have git installed, you can obtain this repository, by
clicking the Download Zip button in the right sidebar.

## Requirements

The `instantcloud` program requires the JSON parsing library [json-simple](http://code.google.com/p/json-simple/),
which is licensed under the Apache 2.0 license. See [this page](https://github.com/fangyidong/json-simple/blob/master/LICENSE.txt) for more information on the json-simple license.

The json-simple jar file is included in this repository. However you need to make sure this jar file is
in your CLASSPATH.

## Building the Java client

If you are running under Linux or Mac OS X, issue the following command to build the Java client:
```
make all
```

If you are running on Windows, issue the following commands to build the Java client:
```
javac InstantCloudLicense.java
javac InstantCloudMachine.java
javac -classpath ".;%cd%\json-simple-1.1.1.jar" InstantCloudClient.java
javac -classpath ".;%cd%\json-simple-1.1.1.jar" InstantCloud.java

```

You can then run the `instantcloud` program using the included `instantcloud.bat` file.


## Using instantcloud from the command-line

The `instantcloud` program can be used as a command-line client for the API. It provides
access to the four API endpoints: licenses, machines, launch, kill.

### List your licenses

Run the following command to list your licenses:

```
./instantcloud licenses --id INSERT_YOUR_ID_HERE --key INSERT_YOUR_KEY_HERE
```

You should see output like the following:
```
License Credit  Rate Plan       Expiration
95912   659.54  standard        2016-06-30
95913   44.10   nocharge        2016-06-30
```

### List your running machines

Run the following command to list your running machines

```
./instantcloud machines --id INSERT_YOUR_ID_HERE --key INSERT_YOUR_KEY_HERE
```

You should see output like the following:

```
Machine name:  ec2-54-85-186-203.compute-1.amazonaws.com
        license type:  light compute server
        state:  idle
        machine type:  c4.large
        region:  us-east-1
        idle shutdown:  60
        user password:  a446887d
        create time:  2015-10-14T20:27:01.224Z
        license id:  95912
        machine id:  xjZTbW9tdqbT32Cep
```


### Launch a machine

Run the following command to launch a machine

```
./instantcloud launch --id INSERT_YOUR_ID_HERE --key INSERT_YOUR_KEY_HERE -n 1 -m c4.large
```

You should see output similar to the following
```
Machines Launched
Machine name:  -
        license type:  light compute server
        state:  launching
        machine type:  c4.large
        region:  us-east-1
        idle shutdown:  60
        user password:  a446887d
        create time:  2015-10-14T20:27:01.224Z
        license id:  95912
        machine id:  xjZTbW9tdqbT32Cep
```


### Kill a machine

Run the following command to kill a machine

```
./instantcloud kill --id INSERT_YOUR_ID_HERE --key INSERT_YOUR_KEY_HERE xjZTbW9tdqbT32Cep
```

You should see output similar to the following

```
Machines Killed
Machine name:  ec2-54-85-186-203.compute-1.amazonaws.com
        license type:  light compute server
        state:  killing
        machine type:  c4.large
        region:  us-east-1
        idle shutdown:  60
        user password:  a446887d
        create time:  2015-10-14T20:27:01.224Z
        license id:  95856
        machine id:  xjZTbW9tdqbT32Cep

```

## Using InstantCloudClient as a library

You can use the `InstantCloudClient` class within your own Java programs to make
API calls.
