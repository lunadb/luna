package com.lightcraftmc.database;

import com.lightcraftmc.database.command.CommandManager;

public class LCDatabaseServer {

	private static int port = 0;
	private static String accessKey = "";
	private static Manager manager;

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("REQUIRED ARGUMENTS: int:port accessKey");
			System.exit(0);
			return;
		}
		System.out.println("Beginning server start...");
		try {
			System.out.println("Checking port...");
			port = Integer.parseInt(args[0]);
		} catch (Exception ex) {
			System.out.println(args[0]
					+ " is not an integer. Please retry port with an integer.");
			System.exit(0);
			return;
		}
		System.out.println("Port is OK.");
		System.out.println("Checking access key.");
		String blockedChars = "?,!,=";
		for (String b : blockedChars.split(",")) {
			if (args[1].contains(b)) {
				System.out.println("Access Key cannot include the following: "
						+ blockedChars.replace("[comma]", ","));
				System.exit(0);
				return;
			}
		}
		accessKey = args[1];
		System.out.println("Access key is OK.");
		System.out.println("Setting up manager...");
		manager = new Manager();
		manager.setPort(port);
		manager.setAccessKey(accessKey);
		System.out.println("Manager has been created.");
		System.out
				.println("All arguments are parsed correctly. Starting server on port "
						+ manager.getPort());
		manager.startServer();
		/*if(!manager.isServerStarted()){
			System.out.println("Server has not been enabled! Check for errors!");
			System.exit(0);
			return;
		}*/
		CommandManager.initCommands();
		CommandInterpreter interpreter = new CommandInterpreter();
		interpreter.listen();
		
		
	}

	public static Manager getManager() {
		return manager;
	}

}