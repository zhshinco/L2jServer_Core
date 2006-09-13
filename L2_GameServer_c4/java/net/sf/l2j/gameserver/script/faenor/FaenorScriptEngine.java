/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.script.faenor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.script.Parser;
import net.sf.l2j.gameserver.script.ParserNotCreatedException;
import net.sf.l2j.gameserver.script.ScriptDocument;
import net.sf.l2j.gameserver.script.ScriptEngine;
import net.sf.l2j.gameserver.script.ScriptPackage;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.w3c.dom.Node;

/**
 * @author Luis Arias
 *
 */
public class FaenorScriptEngine extends ScriptEngine
{
    static Logger _log = Logger.getLogger(GameServer.class.getName());
    public final static String PACKAGE_DIRECTORY = "data/script/";
    public final static boolean DEBUG = true;

    private static FaenorScriptEngine instance;

    private LinkedList<ScriptDocument> scripts;

    public static FaenorScriptEngine getInstance()
    {
        if (instance == null)
        {
            instance = new FaenorScriptEngine();
        }

        return instance;
    }

    private FaenorScriptEngine()
    {
        scripts = new LinkedList<ScriptDocument>();
        loadPackages();
        parsePackages();

    }

    public void reloadPackages()
    {
        scripts = new LinkedList<ScriptDocument>();
        parsePackages();
    }

    private void loadPackages()
    {
        File packDirectory = new File(Config.DATAPACK_ROOT, PACKAGE_DIRECTORY);//_log.sss(packDirectory.getAbsolutePath());

        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File file)
            {
                return file.getName().endsWith(".zip");
            }
        };

        File[] files = packDirectory.listFiles(fileFilter);
        if (files == null) return;
        ZipFile zipPack;

        for (int i = 0; i < files.length; i++)
        {
            try
            {
                zipPack = new ZipFile(files[i]);
            }
            catch (ZipException e)
            {
                e.printStackTrace();
                continue;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                continue;
            }

            ScriptPackage module = new ScriptPackage(zipPack);

            List<ScriptDocument> scrpts = module.getScriptFiles();
            for (ScriptDocument script : scrpts)
            {
                this.scripts.add(script);
            }

        }
        /*for (ScriptDocument script : scripts)
         {
         _log.sss("Script: "+script);
         }
         _log.sss("Sorting");
         orderScripts();
         for (ScriptDocument script : scripts)
         {
         _log.sss("Script: "+script);
         }*/
    }

    public void orderScripts()
    {
        if (scripts.size() > 1)
        {
            //ScriptDocument npcInfo = null;

            for (int i = 0; i < scripts.size();)
            {
                if (scripts.get(i).getName().contains("NpcStatData"))
                {
                    scripts.addFirst(scripts.remove(i));
                    //scripts.set(i, scripts.get(0));
                    //scripts.set(0, npcInfo);
                }
                else
                {
                    i++;
                }
            }
        }
    }

    public void parsePackages()
    {
        BSFManager context = new BSFManager();
        try
        {
            context.eval("beanshell", "core", 0, 0, "double log1p(double d) { return Math.log1p(d); }");
            context.eval("beanshell", "core", 0, 0,
                         "double pow(double d, double p) { return Math.pow(d,p); }");

            for (ScriptDocument script : scripts)
            {
                parseScript(script, context);
                //System.out.println(script.getName());
            }
        }
        catch (BSFException e)
        {
            e.printStackTrace();
        }
    }

    public void parseScript(ScriptDocument script, BSFManager context)
    {
        if (DEBUG) _log.fine("Parsing Script: " + script.getName());

        Node node = script.getDocument().getFirstChild();
        String parserClass = "faenor.Faenor" + node.getNodeName() + "Parser";

        Parser parser = null;
        try
        {
            parser = createParser(parserClass);
        }
        catch (ParserNotCreatedException e)
        {
            _log.warning("ERROR: No parser registered for Script: " + parserClass);
            e.printStackTrace();
        }

        if (parser == null)
        {
            _log.warning("Unknown Script Type: " + script.getName());
            return;
        }

        try
        {
            parser.parseScript(node, context);
            _log.fine(script.getName() + "Script Sucessfullty Parsed.");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            _log.warning("Script Parsing Failed.");
        }
    }

    public String toString()
    {
        if (scripts.isEmpty()) return "No Packages Loaded.";

        String out = "Script Packages currently loaded:\n";

        for (ScriptDocument script : scripts)
        {
            out += script;
        }
        return out;
    }
}
