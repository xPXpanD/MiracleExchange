package com.github.xpxpand.miracleexchange.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.io.FileUtils;

import static com.github.xpxpand.miracleexchange.MiracleExchange.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IOMethods
{
    private static final Gson gson = new Gson();

    // Checks all given paths, and creates folders there if necessary. Passes back whether it created something.
    public static boolean tryCreateDirs(Path... paths)
    {
        boolean createdStuff = false;

        for (Path path : paths)
        {
            // Create config directories if they don't exist. Silently swallow errors if they do. I/O is awkward.
            try
            {
                Files.createDirectory(path);
                createdStuff = true;
            }
            catch (final IOException ignored) {}
        }

        return createdStuff;
    }

    // Populates all the Pokémon lists with files of the right type.
    public static boolean initializePool()
    {
        try
        {
            boolean foundError = false;

            // Load existing files.
            for(File file : FileUtils.listFiles(poolPath.toFile(), new String[]{"txt"}, true))
            {
                // TODO: Set config pool count.
                // Make sure we don't overfill, even if there are more files than our cap.
                if (pool.size() < 100)
                {
                    if (file.isFile())
                    {
                        try
                        {
                            // Read the file.
                            String contents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                            // Try to validate the file. Skip execution via exception out if something's wrong.
                            gson.fromJson(contents, Object.class);

                            // If we're still here, our Pokémon's importable data is valid. Add it to the pool.
                            pool.add(JsonToNBT.getTagFromJson(contents));
                        }
                        catch(JsonSyntaxException F)
                        {
                            foundError = true;
                            Files.move(file.toPath(), brokenPath);
                            logger.error("    File " + file.getName() + " failed validation, please check syntax! Skipping.");
                        }
                        catch (NBTException F)
                        {
                            foundError = true;
                            Files.move(file.toPath(), brokenPath);
                            logger.error("    File " + file.getName() + " could not be parsed as NBT, please check syntax! Skipping.");
                        }
                    }
                }
                else
                    break;
            }

            if (foundError)
            logger.error("    Broken files have been moved to the \"broken\" folder.");

            // TODO: Set config pool count.
            // Are we still lacking Pokémon? Fill the pool to the cap with fresh normal ones.
            if (pool.size() < 100)
            {
                // TODO: Set config pool count.
                // Subtract the current count from 100. The remaining number is the number of Pokémon we need to generate.
                int i = 100 - pool.size();
                while (i > 0)
                {
                    // Get a random Pokémon and initialize it. Make sure it's not legendary through the arg.
                    Pokemon pokemon = Pixelmon.pokemonFactory.create(EnumSpecies.randomPoke(false))/*.initialize()*/;

                    // We can still get Ultra Beasts, so let's reroll those.
                    while (EnumSpecies.ultrabeasts.contains(pokemon.getSpecies().getPokemonName()))
                    {
                        logger.warn("Got an Ultra Beast for pool slot " + i + ", ew!");
                        pokemon = Pixelmon.pokemonFactory.create(EnumSpecies.randomPoke(false));
                    }

                    // Get NBT data for the new Pokémon.
                    final NBTTagCompound pokemonNBT = pokemon.writeToNBT(new NBTTagCompound());

                    // Add the new Pokémon to our pool.
                    pool.add(pokemonNBT);

                    // Write a random Pokémon to disk, and register the returned Path (as a File) internally.
                    Path pokemonPath = writePokemonAndGetPath(pokemon);

                    // We good? Move on to the next Pokémon.
                    if (pokemonPath != null)
                        i--;
                }
            }
        }
        catch (IOException F)
        {
            logger.fatal("    A fatal I/O error occurred while trying to access the Pokémon files. Stack trace:");
            F.printStackTrace();
            logger.fatal("    Please report this issue, and include the above stack trace. Aborting mod load.");
            return false;
        }

        return true;
    }

    // Parse a Pokémon's NBT data into a prettified JSON-like format that we can directly import again later.
    public static Path writePokemonAndGetPath(Pokemon pokemon)
    {
        // Extract Pokémon NBT into a String representation of MC's JSON-like format.
        final NBTTagCompound dirtyNBT = pokemon.writeToNBT(new NBTTagCompound());

        // Replace quotes with placeholder Strings. Gson, later, will wrap field names, which we can then undo.
        // TODO: Find a less hacky way of doing this. Should be safe (Pixelmon's data is predictable), but yeah.
        String dirtyJson = dirtyNBT.toString().replace(":\"", ":\"§PLACEHOLDER§");
        dirtyJson = dirtyJson.replace("\",", "§PLACEHOLDER§\",");

        /*// Iterate over the NBT keys. MC dislikes JSON's field quotes, but we want to preserve String ones.
        for (String s : dirtyNBT.getKeySet())
        {
            // Get the tag we're looking at right now.
            NBTBase currentTag = dirtyNBT.getTag(s);

            // Is our tag a String?
            if (currentTag instanceof NBTTagString)
            {
                *//*// Trash escape characters ("\") as they mess up parsing. Gson will escape double quotes later.
                String fixedString = ((NBTTagString) currentTag).getString().replace("\\", "");

                // Add placeholders for later replacement, once the field quotes are gone.
                fixedString = "§PLACEHOLDER§" + fixedString + "§PLACEHOLDER§";*//*

                // Add placeholders for later replacement, once the field quotes are gone.
                final String fixedString = "§PLACEHOLDER§" + ((NBTTagString) currentTag).getString() + "§PLACEHOLDER§";

                // Overwrite the old String with a fixed one.
                dirtyNBT.setString(s, fixedString);
            }
        }*/

        // We format/prettify our rough JSON with Gson. Disable escaping to avoid Unicode conversion. ("&" to "/u0026")
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();

        // Create our pretty file. Much easier to edit, that way. Forge/MC don't care about spaces/tabs, easy import!
        String cleanJson = builder.create().toJson(new JsonParser().parse(dirtyJson));

        // Remove Gson's excess quotes and strip potentially-dangerous backslashes. Makes us MC-compliant again.
        cleanJson = cleanJson.replace("\"", "").replace("\\", "");

        // Add quotes back in where they're actually needed.
        cleanJson = cleanJson.replace("§PLACEHOLDER§", "\"");

        try
        {
            // Get some shorthand variables for Pokémon data.
            final String name = pokemon.getSpecies().getPokemonName();
            final String uuid = pokemon.getUUID().toString();

            // Write our "JSON" to a file named after the Pokémon's UUID. Force UTF-8 to store characters properly.
            return Files.write(Paths.get(poolPath.toString(), name + '-' + uuid + ".txt"), cleanJson.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException F)
        {
            logger.fatal("Writing failed! Trace:");
            F.printStackTrace();
        }

        return null;
    }

    // See how many Ultra Beasts are in our pool.
    public static int getUBCount()
    {
        int count = 0;

        for (NBTTagCompound compound : pool)
        {
            if (EnumSpecies.ultrabeasts.contains(compound.getString("Name")))
            {
                logger.info("Detected Pokémon " + compound.getString("Name") + " as an Ultra Beast.");
                count++;
            }
        }

        return count;
    }

    // See how many legendaries are in our pool.
    public static int getLegendaryCount()
    {
        int count = 0;

        for (NBTTagCompound compound : pool)
        {
            if (EnumSpecies.legendaries.contains(compound.getString("Name")))
            {
                logger.info("Detected Pokémon " + compound.getString("Name") + " as a legendary.");
                count++;
            }
        }

        return count;
    }

    // See how many shinies are in our pool.
    public static int getShinyCount()
    {
        int count = 0;

        for (NBTTagCompound compound : pool)
        {
            if (compound.getBoolean("IsShiny"))
            {
                logger.info("Detected Pokémon " + compound.getString("Name") + " as a shiny.");
                count++;
            }
        }

        return count;
    }
}
