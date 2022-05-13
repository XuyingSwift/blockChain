#!/bin/bash

echo -n "Enter Node name: "
read nodeId

echo -n "Enter node type ('stake' or 'pow'): "
read nodeType

configStr="ben 127.0.0.1 6001 xuying 127.0.0.1 6002 omar 127.0.0.1 6003 hattie 127.0.0.1 6004"
args="$nodeType $nodeId $configStr"
echo $args

cd out/artifacts/csci_520_blockchain_jar
java -cp csci-520_blockchain.jar NodeRunner $args