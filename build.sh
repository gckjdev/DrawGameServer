#!/bin/bash

cd ../Common_Java &&
mvn clean install

cd ../Common_Java_Game
mvn clean install 

cd ../DrawGameServer
mvn clean install
