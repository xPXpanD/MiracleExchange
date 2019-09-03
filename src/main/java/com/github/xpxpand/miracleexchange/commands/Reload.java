package com.github.xpxpand.miracleexchange.commands;

import com.github.xpxpand.miracleexchange.utilities.IOMethods;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import static com.github.xpxpand.miracleexchange.MiracleExchange.*;

@SuppressWarnings("NullableProblems")
public class Reload extends CommandBase
{
    @Override
    public String getName()
    {
        return "mereload";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/mereload";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        // Clear the pool so we don't get stuck with old data.
        pool.clear();

        // Add a header, and put a blank line above it to avoid clutter with other marginal'd mods.
        logger.info("");
        logger.info("§f================== M I R A C L E  E X C H A N G E ==================");

        // Create config directories if they don't exist. Silently swallow an error if it does. I/O is awkward.
        logger.info("--> §aLoading and validating Miracle Exchange settings...");
        if (IOMethods.tryCreateDirs(configPath, poolPath, currentShiniesPath, currentLegendariesPath, currentUBsPath, oldPath, brokenPath))
            logger.info("    §eOne or more folders weren't found, and have been created.");
        else
            logger.info("§f--> §aChecking for stored Pokémon...");

        // Initialize the Pokémon pool, and fill it to cap if necessary. If successful, show what we loaded.
        if (IOMethods.initializePool())
        {
            logger.info("§f--> §aAdded a total of " + pool.size() + " Pokémon:");

            int ubNum = IOMethods.getUBCount();
            int legendaryNum = IOMethods.getLegendaryCount();
            int shinyNum = IOMethods.getShinyCount();
            int normieNum = pool.size() - ubNum - legendaryNum - shinyNum;

            if (ubNum > 0)
                logger.info("    §3...§a" + ubNum + "§a Ultra Beasts.");
            if (legendaryNum > 0)
                logger.info("    §3...§a" + legendaryNum + "§a legendaries.");
            if (shinyNum > 0)
                logger.info("    §3...§a" + shinyNum + "§a shinies.");
            if (normieNum > 0)
            {
                if (normieNum < pool.size())
                    logger.info("    §3...§aand " + normieNum + "§a normal ones.");
                else
                    logger.info("    §3...§a" + normieNum + "§a normal ones.");
            }
        }

        // We're done, one way or another. Add a footer and a blank line, same story as the header.
        logger.info("§f====================================================================");
        logger.info("");
    }
}
