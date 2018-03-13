#!/bin/bash
######################################################################
#
# OpenSource MANO controller
# Author: Ignazio Pedone
# Polytechnic Univerity of Turin
#
#
######################################################################


################################################################ CONFIGURATION #####################################################################

## PATH CONTROLLER
pathIntegration=$HOME"/Scrivania"
pathTranslationModule=$pathIntegration"/traduttore"
#pathTranslationModuleBinFolder=$pathTranslationModule"/home/ignazio/Scrivania/"
pathTranslationModuleOutputFolder=$pathTranslationModule"/output/"
pathTranslationModuleInputFolder=$pathTranslationModule"/input/"
pathCreatePackage=$pathIntegration"/RAG/"
#pathAuxFiles=$pathIntegration"Files/"
vNSFConfFolder=$pathTranslationModuleOutputFolder"scripts/"

## PSA INFO
userPSA="root"

## PASSWORD
pswMySQL="stack"  #password of MySQL installed on controller node

## MANAGEMENT NET NAME
managementNet="default"    #name of the net that allows PSA to access internet, it is the net name in OpenMANO that represents the natted network of libvirt

## TIMEOUT BEFORE THE INTERFACE CONFIGURATION
timeslot="2.4"           #change this param on the performance of the worst compute node

## STRING USED FOR COMPARISON
validating="0"
noScenario="No scenarios were found."
noInstanceScenario="No scenario instances were found."

## UPR ADDRESS
uprAddress="http://10.170.36.58"
uprPort=":8081"

## VAR
yamlExt=".yaml"
xmlExt=".xml"

export OSM_HOSTNAME=10.170.36.139
export OSM_RO_HOSTNAME=10.170.36.236 

############################################################################################################################################################

command="deploy"

if [ $command == "deploy" ]; then

	userID="$2"
	nsdName="$3"
	#instanceName="instance-""$2"

	# Downloading RAG from UPR using the userID passed as first argument to this script
	ragBase64=`curl -s "${uprAddress}${uprPort}"'/v1/upr/users/'"${userID}"'/RAG/' | python -mjson.tool | grep "asg" | cut -f 2 -d ":" | cut -f 1 -d "," | cut -f 2 -d '"'`
	echo ${ragBase64} | base64 --decode > ${pathTranslationModuleInputFolder}${nsdName}${xmlExt}


	# Download the low level configurations for the user's network service
	psaAllConf=`curl -s "${uprAddress}${uprPort}"'/v1/upr/users/'"${userID}"'/PSAConf/' | python -mjson.tool`

	#Translation RAG->NSD
	cd $pathTranslationModule
        java -jar RAG.jar ${nsdName}${xmlExt} ${nsdName}
        if [ "$?" -ne 0 ]; then
                echo "Error during translation. EXIT"
                exit
        fi;

	#Transfer nsd to packaging unit and creation package
	mv $pathTranslationModuleOutputFolder${nsdName}${yamlExt} $pathCreatePackage
	cd $pathCreatePackage
	
	
	echo "Starting package creation..."
	source create_package.sh ${nsdName}
	echo "Starting upload package..."
	osm upload-package ${nsdName}".tar.gz"
	echo "Package uploaded!"
	
	#Decode vNFS configurations
	vNSF=`echo "${psaAllConf}" | grep -B 2 "iptables" | head -n 1 | cut -f 2 -d ":" |cut -f 1 -d "," | cut -f 2 -d '"'`
	if [ -z "$vNSF" ]
	then
      		echo "No iptables configuration!"
	else
		echo ${vNSF} | base64 --decode > ${vNSFConfFolder}"iptables.sh"
		echo "Iptables configuration generated!"
		cd $pathIntegration
		scp -i chiavefile.pem -o "StrictHostKeyChecking no" ${vNSFConfFolder}"iptables.sh" ignaziopedone@130.192.1.95:/Users/ignaziopedone/configurazioni/
		echo "Configurazioni trasferite!"
	fi

	vNSF=`echo "${psaAllConf}" | grep -B 2 "squid" | head -n 1 | cut -f 2 -d ":" |cut -f 1 -d "," | cut -f 2 -d '"'`
	if [ -z "$vNSF" ]
	then
      		echo "No squid configuration!"
	else	
		echo ${vNSF} | base64 --decode > ${vNSFConfFolder}"squid.sh"
		echo "Squid configuration generated!"
		scp -i chiavefile.pem -o "StrictHostKeyChecking no" ${vNSFConfFolder}"squid.sh" ignaziopedone@130.192.1.95:/Users/ignaziopedone/configurazioni/
		echo "Configurazioni trasferite!"	
	fi

	vNSF=`echo "${psaAllConf}" | grep -B 2 "strongswan" | head -n 1 | cut -f 2 -d ":" |cut -f 1 -d "," | cut -f 2 -d '"'`
	if [ -z "$vNSF" ]
	then
      		echo "No strongswan configuration!"
	else
		echo ${vNSF} | base64 --decode > ${vNSFConfFolder}"strongswan.sh"
		echo "Strongswan configuration generated!"
		scp -i chiavefile.pem -o "StrictHostKeyChecking no" ${vNSFConfFolder}"strongswan.sh" ignaziopedone@130.192.1.95:/Users/ignaziopedone/configurazioni/
		echo "Configurazioni trasferite!"
	fi		
	
	# Scenario upload and deploy
	
	osm ns-create --ns_name "$nsdName" --nsd_name "$nsdName" --vim_account openstack-site
	echo "Servizio di rete creato!"
	


	
fi;

################################################################################## END DEPLOY REGION #################################################################################



