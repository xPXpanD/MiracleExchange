package com.github.xpxpand.miracleexchange.commands;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.UUID;

@SuppressWarnings("NullableProblems")
public class Clone extends CommandBase
{
    @Override
    public String getName()
    {
        return "mclone";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/mclone <source slot> <target slot>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length != 2)
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid number of arguments."));
        else if (!args[0].matches("^[1-6]"))
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid source slot."));
        else if (!args[1].matches("^[1-6]"))
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid target slot."));
        else if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
            sender.sendMessage(new TextComponentString("§4Error: §cThis command can only be used in-game."));
        else
        {
            final int sourceSlot = Integer.parseInt(args[0]);
            final int targetSlot = Integer.parseInt(args[1]);
            PartyStorage party = Pixelmon.storageManager.getParty(sender.getCommandSenderEntity().getUniqueID());

            Pokemon sourcePokemon = party.get(sourceSlot - 1);
            if (sourcePokemon != null)
            {
                if (party.get(targetSlot - 1) == null)
                {
                    sender.sendMessage(new TextComponentString("We are go!"));

                    // Get source NBT data.
                    final NBTTagCompound sourceNBT = sourcePokemon.writeToNBT(new NBTTagCompound());

                    // Create a new Pokémon with our source NBT data.
                    Pokemon targetPokemon = Pixelmon.pokemonFactory.create(sourceNBT);

                    // Make a new UUID to avoid conflicts.
                    targetPokemon.setUUID(UUID.randomUUID());

                    // Add!
                    party.add(targetPokemon);
                }
                else
                    sender.sendMessage(new TextComponentString("§4Error: §cThe target slot has a Pokémon in it already."));
            }
            else
                sender.sendMessage(new TextComponentString("§4Error: §cNo Pokémon could be found in the source slot."));
        }
    }
}
