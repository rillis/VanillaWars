package com.rillis;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class MyListener implements Listener{
	static Coord base_red;
	static Coord base_blue;
	static boolean raid_blocked = false;
	static boolean raid_blue = false;
	static boolean raid_red = false;
	Database db;
	
	public MyListener(Database db) {
		this.db=db;
		Coord c = null;
		c = getCoord("blue"); 
		base_blue = c;
		c = getCoord("red");
		base_red = c;
	}
	
	public Coord getCoord(String time) {
    	ResultSet rs = null;
		try {
			 rs = db.returnStatement("select * from bases where time = \""+time+"\"");
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
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Location l = event.getPlayer().getBedSpawnLocation();
		if(l==null) {
			event.setRespawnLocation(new Location(Bukkit.getWorlds().get(0),9999,63,10000));
			event.getPlayer().teleport(new Location(Bukkit.getWorlds().get(0),9999,63,10000));
		}
	}
	
	@EventHandler 
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player p = event.getPlayer();
		String msg = event.getMessage();
		
		event.setCancelled(true);
		
		String time = getTime(p.getUniqueId());
		if(time.equals("blue")) {
			
			String mensagem = ChatColor.BLUE+"[Azul] "+p.getDisplayName()+ChatColor.WHITE+": "+msg;
			if(isLider(p.getUniqueId())) {
				mensagem = ChatColor.GOLD+"[Líder] "+mensagem;
			}
			p.sendMessage(mensagem);
			
			for(Player p2 : Bukkit.getOnlinePlayers()){
				if(getTime(p2.getUniqueId()).equals(time) && !p2.getUniqueId().equals(p.getUniqueId())) {
					p2.sendMessage(mensagem);
				}
			}
		}else if(time.equals("red")) {
			String mensagem = ChatColor.RED+"[Vermelho] "+p.getDisplayName()+ChatColor.WHITE+": "+msg;
			if(isLider(p.getUniqueId())) {
				mensagem = ChatColor.GOLD+"[Líder] "+mensagem;
			}
			p.sendMessage(mensagem);
			
			for(Player p2 : Bukkit.getOnlinePlayers()){
				if(getTime(p2.getUniqueId()).equals(time) && !p2.getUniqueId().equals(p.getUniqueId())) {
					p2.sendMessage(mensagem);
				}
			}
		}else{
			Bukkit.broadcastMessage(ChatColor.WHITE+"[Sem time] "+p.getDisplayName()+": "+msg);
		}
		
	}
	
	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
		event.getPlayer().sendMessage("Bem-vindo "+event.getPlayer().getDisplayName()+".");
		
		if(!playerExistsDB(db, event.getPlayer().getUniqueId().toString(),"times")) {
			db.statement("INSERT INTO times (\"time\",\"user\",\"display\") values (\"none\", \""+event.getPlayer().getUniqueId().toString()+"\",\""+event.getPlayer().getName()+"\")");
		}
		
		String time = getTime(event.getPlayer().getUniqueId());
		if(time.equals("none")) {
			event.getPlayer().setBedSpawnLocation(new Location(Bukkit.getWorlds().get(0),9999,63,10000));
			event.getPlayer().teleport(new Location(Bukkit.getWorlds().get(0),9999,63,10000));
			event.getPlayer().sendMessage("Identifiquei que você não está em um time :(. Contate um lider. (/lideres)");
		}else if(time.equals("red")) {
			event.getPlayer().sendMessage(ChatColor.RED+"Você está no time vermelho!");
		}else {
			event.getPlayer().sendMessage(ChatColor.BLUE+"Você está no time azul!");
		}

		if(isLider(event.getPlayer().getUniqueId())) {
			event.getPlayer().sendMessage(ChatColor.GOLD+"-Você é o líder da sua equipe.-");
		}
		
		
		
		
    }
	
	@EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
		Player p = event.getPlayer();
		
		String time = getTime(p.getUniqueId());
		
		if(playerExistsDB(db, p.getUniqueId().toString(), "alistados_"+time)) {
			db.statement("DELETE FROM alistados_"+time+" WHERE user=\""+p.getUniqueId()+"\"");
		}
    }
	
	@EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent event){
		try {
		if(!event.getInventory().getType().equals(InventoryType.CARTOGRAPHY)) {
		
			int x = (int) event.getInventory().getLocation().getX();
			int z = (int) event.getInventory().getLocation().getZ();
			
			int red_x = (int) base_red.x;
			int red_z = (int) base_red.z;
			
			int blue_x = (int) base_blue.x;
			int blue_z = (int) base_blue.z;
			
			if((x<=red_x+50 && x>=red_x-50) && (z<=red_z+50 && z>=red_z-50)) {
				//red
				if (event.getInventory().getType().equals(InventoryType.CHEST) || event.getInventory().getType().equals(InventoryType.BARREL)  || event.getInventory().getType().equals(InventoryType.BLAST_FURNACE) || event.getInventory().getType().equals(InventoryType.SHULKER_BOX) || event.getInventory().getType().equals(InventoryType.FURNACE) || event.getInventory().getType().equals(InventoryType.ENDER_CHEST)){
					if(!getTime(event.getPlayer().getUniqueId()).contentEquals("red") && !raid_red) {
						event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
						event.setCancelled(true);
					}else if(raid_red && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("red")) {
						event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
						event.setCancelled(true);
					}
		        }
			}
			if((x<=blue_x+50 && x>=blue_x-50) && (z<=blue_z+50 && z>=blue_z-50)) {
				//blue
				if (event.getInventory().getType().equals(InventoryType.CHEST) || event.getInventory().getType().equals(InventoryType.BARREL)  || event.getInventory().getType().equals(InventoryType.BLAST_FURNACE)  || event.getInventory().getType().equals(InventoryType.SHULKER_BOX) || event.getInventory().getType().equals(InventoryType.FURNACE) || event.getInventory().getType().equals(InventoryType.ENDER_CHEST)){
					if(!getTime(event.getPlayer().getUniqueId()).contentEquals("blue") && !raid_blue) {
						event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
						event.setCancelled(true);
					}else if(raid_blue && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("blue")) {
						event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
						event.setCancelled(true);
					}
				}
			}
		
		}
		}catch(Exception e) {}
    }
	
	@EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
		String time = getTime(event.getPlayer().getUniqueId());
		
		if(time.equals("none")) {
			event.setCancelled(true);
		}
		
		int x = (int) event.getBlock().getX();
		int z = (int) event.getBlock().getZ();
		
		int red_x = (int) base_red.x;
		int red_z = (int) base_red.z;
		
		int blue_x = (int) base_blue.x;
		int blue_z = (int) base_blue.z;
		
		if((x<=red_x+50 && x>=red_x-50) && (z<=red_z+50 && z>=red_z-50)) {
			//red
			if(!getTime(event.getPlayer().getUniqueId()).contentEquals("red") && !raid_red) {
				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
				event.setCancelled(true);
			}else if(raid_red && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("red")) {
				event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
				event.setCancelled(true);
			}
		}
		if((x<=blue_x+50 && x>=blue_x-50) && (z<=blue_z+50 && z>=blue_z-50)) {
			//blue
			if(!getTime(event.getPlayer().getUniqueId()).contentEquals("blue") && !raid_blue) {
				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
				event.setCancelled(true);
			}else if(raid_blue && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("blue")) {
				event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
				event.setCancelled(true);
			}
		}
    }
	
	@EventHandler
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		int x = (int) event.getBlock().getX();
		int z = (int) event.getBlock().getZ();
		
		int red_x = (int) base_red.x;
		int red_z = (int) base_red.z;
		
		int blue_x = (int) base_blue.x;
		int blue_z = (int) base_blue.z;
		
		if((x<=red_x+50 && x>=red_x-50) && (z<=red_z+50 && z>=red_z-50)) {
			//red
			if(!getTime(event.getPlayer().getUniqueId()).contentEquals("red") && !raid_red) {
				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
				event.setCancelled(true);
			}else if(raid_red && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("red")) {
				event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
				event.setCancelled(true);
			}
		}
		if((x<=blue_x+50 && x>=blue_x-50) && (z<=blue_z+50 && z>=blue_z-50)) {
			//blue
			if(!getTime(event.getPlayer().getUniqueId()).contentEquals("blue") && !raid_blue) {
				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
				event.setCancelled(true);
			}else if(raid_blue && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("blue")) {
				event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
    public void onBlockBreak(BlockBreakEvent event){
		String time = getTime(event.getPlayer().getUniqueId());
		
		if(time.equals("none")) {
			event.setCancelled(true);
		}
		
		int x = (int) event.getBlock().getX();
		int z = (int) event.getBlock().getZ();
		
		int red_x = (int) base_red.x;
		int red_z = (int) base_red.z;
		
		int blue_x = (int) base_blue.x;
		int blue_z = (int) base_blue.z;
		
		if((x<=red_x+50 && x>=red_x-50) && (z<=red_z+50 && z>=red_z-50)) {
			//red
			if(!getTime(event.getPlayer().getUniqueId()).contentEquals("red") && !raid_red) {
				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
				event.setCancelled(true);
			}else if(raid_red && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("red")) {
				event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
				event.setCancelled(true);
			}
		}
		if((x<=blue_x+50 && x>=blue_x-50) && (z<=blue_z+50 && z>=blue_z-50)) {
			//blue
			if(!getTime(event.getPlayer().getUniqueId()).contentEquals("blue") && !raid_blue) {
				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
				event.setCancelled(true);
			}else if(raid_blue && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("blue")) {
				event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
				event.setCancelled(true);
			}
		}
    }
	
	@EventHandler
    public void onPlayerDoorOpen(PlayerInteractEvent event)
    {
        Action action = event.getAction();
        Block clicked = event.getClickedBlock();
             
        //Left or Right click?
        if ((action == Action.RIGHT_CLICK_BLOCK) || (action == Action.LEFT_CLICK_BLOCK))
        {
            //Door Block?
            if((clicked.getType() == Material.IRON_DOOR) ||  (clicked.getType() == Material.OAK_DOOR) || (clicked.getType() == Material.ACACIA_DOOR) || (clicked.getType() == Material.DARK_OAK_DOOR) ||(clicked.getType() == Material.BIRCH_DOOR) ||(clicked.getType() == Material.JUNGLE_DOOR) ||(clicked.getType() == Material.SPRUCE_DOOR) ||(clicked.getType() == Material.OAK_TRAPDOOR) ||(clicked.getType() == Material.ACACIA_TRAPDOOR) ||(clicked.getType() == Material.DARK_OAK_TRAPDOOR) ||(clicked.getType() == Material.BIRCH_TRAPDOOR) ||(clicked.getType() == Material.JUNGLE_TRAPDOOR) || (clicked.getType() == Material.SPRUCE_TRAPDOOR) ||(clicked.getType() == Material.OAK_FENCE_GATE) ||(clicked.getType() == Material.ACACIA_FENCE_GATE) ||(clicked.getType() == Material.DARK_OAK_FENCE_GATE) ||(clicked.getType() == Material.BIRCH_FENCE_GATE) ||(clicked.getType() == Material.JUNGLE_FENCE_GATE) ||(clicked.getType() == Material.SPRUCE_FENCE_GATE)||(clicked.getType() == Material.LEVER)||(clicked.getType() == Material.STONE_BUTTON)||(clicked.getType() == Material.STONE_PRESSURE_PLATE)||(clicked.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE)||(clicked.getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE)||(clicked.getType() == Material.ACACIA_BUTTON)||(clicked.getType() == Material.DARK_OAK_BUTTON)||(clicked.getType() == Material.OAK_BUTTON)||(clicked.getType() == Material.SPRUCE_BUTTON)||(clicked.getType() == Material.BIRCH_BUTTON)||(clicked.getType() == Material.JUNGLE_BUTTON)||(clicked.getType() == Material.ACACIA_PRESSURE_PLATE)||(clicked.getType() == Material.OAK_PRESSURE_PLATE)||(clicked.getType() == Material.DARK_OAK_PRESSURE_PLATE)||(clicked.getType() == Material.JUNGLE_PRESSURE_PLATE)||(clicked.getType() == Material.BIRCH_PRESSURE_PLATE)||(clicked.getType() == Material.SPRUCE_PRESSURE_PLATE))
            {
            	
            	int x = (int) clicked.getX();
        		int z = (int) clicked.getZ();
        		
        		int red_x = (int) base_red.x;
        		int red_z = (int) base_red.z;
        		
        		int blue_x = (int) base_blue.x;
        		int blue_z = (int) base_blue.z;
        		
        		if((x<=red_x+50 && x>=red_x-50) && (z<=red_z+50 && z>=red_z-50)) {
        			//red
        			if(!getTime(event.getPlayer().getUniqueId()).contentEquals("red") && !raid_red) {
        				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
        				event.setCancelled(true);
        			}else if(raid_red && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("red")) {
        				event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
        				event.setCancelled(true);
        			}
        		}
            	if((x<=blue_x+50 && x>=blue_x-50) && (z<=blue_z+50 && z>=blue_z-50)) {
        			//blue
        			if(!getTime(event.getPlayer().getUniqueId()).contentEquals("blue") && !raid_blue) {
        				event.getPlayer().sendMessage(ChatColor.RED+"Você não pode fazer isso no território inimigo sem estar em RAID.");
        				event.setCancelled(true);
        			}else if(raid_blue && !playerAlisted(event.getPlayer().getUniqueId()) && !getTime(event.getPlayer().getUniqueId()).contentEquals("blue")) {
        				event.getPlayer().sendMessage(ChatColor.RED+"Seu time está em RAID mas você não está alistado.");
        				event.setCancelled(true);
        			}
        		}
             
            }
            else{    }
        }
        else{    }
    }
	
	public Block getSecondChest(String facing, BlockBreakEvent event) {
		double x = event.getBlock().getX();
		double y = event.getBlock().getY();
		double z = event.getBlock().getZ();
		if(event.getBlock().toString().contains("type=left")){
			if(facing.equals("north")) {
				return event.getPlayer().getWorld().getBlockAt((int)x+1,(int)y,(int)z);
			}
			if(facing.equals("south")) {
				return event.getPlayer().getWorld().getBlockAt((int)x-1,(int)y,(int)z);
			}
			if(facing.equals("west")) {
				return event.getPlayer().getWorld().getBlockAt((int)x,(int)y,(int)z-1);
			}
			if(facing.equals("east")) {
				return event.getPlayer().getWorld().getBlockAt((int)x,(int)y,(int)z+1);
			}
		}else {
			if(facing.equals("north")) {
				return event.getPlayer().getWorld().getBlockAt((int)x-1,(int)y,(int)z);
			}
			if(facing.equals("south")) {
				return event.getPlayer().getWorld().getBlockAt((int)x+1,(int)y,(int)z);
			}
			if(facing.equals("west")) {
				return event.getPlayer().getWorld().getBlockAt((int)x,(int)y,(int)z+1);
			}
			if(facing.equals("east")) {
				return event.getPlayer().getWorld().getBlockAt((int)x,(int)y,(int)z-1);
			}
		}
		return null;
	}
	
	
	private boolean playerAlisted(UUID uuid) {
		boolean fim = false;
		try {
			ResultSet rs = db.returnStatement("select count(*) from alistados_red where user = \""+uuid+"\"");
			int n = 0;
			if ( rs.next() ) {
				n = rs.getInt(1);
			}
			if ( n > 0 ) {
			   fim = true;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			ResultSet rs = db.returnStatement("select count(*) from alistados_blue where user = \""+uuid+"\"");
			int n = 0;
			if ( rs.next() ) {
				n = rs.getInt(1);
			}
			if ( n > 0 ) {
			   fim = true;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fim;
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
	
}
