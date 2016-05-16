package info.tregmine.listeners;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import info.tregmine.Tregmine;
import info.tregmine.api.Notification;
import info.tregmine.api.TregminePlayer;
import info.tregmine.database.DAOException;
import info.tregmine.database.IBlessedBlockDAO;
import info.tregmine.database.IContext;
import info.tregmine.database.IWalletDAO;
import net.md_5.bungee.api.chat.TextComponent;

public class BlessedBlockListener implements Listener {
	public final static Set<Material> ALLOWED_MATERIALS = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST,
			Material.FURNACE, Material.BURNING_FURNACE, Material.BREWING_STAND, Material.WOOD_DOOR,
			Material.WOODEN_DOOR, Material.LEVER, Material.STONE_BUTTON, Material.STONE_PLATE, Material.WOOD_PLATE,
			Material.WORKBENCH, Material.SIGN_POST, Material.DIODE, Material.DIODE_BLOCK_OFF, Material.TRAP_DOOR,
			Material.DIODE_BLOCK_ON, Material.JUKEBOX, Material.SIGN, Material.FENCE_GATE, Material.DISPENSER,
			Material.WOOD_BUTTON, Material.NOTE_BLOCK, Material.REDSTONE_COMPARATOR, Material.REDSTONE_COMPARATOR_OFF,
			Material.REDSTONE_COMPARATOR_ON, Material.HOPPER, Material.DROPPER);

	private Tregmine plugin;

	public BlessedBlockListener(Tregmine instance) {
		plugin = instance;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Location loc = event.getBlock().getLocation();
		Player player = event.getPlayer();

		Map<Location, Integer> blessedBlocks = plugin.getBlessedBlocks();
		if (blessedBlocks.containsKey(loc)) {
			player.sendMessage(ChatColor.RED + "You can't destroy a blessed item.");
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlockPlaced();

		Map<Location, Integer> blessedBlocks = plugin.getBlessedBlocks();
		if (block.getType() == Material.CHEST) {
			Player player = event.getPlayer();

			Location loc = block.getLocation();
			Location loc1 = loc.add(new Vector(1, 0, 0));
			Location loc2 = loc.subtract(new Vector(1, 0, 0));
			Location loc3 = loc.add(new Vector(0, 0, 1));
			Location loc4 = loc.subtract(new Vector(0, 0, 1));

			if (blessedBlocks.containsKey(loc1) || blessedBlocks.containsKey(loc2) || blessedBlocks.containsKey(loc3)
					|| blessedBlocks.containsKey(loc4)) {

				player.sendMessage(ChatColor.RED + "You can't place a chest next to one that is already blessed.");
				event.setCancelled(true);
				return;
			}
		} else if (block.getType() == Material.HOPPER) {
			TregminePlayer player = plugin.getPlayer(event.getPlayer());

			Location loc = block.getLocation();
			Location loc1 = loc.subtract(new Vector(0, 1, 0));

			if (blessedBlocks.containsKey(loc) || blessedBlocks.containsKey(loc1)) {

				player.sendStringMessage(ChatColor.RED + "You can't place a hopper under a blessed chest.");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onDoorBreak(EntityBreakDoorEvent event) {
		Location l = event.getBlock().getLocation();
		Entity e = event.getEntity();

		Map<Location, Integer> b = plugin.getBlessedBlocks();

		if (b.containsKey(l)) {
			if (e instanceof Zombie) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		TregminePlayer player = plugin.getPlayer(event.getPlayer());
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.getItemInHand().getType() == Material.FEATHER
				&& player.getRank().canUnbless() && ALLOWED_MATERIALS.contains(block.getType())) {
			Location loc = block.getLocation();

			try (IContext ctx = plugin.createContext()) {
				IBlessedBlockDAO blessDAO = ctx.getBlessedBlockDAO();
				int owner = blessDAO.owner(loc);
				if (owner == -1) {
					return;
				}
				TregminePlayer blockOwner = plugin.getPlayer(owner);
				if (blockOwner.isOnline()) {
					blockOwner.sendSpigotMessage(
							new TextComponent(ChatColor.AQUA + "One of your blocks at X" + loc.getBlockX() + " Y"
									+ loc.getBlockY() + " Z" + loc.getBlockZ() + " has been unblessed by "),
							player.getChatName());
				}
				IWalletDAO walletDAO = ctx.getWalletDAO();
				walletDAO.add(blockOwner, 25000);
				blessDAO.delete(loc);
				player.sendStringMessage(ChatColor.AQUA + "You unblessed a block at X" + loc.getBlockX() + " Y"
						+ loc.getBlockY() + " Z" + loc.getBlockZ() + "");
				Map<Location, Integer> blessedBlocks = plugin.getBlessedBlocks();
				blessedBlocks.remove(loc);
				event.setCancelled(true);
			} catch (DAOException e) {
				throw new RuntimeException(e);
			}

		}
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.getItemInHand().getType() == Material.BONE
				&& player.getRank().canBless() && ALLOWED_MATERIALS.contains(block.getType())) {

			int targetId = player.getBlessTarget();
			if (targetId == 0) {
				player.sendStringMessage(ChatColor.RED + "Use /bless [name] first!");
				return;
			}

			TregminePlayer target = plugin.getPlayerOffline(targetId);
			if (target == null) {
				player.sendStringMessage(ChatColor.RED + "Use /bless [name] first!");
				return;
			}

			int amount = player.getRank().getBlessCost(block);
			if (amount > 0) {
				try (IContext ctx = plugin.createContext()) {
					IWalletDAO walletDAO = ctx.getWalletDAO();

					if (walletDAO.take(player, amount)) {
						player.sendStringMessage(ChatColor.LIGHT_PURPLE + (amount + " tregs was taken from you"));
					} else {
						player.sendStringMessage(ChatColor.RED + "You need " + amount + " tregs");
						return;
					}
				} catch (DAOException e) {
					throw new RuntimeException(e);
				}
			}

			Location loc = block.getLocation();
			if (target.isOnline()) {
				target.sendNotification(Notification.BLESS,
						new TextComponent(ChatColor.AQUA + "Your god blessed it in your name!"));
			}
			player.sendSpigotMessage(new TextComponent(ChatColor.AQUA + "You blessed for "), target.getChatNameStaff(),
					new TextComponent("."));
			Tregmine.LOGGER.info(player.getName() + " Blessed a block " + loc + " to " + target.getName() + ".");

			Map<Location, Integer> blessedBlocks = plugin.getBlessedBlocks();
			blessedBlocks.put(loc, targetId);

			try (IContext ctx = plugin.createContext()) {
				IBlessedBlockDAO blessDAO = ctx.getBlessedBlockDAO();
				blessDAO.insert(target, loc);
			} catch (DAOException e) {
				throw new RuntimeException(e);
			}

			event.setCancelled(true);
			return;
		}

		if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)
				&& ALLOWED_MATERIALS.contains(block.getType())) {

			Location loc = block.getLocation();

			Map<Location, Integer> blessedBlocks = plugin.getBlessedBlocks();
			if (blessedBlocks.containsKey(loc)) {
				int id = blessedBlocks.get(loc);
				TregminePlayer target = plugin.getPlayerOffline(id);
				if (id != player.getId()) {
					player.sendSpigotMessage(new TextComponent(ChatColor.YELLOW + "Blessed to: "), target.getChatName(),
							new TextComponent("."));
					if (!player.getRank().canInspectInventories()) {
						event.setCancelled(true);
					}
				} else {
					player.sendStringMessage(ChatColor.AQUA + "Blessed to you.");
				}
			}
		}
	}
}
