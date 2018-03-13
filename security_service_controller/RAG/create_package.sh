#!/bin/bash

#You should only insert the xxxx_nsd.yaml file in the RAG folder

rag=$1
rag_file=$1".yaml"

mkdir $rag
mv $rag_file $rag
 
./generate_descriptor_pkg.sh ./ $rag
echo "Package creation done!"
