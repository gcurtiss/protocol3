package protocol3.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.TextComponent;
import protocol3.backend.PlayerMeta;
import protocol3.backend.PlayerMeta.MuteType;

// Mute somebody. OPs only.

public class Mute implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		String name = sender.getName();
		
		if (!PlayerMeta.isOp(sender)) {
			sender.sendMessage("§cYou can't use this.");
			return true;
		}

		if (args.length < 1) {
			sender.sendMessage("§cInvalid syntax. Syntax: /mute <perm/temp/none/all> [player]");
			return true;
		}


		String mode = args[0];
		if (mode.equals("all")) {
			PlayerMeta.MuteAll = !PlayerMeta.MuteAll;
			Bukkit.getServer().spigot()
					.broadcast(PlayerMeta.MuteAll ?
							new TextComponent("§4The chat is now silenced.") :
							new TextComponent("§aThe chat is no longer silenced."));
			return true;
		}

		Player toMute = Bukkit.getPlayer(args[1]);
		if (toMute == null) {
			sender.sendMessage("§cPlayer is not online.");
			return true;
		}
		if (toMute.isOp()) {
			sender.sendMessage("§cYou can't mute this person.");
			return true;
		}

		switch (mode.toUpperCase()) {
			case "PERM":
				if(PlayerMeta.isMuted(toMute)) {
					sender.spigot().sendMessage(new TextComponent("§cPlayer is already muted."));
					break;
				}
				Bukkit.getServer().spigot().broadcast(new TextComponent("§4§l" + toMute.getName() + " §r§4is now permanently muted."));
				PlayerMeta.setMuteType(toMute, MuteType.PERMANENT);
				break;
			case "TEMP":
				if(PlayerMeta.isMuted(toMute)) {
					sender.spigot().sendMessage(new TextComponent("§cPlayer is already muted."));
					break;
				}
				Bukkit.getServer().spigot().broadcast(new TextComponent("§c§l" + toMute.getName() + " §r§cis now temporarily muted."));
				PlayerMeta.setMuteType(toMute, MuteType.TEMPORARY);
				break;
			case "NONE":
				if(!PlayerMeta.isMuted(toMute)) {
					sender.spigot().sendMessage(new TextComponent("§cPlayer isn't muted."));
					break;
				}
				PlayerMeta.setMuteType(toMute, MuteType.NONE);
				break;
			case "IP":
				if(PlayerMeta.isMuted(toMute)) {
					sender.spigot().sendMessage(new TextComponent("§cIP is already muted."));
					break;
				}
				Bukkit.getServer().spigot().broadcast(new TextComponent("§4§l" + toMute.getName() + " §r§4is now permanently muted."));
				PlayerMeta.setMuteType(toMute, MuteType.IP);
				break;
			default:
				sender.sendMessage("§cInvalid syntax. Syntax: /mute <perm/temp/ip/none/all> [player]");
				return true;
		}
		return true;
	}

}
