package uk.org.tucks.minecraft.wrappers.mineosbedrockwrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Scanner;

/*
 * Main App
 *
 * Shamelessly lifted the super example from deFreitas (https://stackoverflow.com/users/2979435/defreitas)
 * ref: https://stackoverflow.com/questions/13431473/opening-a-shell-and-interacting-with-its-i-o-in-java/35261487#35261487
 *
 */
public class App
{
    public static void main( String[] args ) throws IOException, InterruptedException
    {
        // create a logger 
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tT] %5$s%n");
        Logger logger = Logger.getLogger("mineos-bedrock-wrapper");
        //logger.setUseParentHandlers(false);

        /* 
         * Setup the wrapper
         *
         */

        // check bedrock_server in same ./dir and executable
        try {
            File bedrockServer = new File("./bedrock_server");
            if (bedrockServer.exists()) {
                if (!bedrockServer.canExecute()) {
                    System.out.println(bedrockServer.getPath() + " is not executable");
                }
            } else {
                System.out.println("Linux/Ubuntu Bedrock Server is not found at " + bedrockServer.getPath());
            }

            /* make log dir if does not exist */
            File directory = new File("./logs");
            if (! directory.exists()) {
                directory.mkdir();
            }                 

            FileHandler logFile;
            logFile = new FileHandler("./logs/latest.log");
            logger.addHandler(logFile);
            
            SimpleFormatter formatter = new SimpleFormatter();
            logFile.setFormatter(formatter);
        
            logger.info("mineos-bedrock-wrapper starting Bedrock dedicated server...");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create a child process
        final Process process = new ProcessBuilder().command("sh", "-c", "LD_LIBRARY_PATH=. ./bedrock_server").start();

        // stderr from child
        new Thread(() -> {
            BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = null;
            try {
                while((line = stderr.readLine()) != null){
                    // write the console stderr to ./logs/latest.log for mineos to tail
                    logger.info(line);
                }
            } catch(IOException e) {}
        }).start();

        // stdout from child
        new Thread(() -> {
            BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            try {
                while((line = stdout.readLine()) != null){
                    // write the console stdout to ./logs/latest.log for mineos to tail
                    logger.info(line);
                }
            } catch(IOException e) {}
        }).start();

        // process control
        new Thread(() -> {
            
            int exitCode = 0;
            try {
                exitCode = process.waitFor();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            System.out.printf("Exited with code %d\n", exitCode);
            logger.info("mineos-bedrock-wrapper stopped Bedrock dedicated server...");
            // pass through the bedrock_server exit code
            System.exit(exitCode);
        }).start();

        // process 'user' input (stdin to child)
        final Scanner scanner = new Scanner(System.in);
        final BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        final String newLine = System.getProperty("line.separator");
        while(true){
            // pass any commands straight through to bedrock_server
            String commandLine = scanner.nextLine();
            stdin.write(commandLine);
            stdin.newLine();
            stdin.flush();
        }
    }
}


/* 

command line args from Java edition

--bonusChest
If a bonus chest should be generated, when the world is first generated.
--demo
If the server is in demo mode. (Shows the players a demo pop-up, and players cannot break or place blocks or eat if the demo time has expired)
--eraseCache
Erases the lighting caches, etc. Same option as when optimizing single player worlds.
--forceUpgrade
Forces upgrade on all the chunks, such that the data version of all chunks matches the current server version (same as with sp worlds).
--help
Shows this help.
--initSettings
Initializes 'server.properties' and 'eula.txt', then quits.
--nogui
Doesn't open the GUI when launching the server.
--port <Integer>
Which port to listen on, overrides the server.properties value. (default: -1)
--safeMode
Loads level with vanilla datapack only.
--serverId <String>
Gives an ID to the server. (??)
--singleplayer <String>
Runs the server in offline mode (unknown where <String> is used for, probably used internally by mojang?)
--universe <String>
The folder in which to look for world folders. (default: .)
--world <String>
The name of the world folder in which the level.dat resides.

*/


/* 

                <td>kick &lt;player name or xuid&gt; &lt;reason&gt;</td>
                <td>Immediately kicks a player. The reason will be shown on the kicked players screen.</td>

                <td>stop</td>
                <td>Shuts down the server gracefully.</td>

                <td>save &lt;hold | resume | query&gt;</td>
                <td>Used to make atomic backups while the server is running. See the backup section for more information.</td>

                <td>whitelist &lt;on | off | list | reload&gt;</td>

                        <code>on</code> and <code>off</code> turns the whitelist on and off. Note that this does not change the value in the <code>server.properties</code> 
file!

                        <code>list</code> prints the current whitelist used by the server

                        <code>reload</code> makes the server reload the whitelist from the file.

                        See the Whitelist section for more information.

                <td>whitelist &lt;add | remove&gt; &lt;name&gt;</td>
                <td>Adds or removes a player from the whitelist file. The name parameter should be the Xbox Gamertag of the player you want to add or remove. You don't need
 to specify a XUID here, it will be resolved the first time the player connects.<br/><br/>See the Whitelist section for more information.</td>
            </tr>
            <tr>
                <td>permission &lt;list | reload&gt;</td>
                <td>
                    <p>
                        <code>list</code> prints the current used operator list.
                    </p>
                    <p>
                        <code>reload</code> makes the server reload the operator list from the ops file.
                    </p>
                    <p>
                        See the Permissions section for more information.
                    </p>
                </td>
            </tr>
                        <tr>
                <td>op &lt;player&gt;</td>
                <td>
                    <p>
                        Promote a player to <code>operator</code>. This will also persist in <code>permissions.json</code> if the player is authenticated to XBL. If <code>p
ermissions.json</code> is missing it will be created. If the player is not connected to XBL, the player is promoted for the current server session and it will not be persis
ted on disk. Defualt server permission level will be assigned to the player after a server restart.
                    </p>
                </td>
            </tr>
                        <tr>
                <td>deop &lt;player&gt;</td>
                <td>
                    <p>
                        Demote a player to <code>member</code>. This will also persist in <code>permissions.json</code> if the player is authenticated to XBL. If <code>permissions.json</code> is missing it will be created.
                    </p>
                </td>
            </tr>
            <tr>
                <td>changesetting &lt;setting&gt; &lt;value&gt;</td>
                <td>Changes a server setting without having to restart the server. Currently only two settings are supported to be changed, <code>allow-cheats</code> (true or false) and <code>difficulty</code> (0, <code>peaceful</code>, 1, <code>easy</code>, 2, <code>normal</code>, 3 or <code>hard</code>). They do not modify the value that's specified in <code>server.properties</code>.</td>
            </tr>
        </tbody>
    </table>




    <h2>Backups</h2>
        The server supports taking backups of the world files while the server is running. It's not particularly friendly for taking manual backups, but works better when automated. The backup (from the servers perspective) consists of three commands.

                <td>save hold</td>
                <td>This will ask the server to prepare for a backup. It’s asynchronous and will return immediately.</td>

                <td>save query</td>
                <td>After calling <code>save hold</code> you should call this command repeatedly to see if the preparation has finished. When it returns a success it will return a file list (with lengths for each file) of the files you need to copy. The server will not pause while this is happening, so some files can be modified while the backup is taking place. As long as you only copy the files in the given file list and truncate the copied files to the specified lengths, then the backup should be valid.</td>

                <td>save resume</td>
                <td>When you’re finished with copying the files you should call this to tell the server that it’s okay to remove old files again.</td>

*/