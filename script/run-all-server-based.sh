#!/bin/bash

#java -cp borg-simulator.jar org.cloudish.borg.MainExecutor all-with-constraint-server-centric-1.conf > all-with-constraint-01.log

#java -cp borg-simulator.jar org.cloudish.borg.MainExecutor all-with-constraint-server-centric-5.conf > all-with-constraint-05.log

#java -cp borg-simulator.jar org.cloudish.borg.MainExecutor all-with-constraint-server-centric-40.conf > all-with-constraint-40.log

#java -cp borg-simulator.jar org.cloudish.borg.MainExecutor prod-with-constraint-server-centric-05.conf > prod-with-constraint-05.log

java -cp borg-simulator.jar org.cloudish.borg.MainExecutor all-constraint-on-server-based.conf > all-constraint-on-server-based.log

java -cp borg-simulator.jar org.cloudish.borg.MainExecutor all-constraint-off-server-based.conf > all-constraint-off-server-based.log

java -cp borg-simulator.jar org.cloudish.borg.MainExecutor prod-constraint-on-server-based.conf > prod-constraint-on-server-based.log

java -cp borg-simulator.jar org.cloudish.borg.MainExecutor prod-constraint-off-server-based.conf > prod-constraint-off-server-based.log

