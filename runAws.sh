#!/bin/bash

echo -n "Enter Node name: "
read nodeId

echo -n "Enter node type ('stake' or 'pow'): "
read nodeType

configStr="ben 18.212.28.37 4000 xuying 52.207.209.55 4000 omar 44.204.63.193 4000 hattie 18.118.51.255 4000"
args="$nodeType $nodeId $configStr"
echo $args

cd out/artifacts/csci_520_blockchain_jar
java -cp csci-520_blockchain.jar NodeRunner $args