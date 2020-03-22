package com.github.xpxpand.miracleexchange;

import com.github.xpxpand.miracleexchange.objects.Pool;
import com.github.xpxpand.miracleexchange.commands.*;
import com.github.xpxpand.miracleexchange.utilities.IOMethods;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mod
(
        modid = MiracleExchange.MOD_ID,
        name = MiracleExchange.MOD_NAME,
        version = MiracleExchange.VERSION,
        dependencies = "required-after:pixelmon",
        acceptableRemoteVersions = "*"
)

public class MiracleExchange
{
    // Set up base mod info.
    static final String MOD_ID = "miracleexchange";
    static final String MOD_NAME = "MiracleExchange";
    static final String VERSION = "0.1";

    // Set up our config paths, and grab an OS-specific file path separator. This will usually be a forward slash.
    private static final String separator = FileSystems.getDefault().getSeparator();
    public static final Path configPath =
            Paths.get("config" + separator + MOD_NAME + separator);
    public static final Path poolPath =
            Paths.get("config" + separator + MOD_NAME + separator + "current" + separator);
    public static final Path currentShiniesPath =
            Paths.get("config" + separator + MOD_NAME + separator + "current" + separator + "shinies" + separator);
    public static final Path currentLegendariesPath =
            Paths.get("config" + separator + MOD_NAME + separator + "current" + separator + "legendaries" + separator);
    public static final Path currentUBsPath =
            Paths.get("config" + separator + MOD_NAME + separator + "current" + separator + "ultrabeasts" + separator);
    public static final Path oldPath =
            Paths.get("config" + separator + MOD_NAME + separator + "old" + separator);
    public static final Path brokenPath =
            Paths.get("config" + separator + MOD_NAME + separator + "broken" + separator);

    // Set up other necessary things.
    public static final Logger logger = LogManager.getLogger("Miracle Exchange");

    // Set up file lists for the different types of Pokémon in our pool.
    //public static List<NBTTagCompound> pool = new ArrayList<>();
    private Pool pool = Pool.getInstance();

    @Mod.EventHandler
    public void onFMLInitEvent(FMLInitializationEvent event)
    {
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
            logger.info("§f--> §aAdded a total of " + pool.getList().size() + " Pokémon:");

            // Get our current counts by type, and format messages accordingly.
            final int ubNum = IOMethods.getCounts().get("UBs");
            final int legendaryNum = IOMethods.getCounts().get("Legendaries");
            final int shinyNum = IOMethods.getCounts().get("Shinies");
            final int normieNum = pool.getList().size() - ubNum - legendaryNum - shinyNum;

            if (ubNum == 1)
                logger.info("    §3...§aone Ultra Beast.");
            else if (ubNum > 1)
                logger.info("    §3...§a" + ubNum + "§a Ultra Beasts.");

            if (legendaryNum == 1)
                logger.info("    §3...§aone legendary.");
            else if (legendaryNum > 1)
                logger.info("    §3...§a" + legendaryNum + "§a legendaries.");

            if (shinyNum == 1)
                logger.info("    §3...§aone shiny.");
            else if (shinyNum > 1)
                logger.info("    §3...§a" + shinyNum + "§a shinies.");

            if (normieNum > 0)
            {
                if (normieNum < pool.getList().size())
                    logger.info("    §3...§aand " + normieNum + "§a normal ones.");
                else
                    logger.info("    §3§a" + normieNum + "§a normal ones. That's it. Yawn.");
            }
        }

        // We're done, one way or another. Add a footer and a blank line, same story as the header.
        logger.info("§f====================================================================");
        logger.info("");
    }

    @Mod.EventHandler
	public void onServerStartedEvent(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new Export());
		event.registerServerCommand(new Import());
		event.registerServerCommand(new Clone());
		event.registerServerCommand(new Flag());
		event.registerServerCommand(new Reload());

        PermissionAPI.registerNode("miracleexchange.command.staff.reload", DefaultPermissionLevel.OP, "Allows reloading Miracle Exchange's configs via /mereload.");
	}

}
