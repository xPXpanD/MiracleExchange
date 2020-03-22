package com.github.xpxpand.miracleexchange.commands;

import com.github.xpxpand.miracleexchange.objects.Pool;
import com.github.xpxpand.miracleexchange.utilities.IOMethods;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.github.xpxpand.miracleexchange.MiracleExchange.logger;

@SuppressWarnings("NullableProblems")
public class Trade extends CommandBase
{
    @Override
    public String getName()
    {
        return "miracleexchange";
    }

    @Override
    public List<String> getAliases()
    {
        // TODO: Load alias from config.
        // TODO: Insert WT aliases once done.
        //noinspection ArraysAsListWithZeroOrOneArgument
        return Arrays.asList("mex");
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/mex <slot>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length < 1)
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid number of arguments."));
        else if (!args[0].matches("^[1-6]"))
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid slot. Valid slots are 1-6."));
        else if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
            sender.sendMessage(new TextComponentString("§4Error: §cThis command can only be used in-game."));
        else
        {
            final int slot = Integer.parseInt(args[0]);
            PartyStorage party = Pixelmon.storageManager.getParty(sender.getCommandSenderEntity().getUniqueID());
            Pokemon pokemon = party.get(slot - 1);

            if (pokemon == null)
                sender.sendMessage(new TextComponentString("§4Error: §cThere's nothing in that slot!"));
            else if (pokemon.isEgg())
                sender.sendMessage(new TextComponentString("§4Error: §cThat's an egg! Go hatch it, first."));
            // TODO: Config option, Untradeable.
            else if (PokemonSpec.from("untradeable").matches(pokemon))
                sender.sendMessage(new TextComponentString("Untradeable, check not yet implemented."));
            else
            {
                // If at all possible, write the Pokémon.
                try
                {
                    // TODO: Set OT to Server or something.
                    // Try our write. If things fail, exit.
                    IOMethods.writePokemonAndGetPath(pokemon);

                    // Get our pool instance for manipulation.
                    Pool pool = Pool.getInstance();

                    NBTTagCompound compound = pool.getRandomEntry()


                    sender.sendMessage(new TextComponentString("§4Error: §cPokémon write failed! Please report this to staff."));
                }
                catch (IOException F)
                {
                    sender.sendMessage(new TextComponentString("§4Error: §cPokémon write failed! Please report this to staff."));
                    logger.error("Write for " + sender.getName() + "'s " + pokemon.getSpecies().getPokemonName() + " failed! Trace:");
                    F.printStackTrace();
                }
            }
        }
    }
}
