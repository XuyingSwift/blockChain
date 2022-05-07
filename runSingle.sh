#!/bin/bash

echo -n "Enter Node name: "
read nodeId

configStr="ben 127.0.0.1 6001"
args="$nodeId $configStr"
echo $args

cd out/artifacts/csci_520_blockchain_jar
java -cp csci-520_blockchain.jar NodeRunner $args