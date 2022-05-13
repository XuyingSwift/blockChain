#!/bin/bash

echo -n "Enter Node name: "
read nodeId

echo -n "Enter node type ('stake' or 'pow'): "
read nodeType

configStr="ben 44.202.108.131 4000 xuying 54.165.151.239 4000 omar 18.219.253.101 4000 hattie 54.67.66.98 4000"
args="$nodeType $nodeId $configStr"
echo $args

cd out/artifacts/csci_520_blockchain_jar
java -cp csci-520_blockchain.jar NodeRunner $args