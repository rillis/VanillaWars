package com.rillis;

import java.io.File;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Vanilla extends JavaPlugin {
	Database db = null;
	@Override
    public void onEnable() {
		
		
		boolean first = false;
		String name = "Vanilla.db";
		File f = new File(name);
		
		if(!f.exists()) {
			first=true;
		}
		
		db = new Database(name);
		Commands c = new Commands(db);
		this.getCommand("lideres").setExecutor(c);
		this.getCommand("setbase").setExecutor(c);
		this.getCommand("base").setExecutor(c);
		this.getCommand("recrutar").setExecutor(c);
		this.getCommand("alistar").setExecutor(c);
		this.getCommand("alistados").setExecutor(c);
		this.getCommand("raid").setExecutor(c);
		this.getCommand("highlight").setExecutor(c);
		this.getCommand("g").setExecutor(c);
		this.getCommand("equipe").setExecutor(c);
		
		if(first) {
			
		}
		
		getServer().getPluginManager().registerEvents(new MyListener(db), this);
		
		db.truncate("alistados_red");
		db.truncate("alistados_blue");
    }
    @Override
    public void onDisable() {
    	db.close();
    }
}
