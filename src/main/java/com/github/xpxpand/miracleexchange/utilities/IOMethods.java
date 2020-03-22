package com.github.xpxpand.miracleexchange.utilities;

import com.github.xpxpand.miracleexchange.objects.Pool;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.xpxpand.miracleexchange.MiracleExchange.*;

public class IOMethods
{
    private static final Gson gson = new Gson();
    private static final Pool pool = Pool.getInstance();

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
        pool.reset();

        try
        {
            boolean foundError = false;

            // TODO: Load special Pokémon first, so they're always in the pool if present.
            // Get a list of files sorted by importance. This ensures we always import the most valuable Pokémon first.
            ArrayList<Path> prioritizedFileList = getImportanceSortedList(currentLegendariesPath, currentUBsPath, currentShiniesPath, poolPath);

            // Start loading existing files.
            for(Path path : prioritizedFileList)
            {
                // TODO: Set config pool count.
                // Make sure we don't overfill, even if there are more files than our cap.
                if (pool.getList().size() < 100)
                {
                    if (Files.isRegularFile(path) && path.toString().endsWith(".txt"))
                    {
                        // Read the file. Force UTF-8 to recover characters properly.
                        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8))
                        {
                            // Dump everything into a big ol' String.
                            String contents = lines.collect(Collectors.joining());

                            // Try to validate our file's contents. Skip execution via exception out if something's wrong.
                            gson.fromJson(contents, Object.class);

                            // If we're still here, our Pokémon's importable data is valid. Get NBT.
                            NBTTagCompound pokemonNBT = JsonToNBT.getTagFromJson(contents);

                            // Make sure it's in the right folder. Move it if not.
                            try
                            {
                                if (EnumSpecies.ultrabeasts.contains(pokemonNBT.getString("Name")))
                                {
                                    if (!path.getParent().equals(currentUBsPath))
                                    {
                                        logger.info("    §eMoving §6" + path.getFileName() + "§e to UB folder.");
                                        Files.move(path, currentUBsPath.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }
                                else if (EnumSpecies.legendaries.contains(pokemonNBT.getString("Name")))
                                {
                                    if (!path.getParent().equals(currentLegendariesPath))
                                    {
                                        logger.info("    §eMoving §6" + path.getFileName() + "§e to legend folder.");
                                        Files.move(path, currentLegendariesPath.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }
                                else if (pokemonNBT.getBoolean("IsShiny"))
                                {
                                    if (!path.getParent().equals(currentShiniesPath))
                                    {
                                        logger.info("    §eMoving §6" + path.getFileName() + "§e to shiny folder.");
                                        Files.move(path, currentShiniesPath.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }
                                else if (!path.getParent().equals(poolPath))
                                {
                                    logger.info("    §eMoving §6" + path.getFileName() + "§e to normal folder.");
                                    Files.move(path, poolPath.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                            catch (IOException F)
                            {
                                logger.info("    §cError moving file. Is something accessing it?");
                            }

                            // And finally, add it to the pool.
                            pool.getList().add(JsonToNBT.getTagFromJson(contents));
                        }
                        catch(JsonSyntaxException F)
                        {
                            foundError = true;
                            Files.move(path, brokenPath.resolve(path.getFileName()));
                            logger.info("    §cFile §4" + path.getFileName() + "§c failed syntax validation!");
                        }
                        catch (NBTException F)
                        {
                            foundError = true;
                            Files.move(path, brokenPath.resolve(path.getFileName()));
                            logger.info("    §cFile §4" + path.getFileName() + "§c could not be parsed as NBT!");
                        }
                    }
                }
                else
                    break;
            }

            if (foundError)
                logger.info("    §cBroken files were moved to the \"§4broken§c\" folder. Please check their syntax.");


            }
        }
        catch (IOException F)
        {
            logger.fatal("    A fatal I/O error occurred while trying to access the Pokémon files. Stack trace:");
            F.printStackTrace();
            logger.fatal("    Please report this issue, and include the above stack trace. Aborting.");

            return false;
        }

        return true;
    }

    // See how many special Pokémon (legendaries, UBs, shinies) are in our pool. Write this to the global Map variable.
    public static HashMap<String, Integer> getCounts()
    {
        HashMap<String, Integer> counts = new HashMap<>();

        // Start upping the counts based on what we find in the pool.
        for (NBTTagCompound compound : pool.getList())
        {
            if (EnumSpecies.ultrabeasts.contains(compound.getString("Name")))
                counts.merge("UBs", 1, Integer::sum);
            else if (EnumSpecies.legendaries.contains(compound.getString("Name")))
                counts.merge("Legendaries", 1, Integer::sum);
            else if (compound.getBoolean("IsShiny"))
                counts.merge("Shinies", 1, Integer::sum);
        }

        // If we find no Pokémon of a given type, anything grabbing counts will get a null! To avoid that, set 0 here.
        counts.putIfAbsent("UBs", 0);
        counts.putIfAbsent("Legendaries", 0);
        counts.putIfAbsent("Shinies", 0);

        return counts;
    }
}
