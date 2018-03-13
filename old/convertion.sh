#!/bin/sh
sudo sh -c "tr -d '\\r' < script.sh > script1.sh"
sudo mv script1.sh script.sh