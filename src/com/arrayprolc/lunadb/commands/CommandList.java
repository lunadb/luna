package com.arrayprolc.lunadb.commands;

import java.io.File;

import com.arrayprolc.lunadb.command.Command;

public class CommandList extends Command {

    public CommandList() {
        super("list");
        this.setDescription("List part of the filesystem!");
    }

    @Override
    public String runCommand(String ip, boolean isLocal, String[] args, boolean isAdmin) {
        long time = System.currentTimeMillis();
        if (args.length == 0) {
            return "Usage: list filesystem (use '@a' for root data dir)";
        }
        try {
            String look = "";
            if (args[0].equalsIgnoreCase("@a")) {
                look = "./data/";
            } else {
                look = "./data/" + args[0];
            }
            look = look.replace("..", "");
            File file = new File(look);
            String files = "SUCCESS: ";
            int counter = 0;
            for (File f : file.listFiles()) {
                counter++;
                files = files + f.getName().split(".txt")[0] + ",";
            }
            files = files + ":: (Total: " + counter + " file(s).) (Finished in " + (System.currentTimeMillis() - time) + " ms)";
            return files;
        } catch (Exception ex) {
            return "FAILED: Could not find directory.";

        }

    }

}
