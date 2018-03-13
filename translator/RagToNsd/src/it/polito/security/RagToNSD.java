package it.polito.security;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RagToNSD {

	public ArrayList<String> list_psa_creation(String input_file) {

		ArrayList<String> ordered_psa_list = new ArrayList<String>();
		HashMap<String, String> map_service_psa = new HashMap<String, String>();

		try {
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = fac.newDocumentBuilder();
			Document document = builder.parse(new File("input/" + input_file));

			Element root = document.getDocumentElement();
			NodeList service_graph = root.getElementsByTagName("service_graph");
			Element service_graph_element = (Element) service_graph.item(0);

			/* Read service name and its internal psa name */
			NodeList service = service_graph_element.getElementsByTagName("service");
			for (int i = 0; i < service.getLength(); i++) {
				Element service_element = (Element) service.item(i);
				Element PSA = (Element) service_element.getElementsByTagName("PSA").item(0);
				map_service_psa.put(service_element.getAttribute("serviceID"), PSA.getAttribute("name"));
			}

			/* Read root service name */
			Element rootService = (Element) service_graph_element.getElementsByTagName("rootService").item(0);
			String rootServiceName = rootService.getTextContent();
			ordered_psa_list.add(map_service_psa.get(rootServiceName));

			/* Read end service name */
			Element endService = (Element) service_graph_element.getElementsByTagName("endService").item(0);
			String endServiceName = endService.getTextContent();

			/*
			 * A map is useful because if the edges are not ordered it doesn't
			 * affect the algorithm
			 */
			HashMap<String, String> map_src_dst = new HashMap<String, String>();
			NodeList edges = service_graph_element.getElementsByTagName("edge");
			for (int i = 0; i < edges.getLength(); i++) {
				Element edge_element = (Element) edges.item(i);
				String src_Service = ((Element) edge_element.getElementsByTagName("src_Service").item(0))
						.getTextContent();
				String dst_Service = ((Element) edge_element.getElementsByTagName("dst_Service").item(0))
						.getTextContent();
				map_src_dst.put(src_Service, dst_Service);
			}

			/* Creates the ordered list of psa that will be deployed */
			String previous_service = rootServiceName;
			while (!endServiceName.equals(previous_service)) {
				String next_service = map_src_dst.get(previous_service);
				String next_psa = map_service_psa.get(next_service);
				if (!ordered_psa_list.contains(next_psa))
					ordered_psa_list.add(next_psa);
				map_src_dst.remove(previous_service);
				previous_service = next_service;
			}

		} catch (Exception e) {
			System.out.println("Traduction didn't succeed: " + e.getMessage());
			System.exit(-1);
		}
		return ordered_psa_list;
	}

	public void rag_translation(ArrayList<String> ordered_psa_list, String output_file) throws Exception {
		
		/*
		 * Add the client that starts the service chain 
		 */
		
		ordered_psa_list.add(0, "user");
		/*
		 * Add the gateway/nat that ends the service chain offering connection to
		 * Internet
		 */
		ordered_psa_list.add("gateway");

		/* Writing the NSD */
		PrintWriter writer = new PrintWriter("output/" + output_file + ".yaml", "UTF-8");
		writer.println("nsd:nsd-catalog:");
		writer.println("    nsd:");
		writer.println("    -   id: "+output_file);
		writer.println("        name: "+output_file);
		writer.println("        short-name: "+output_file);
		writer.println("        description: Generated by OSM pacakage generator");
		writer.println("        vendor: Ignazio Pedone");
		writer.println("        version: '3.0'");
		writer.println("        constituent-vnfd:");

		//int x_gap = 1000 / ordered_psa_list.size();
		
		System.out.println("Start");
		
		for (int i = 0; i < ordered_psa_list.size(); i++) {
			
			writer.println("        -   member-vnf-index: '"+(i+1)+"'");
			writer.println("            start-by-default: 'true'");
			writer.println("            vnfd-id-ref: "+ordered_psa_list.get(i));
			
		}		
		
		writer.println("");
		writer.println("        vld:");
		writer.println("        # Networks for the VNFs");
		writer.println("            -   id: mgmt");
		writer.println("                name: mgmt");
		writer.println("                short-name: mgmt");
		writer.println("                mgmt-network: 'true'");
		writer.println("                vim-network-name: mgmt");
		writer.println("                vnfd-connection-point-ref:");
		
		for (int i = 0; i < ordered_psa_list.size(); i++) {
			
			writer.println("                -   member-vnf-index-ref: "+(i+1));
			writer.println("                    vnfd-id-ref: "+ordered_psa_list.get(i));
			writer.println("                    vnfd-connection-point-ref: eth0");
			
		}
		
		//writer.println("            rw-nsd:ipv4-nat-pool-name: openstack-site");
		
		
		
		for (int i = 0; i < (ordered_psa_list.size()-1); i++) {
			
			writer.println("            -   id: link_"+(i+1));
			writer.println("                mgmt-network: 'false'");
			writer.println("                name: link_"+(i+1));
			writer.println("                vim-network-name: link_"+(i+1));
			writer.println("                vnfd-connection-point-ref:");
			writer.println("                -   member-vnf-index-ref: '"+(i+1)+"'");
			writer.println("                    vnfd-connection-point-ref: eth2");
			writer.println("                    vnfd-id-ref: "+ordered_psa_list.get(i));
			writer.println("                -   member-vnf-index-ref: '"+(i+2)+"'");
			writer.println("                    vnfd-connection-point-ref: eth1");
			writer.println("                    vnfd-id-ref: "+ordered_psa_list.get(i+1));
			
		}

		
		writer.println("            -   id: internet");
		writer.println("                mgmt-network: 'false'");
		writer.println("                name: internet");
		writer.println("                vim-network-name: data");
		writer.println("                vnfd-connection-point-ref:");
		writer.println("                -   member-vnf-index-ref: '"+ordered_psa_list.size()+"'");
		writer.println("                    vnfd-connection-point-ref: eth2");
		writer.println("                    vnfd-id-ref: "+ordered_psa_list.get((ordered_psa_list.size()-1)));
		
		writer.close();
		System.out.println("NSD creation finished");
	}

	public void script_for_psa_creation(ArrayList<String> ordered_psa_list) throws Exception {

		/*
		 * First two byte of the nets between the psa selected randomly in order
		 * to avoid collision between different NSD, if two PSA of different NSD
		 * are on the same net they can communicate, selecting randomly the net
		 * this could be probabilistically avoided
		 * CollisionProbability=1/(192^2)
		 */
		int randomByte1 = ThreadLocalRandom.current().nextInt(1, 192 + 1);
		int randomByte2 = ThreadLocalRandom.current().nextInt(1, 192 + 1);

		String ipNet = randomByte1 + "." + randomByte2 + ".";

		/* Managing net and creating psa scripts */
		for (int i = 0; i < ordered_psa_list.size(); i++) {

			/*
			 * Root Service
			 */
			if (i == 0 && ordered_psa_list.size() > 1) {
				PrintWriter scriptWriter = new PrintWriter(
						"output/scripts/" + ordered_psa_list.get(i) + "_" + i + ".sh", "UTF-8");
				scriptWriter.println("#!/bin/bash");

				/*
				 * Configuring ip address for interface at right side and run
				 * init.sh
				 */
				scriptWriter.println("ifconfig eth1 " + ipNet + i + ".1" + " netmask 255.255.255.252");
				scriptWriter.println("route del default");
				scriptWriter.println("route add default gw " + ipNet + i + ".2");
				scriptWriter.println("./scripts/init.sh");
				scriptWriter.println("exit");
				scriptWriter.close();
			}

			/*
			 * Case NON rootService: creation of connection in NSD configuring
			 * ip address of the interfaces 
			 */
			if (i != 0) {
				if (i == ordered_psa_list.size() - 1) {
					PrintWriter scriptWriter = new PrintWriter(
							"output/scripts/" + ordered_psa_list.get(i) + "_" + i + ".sh", "UTF-8");
					scriptWriter.println("#!/bin/bash");

					/*
					 * Configuring ip address for interface at left side and run
					 * init.sh
					 */
					scriptWriter.println("ifconfig eth1 " + ipNet + (i - 1) + ".2" + " netmask 255.255.255.252");

					scriptWriter
							.println("iptables --table nat --append POSTROUTING --out-interface eth0 -j MASQUERADE");
					scriptWriter.println("iptables --append FORWARD --in-interface eth1 -j ACCEPT");
					scriptWriter.println("./scripts/init.sh");
					scriptWriter.println("exit");
					scriptWriter.close();

				} else {
					PrintWriter scriptWriter = new PrintWriter(
							"output/scripts/" + ordered_psa_list.get(i) + "_" + i + ".sh", "UTF-8");
					scriptWriter.println("#!/bin/bash");

					/* Configuring ip address for interface at left side */
					scriptWriter.println("ifconfig eth1 " + ipNet + (i - 1) + ".2" + " netmask 255.255.255.252");

					/*
					 * Configuring ip address for interface at right side and
					 * setting next psa as next routing hop and run init.sh
					 */
					scriptWriter.println("ifconfig eth2 " + ipNet + i + ".1" + " netmask 255.255.255.252");
					scriptWriter.println("route del default");
					scriptWriter.println("route add default gw " + ipNet + i + ".2");

					scriptWriter
							.println("iptables --table nat --append POSTROUTING --out-interface eth2 -j MASQUERADE");
					scriptWriter.println("iptables --append FORWARD --in-interface eth1 -j ACCEPT");
					scriptWriter.println("./scripts/init.sh");
					scriptWriter.println("exit");
					scriptWriter.close();
				}
			}
		}
		System.out.println("Script generation finished");

	}
}
