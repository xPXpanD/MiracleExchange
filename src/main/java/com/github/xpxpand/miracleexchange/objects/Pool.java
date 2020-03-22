package com.github.xpxpand.miracleexchange.objects;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Stream;

import static com.github.xpxpand.miracleexchange.MiracleExchange.*;

// Set up a Pool singleton that will contain our Pokémon pool, and that has many methods for adjusting said pool.
public class Pool
{
    private static volatile Pool instance = new Pool();
    private Pool(){}

    public static Pool getInstance()
    {
        return instance;
    }

    // Include a List that we can access from anywhere.
    private ArrayList<Path> list = new ArrayList<>();

    // Sort the list's files by priority.
    private ArrayList<Path> getImportanceSortedList(Path... paths) throws IOException
    {
        ArrayList<Path> list = new ArrayList<>();

        // Loop through the given master paths, and see which specific paths are inside.
        for (Path path : paths)
        {
            // Use a try-with-resources, so we release our locks as we go. Add items to the list, read in passing order.
            try (Stream<Path> files = Files.list(path))
            {
                files.filter(Files::isRegularFile).forEach(list::add);
            }
        }

        return list;
    }

    // Return the contained List.
    public ArrayList<Path> getList()
    {
        return this.list;
    }

    // Yeet the old List, clearing all pool Pokémon. Generally combined with a pool re-read from disk to fill it up again.
    public void reset() { this.list = new ArrayList<>(); }

    // Get a random file from our list and return it. Discard Pokémon above the limit.
    public Path getRandomFile()
    {
        final int slot;

        // TODO: Set config pool count on both.
        if (this.list.size() < 100)
            slot = new Random().nextInt(this.list.size());
        else
            slot = new Random().nextInt(100);

        final Path path = this.list.get(slot);

        this.list.remove(slot);
        try
        {
            Files.move(path, oldPath.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException F)
        {
            logger.fatal("    A fatal I/O error occurred while trying to access the Pokémon files. Stack trace:");
            F.printStackTrace();
            logger.fatal("    Please report this issue, and include the above stack trace. Aborting.");
        }

        return path;
    }

    // Fill the pool to our set cap with random Pokémon, if we're lacking Pokémon.
    public void fill()
    {
        // TODO: Set config pool count.
        // Check whether we're lacking Pokémon.
        if (this.list.size() < 100)
        {
            // TODO: Set config pool count.
            // Subtract the pool's current size from the max. The remainder is how many Pokémon we need to generate.
            int i = 100 - this.list.size();
            while (i > 0)
            {
                // Get a random Pokémon and initialize it. Make sure it's not legendary through the arg.
                Pokemon pokemon = Pixelmon.pokemonFactory.create(EnumSpecies.randomPoke(false));

                // We can still get Ultra Beasts, so let's reroll those.
                while (EnumSpecies.ultrabeasts.contains(pokemon.getSpecies().getPokemonName()))
                    pokemon = Pixelmon.pokemonFactory.create(EnumSpecies.randomPoke(false));

                // Un-shiny the Pokémon, since randoms seem to get tagged shiny way too often.
                pokemon.setShiny(false);

                // Write our random Pokémon to disk, and register the returned Path internally.
                try
                {
                    // Write the Pokémon to disk. If something breaks, we won't get past this.
                    final Path pokemonPath = writePokemon(pokemon);

                    // Insert the new Pokémon into our tracking pool.
                    this.list.add(pokemonPath);
                }
                catch (IOException F)
                {
                    logger.fatal("A fatal I/O error occurred while trying to write a new Pokémon file. Stack trace:");
                    F.printStackTrace();
                    logger.fatal("Please report this issue, and include the above stack trace. Aborting pool re-fill.");
                    break;
                }

                // Decrement the counter to we can move on, provided we're still running.
                i--;
            }
        }
    }
    
    // Parse a Pokémon's NBT data into a prettified JSON-like format that we can directly import again later. Write it.
    private Path writePokemon(final Pokemon pokemon) throws IOException
    {
        // Get some shorthand variables for Pokémon data.
        final String name = pokemon.getSpecies().getPokemonName();
        final String uuid = pokemon.getUUID().toString();

        // Write our "JSON" to a file named after the Pokémon's UUID. Force UTF-8 to store characters properly.
        return Files.write(Paths.get(poolPath.toString(), name + '-' + uuid + ".txt"), getPrettifiedData(pokemon));
    }

    private byte[] getPrettifiedData(final Pokemon pokemon)
    {
        // Extract Pokémon NBT into a String representation of MC's JSON-like format.
        final NBTTagCompound dirtyNBT = pokemon.writeToNBT(new NBTTagCompound());

        // Replace quotes with placeholder Strings. Gson, later, will wrap field names, which we can then undo.
        // TODO: Find a less hacky way of doing this. Should be safe (Pixelmon's data is predictable), but yeah.
        String input = dirtyNBT.toString().replace(":\"", ":\"§PLACEHOLDER§");
        input = input.replace("\",", "§PLACEHOLDER§\",");

        // We format/prettify our rough JSON with Gson. Disable escaping to avoid Unicode conversion. ("&" to "/u0026")
        // Forge/MC don't care about spaces and tabs (and our line reader breaks them anyways), so importing still works!
        String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(new JsonParser().parse(input));

        // Remove Gson's excess quotes and strip potentially-dangerous backslashes. Makes us MC-compliant again.
        json = json.replace("\"", "").replace("\\", "");

        // Add quotes back in where they're actually needed.
        return json.replace("§PLACEHOLDER§", "\"").getBytes(StandardCharsets.UTF_8);
    }
}