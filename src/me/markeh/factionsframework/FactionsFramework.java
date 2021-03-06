/*
 * 
 *   FactionsFramework is apart of FactionsPlus.
 *   
 *   FactionsPlus extends on-top of Factions to provide more features.
 *   Copyright (C) 2015  Mark Hughes
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.markeh.factionsframework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import me.markeh.factionsframework.command.FactionsCommandManager;
import me.markeh.factionsframework.events.listeners.FFListenerFactions1_6UUID;
import me.markeh.factionsframework.events.listeners.FFListenerFactions2_6;
import me.markeh.factionsframework.events.listeners.FFListenerFactions2_X;
import me.markeh.factionsframework.events.listeners.FFListenerGlobal;
import me.markeh.factionsframework.factionsmanager.FactionsManager;
import me.markeh.factionsframework.factionsmanager.FactionsVersion;
import me.markeh.factionsframework.objs.NotifyEvent;

public class FactionsFramework {
	
	// ------------------------------
	//  Singleton
	// ------------------------------
	
	private static FactionsFramework instance;
	protected FactionsFramework(JavaPlugin plugin) {
		plugins.add(plugin);
		if(parent == null) parent = plugin;
	}
	
	public static FactionsFramework get(JavaPlugin plugin) { 
		if(instance == null) instance = new FactionsFramework(plugin);
		
		return instance;
	}
	
	public static FactionsFramework get() { 
		if(instance == null) instance = new FactionsFramework(null);
		
		return instance;
	}
	
	// ------------------------------
	//  Fields
	// ------------------------------
	
	private FactionsManager factionsManager = null;
	
	private Listener listener = null;
	private Boolean isSetup = false;
	
	private JavaPlugin parent = null;
	private List<JavaPlugin> plugins = new ArrayList<JavaPlugin>();
	
	// ------------------------------
	//  Methods
	// ------------------------------
	
	// Get the parent plugin
	public JavaPlugin getParent() {
		return this.parent;
	}
	
	// Ensure our listeners are enabled etc
	public final void ensureSetup() {
		try { 
			if (isSetup) return;
			
			factionsManager = FactionsManager.get();
			
			if (listener == null) {
				if (factionsManager.determineVersion() == FactionsVersion.FactionsUUID) listener = new FFListenerFactions1_6UUID();
				if (factionsManager.determineVersion() == FactionsVersion.Factions2_6) listener = new FFListenerFactions2_6();
				if (factionsManager.determineVersion() == FactionsVersion.Factions2_X || factionsManager.determineVersion() == FactionsVersion.Factions2_8_6) listener = new FFListenerFactions2_X();
				
				if (listener == null) {
					Bukkit.getLogger().log(Level.SEVERE, "[FactionsFramework] FactionsFramework can not work out your Factions version.");
					Bukkit.getLogger().log(Level.SEVERE, "[FactionsFramework] FactionsFramework did not register our listener.");	
				}
			}
			
			if (listener != null) {
				// If the parent is there we will check if it's still useable 
				if (parent != null) {
					// if its not enabled ... 
					if ( ! Bukkit.getServer().getPluginManager().isPluginEnabled(parent)) {
						// remove it from the plugins list
						plugins.remove(parent);
						// set parent to null
						parent = null;
					}
				}
				
				if (parent == null && this.plugins.size() > 0) {
					for (JavaPlugin plugin : this.plugins) {
						if ( ! plugin.isEnabled()) {
							plugins.remove(parent);
						} else {
							parent = plugin;
						}
					}
				} 
				
				if(parent == null) throw new Error("No parents! Pass a plugin to FactionsFramework.get() so we have a parent.");
				
				HandlerList.unregisterAll(listener);
				
				Bukkit.getServer().getPluginManager().registerEvents(listener, parent);
				Bukkit.getLogger().log(Level.INFO, "[FactionsFramework] FactionsFramework has set its listener parent to " + parent.getName());
			}
			
			// Global Listener that is factions specific events 
			FFListenerGlobal globalListener = new FFListenerGlobal();
			Bukkit.getServer().getPluginManager().registerEvents(globalListener, parent);
	
			// Schedule tasks only start running when all plugins are loaded 
			parent.getServer().getScheduler().scheduleSyncDelayedTask(parent, new Runnable() {
				@Override
				public void run() {
					// Notify our services of things
					FactionsCommandManager.get().notify(NotifyEvent.Loaded);
					FactionsManager.get().notify(NotifyEvent.Loaded);
				}
			});
		} catch (Throwable e) {
			this.logError(e);
		}
	}
	
	// Simple list of tested and supported versions, should be checked with .startsWith() 
	public final List<String> getSupported() {
		List<String> supported = new ArrayList<String>();
		
		// Original Factions
		supported.add("2.5");
		supported.add("2.6");
		supported.add("2.7");
		supported.add("2.8");
		
		// Factions 1.6 UUID
		supported.add("1.6.9.5-U");
		
		return supported;
	}
	
	public final void logError(Throwable e) {
		// TODO: move to NIO instead of older file crap 
		File logFolder = new File(this.parent.getDataFolder(), "FactionsFramework");
		
		if ( ! logFolder.exists()) logFolder.mkdir();
		
		File errorLog = new File(logFolder, new Date().getTime() + ".errorlog");
		
		if ( ! logFolder.exists()) logFolder.mkdirs();
		
		PrintWriter writer = null;
		
		try {
			writer = new PrintWriter(errorLog, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e2) {
			e2.printStackTrace();
		} finally { 

			log(ChatColor.RED + "[Error]" + ChatColor.DARK_PURPLE + " Oh no, an internal error has occurred! :-(");
			e.printStackTrace();

			if (writer == null) return;
			log(ChatColor.RED + "[Error]" + ChatColor.DARK_PURPLE + " It has been saved to " + errorLog.getPath());
			log(ChatColor.RED + "[Error]" + ChatColor.DARK_PURPLE + " Please upload to pastebin.com and include in any error reports.");

			writer.println("----------------------------------------");
			writer.println("Error Log started on " + new Date().toString());
			writer.println("----------------------------------------");
			writer.println("Server Version: " + parent.getServer().getVersion());
			writer.println("Supported Versions: " + this.getSupported().toString());
			
			try {
				writer.println("Factions Version: " + parent.getServer().getPluginManager().getPlugin("Factions").getDescription().getVersion());
			} catch(Exception e3) { }

			try {
				writer.println("MassiveCore Version: " + parent.getServer().getPluginManager().getPlugin("MassiveCore").getDescription().getVersion());
			} catch(Exception e3) { }
			
			try {
				writer.println("MCore Version: " + parent.getServer().getPluginManager().getPlugin("MCore").getDescription().getVersion());
			} catch(Exception e3) { }
			
			writer.println("----------------------------------------");
			writer.println("Error:" + e.getMessage());
			writer.println("----------------------------------------");
			
			e.printStackTrace(writer);
			
			writer.println("----------------------------------------");
							
			for (Player p : parent.getServer().getOnlinePlayers()) if (p.isOp()) p.sendMessage(ChatColor.RED + "An internal error has occured inside FactionsFramework (part of FactionsPlus). Please check console.");	
		}
		
		writer.close();
	}
	
	// log to console 
	private void log(String msg) {
		Bukkit.getServer().getConsoleSender().sendMessage("" + ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "[FactionsFramework]" + ChatColor.RESET + " " + ChatColor.WHITE + this.colourise(msg));
	}
	
	// colourise a string 
	public String colourise(String msg) {
		for (ChatColor colour : ChatColor.values()) {
			msg = msg.replace("<"+colour.name().toLowerCase()+">", colour+"");
		}
		
		return msg;
	}
	
	// Get Factions version as a String
	public String getFactionsVersion() {
		return parent.getServer().getPluginManager().getPlugin("Factions").getDescription().getVersion();
	}
}
