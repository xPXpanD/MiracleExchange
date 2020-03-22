package com.github.xpxpand.miracleexchange.commands;

import com.github.xpxpand.miracleexchange.utilities.IOMethods;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

@SuppressWarnings("NullableProblems")
public class Export extends CommandBase
{
    @Override
    public String getName()
    {
        return "export";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/export <slot>";
    }

    // TODO: Check eggs.
    // TODO: Do the forced UTF thing.
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
/*        if (args.length != 1)
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid number of arguments."));
        else if (!args[0].matches("^[1-6]"))
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid slot."));
        else if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
            sender.sendMessage(new TextComponentString("§4Error: §cThis command can only be used in-game."));
        else
        {
            int slot = Integer.parseInt(args[0]);
            PartyStorage party = Pixelmon.storageManager.getParty(sender.getCommandSenderEntity().getUniqueID());

            Pokemon pokemon = party.get(slot - 1);
            if (pokemon != null)
                IOMethods.writePokemonAndGetPath(pokemon);
        }*/
    }
}
