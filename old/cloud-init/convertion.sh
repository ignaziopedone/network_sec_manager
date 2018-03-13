#!/bin/sh
sudo tr -d '\\r' < script.sh > script1.sh
sudo mv script1^M.sh script.sh
