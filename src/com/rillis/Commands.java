package com.rillis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Commands implements CommandExecutor {
	Database db;
	static boolean raid_red_pc = false;
	static boolean raid_blue_pc = false;
	static boolean raid_red_c = false;
	static boolean raid_blue_c = false;
	public Commands(Database db) {
		this.db=db;
	}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	
    	if((command.getName().equalsIgnoreCase("setbase") || command.getName().equalsIgnoreCase("alistar") || command.getName().equalsIgnoreCase("recrutar") || command.getName().equalsIgnoreCase("base")) && MyListener.raid_blocked){
    		sender.sendMessage(ChatColor.RED+"Você não pode fazer isso com uma RAID em andamento.");
    		return true;
    	}
    	
    	if (command.getName().equalsIgnoreCase("equipe") && sender instanceof Player) {
			Player p = (Player) sender;
			ArrayList<String> arr = new ArrayList<String>();
	    	ArrayList<String> uuids = new ArrayList<String>();
			try {
				ResultSet rs = db.returnStatement("SELECT * FROM times where time=\""+getTime(p.getUniqueId())+"\"");
				while (rs.next()) {
					  arr.add(rs.getString("display"));
					  uuids.add(rs.getString("user"));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			String[] integrantes = arr.toArray(new String[0]);
			String[] uuid = uuids.toArray(new String[0]);
			
			p.sendMessage(ChatColor.AQUA+""+ChatColor.ITALIC+"Lista de integrantes da sua equipe:");
			p.sendMessage("");
			
			for (int i = 0; i < integrantes.length; i++) {
				if(isLider(UUID.fromString(uuid[i]))) {
					p.sendMessage(ChatColor.GREEN+""+ChatColor.BOLD+"["+(i+1)+"]: "+integrantes[i]);
				}else {
					p.sendMessage(ChatColor.GREEN+"["+(i+1)+"]: "+integrantes[i]);
				}
			}
			return true;
		}
    	
    	if(command.getName().equalsIgnoreCase("highlight")){
			//System.out.println("setando");
			Player p = (Player) sender;
			Coord c = getCoord(p.getUniqueId());
			
			if(!isLider(p.getUniqueId())) {
				sender.sendMessage(ChatColor.RED+"Somente lideres podem usar esse comando.");
				return true;
			}
    		int x = c.x;
    		int y = c.y;
    		int z = c.z;
    		
    		Block[] blocks = getBlockFromRadius(p.getWorld(),50,x,y-1,z);
    		System.out.println(blocks.length);
    		ArrayList<BlockData> ob = new ArrayList<BlockData>();
    		
    		for (int i = 0; i < blocks.length; i++) {
    			//System.out.println("a");
    			Block v = getBlockReal(p.getWorld(), blocks[i].getX(), blocks[i].getY(), blocks[i].getZ());
    			ob.add(v.getBlockData());
    			getBlockReal(p.getWorld(), blocks[i].getX(), blocks[i].getY(), blocks[i].getZ()).setType(Material.BEDROCK);
    			
			}
    		//sender.sendMessage(ChatColor.YELLOW+"Limites da base exibidos.");
    		new Thread() {
    			public void run() {
    				try {
    					BossBar boss = Bukkit.createBossBar(ChatColor.DARK_AQUA+"Fronteira exibida: 30 segundos restantes", BarColor.BLUE, BarStyle.SOLID);
    					boss.setProgress(1);
    					boss.addPlayer(p);
    					
    					double max = 30;
    					double seg = max;
    					Thread.sleep(1000);
    					while(seg>0) {
    						seg--;
    						boss.setProgress(seg/max);
    						boss.setTitle(ChatColor.DARK_AQUA+"Fronteira exibida: "+((int)seg)+" segundos restantes");
    						Thread.sleep(1000);
    					}
    					boss.removePlayer(p);
    				}catch(Exception e) {
    					e.printStackTrace();
    				}
    			}
    		}.start();
    		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("VanillaWars"), new Runnable() {
    		     public void run() {
    		    	 for (int i = 0; i < ob.size(); i++) {
    		    			
    		    			Block b = getBlockReal(p.getWorld(), blocks[i].getX(), blocks[i].getY(), blocks[i].getZ());
    		    			b.setBlockData(ob.get(i));
    		    			
    		    			
    					}
    		    	 //sender.sendMessage(ChatColor.YELLOW+"Limites da base ocultados.");
    		     }
    		}, (30 * 20));
    		
    		
    	}
    	if (command.getName().equalsIgnoreCase("g")) {
    		if(args.length<1) {
    			sender.sendMessage(ChatColor.RED+"Use: /g <msg>.");
        		return true;
    		}else {
    			Player p = (Player) sender;
    			String time = getTime(p.getUniqueId());
    			String mensagem = ChatColor.GRAY+"[G] ";
    			if(time.equals("blue")) {
    				mensagem += ChatColor.BLUE+"[Azul] "+p.getDisplayName()+ChatColor.GRAY+":";
    				if(isLider(p.getUniqueId())) {
    					mensagem = ChatColor.GOLD+"[Líder] "+mensagem;
    				}
    				for (int i = 0; i < args.length; i++) {
						mensagem+=" "+args[i];
					}
    				Bukkit.broadcastMessage(mensagem);
    				
    			}else if(time.equals("red")) {
    				mensagem += ChatColor.RED+"[Vermelho] "+p.getDisplayName()+ChatColor.GRAY+":";
    				if(isLider(p.getUniqueId())) {
    					mensagem = ChatColor.GOLD+"[Líder] "+mensagem;
    				}
    				for (int i = 0; i < args.length; i++) {
						mensagem+=" "+args[i];
					}
    				Bukkit.broadcastMessage(mensagem);
    			}else {
    				mensagem += ChatColor.WHITE+"[Sem time] "+p.getDisplayName()+ChatColor.GRAY+":";
    				if(isLider(p.getUniqueId())) {
    					mensagem = ChatColor.GOLD+"[Líder] "+mensagem;
    				}
    				for (int i = 0; i < args.length; i++) {
						mensagem+=" "+args[i];
					}
    				Bukkit.broadcastMessage(mensagem);
    			}
    			return true;
    		}
    	}
    	if (command.getName().equalsIgnoreCase("raid") && MyListener.raid_blocked) {
    		sender.sendMessage(ChatColor.RED+"RAID já em andamento.");
    		return true;
    	}else if (command.getName().equalsIgnoreCase("raid") && args.length==0) {
    		if (sender instanceof Player) {
    			Player p = (Player) sender;
    			String time = getTime(p.getUniqueId());
    			
    			String [] alistados = db.getList("alistados_"+time, "user");
				int qtdAtacantes = alistados.length;
				
				if(qtdAtacantes==0) {
					sender.sendMessage(ChatColor.RED+"Ninguém está alistado na sua equipe. (/alistados | /alistar)");
		    		return true;
				}
				
				String [] defensores = null;
				if(time.equals("blue")) {
					defensores = getListaTimeOnline("red");
				}else {
					defensores = getListaTimeOnline("blue");
				}
				int qtdDefensores = defensores.length;
				if(qtdDefensores==0) {
					sender.sendMessage(ChatColor.RED+"Não há defensores online.");
		    		return true;
				}
    			if(qtdDefensores<qtdAtacantes) {
    				sender.sendMessage(ChatColor.RED+"Deverá ter a mesma quantidade de defensores ou mais do que atacantes.");
		    		return true;
    			}
    			
    			sender.sendMessage(ChatColor.DARK_RED+"------------- RAID -------------");
    			sender.sendMessage(ChatColor.YELLOW+"Lista de Atacantes:");
    			
    			String msg = "[";
    			for (int i = 0; i < alistados.length; i++) {
					if(i>0) {
						msg+=", "+getNameByUUID(UUID.fromString(alistados[i]));
					}else {
						msg+=""+getNameByUUID(UUID.fromString(alistados[i]));
					}
				}
    			msg += "]";
    			sender.sendMessage(ChatColor.RED+msg);
    			sender.sendMessage(ChatColor.YELLOW+"");
    			sender.sendMessage(ChatColor.YELLOW+"Lista de Defensores:");
    			
    			msg = "[";
    			for (int i = 0; i < defensores.length; i++) {
					if(i>0) {
						msg+=", "+defensores[i];
					}else {
						msg+=""+defensores[i];
					}
				}
    			msg += "]";
    			sender.sendMessage(ChatColor.GREEN+msg);
    			sender.sendMessage(ChatColor.YELLOW+"");
    			sender.sendMessage(ChatColor.YELLOW+"Para confirmar a Raid use"+ChatColor.WHITE+" /raid "+ChatColor.DARK_GREEN+"confirmar");
    			sender.sendMessage(ChatColor.DARK_RED+"--------------------------------");
    			
    			if(time.equals("blue")){
    				raid_red_pc = true;
    			}else {
    				raid_blue_pc = true;
    			}
    			
    			return true;
    		}
    	}else if (command.getName().equalsIgnoreCase("raid") && args.length==1) {
    		if(args[0].equalsIgnoreCase("confirmar")) {
    			Player p = (Player) sender;
    			String time = getTime(p.getUniqueId());
    			if(time.equals("blue")){
    				if(raid_red_pc) {
    					raid_red_pc=false;
    					raid_red_c=true;
    					sender.sendMessage(ChatColor.GREEN+"Raid confirmada!");
    					
    					Bukkit.broadcastMessage(ChatColor.AQUA+"A equipe "+ChatColor.BLUE+"Azul"+ChatColor.AQUA+" desafiou a equipe "+ChatColor.RED+"Vermelha"+ChatColor.AQUA+".");
    					String [] alistados = db.getList("alistados_"+time, "user");
    					String msg = "[";
    	    			for (int i = 0; i < alistados.length; i++) {
    						if(i>0) {
    							msg+=", "+getNameByUUID(UUID.fromString(alistados[i]));
    						}else {
    							msg+=""+getNameByUUID(UUID.fromString(alistados[i]));
    						}
    					}
    	    			msg += "]";
    	    			Bukkit.broadcastMessage(ChatColor.AQUA+"Atacantes: "+ChatColor.RED+msg);
    	    			Bukkit.broadcastMessage(ChatColor.AQUA+"Para aceitar use "+ChatColor.WHITE+"/raid "+ChatColor.DARK_GREEN+"aceitar");
    	    			
    					return true;
    				}else {
    					sender.sendMessage(ChatColor.RED+"Não há nada para confirmar.");
    					
    		    		return true;
    				}
    			}else {
    				if(raid_blue_pc) {
    					raid_blue_pc=false;
    					raid_blue_c=true;
    					sender.sendMessage(ChatColor.GREEN+"Raid confirmada!");
    					Bukkit.broadcastMessage(ChatColor.AQUA+"A equipe "+ChatColor.RED+"Vermelha"+ChatColor.AQUA+" desafiou a equipe "+ChatColor.BLUE+"Azul"+ChatColor.AQUA+".");
    					String [] alistados = db.getList("alistados_"+time, "user");
    					String msg = "[";
    	    			for (int i = 0; i < alistados.length; i++) {
    						if(i>0) {
    							msg+=", "+getNameByUUID(UUID.fromString(alistados[i]));
    						}else {
    							msg+=""+getNameByUUID(UUID.fromString(alistados[i]));
    						}
    					}
    	    			msg += "]";
    	    			Bukkit.broadcastMessage(ChatColor.AQUA+"Atacantes: "+ChatColor.RED+msg);
    	    			Bukkit.broadcastMessage(ChatColor.AQUA+"Para aceitar use "+ChatColor.WHITE+"/raid "+ChatColor.DARK_GREEN+"aceitar");
    					return true;
    				}else {
    					sender.sendMessage(ChatColor.RED+"Não há nada para confirmar.");
    		    		return true;
    				}
    			}
    			
    		}else if(args[0].equalsIgnoreCase("aceitar")) {
    			Player p = (Player) sender;
    			String time = getTime(p.getUniqueId());
    			if(time.equals("blue")){
    				if(raid_blue_c) {
    					new Thread() {
    						public void run() {
    							raid_blue_c=false;
    							double seg_max= 60;
    							double seg = 60;
    							BossBar bossBar = Bukkit.createBossBar(ChatColor.AQUA+"Preparação raid no time "+ChatColor.BLUE+"Azul"+ChatColor.AQUA+". 60 segundos restantes.", BarColor.YELLOW, BarStyle.SEGMENTED_20);
    							for(Player p : Bukkit.getOnlinePlayers()){
    								bossBar.addPlayer(p);
    							}
    							
        						bossBar.setProgress(0);
        						MyListener.raid_blocked=true;
        						
        						try {
        							while(seg>0) {
        								Thread.sleep(1000);
        								double progress = seg/seg_max;
            							bossBar.setProgress(progress);
            							bossBar.setTitle(ChatColor.AQUA+"Preparação raid no time "+ChatColor.BLUE+"Azul"+ChatColor.AQUA+". "+seg+" segundos restantes.");
            							seg--;
        							}
        							Thread.sleep(1000);
        							for(Player p : Bukkit.getOnlinePlayers()){
        								bossBar.removePlayer(p);
        							}
        							
        							Thread.sleep(2000);
        							
        							MyListener.raid_blue=true;
        							
        							bossBar.setProgress(1);
        							bossBar.setColor(BarColor.RED);
        							bossBar.setTitle(ChatColor.DARK_PURPLE+"RAID no time "+ChatColor.BLUE+"Azul"+ChatColor.DARK_PURPLE+". 300 segundos restantes.");
        							for(Player p : Bukkit.getOnlinePlayers()){
        								bossBar.addPlayer(p);
        							}
        							
        							seg=300;
        							seg_max=300;
        							while(seg>0) {
        								Thread.sleep(1000);
        								double progress = seg/seg_max;
            							bossBar.setProgress(progress);
            							bossBar.setTitle(ChatColor.DARK_PURPLE+"RAID no time "+ChatColor.BLUE+"Azul"+ChatColor.DARK_PURPLE+". "+seg+" segundos restantes.");
            							seg--;
        							}
        							Thread.sleep(1000);
        							for(Player p : Bukkit.getOnlinePlayers()){
        								bossBar.removePlayer(p);
        							}
        							
        							Bukkit.broadcastMessage(ChatColor.AQUA+"---------- RAID FINALIZADA ----------");
        							
        							MyListener.raid_blue=false;
        							MyListener.raid_blocked=false;
        						}catch(Exception e) {
        							
        						}
    						}
    					}.start();
    					return true;    					
    				}else {
    					sender.sendMessage(ChatColor.RED+"Não há nada para confirmar.");
    		    		return true;
    				}
    			}else {
    				if(raid_red_c) {
    					new Thread() {
    						public void run() {
    							raid_red_c=false;
    							double seg_max= 60;
    							double seg = 60;
    							BossBar bossBar = Bukkit.createBossBar(ChatColor.AQUA+"Preparação raid no time "+ChatColor.RED+"Vermelho"+ChatColor.AQUA+". 60 segundos restantes.", BarColor.YELLOW, BarStyle.SEGMENTED_20);
    							for(Player p : Bukkit.getOnlinePlayers()){
    								bossBar.addPlayer(p);
    							}
    							
        						bossBar.setProgress(0);
        						MyListener.raid_blocked=true;
        						
        						try {
        							while(seg>0) {
        								Thread.sleep(1000);
        								double progress = seg/seg_max;
            							bossBar.setProgress(progress);
            							bossBar.setTitle(ChatColor.AQUA+"Preparação raid no time "+ChatColor.RED+"Vermelho"+ChatColor.AQUA+". "+seg+" segundos restantes.");
            							seg--;
        							}
        							Thread.sleep(1000);
        							for(Player p : Bukkit.getOnlinePlayers()){
        								bossBar.removePlayer(p);
        							}
        							
        							Thread.sleep(2000);
        							
        							MyListener.raid_red=true;
        							
        							bossBar.setProgress(1);
        							bossBar.setColor(BarColor.RED);
        							bossBar.setTitle(ChatColor.DARK_PURPLE+"RAID no time "+ChatColor.RED+"Vermelho"+ChatColor.DARK_PURPLE+". 300 segundos restantes.");
        							for(Player p : Bukkit.getOnlinePlayers()){
        								bossBar.addPlayer(p);
        							}
        							
        							seg=300;
        							seg_max=300;
        							while(seg>0) {
        								Thread.sleep(1000);
        								double progress = seg/seg_max;
            							bossBar.setProgress(progress);
            							bossBar.setTitle(ChatColor.DARK_PURPLE+"RAID no time "+ChatColor.RED+"Vermelho"+ChatColor.DARK_PURPLE+". "+seg+" segundos restantes.");
            							seg--;
        							}
        							Thread.sleep(1000);
        							for(Player p : Bukkit.getOnlinePlayers()){
        								bossBar.removePlayer(p);
        							}
        							
        							Bukkit.broadcastMessage(ChatColor.AQUA+"---------- RAID FINALIZADA ----------");
        							
        							MyListener.raid_red=false;
        							MyListener.raid_blocked=false;
        						}catch(Exception e) {
        							
        						}
    						}
    					}.start();
    					return true;    		
    				}else {
    					sender.sendMessage(ChatColor.RED+"Não há nada para confirmar.");
    		    		return true;
    				}
    			}
    		}else {
    			sender.sendMessage(ChatColor.RED+"Comando desconhecido, use /raid.");
	    		return true;
    		}
    	}
    	
    	if (command.getName().equalsIgnoreCase("lideres")) {
    		sender.sendMessage("-- Lista de lideres --");
			sender.sendMessage("\n"+ChatColor.BLUE + "[Equipe Azul] " + getLider("blue"));
			sender.sendMessage(ChatColor.RED + "[Equipe Vermelha] "+ getLider("red"));
			return true;
    	}else if (command.getName().equalsIgnoreCase("setbase")) {
    		if(sender instanceof Player) {
    			if(!isLider(((Player) sender).getUniqueId())){
    				sender.sendMessage(ChatColor.RED+"Você deve ser o líder para isso.");
    				return true;
    			}
    			
    			if(args.length!=0) {
        			sender.sendMessage(ChatColor.RED+"Uso incorreto! Use /setbase");
        			return true;
        		}else {
        			Player p = ((Player) sender);
        			String time = getTime(p.getUniqueId());
        			db.statement("update bases set x=" + (int)p.getLocation().getX() + ", y="+ (int)(p.getLocation().getY()+1) + ", z="+ (int)p.getLocation().getZ() +" where time=\""+getTime(p.getUniqueId())+"\"");
        			if(time.equals("red")) {
        				MyListener.base_red = new Coord((int) p.getLocation().getX(), (int) (p.getLocation().getY()+1), (int) p.getLocation().getZ());
        			}else {
        				MyListener.base_blue = new Coord((int) p.getLocation().getX(), (int) (p.getLocation().getY()+1), (int) p.getLocation().getZ());
        			}
        			p.sendMessage(ChatColor.YELLOW+"Base definida.");
        		}
        		
    		}else {
    			return true;
    		}
    		
			return true;
    	}else if (command.getName().equalsIgnoreCase("base")) {
    		if(sender instanceof Player) {
    		}else {
    			return true;
    		}
    		if(args.length!=0) {
    			sender.sendMessage(ChatColor.RED+"Uso incorreto! Use /base");
    			return true;
    		}else {
    			Player p = ((Player) sender);
    			Coord c = getCoord(p.getUniqueId()); 
    			p.teleport(new Location(Bukkit.getWorlds().get(0),c.x,c.y,c.z));
    			p.sendMessage(ChatColor.YELLOW+"Teleportado!.");
    		}
    		
			return true;
    	}else if (command.getName().equalsIgnoreCase("alistados")) {
    		if(sender instanceof Player) {
    		}else {
    			return true;
    		}
    		if(args.length!=0) {
    			sender.sendMessage(ChatColor.RED+"Uso incorreto! Use /alistados");
    			return true;
    		}else {
    			Player p = (Player) sender;
    			String time = getTime(p.getUniqueId());
    			
    			if(dbEmpty(db, "alistados_"+time)) {
    				p.sendMessage(ChatColor.YELLOW+"Não tem ninguém alistado na sua equipe.");
    			}else {
    				String [] alistados = db.getList("alistados_"+time, "user");
    				p.sendMessage(ChatColor.YELLOW+"Lista de alistados da sua equipe:");
    				
    				for (int i = 0; i < alistados.length; i++) {
    					p.sendMessage(ChatColor.YELLOW+"- "+getNameByUUID(UUID.fromString(alistados[i])));
					}
    			}
    			
    			
    		}
    		
			return true;
    	}else if (command.getName().equalsIgnoreCase("alistar")) {
    		if(sender instanceof Player) {
    		}else {
    			return true;
    		}
    		Player p = (Player) sender;
    		if(args.length==0) {
    			String time = getTime(p.getUniqueId());
    			if(playerExistsDB(db, p.getUniqueId().toString(), "alistados_"+time)) {
    				sender.sendMessage(ChatColor.RED+"Você já está alistado.");
    				return true;
    			}
    			
    			db.statement("INSERT INTO alistados_"+time+" (\"user\") values (\""+p.getUniqueId().toString()+"\")");
    			sender.sendMessage(ChatColor.YELLOW+"Você foi alistado.");
    			return true;
    			
    		}else if(args.length==1) {
    			//alistar alguem
    			if(!isLider(((Player) sender).getUniqueId())){
    				sender.sendMessage(ChatColor.RED+"Você deve ser o líder para isso.");
    				return true;
    			}
    			
    			Player p2 = Bukkit.getPlayerExact(args[0]);
        		if(p2==null) {
        			System.out.println(ChatColor.RED+"Player não encontrado.");
        			return true;
        		}
        		
        		String time = getTime(p.getUniqueId());
        		
        		if(!time.equals(getTime(p2.getUniqueId()))){
    				sender.sendMessage(ChatColor.RED+"O Player não é da sua equipe.");
    				return true;
    			}
        		
    			if(playerExistsDB(db, p2.getUniqueId().toString(), "alistados_"+time)) {
    				sender.sendMessage(ChatColor.RED+"O Player já está alistado.");
    				return true;
    			}
    			
    			db.statement("INSERT INTO alistados_"+time+" (\"user\") values (\""+p2.getUniqueId().toString()+"\")");
    			sender.sendMessage(ChatColor.YELLOW+p2.getDisplayName()+" foi alistado.");
    			p2.sendMessage(ChatColor.YELLOW+p.getDisplayName()+" te alistou.");
    			return true;
    		}else {
    			//errou
    			sender.sendMessage(ChatColor.RED+"Uso incorreto! Use /alistar");
    		}
    		
			return true;
    	}  	
    	else if (command.getName().equalsIgnoreCase("recrutar")) {
    		if(sender instanceof Player) {
        		Player p = (Player) sender;
        		
        		if(!isLider(p.getUniqueId())) {
        			p.sendMessage(ChatColor.RED+"Você deve ser o líder para isso.");
        			return true;
        		}
        		if(args.length != 1) {
        			p.sendMessage(ChatColor.RED+"Uso incorreto! Use /recrutar <nick>");
        			return true;
        		}
        			Player p2 = Bukkit.getPlayerExact(args[0]);
        		if(p2==null) {
        			p.sendMessage(ChatColor.RED+"Player não encontrado.");
        			return true;
        		}
        		
        		if(p.getUniqueId().equals(p2.getUniqueId())) {
        			p.sendMessage(ChatColor.RED+"Você não pode se recrutar.");
        			return true;
        		}
        		
        		String time = getTime(p.getUniqueId());
    			
        		db.statement("update times set time=\""+time+"\" where user=\""+p2.getUniqueId()+"\"");
        		if(time.equals("blue")) {
        			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team join Azul "+p2.getName());
        			Bukkit.broadcastMessage(ChatColor.YELLOW+p2.getName()+" agora é da equipe "+ChatColor.BLUE+"Azul"+ChatColor.YELLOW+"!");
        			p2.sendMessage(ChatColor.YELLOW+"Você agora é da equipe "+ChatColor.BLUE+"Azul"+ChatColor.YELLOW+"!");
        		}else {
        			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team join Vermelho "+p2.getName());
        			Bukkit.broadcastMessage(ChatColor.YELLOW+p2.getName()+" agora é da equipe "+ChatColor.RED+"Vermelha"+ChatColor.YELLOW+"!");
        			p2.sendMessage(ChatColor.YELLOW+"Você agora é da equipe "+ChatColor.RED+"Vermelha"+ChatColor.YELLOW+"!");
        		}
        		
        		
    		}else {
    			if(args.length != 2) {
        			System.out.println(ChatColor.RED+"Uso incorreto! Use /recrutar <nick> <time>");
        			return true;
        		}
        			Player p2 = Bukkit.getPlayerExact(args[0]);
        		if(p2==null) {
        			System.out.println(ChatColor.RED+"Player não encontrado.");
        			return true;
        		}
        		
        		String time = args[1];
    			
        		db.statement("update times set time=\""+time+"\" where user=\""+p2.getUniqueId()+"\"");
        		if(time.equals("blue")) {
        			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team join Azul "+p2.getName());
        			Bukkit.broadcastMessage(ChatColor.YELLOW+p2.getName()+" agora é da equipe "+ChatColor.BLUE+"Azul"+ChatColor.YELLOW+"!");
        			p2.sendMessage(ChatColor.YELLOW+"Você agora é da equipe "+ChatColor.BLUE+"Azul"+ChatColor.YELLOW+"! Use /base para ir até a base.");
        		}else {
        			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team join Vermelho "+p2.getName());
        			Bukkit.broadcastMessage(ChatColor.YELLOW+p2.getName()+" agora é da equipe "+ChatColor.RED+"Vermelha"+ChatColor.YELLOW+"!");
        			p2.sendMessage(ChatColor.YELLOW+"Você agora é da equipe "+ChatColor.RED+"Vermelha"+ChatColor.YELLOW+"! Use /base para ir até a base.");
        		}
        		
    			
    			return true;
    		}
			return true;
    	}
    	return false; 
    }
    
    Block[] getBlockFromRadius(World world, int radius, int x, int y, int z) {
		int xMax = x+radius;
		int zMax = z+radius;
		int xMin = x-radius;
		int zMin = z-radius;
		
		ArrayList<Coord> c = new ArrayList<Coord>();
		
		for (int i = xMin; i <= xMax; i++) {
			c.add(new Coord(i, y, zMin));
			c.add(new Coord(i, y, zMax));
		}
		for (int i = zMin; i <= zMax; i++) {
			c.add(new Coord(xMin, y, i));
			c.add(new Coord(xMax, y, i));
		}
		
		Block[] b = new Block[c.size()];
		for (int i = 0; i < c.size(); i++) {
			b[i]=world.getBlockAt(c.get(i).x,c.get(i).y,c.get(i).z);
		}
		b = removeDuplicates(b);
		return b;
	}
    
    private Block[] removeDuplicates(Block[] i2) {
    		ArrayList<Block> antiga = new ArrayList<Block>();
    		ArrayList<Block> unicos = new ArrayList<Block>();
    		
    		for (int i = 0; i < i2.length; i++) {
    			antiga.add(i2[i]);
    		}
    		
    		for (int i = 0; i < antiga.size(); i++) {
    			Block compare = i2[i];
    			boolean unico = true;
    			for (int j = 0; j < unicos.size(); j++) {
    				if(compare.equals(unicos.get(j))) {
    					unico = false;
    				}
    			}
    			if(unico) {
    				unicos.add(compare);
    			}
    			
    			
    		}
    		return unicos.toArray(new Block[0]);
    	}

	String[] getListaTimeOnline(String time) {
    	ArrayList<String> arr = new ArrayList<String>();
    	ArrayList<String> arr2 = new ArrayList<String>();
		try {
			ResultSet rs = db.returnStatement("SELECT * FROM times where time=\""+time+"\"");
			while (rs.next()) {
				  arr.add(rs.getString("display"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < arr.size(); i++) {
			Player p2 = Bukkit.getPlayerExact(arr.get(i));
    		if(p2==null) {}else {
    			arr2.add(arr.get(i));
    		}
		}
		
		return arr2.toArray(new String[0]);
    }
    public String getLider(String equipe) {
    	ResultSet rs = null;
		try {
			 rs = db.returnStatement("SELECT display FROM times WHERE time=\""+equipe+"\" AND lider=1");
			 if(rs.isClosed()) {
				 return null;
			 }
			 
			 return rs.getString("display");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
    }
    
    
    
    public String getNameByUUID(UUID id) {
    	//System.out.println(id);
    	if(id==null) {
    		return "null";
    	}else {
    		return Bukkit.getOfflinePlayer(UUID.fromString(id.toString())).getName();
    	}
    	
    }
    
    public String getTime(UUID uuid) {
    	ResultSet rs = null;
		try {
			 rs = db.returnStatement("select * from times where user = \""+uuid+"\"");
			 if(rs.isClosed()) {
				 return null;
			 }
			 
			 return rs.getString("time");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
    }
    
    public boolean playerExistsDB(Database db,String uuid, String table) {
		try {
			ResultSet rs = db.returnStatement("select count(*) from "+table+" where user = \""+uuid+"\"");
			int n = 0;
			if ( rs.next() ) {
				n = rs.getInt(1);
			}
			if ( n > 0 ) {
			   return true;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
    
    public boolean dbEmpty(Database db, String table) {
		try {
			ResultSet rs = db.returnStatement("select count(*) from "+table);
			int n = 0;
			if ( rs.next() ) {
				n = rs.getInt(1);
			}
			if ( n == 0 ) {
			   return true;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
    
    public Coord getCoord(UUID uuid) {
    	ResultSet rs = null;
		try {
			 rs = db.returnStatement("select * from bases where time = \""+getTime(uuid)+"\"");
			 if(rs.isClosed()) {
				 return null;
			 }
			 int x = rs.getInt("x");
			 int y = rs.getInt("y");
			 int z = rs.getInt("z");
			 return new Coord(x,y,z);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
    }
	
	public boolean isLider(UUID uuid) {
    	ResultSet rs = null;
		try {
			 rs = db.returnStatement("select * from times where user = \""+uuid+"\"");
			 if(rs.isClosed()) {
				 return false;
			 }
			 
			 if(rs.getInt("lider")==0) {
				 return false;
			 }else{
				 return true;
			 }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
    }
	Block getBlockReal(World w, int x, int y, int z) {
		Block b = w.getBlockAt(x, y, z);
		if (b.getType().equals(Material.AIR)) {
			do {
				b = w.getBlockAt(b.getX(), (b.getY()-1), b.getZ());
			}while(b.getType().equals(Material.AIR));
		}else {
			do {
				b = w.getBlockAt(b.getX(), (b.getY()+1), b.getZ());
			}while(!b.getType().equals(Material.AIR));
			b = w.getBlockAt(b.getX(), (b.getY()-1), b.getZ());
		}
		return b;
	}
}