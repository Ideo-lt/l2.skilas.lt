/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * Command /offline_shop like L2OFF
 * @author Nefer
 */
public class OfflineShop implements IUserCommandHandler
{
    private static final int[] COMMAND_IDS =
    {
        114
    };

    /*
     * (non-Javadoc)
     * @see com.l2jfrozen.gameserver.handler.IUserCommandHandler#useUserCommand(int, com.l2jfrozen.gameserver.model.L2PcInstance)
     */
    @SuppressWarnings("null")
    @Override
    public synchronized boolean useUserCommand(final int id, final L2PcInstance player)
    {
        if (player == null)
            return false;

        // Message like L2OFF
        if ((!player.isInStoreMode() && (!player.isInCraftMode())) || !player.isSitting())
        {
            player.sendMessage("You are not running a private store or private work shop.");
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        final TradeList storeListBuy = player.getBuyList();
        if (storeListBuy == null && storeListBuy.getItemCount() == 0)
        {
            player.sendMessage("Your buy list is empty.");
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        final TradeList storeListSell = player.getSellList();
        if (storeListSell == null && storeListSell.getItemCount() == 0)
        {
            player.sendMessage("Your sell list is empty.");
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        player.getInventory().updateDatabase();

        if (AttackStanceTaskManager.getInstance().get(player) && !(player.isGM()))
        {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING));
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Dont allow leaving if player is in combat
        if (player.isInCombat() && !player.isGM())
        {
            player.sendMessage("You cannot Logout while is in Combat mode.");
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Dont allow leaving if player is teleporting
        if (player.isTeleporting() && !player.isGM())
        {
            player.sendMessage("You cannot Logout while is Teleporting.");
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        if (player.isInOlympiadMode() || Olympiad.getInstance().playerInStadia(player))
        {
            player.sendMessage("You can't Logout in Olympiad mode.");
            return false;
        }

        // Prevent player from logging out if they are a festival participant nd it is in progress,
        // otherwise notify party members that the player is not longer a participant.
        if (player.isFestivalParticipant())
        {
            if (SevenSignsFestival.getInstance().isFestivalInitialized())
            {
                player.sendMessage("You cannot Logout while you are a participant in a Festival.");
                return false;
            }

            final L2Party playerParty = player.getParty();
            if (playerParty != null)
                player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming Festival."));
        }

        if (player.isFlying())
            player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));

        if ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE) || (player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE))
        {
            player.sendMessage("Your private store has succesfully been flagged as an offline shop and will remain active for ever.");

            player.logout();

            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.l2jfrozen.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    @Override
    public int[] getUserCommandList()
    {
        return COMMAND_IDS;
    }
}