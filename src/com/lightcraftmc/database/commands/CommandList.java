package com.lightcraftmc.database.commands;

import java.io.File;

import com.lightcraftmc.database.command.Command;

public class CommandList extends Command {

	public CommandList() {
		super("list");
		this.setDescription("List part of the filesystem!");
	}

	@Override
	public String runCommand(boolean isLocal, String[] args) {
		long time = System.currentTimeMillis();
		if (args.length == 0) {
			return "Usage: list filesystem (use '@a' for root data dir)";
		}
		String look = "";
		if(args[0].equalsIgnoreCase("@a")){
			look = "./data/";
		}else{
			look = "./data/" + args[0];
		}
		File file = new File(look);
		String files = "DIRECTORY LISTING OF " + look + "::\n";
		int counter = 0;
		for(File f : file.listFiles()){
			counter++;
			files = files + f.getName().split(".txt")[0] + "\n";
		}
		files = files + ":: (Total: " + counter + " file(s).) (Finished in " + (System.currentTimeMillis() - time) + " ms)";
		return files;

	}

}