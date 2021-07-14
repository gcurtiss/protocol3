package protocol3.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.TextComponent;
import protocol3.backend.Config;
import protocol3.backend.PlayerMeta;
import protocol3.backend.PlayerMeta.MuteType;

// Vote mute

public class VoteMute implements CommandExecutor {
	static HashMap<UUID, Integer> _votes = new HashMap<UUID, Integer>();
	static HashMap<UUID, List<UUID>> _voters = new HashMap<UUID, List<UUID>>();
	static HashMap<String, List<UUID>> _voterIps = new HashMap<String, List<UUID>>();
	public static int cooldown = 0;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (args.length != 1) {
			sender.spigot().sendMessage(new TextComponent("§cInvalid syntax. Syntax: /vm [player]"));
			return true;
		}

		if (Bukkit.getPlayer(args[0]) == null) {
			sender.spigot().sendMessage(new TextComponent("§cPlayer is not online."));
			return true;
		}
		
		if(Config.getValue("mute.enabled").equals("false")) {
			sender.spigot().sendMessage(new TextComponent("§cThis command has been disabled by your server administrator."));
			return true;
		}
		
		if(!canVoteMute()) {
			sender.spigot().sendMessage(new TextComponent("§cThis command is disabled because "+Config.getValue("mute.max")+ "% of the server or more is muted. "
					+ "If you believe there is enough reason for another mute, contact your server administrator."));
			return true;
		}

		Player voter = (Player) sender;
		Player toMute = Bukkit.getPlayer(args[0]);
		int popNeeded = (int) (Bukkit.getOnlinePlayers().size()
				* (Float.parseFloat(Config.getValue("mute.pop")) / 100.0f));

		// [1:01 AM] d2k11: if((20% of pop) < 1) message("no votey not enough")
		// [1:02 AM] suicide is badass: make sure it announces that message too
		// [1:02 AM] suicide is badass: verbatim
		if (popNeeded <= 1) {
			int rnd = (int) ((Math.random() * (5 - 0)) + 0);
			if (rnd == 3) {
				voter.spigot().sendMessage(new TextComponent("§cno votey not enough"));
			} else {
				voter.spigot()
						.sendMessage(new TextComponent("§cThere are not enough players online to conduct a vote."));
			}
			return true;
		}

		// Can't vote on an already-muted person.
		if (PlayerMeta.isMuted(toMute)) {
			voter.spigot().sendMessage(new TextComponent("§cPlayer is already muted."));
			return true;
		}

		// Muted people can't vote.
		if ((PlayerMeta.isMuted(voter) && Config.getValue("mute.hypocrisy").equals("1")) || PlayerMeta.isLagfag(voter)) {
			voter.spigot().sendMessage(new TextComponent("§cYou can't vote."));
			return true;
		}

		// Can't mute ops
		if (toMute.isOp()) {
			voter.spigot().sendMessage(new TextComponent("§cYou can't vote to mute this person."));
			return true;
		}

		// List of previous votes to be filled.
		List<UUID> previousVotes;

		// List of previous IP votes
		List<UUID> previousIpVotes;

		// Load previous votes
		if (_voters.containsKey(voter.getUniqueId())) {
			previousVotes = _voters.get(voter.getUniqueId());
			if (previousVotes.contains(toMute.getUniqueId())) {
				int votes = _votes.get(toMute.getUniqueId());
				int need = popNeeded - votes;
				if (need <= 0) {
					Bukkit.spigot().broadcast(new TextComponent(
							"§6The vote to mute " + toMute.getName() + " passed after " + popNeeded + " votes."));
					_votes.remove(toMute.getUniqueId());
					previousVotes.remove(toMute.getUniqueId());
					PlayerMeta.setMuteType(toMute, MuteType.TEMPORARY);
					return true;
				} else {
					voter.spigot().sendMessage(new TextComponent(
							"§cYou've already voted for " + toMute.getName() + ". You need " + need + " more votes."));
				}
				return true;
			}
		}
		// Create new list of votes
		else {
			previousVotes = new ArrayList<UUID>();
		}

		// Load previous IP votes
		if (_voterIps.containsKey(voter.getAddress().toString())) {
			previousIpVotes = _voterIps.get(voter.getAddress().toString());
			if (previousIpVotes.contains(toMute.getUniqueId())) {
				int votes = _votes.get(toMute.getUniqueId());
				int need = popNeeded - votes;
				if (need <= 0) {
					Bukkit.spigot().broadcast(new TextComponent(
							"§6The vote to mute " + toMute.getName() + " passed after " + popNeeded + " votes."));
					_votes.remove(toMute.getUniqueId());
					previousVotes.remove(toMute.getUniqueId());
					PlayerMeta.setMuteType(toMute, MuteType.TEMPORARY);
					return true;
				} else {
					voter.spigot().sendMessage(new TextComponent(
							"§cYou've already voted for " + toMute.getName() + ". You need " + need + " more votes."));
				}
				return true;
			}
		}

		// Create new list of IP votes
		else {
			previousIpVotes = new ArrayList<UUID>();
		}
		if (((Player) sender).getUniqueId().toString().equals(toMute.getUniqueId().toString())) {
			sender.spigot().sendMessage(new TextComponent("§cI don't think its a good idea to vote mute yourself."));
			return true;
		}

		// If our candidate has been voted on before
		if (_votes.containsKey(toMute.getUniqueId())) {
			// New votes value
			int votes = _votes.get(toMute.getUniqueId()) + 1;
			// Put our vote into repository
			_votes.put(toMute.getUniqueId(), votes);
			// Flag that we've voted for this person and can't vote again.
			previousVotes.add(toMute.getUniqueId());
			_voters.put(voter.getUniqueId(), previousVotes);
			// Flag that this IP has voted for this person and can't vote again.
			previousIpVotes.add(toMute.getUniqueId());
			_voterIps.put(voter.getAddress().toString(), previousIpVotes);
			if (popNeeded - votes <= 0) {
				PlayerMeta.setMuteType(toMute, MuteType.TEMPORARY);
				Bukkit.spigot().broadcast(
						new TextComponent("§6The vote to mute " + args[0] + " passed after " + popNeeded + " votes."));
				_votes.remove(toMute.getUniqueId());
				previousVotes.remove(toMute.getUniqueId());
				return true;

			} else {
				voter.spigot().sendMessage(new TextComponent("§aSuccessfully submitted vote for " + toMute.getName()
						+ ". " + (popNeeded - votes) + " more needed for mute."));
				return true;
			}
		}
		// If our candidate has not been voted on before
		else {
			if (cooldown != 0) {
				sender.spigot().sendMessage(new TextComponent(
						"§cServer must wait " + (cooldown / 20) + " more seconds to vote mute again."));
				return true;
			}

			_votes.put(toMute.getUniqueId(), 1);
			previousVotes.add(toMute.getUniqueId());
			_voters.put(voter.getUniqueId(), previousVotes);
			previousIpVotes.add(toMute.getUniqueId());
			_voterIps.put(voter.getAddress().toString(), previousIpVotes);
			Bukkit.spigot()
					.broadcast(new TextComponent("§6" + voter.getName() + " has started a vote to mute "
							+ toMute.getName() + ". " + (popNeeded - 1) + " more votes are needed to mute. §lUse /vm "
							+ toMute.getName() + " to vote."));
			cooldown = 1500;
			return true;
		}
	}
	
	public static boolean canVoteMute() {
		double muted = 0;
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(PlayerMeta.isMuted(p)) {
				muted++;
			}
		}
		double total = Bukkit.getOnlinePlayers().size();
		if((muted / total) * 100 < Integer.parseInt(Config.getValue("mute.max"))) {
			return true;
		}
		return false;
	}

	public static void clear() {
		_votes.clear();
		_voters.clear();
		_voterIps.clear();
	}

	public static void processVoteCooldowns() {
		if (cooldown != 0) {
			cooldown = cooldown - 1;
		}
	}

}
