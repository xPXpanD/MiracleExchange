package com.github.xpxpand.miracleexchange.commands;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

@SuppressWarnings("NullableProblems")
public class Flag extends CommandBase
{
    @Override
    public String getName()
    {
        return "flag";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "flag";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length != 2)
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid number of arguments."));
        else if (!args[0].matches("set|unset|test|delete"))
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid flag argument."));
        else if (!args[1].matches("^[1-6]"))
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid slot."));
        else if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
            sender.sendMessage(new TextComponentString("§4Error: §cThis command can only be used in-game."));
        else
        {
            int slot = Integer.parseInt(args[1]);
            Pokemon pokemon = Pixelmon.storageManager.getParty(sender.getCommandSenderEntity().getUniqueID()).get(slot - 1);

            if (pokemon != null)
            {
                sender.sendMessage(new TextComponentString("Entering execution."));

                if (args[0].matches("test"))
                {
                    if (!pokemon.getPersistentData().getKeySet().contains("hasFlag"))
                        sender.sendMessage(new TextComponentString("Flag does not exist."));
                    else
                        sender.sendMessage(new TextComponentString("Flag status: " + pokemon.getPersistentData().getBoolean("hasFlag")));
                }
                else
                {
                    boolean flagStatus = pokemon.getPersistentData().getBoolean("hasFlag");

                    if (args[0].matches("set"))
                    {
                        sender.sendMessage(new TextComponentString("Setting flag. Old status: " + flagStatus));
                        pokemon.getPersistentData().setBoolean("hasFlag", true);
                    }
                    else if (args[0].matches("unset"))
                    {
                        sender.sendMessage(new TextComponentString("Unsetting flag. Old status: " + flagStatus));
                        pokemon.getPersistentData().setBoolean("hasFlag", false);
                    }
                    else
                    {
                        sender.sendMessage(new TextComponentString("Removing flag. Old status: " + flagStatus));
                        pokemon.getPersistentData().removeTag("hasFlag");
                    }
                }
            }
        }
    }
}
