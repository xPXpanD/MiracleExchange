package com.github.xpxpand.miracleexchange.commands;

import com.github.xpxpand.miracleexchange.MiracleExchange;
import com.google.gson.Gson;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.xpxpand.miracleexchange.MiracleExchange.poolPath;

@SuppressWarnings("NullableProblems")
public class Import extends CommandBase
{
    private static final Gson gson = new Gson();

    @Override
    public String getName()
    {
        return "import";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/import <slot>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length != 1)
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid number of arguments."));
        else if (!args[0].matches("^[1-6]"))
            sender.sendMessage(new TextComponentString("§4Error: §cInvalid slot."));
        else if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
            sender.sendMessage(new TextComponentString("§4Error: §cThis command can only be used in-game."));
        else
        {
            int slot = Integer.parseInt(args[0]);
            PartyStorage party = Pixelmon.storageManager.getParty(sender.getCommandSenderEntity().getUniqueID());

            if (party.get(slot - 1) == null)
            {
                List<File> files = new ArrayList<>();

                try (Stream<Path> filePathStream = Files.walk(poolPath))
                {
                    filePathStream.forEach(filePath ->
                    {
                        if (Files.isRegularFile(filePath))
                        {
                            sender.sendMessage(new TextComponentString("Checking UUID " + filePath.getFileName()));

                            try
                            {
                                MiracleExchange.logger.warn("Contents: " + FileUtils.readFileToString(filePath.toFile(), StandardCharsets.UTF_8), Object.class);
                                // Try to validate the file.
                                gson.fromJson(FileUtils.readFileToString(filePath.toFile(), StandardCharsets.UTF_8), Object.class);

                                // If we're still here, we've passed. Add the file to the pile!
                                files.add(filePath.toFile());
                                sender.sendMessage(new TextComponentString("Added."));
                            }
                            catch(com.google.gson.JsonSyntaxException F)
                            {
                                sender.sendMessage(new TextComponentString("Invalid JSON, skipping!"));
                            }
                            catch (IOException F)
                            {
                                sender.sendMessage(new TextComponentString("Weird IO error, skipping!"));
                                F.printStackTrace();
                            }
                        }
                    });
                }
                catch (IOException F)
                {
                    sender.sendMessage(new TextComponentString("ded."));
                    F.printStackTrace();
                }

                if (!files.isEmpty())
                {
                    sender.sendMessage(new TextComponentString("Added a total of " + files.size() + " files."));

                    final int randomPick = new Random().nextInt(files.size());
                    File file = files.get(randomPick);
                    sender.sendMessage(new TextComponentString("Picked file number " + randomPick + ", UUID" + file.getName()));

                    try
                    {
                        MiracleExchange.logger.warn("Grabbed JSON: \n" + FileUtils.readFileToString(file, StandardCharsets.UTF_8));

                        // Create a Pokémon from the file we chose. All files were validated earlier.
                        Pokemon pokemon = Pixelmon.pokemonFactory.create(JsonToNBT.getTagFromJson(FileUtils.readFileToString(file, StandardCharsets.UTF_8)));

                        // Make a new UUID to avoid conflicts.
                        pokemon.setUUID(UUID.randomUUID());
                        pokemon.markDirty(EnumUpdateType.ALL);

                        // Add!
                        party.add(pokemon);

                        // Remove the file.
                        Files.delete(Paths.get(file.getPath()));
                    }
                    catch (NBTException F)
                    {
                        sender.sendMessage(new TextComponentString("Could not parse NBT!"));
                        F.printStackTrace();
                    }
                    catch (IOException F)
                    {
                        sender.sendMessage(new TextComponentString("Weird IO error!"));
                        F.printStackTrace();
                    }
                }
                else
                    sender.sendMessage(new TextComponentString("Could not find any files to add."));
            }
            else
                sender.sendMessage(new TextComponentString("there's something in there, abort"));
        }
    }
}
