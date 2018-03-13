package it.polito.security;

import java.util.ArrayList;

public class Main {

	/* Supposition : vnf, nets and the NAT vnf must already exists on openmano */
	
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.out.println("Wrong parameters number : " + args.length);
			System.exit(-1);
		}

		String input_file = args[0];
		String output_file = args[1];
		
		RagToNSD translation_module = new RagToNSD();
		ArrayList<String> ordered_psa_list = translation_module.list_psa_creation(input_file);
		
		/* translation */
		try {
			long nsec = System.nanoTime();
			translation_module.rag_translation(ordered_psa_list, output_file);
			nsec = System.nanoTime() - nsec;
			System.out.println("Translate time sec: " + (double)nsec/ 1000000000.0);
			
		} catch (Exception e) {
			System.out.println("NSD creation didn't succeed: " + e.getMessage());
			System.exit(-1);
		}
		
		/* PSA scripts creation */
		try {
			long nsec = System.nanoTime();
			translation_module.script_for_psa_creation(ordered_psa_list);
			nsec = System.nanoTime() - nsec;
			System.out.println("Script generation time sec: " + (double)nsec/ 1000000000.0);
		} catch (Exception e) {
			System.out.println("Script generation didn't succeed: " + e.getMessage());
			System.exit(-1);
		}
		
		System.exit(0);

	}

}
